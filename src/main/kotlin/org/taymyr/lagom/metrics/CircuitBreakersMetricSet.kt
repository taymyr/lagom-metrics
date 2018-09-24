package org.taymyr.lagom.metrics

import com.codahale.metrics.Gauge
import com.codahale.metrics.Metric
import com.codahale.metrics.MetricSet
import com.lightbend.lagom.javadsl.server.status.CircuitBreakerStatus
import java.util.concurrent.ConcurrentHashMap

/**
 * [MetricSet] for Lagom circuit breakers [CircuitBreakerStatus]
 *
 * @author Sergey Morgunov
 */
class CircuitBreakersMetricSet(
    private val id: String,
    private val statusCircuitBreakers: ConcurrentHashMap<String, CircuitBreakerStatus>
) : MetricSet {

    override fun getMetrics(): MutableMap<String, Metric> = mapOf<String, Metric>(
        "state" to Gauge<Int> {
            when (statusCircuitBreakers[id]?.state) {
                "open" -> 1
                "half-open" -> 2
                "closed" -> 3
                else -> null
            }
        },
        "totalSuccessCount" to Gauge<Long> { statusCircuitBreakers[id]?.totalSuccessCount },
        "totalFailureCount" to Gauge<Long> { statusCircuitBreakers[id]?.totalFailureCount },
        "throughputOneMinute" to Gauge<Double> { statusCircuitBreakers[id]?.throughputOneMinute },
        "failedThroughputOneMinute" to Gauge<Double> { statusCircuitBreakers[id]?.failedThroughputOneMinute },
        "latency.mean" to Gauge<Double> { statusCircuitBreakers[id]?.latencyMicros?.mean },
        "latency.median" to Gauge<Double> { statusCircuitBreakers[id]?.latencyMicros?.median },
        "latency.p98" to Gauge<Double> { statusCircuitBreakers[id]?.latencyMicros?.percentile98th },
        "latency.p99" to Gauge<Double> { statusCircuitBreakers[id]?.latencyMicros?.percentile99th },
        "latency.p999" to Gauge<Double> { statusCircuitBreakers[id]?.latencyMicros?.percentile999th },
        "latency.min" to Gauge<Long> { statusCircuitBreakers[id]?.latencyMicros?.min },
        "latency.max" to Gauge<Long> { statusCircuitBreakers[id]?.latencyMicros?.max }
    ).toMutableMap()
}
