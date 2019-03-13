package org.taymyr.lagom.metrics

import akka.stream.Materializer
import com.codahale.metrics.JvmAttributeGaugeSet
import com.codahale.metrics.Meter
import com.codahale.metrics.MetricFilter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import com.codahale.metrics.graphite.Graphite
import com.codahale.metrics.graphite.GraphiteReporter
import com.codahale.metrics.graphite.GraphiteUDP
import com.codahale.metrics.graphite.PickledGraphite
import com.codahale.metrics.jvm.GarbageCollectorMetricSet
import com.codahale.metrics.jvm.MemoryUsageGaugeSet
import com.codahale.metrics.jvm.ThreadStatesGaugeSet
import com.google.inject.ConfigurationException
import com.google.inject.Injector
import com.lightbend.lagom.internal.server.status.MetricsServiceImpl
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.javadsl.server.status.CircuitBreakerStatus
import com.typesafe.config.Config
import com.zaxxer.hikari.HikariDataSource
import io.github.config4k.extract
import mu.KLogging
import org.taymyr.lagom.metrics.GraphiteReporterType.PICKLE
import org.taymyr.lagom.metrics.GraphiteReporterType.TCP
import org.taymyr.lagom.metrics.GraphiteReporterType.UDP
import play.db.DBApi
import play.inject.ApplicationLifecycle
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Integration [Lagom](https://www.lagomframework.com)/[Play](https://playframework.com)
 * with [Dropwizard Metrics](https://metrics.dropwizard.io).
 *
 * @property conf Typesafe configuration
 * @property registry Registry Dropwizard Metrics
 */
@Singleton
class Metrics @Inject
constructor(conf: Config, @Suppress("MemberVisibilityCanBePrivate") val registry: MetricRegistry) {

    private val config = conf.extract<MetricsConfig>("taymyr.lagom.metrics")

    /** Register circuit breakers metrics. */
    @Inject
    private fun registerCircuitBreaker(injector: Injector, mat: Materializer) {
        if (config.enableCircuitBreaker) {
            val metricsService = try { injector.getInstance(MetricsServiceImpl::class.java) } catch (_: Throwable) { null }
            metricsService ?: logger.error { "Only Lagom framework module support metrics for circuit breakers" }
            metricsService?.run {
                circuitBreakers().invoke().thenAccept { source ->
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

    /** Register JVM metrics. */
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
                logger.error { "Library 'metrics-jvm' not found in runtime classpath for `enableJVM = true`" }
            }
        }
    }

    /** Register HikariCP metrics. */
    @Inject
    private fun registerHikari(injector: Injector) {
        if (config.enableHikari) {
            try {
                val dbApi = injector.getInstance(DBApi::class.java)
                dbApi.run {
                    databases.forEach {
                        (it.dataSource as? HikariDataSource)?.run {
                            metricRegistry = registry
                            logger.info { "Metrics for Hikari pool '$poolName' enabled" }
                        }
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is NoClassDefFoundError, is ConfigurationException -> {
                        logger.error { "Libraries 'play-jdbc-api' and 'HikariCP' not found in runtime classpath for `enableHikari = true`" }
                    }
                    else -> logger.error(e) { "Can't enable metrics for Hikari connection pool: $e" }
                }
            }
        }
    }

    private fun initGraphiteReporterForRegistry(graphiteConfig: GraphiteReporterConfig, registry: MetricRegistry, lifecycle: ApplicationLifecycle) {
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
            reporter.start(graphiteConfig.period.toMillis(), MILLISECONDS)
            logger.info { "Graphite reporter for registry $registry started with configuration ${config.graphiteReporter}" }
            lifecycle.addStopHook {
                reporter.close()
                completedFuture<Any>(null)
            }
        } catch (e: NoClassDefFoundError) {
            logger.error { "Library 'metrics-graphite' not found in runtime classpath for specified `graphiteReporter`" }
        }
    }

    /** Initialization Graphite Reporter. */
    @Inject
    private fun initGraphiteReporter(injector: Injector, lifecycle: ApplicationLifecycle) {
        config.graphiteReporter?.let { graphiteConfig ->
            initGraphiteReporterForRegistry(graphiteConfig, registry, lifecycle)
            if (config.enableCassandra) {
                val cassandraSession = try { injector.getInstance(CassandraSession::class.java) } catch (_: Throwable) { null }
                cassandraSession ?: logger.error { "Only Lagom with Persistence Cassandra module support metrics for cassandra" }
                cassandraSession?.underlying()?.thenAccept {
                    val graphiteConfigCassandra = graphiteConfig.copy(prefix = "${graphiteConfig.prefix}.${config.prefix}.cassandra")
                    initGraphiteReporterForRegistry(graphiteConfigCassandra, it.cluster.metrics.registry, lifecycle)
                }
            }
        }
    }

    /**
     * Create [Meter] metric for HTTP route.
     * @param name An array of parts route
     * @return [Meter]
     */
    fun routeMeter(vararg name: String): Meter = registry.meter(name("routes", *name, "meter"))

    /**
     * Create [Timer] metric for HTTP route.
     * @param name An array of parts route
     * @return [Timer]
     */
    fun routeTimer(vararg name: String): Timer = registry.timer(name("routes", *name, "timer"))

    private fun name(vararg name: String): String = MetricRegistry.name(config.prefix, *name)

    /** Companion of logging */
    companion object : KLogging()
}
