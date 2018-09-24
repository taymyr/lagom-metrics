package org.taymyr.lagom.metrics

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

enum class GraphiteReporterType {
    TCP, UDP, PICKLE
}

data class GraphiteReporterConfig(
    val type: GraphiteReporterType = GraphiteReporterType.PICKLE,
    val period: Long = 10,
    val periodUnit: TimeUnit = SECONDS,
    val host: String,
    val port: Int,
    val batchSize: Int?,
    val rateUnit: TimeUnit = SECONDS,
    val durationUnit: TimeUnit = MILLISECONDS
)

data class MetricsConfig(
    val prefix: String,
    val enableCircuitBreaker: Boolean,
    val enableJVM: Boolean,
    val graphiteReporter: GraphiteReporterConfig?
)
