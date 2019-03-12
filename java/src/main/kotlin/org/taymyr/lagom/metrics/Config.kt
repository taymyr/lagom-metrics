package org.taymyr.lagom.metrics

import org.taymyr.lagom.metrics.GraphiteReporterType.PICKLE
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Enums of protocols for Graphite Reporter.
 */
enum class GraphiteReporterType {
    /**
     * TCP protocol for Graphite Reporter.
     * See [com.codahale.metrics.graphite.Graphite]
     */
    TCP,

    /**
     * UDP protocol for Graphite Reporter.
     * See [com.codahale.metrics.graphite.GraphiteUDP]
     */
    UDP,

    /**
     * PICKLE protocol for Graphite Reporter.
     * See [com.codahale.metrics.graphite.PickledGraphite]
     */
    PICKLE
}

/**
 * Configuration of Graphite Reporter.
 * See [com.codahale.metrics.graphite.GraphiteReporter]
 */
data class GraphiteReporterConfig(

    /** Prefix for all metrics, that will be sent to graphite. */
    val prefix: String? = null,

    /** The protocol for connection to graphite application. */
    val type: GraphiteReporterType = PICKLE,

    /** Period of sending metrics to graphite. */
    val period: Duration = Duration.ofSeconds(10),

    /** The host of graphite application. */
    val host: String,

    /** The port of graphite application. */
    val port: Int,

    /** Size of the batch for PICKLE protocol. */
    val batchSize: Int?,

    /** The time unit of in which rates will be converted. */
    val rateUnit: TimeUnit = SECONDS,

    /** The time unit of in which durations will be converted. */
    val durationUnit: TimeUnit = MILLISECONDS
)

/**
 * Configuration of Lagom-Metrics library.
 */
data class MetricsConfig(

    /** Default prefix for all (exclude Hikari) registered metrics. */
    val prefix: String,

    /** Enable circuit breakers metrics. */
    val enableCircuitBreaker: Boolean,

    /** Enable JVM metrics. */
    val enableJVM: Boolean,

    /** Enable HikariCP metrics. */
    val enableHikari: Boolean,

    /** Enable Datastax Driver for Cassandra metrics. */
    val enableCassandra: Boolean,

    /** Settings for reporting metrics to Graphite. */
    val graphiteReporter: GraphiteReporterConfig?
)
