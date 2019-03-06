package org.taymyr.lagom.metrics

import com.codahale.metrics.Gauge
import com.lightbend.lagom.javadsl.server.status.CircuitBreakerStatus
import com.lightbend.lagom.javadsl.server.status.Latency
import io.kotlintest.inspectors.forAll
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.util.concurrent.ConcurrentHashMap

class CircuitBreakersMetricSetTest : StringSpec({

    "Metrics for open circuit breaker should be correct" {
        val cbMetricSet = CircuitBreakersMetricSet(openCircuitBreaker.id, statusCircuitBreakers)
        cbMetricSet.metrics.values.forAll { it shouldBe beInstanceOf(Gauge::class) }
        (cbMetricSet.metrics["state"] as Gauge<*>).value shouldBe 1
        (cbMetricSet.metrics["totalSuccessCount"] as Gauge<*>).value shouldBe openCircuitBreaker.totalSuccessCount
        (cbMetricSet.metrics["totalFailureCount"] as Gauge<*>).value shouldBe openCircuitBreaker.totalFailureCount
        (cbMetricSet.metrics["throughputOneMinute"] as Gauge<*>).value shouldBe openCircuitBreaker.throughputOneMinute
        (cbMetricSet.metrics["failedThroughputOneMinute"] as Gauge<*>).value shouldBe openCircuitBreaker.failedThroughputOneMinute
        (cbMetricSet.metrics["latency.mean"] as Gauge<*>).value shouldBe openCircuitBreaker.latencyMicros.mean
        (cbMetricSet.metrics["latency.median"] as Gauge<*>).value shouldBe openCircuitBreaker.latencyMicros.median
        (cbMetricSet.metrics["latency.p98"] as Gauge<*>).value shouldBe openCircuitBreaker.latencyMicros.percentile98th
        (cbMetricSet.metrics["latency.p99"] as Gauge<*>).value shouldBe openCircuitBreaker.latencyMicros.percentile99th
        (cbMetricSet.metrics["latency.p999"] as Gauge<*>).value shouldBe openCircuitBreaker.latencyMicros.percentile999th
        (cbMetricSet.metrics["latency.min"] as Gauge<*>).value shouldBe openCircuitBreaker.latencyMicros.min
        (cbMetricSet.metrics["latency.max"] as Gauge<*>).value shouldBe openCircuitBreaker.latencyMicros.max
    }

    "Metrics for half-open circuit breaker should be correct" {
        val cbMetricSet = CircuitBreakersMetricSet(halfOpenCircuitBreaker.id, statusCircuitBreakers)
        cbMetricSet.metrics.values.forAll { it shouldBe beInstanceOf(Gauge::class) }
        (cbMetricSet.metrics["state"] as Gauge<*>).value shouldBe 2
        (cbMetricSet.metrics["totalSuccessCount"] as Gauge<*>).value shouldBe halfOpenCircuitBreaker.totalSuccessCount
        (cbMetricSet.metrics["totalFailureCount"] as Gauge<*>).value shouldBe halfOpenCircuitBreaker.totalFailureCount
        (cbMetricSet.metrics["throughputOneMinute"] as Gauge<*>).value shouldBe halfOpenCircuitBreaker.throughputOneMinute
        (cbMetricSet.metrics["failedThroughputOneMinute"] as Gauge<*>).value shouldBe halfOpenCircuitBreaker.failedThroughputOneMinute
        (cbMetricSet.metrics["latency.mean"] as Gauge<*>).value shouldBe halfOpenCircuitBreaker.latencyMicros.mean
        (cbMetricSet.metrics["latency.median"] as Gauge<*>).value shouldBe halfOpenCircuitBreaker.latencyMicros.median
        (cbMetricSet.metrics["latency.p98"] as Gauge<*>).value shouldBe halfOpenCircuitBreaker.latencyMicros.percentile98th
        (cbMetricSet.metrics["latency.p99"] as Gauge<*>).value shouldBe halfOpenCircuitBreaker.latencyMicros.percentile99th
        (cbMetricSet.metrics["latency.p999"] as Gauge<*>).value shouldBe halfOpenCircuitBreaker.latencyMicros.percentile999th
        (cbMetricSet.metrics["latency.min"] as Gauge<*>).value shouldBe halfOpenCircuitBreaker.latencyMicros.min
        (cbMetricSet.metrics["latency.max"] as Gauge<*>).value shouldBe halfOpenCircuitBreaker.latencyMicros.max
    }

    "Metrics for closed circuit breaker should be correct" {
        val cbMetricSet = CircuitBreakersMetricSet(closedCircuitBreaker.id, statusCircuitBreakers)
        cbMetricSet.metrics.values.forAll { it shouldBe beInstanceOf(Gauge::class) }
        (cbMetricSet.metrics["state"] as Gauge<*>).value shouldBe 3
        (cbMetricSet.metrics["totalSuccessCount"] as Gauge<*>).value shouldBe closedCircuitBreaker.totalSuccessCount
        (cbMetricSet.metrics["totalFailureCount"] as Gauge<*>).value shouldBe closedCircuitBreaker.totalFailureCount
        (cbMetricSet.metrics["throughputOneMinute"] as Gauge<*>).value shouldBe closedCircuitBreaker.throughputOneMinute
        (cbMetricSet.metrics["failedThroughputOneMinute"] as Gauge<*>).value shouldBe closedCircuitBreaker.failedThroughputOneMinute
        (cbMetricSet.metrics["latency.mean"] as Gauge<*>).value shouldBe closedCircuitBreaker.latencyMicros.mean
        (cbMetricSet.metrics["latency.median"] as Gauge<*>).value shouldBe closedCircuitBreaker.latencyMicros.median
        (cbMetricSet.metrics["latency.p98"] as Gauge<*>).value shouldBe closedCircuitBreaker.latencyMicros.percentile98th
        (cbMetricSet.metrics["latency.p99"] as Gauge<*>).value shouldBe closedCircuitBreaker.latencyMicros.percentile99th
        (cbMetricSet.metrics["latency.p999"] as Gauge<*>).value shouldBe closedCircuitBreaker.latencyMicros.percentile999th
        (cbMetricSet.metrics["latency.min"] as Gauge<*>).value shouldBe closedCircuitBreaker.latencyMicros.min
        (cbMetricSet.metrics["latency.max"] as Gauge<*>).value shouldBe closedCircuitBreaker.latencyMicros.max
    }

    "Metrics for unknown circuit breaker should be correct" {
        val cbMetricSet = CircuitBreakersMetricSet(unknownStateCircuitBreaker.id, statusCircuitBreakers)
        cbMetricSet.metrics.values.forAll { it shouldBe beInstanceOf(Gauge::class) }
        (cbMetricSet.metrics["state"] as Gauge<*>).value shouldBe null
        (cbMetricSet.metrics["totalSuccessCount"] as Gauge<*>).value shouldBe unknownStateCircuitBreaker.totalSuccessCount
        (cbMetricSet.metrics["totalFailureCount"] as Gauge<*>).value shouldBe unknownStateCircuitBreaker.totalFailureCount
        (cbMetricSet.metrics["throughputOneMinute"] as Gauge<*>).value shouldBe unknownStateCircuitBreaker.throughputOneMinute
        (cbMetricSet.metrics["failedThroughputOneMinute"] as Gauge<*>).value shouldBe unknownStateCircuitBreaker.failedThroughputOneMinute
        (cbMetricSet.metrics["latency.mean"] as Gauge<*>).value shouldBe unknownStateCircuitBreaker.latencyMicros.mean
        (cbMetricSet.metrics["latency.median"] as Gauge<*>).value shouldBe unknownStateCircuitBreaker.latencyMicros.median
        (cbMetricSet.metrics["latency.p98"] as Gauge<*>).value shouldBe unknownStateCircuitBreaker.latencyMicros.percentile98th
        (cbMetricSet.metrics["latency.p99"] as Gauge<*>).value shouldBe unknownStateCircuitBreaker.latencyMicros.percentile99th
        (cbMetricSet.metrics["latency.p999"] as Gauge<*>).value shouldBe unknownStateCircuitBreaker.latencyMicros.percentile999th
        (cbMetricSet.metrics["latency.min"] as Gauge<*>).value shouldBe unknownStateCircuitBreaker.latencyMicros.min
        (cbMetricSet.metrics["latency.max"] as Gauge<*>).value shouldBe unknownStateCircuitBreaker.latencyMicros.max
    }

    "Metrics for not found circuit breaker should be null" {
        val cbMetricSet = CircuitBreakersMetricSet("not_found", statusCircuitBreakers)
        cbMetricSet.metrics.values.forAll { it shouldBe beInstanceOf(Gauge::class) }
        cbMetricSet.metrics.values.forAll { (it as Gauge<*>).value shouldBe null }
    }
}) {
    companion object {
        private val shareDataCircuitBreaker: CircuitBreakerStatus.Builder = CircuitBreakerStatus.builder()
            .totalSuccessCount(2)
            .totalFailureCount(3)
            .throughputOneMinute(0.2)
            .failedThroughputOneMinute(0.3)
            .latencyMicros(Latency.builder()
                .mean(111.0)
                .median(222.0)
                .percentile98th(333.0)
                .percentile99th(444.0)
                .percentile999th(555.0)
                .min(666)
                .max(777)
                .build()
            )

        val openCircuitBreaker: CircuitBreakerStatus = shareDataCircuitBreaker.id("open").state("open").build()

        val closedCircuitBreaker: CircuitBreakerStatus = shareDataCircuitBreaker.id("closed").state("closed").build()

        val halfOpenCircuitBreaker: CircuitBreakerStatus = shareDataCircuitBreaker.id("half-open").state("half-open").build()

        val unknownStateCircuitBreaker: CircuitBreakerStatus = shareDataCircuitBreaker.id("unknown").state("unknown").build()

        val statusCircuitBreakers: ConcurrentHashMap<String, CircuitBreakerStatus> = ConcurrentHashMap()

        init {
            statusCircuitBreakers[openCircuitBreaker.id] = openCircuitBreaker
            statusCircuitBreakers[closedCircuitBreaker.id] = closedCircuitBreaker
            statusCircuitBreakers[halfOpenCircuitBreaker.id] = halfOpenCircuitBreaker
            statusCircuitBreakers[unknownStateCircuitBreaker.id] = unknownStateCircuitBreaker
        }
    }
}
