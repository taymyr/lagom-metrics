package org.taymyr.lagom.metrics

import akka.stream.Materializer
import com.codahale.metrics.Meter
import com.codahale.metrics.MetricFilter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import com.codahale.metrics.graphite.Graphite
import com.codahale.metrics.graphite.GraphiteReporter
import com.codahale.metrics.graphite.GraphiteUDP
import com.codahale.metrics.graphite.PickledGraphite
import com.codahale.metrics.jvm.GarbageCollectorMetricSet
import com.codahale.metrics.jvm.JvmAttributeGaugeSet
import com.codahale.metrics.jvm.MemoryUsageGaugeSet
import com.codahale.metrics.jvm.ThreadStatesGaugeSet
import com.google.inject.Injector
import com.lightbend.lagom.internal.server.status.MetricsServiceImpl
import com.lightbend.lagom.javadsl.server.status.CircuitBreakerStatus
import com.typesafe.config.Config
import io.github.config4k.extract
import mu.KLogging
import org.taymyr.lagom.metrics.GraphiteReporterType.PICKLE
import org.taymyr.lagom.metrics.GraphiteReporterType.TCP
import org.taymyr.lagom.metrics.GraphiteReporterType.UDP
import play.inject.ApplicationLifecycle
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Integration [Lagom](https://www.lagomframework.com)/[Play](https://playframework.com)
 * with [Dropwizard Metrics](https://metrics.dropwizard.io).
 *
 */
@Singleton
class Metrics @Inject
constructor(conf: Config, val registry: MetricRegistry) {

    private val config = conf.extract<MetricsConfig>("taymyr.lagom.metrics")

    @Inject
    private fun registerCircuitBreaker(injector: Injector, mat: Materializer) {
        if (config.enableCircuitBreaker) {
            val metricsService = try { injector.getInstance(MetricsServiceImpl::class.java) } catch (_: Throwable) { null }
            metricsService ?: logger.error { "Only Lagom framework module support metrics for circuit breakers" }
            metricsService?.let {
                it.circuitBreakers().invoke().thenAccept { source ->
                    logger.info { "Metrics for circuit breakers enabled" }
                    val statusCircuitBreakers: ConcurrentHashMap<String, CircuitBreakerStatus> = ConcurrentHashMap()
                    source.runForeach({ statuses ->
                        statuses.forEach { status ->
                            statusCircuitBreakers.compute(status.id) { id, prev ->
                                prev ?: registry.register(name("cb", id), CircuitBreakersMetricSet(id, statusCircuitBreakers))
                                status
                            }
                        }
                    }, mat)
                }
            }
        }
    }

    @Inject
    private fun registerJVM() {
        if (config.enableJVM) {
            try {
                registry.register(name("jvm.attr"), JvmAttributeGaugeSet())
                registry.register(name("jvm.gc"), GarbageCollectorMetricSet())
                registry.register(name("jvm.memory"), MemoryUsageGaugeSet())
                registry.register(name("jvm.threads"), ThreadStatesGaugeSet())
                logger.info { "Metrics for JVM enabled" }
            } catch (e: NoClassDefFoundError) {
                logger.error { "Library 'metrics-jvm' not found in runtime classpath for `lagom.metrics.enableJVM = true`" }
            }
        }
    }

    @Inject
    private fun initGraphiteReporter(lifecycle: ApplicationLifecycle) {
        config.graphiteReporter?.let { graphiteConfig ->
            try {
                val graphite = when (graphiteConfig.type) {
                    UDP -> GraphiteUDP(graphiteConfig.host, graphiteConfig.port)
                    TCP -> Graphite(graphiteConfig.host, graphiteConfig.port)
                    PICKLE -> PickledGraphite(graphiteConfig.host, graphiteConfig.port, graphiteConfig.batchSize ?: 100)
                }
                val reporter = GraphiteReporter.forRegistry(registry)
                    .prefixedWith(graphiteConfig.prefix)
                    .convertRatesTo(graphiteConfig.rateUnit)
                    .convertDurationsTo(graphiteConfig.durationUnit)
                    .filter(MetricFilter.ALL)
                    .build(graphite)
                reporter.start(graphiteConfig.period, graphiteConfig.periodUnit)
                lifecycle.addStopHook {
                    reporter.close()
                    completedFuture<Any>(null)
                }
                logger.info { "Graphite reporter started with configuration ${config.graphiteReporter}" }
            } catch (e: NoClassDefFoundError) {
                logger.error { "Library 'metrics-graphite' not found in runtime classpath for specified `lagom.metrics.graphiteReporter`" }
            }
        }
    }

    fun routeMeter(vararg name: String): Meter = registry.meter(name("routes", *name, "meter"))
    fun routeTimer(vararg name: String): Timer = registry.timer(name("routes", *name, "timer"))

    private fun name(vararg name: String): String = MetricRegistry.name(config.prefix, *name)

    companion object : KLogging()
}
