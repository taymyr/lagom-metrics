package org.taymyr.lagom.metrics

import akka.NotUsed
import akka.stream.javadsl.Source
import akka.stream.javadsl.Source.from
import akka.stream.javadsl.Source.maybe
import com.codahale.metrics.JvmAttributeGaugeSet
import com.codahale.metrics.MetricFilter.ALL
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.jvm.GarbageCollectorMetricSet
import com.codahale.metrics.jvm.MemoryUsageGaugeSet
import com.codahale.metrics.jvm.ThreadStatesGaugeSet
import com.lightbend.lagom.internal.server.status.MetricsServiceImpl
import com.lightbend.lagom.javadsl.api.ServiceCall
import com.lightbend.lagom.javadsl.api.transport.BadRequest
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode.InternalServerError
import com.lightbend.lagom.javadsl.api.transport.TransportException
import com.lightbend.lagom.javadsl.server.status.CircuitBreakerStatus
import com.lightbend.lagom.javadsl.testkit.ServiceTest.TestServer
import com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup
import com.lightbend.lagom.javadsl.testkit.ServiceTest.startServer
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isA
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.zaxxer.hikari.HikariDataSource
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.WordSpec
import org.taymyr.lagom.metrics.CircuitBreakersMetricSetTest.Companion.closedCircuitBreaker
import org.taymyr.lagom.metrics.CircuitBreakersMetricSetTest.Companion.halfOpenCircuitBreaker
import org.taymyr.lagom.metrics.CircuitBreakersMetricSetTest.Companion.openCircuitBreaker
import play.db.DBApi
import play.db.Database
import play.inject.Bindings.bind
import java.lang.Thread.sleep
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

class MetricsTest : WordSpec({

    val registrySpy = spy(MetricRegistry())
    val dbApiMock = mock<DBApi> {
        val hikari = mock<Database> {
            on { dataSource }.thenReturn(HikariDataSource())
        }
        val other = mock<Database>()
        on { databases }.thenReturn(listOf(hikari, other))
    }
    val metricsServiceImpl = mock<MetricsServiceImpl> { _ ->
        val cbStatusSource = from(listOf(listOf(openCircuitBreaker, closedCircuitBreaker, halfOpenCircuitBreaker))).concat(maybe())
        val cbStatusCall = mock<ServiceCall<NotUsed, Source<List<CircuitBreakerStatus>, *>>> {
            on { invoke() }.thenReturn(CompletableFuture.completedFuture(cbStatusSource))
        }
        on { circuitBreakers() }.thenReturn(cbStatusCall)
    }
    val server: TestServer = startServer(defaultSetup().withCluster(false).configureBuilder { b -> b
        .overrides(
            bind(MetricRegistry::class.java).toInstance(registrySpy),
            bind(MetricsServiceImpl::class.java).toInstance(metricsServiceImpl),
            bind(DBApi::class.java).toInstance(dbApiMock)
        )
    })

    "Initialization Metrics" should {

        "initialize JVM metrics" {
            verify(registrySpy).register(eq("prefix.jvm.attr"), isA<JvmAttributeGaugeSet>())
            verify(registrySpy).register(eq("prefix.jvm.gc"), isA<GarbageCollectorMetricSet>())
            verify(registrySpy).register(eq("prefix.jvm.memory"), isA<MemoryUsageGaugeSet>())
            verify(registrySpy).register(eq("prefix.jvm.threads"), isA<ThreadStatesGaugeSet>())
        }

        "initialize CircuitBreakers metrics" {
            verify(registrySpy).register(eq("prefix.cb.open"), isA<CircuitBreakersMetricSet>())
            verify(registrySpy).register(eq("prefix.cb.closed"), isA<CircuitBreakersMetricSet>())
            verify(registrySpy).register(eq("prefix.cb.half-open"), isA<CircuitBreakersMetricSet>())
        }

        "initialize Graphite reporter" {
            clearInvocations(registrySpy)
            sleep(10000) // Wait more then reporter period
            verify(registrySpy, atLeast(1)).getGauges(ALL)
            verify(registrySpy, atLeast(1)).getCounters(ALL)
            verify(registrySpy, atLeast(1)).getHistograms(ALL)
            verify(registrySpy, atLeast(1)).getMeters(ALL)
            verify(registrySpy, atLeast(1)).getTimers(ALL)
        }
    }

    "Metrics filter" should {
        "correct register timer and meter for successful request" {
            clearInvocations(registrySpy)
            val testService = server.client(TestService::class.java)
            testService.simpleMethod().invoke().toCompletableFuture().get()
            verify(registrySpy).timer("prefix.routes.all.timer")
            verify(registrySpy).timer("prefix.routes.root.foo.bar.GET.timer")
            verify(registrySpy).meter("prefix.routes.root.foo.bar.GET.200.meter")
        }

        "correct register timer and meter for bad request" {
            clearInvocations(registrySpy)
            val testService = server.client(TestService::class.java)
            val badRequest = shouldThrow<ExecutionException> {
                testService.methodWithPathParams(0, "0").invoke("").toCompletableFuture().get()
            }
            badRequest.cause shouldBe beInstanceOf(BadRequest::class)
            verify(registrySpy).timer("prefix.routes.all.timer")
            verify(registrySpy).timer("prefix.routes.root.foo._firstId.bar._secondId.POST.timer")
            verify(registrySpy).meter("prefix.routes.root.foo._firstId.bar._secondId.POST.400.meter")
        }

        "correct register timer and meter for unsuccessful request" {
            clearInvocations(registrySpy)
            val testService = server.client(TestService::class.java)
            val internalError = shouldThrow<ExecutionException> {
                testService.methodWithQueryParams(0, 0, 0).invoke().toCompletableFuture().get()
            }
            internalError.cause shouldBe beInstanceOf(TransportException::class)
            (internalError.cause as TransportException).errorCode() shouldBe InternalServerError
            verify(registrySpy).timer("prefix.routes.all.timer")
            verify(registrySpy).timer("prefix.routes.root.foo._firstId.bar.pageNo_pageSize.DELETE.timer")
            verify(registrySpy).meter("prefix.routes.root.foo._firstId.bar.pageNo_pageSize.DELETE.500.meter")
        }
    }
})
