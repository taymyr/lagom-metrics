package org.taymyr.lagom.metrics

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.kotlintest.matchers.string.shouldBeEmpty
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.WordSpec
import org.taymyr.lagom.metrics.GraphiteReporterType.PICKLE
import org.taymyr.lagom.metrics.GraphiteReporterType.TCP
import java.time.Duration
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

class ConfigTest : WordSpec({

    "MetricsConfig" should {

        "be correct for reference.conf" {
            val config = ConfigFactory.defaultReference().extract<MetricsConfig>("taymyr.lagom.metrics")
            config.enableJVM shouldBe false
            config.enableCircuitBreaker shouldBe false
            config.enableHikari shouldBe false
            config.prefix.shouldBeEmpty()
            config.graphiteReporter shouldBe null
        }

        "be able to enable JVM metrics" {
            val config = ConfigFactory.load("default.conf").extract<MetricsConfig>("taymyr.lagom.metrics")
            config.enableJVM shouldBe true
        }

        "be able to enable circuit breakers metrics" {
            val config = ConfigFactory.load("default.conf").extract<MetricsConfig>("taymyr.lagom.metrics")
            config.enableCircuitBreaker shouldBe true
        }

        "be able to enable HikariCP metrics" {
            val config = ConfigFactory.load("default.conf").extract<MetricsConfig>("taymyr.lagom.metrics")
            config.enableHikari shouldBe true
        }

        "be throw exception for incorrect settings graphite reporter" {
            shouldThrow<Exception> {
                ConfigFactory.load("bad_graphite.conf").extract<MetricsConfig>("taymyr.lagom.metrics")
            }
        }

        "be correct for graphite reporter with minimal settings" {
            val config = ConfigFactory.load("min_graphite.conf").extract<MetricsConfig>("taymyr.lagom.metrics")
            config.graphiteReporter shouldNotBe null
            config.graphiteReporter?.let {
                it.type shouldBe PICKLE
                it.host shouldBe "localhost"
                it.port shouldBe 1000
                it.batchSize shouldBe null
                it.durationUnit shouldBe MILLISECONDS
                it.rateUnit shouldBe SECONDS
                it.period shouldBe Duration.ofSeconds(10)
            }
        }

        "be correct for graphite reporter with full settings" {
            val config = ConfigFactory.load("full_graphite.conf").extract<MetricsConfig>("taymyr.lagom.metrics")
            config.graphiteReporter shouldNotBe null
            config.graphiteReporter?.run {
                prefix shouldBe "prefix"
                type shouldBe TCP
                host shouldBe "localhost"
                port shouldBe 1000
                batchSize shouldBe 1000
                durationUnit shouldBe SECONDS
                rateUnit shouldBe MILLISECONDS
                period shouldBe Duration.ofMillis(500)
            }
        }
    }
})
