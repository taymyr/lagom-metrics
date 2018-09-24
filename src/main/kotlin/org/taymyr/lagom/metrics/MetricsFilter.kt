package org.taymyr.lagom.metrics

import akka.stream.Materializer
import com.codahale.metrics.Meter
import com.codahale.metrics.Timer
import mu.KLogging
import play.api.routing.HandlerDef
import play.mvc.Filter
import play.mvc.Http.RequestHeader
import play.mvc.Result
import play.routing.Router.Attrs.HANDLER_DEF
import java.util.Optional
import java.util.concurrent.CompletionStage
import java.util.function.Function
import javax.inject.Inject

/**
 * Play HTTP [Filter] for metrics instrumentation HTTP requests.
 *
 * Creating [Timer] and [Meter] for all requests in general and
 * particular per route.
 *
 * @author Sergey Morgunov
 */
class MetricsFilter @Inject
constructor(mat: Materializer, private val metrics: Metrics) : Filter(mat) {

    private fun normalize(path: String) = path.replace('?', '.').replace(Regex("[:&]"), "_")

    private fun routeTimerContext(requestHeader: RequestHeader, handlerDef: Optional<HandlerDef>): Timer.Context? =
        if (handlerDef.isPresent) metrics.routeTimer(normalize(handlerDef.get().path()), requestHeader.method()).time()
        else null

    private fun routeMeter(requestHeader: RequestHeader, handlerDef: Optional<HandlerDef>, result: Result): Meter? =
        if (handlerDef.isPresent) metrics.routeMeter(normalize(handlerDef.get().path()), requestHeader.method(), "${result.status()}")
        else null

    override fun apply(
        nextFilter: Function<RequestHeader, CompletionStage<Result>>,
        requestHeader: RequestHeader
    ): CompletionStage<Result> {
        val handlerDef = requestHeader.attrs().getOptional(HANDLER_DEF)
        val allTimer = metrics.routeTimer("all").time()
        val routeTimer = routeTimerContext(requestHeader, handlerDef)
        return nextFilter.apply(requestHeader).thenApply<Result> { result ->
            allTimer.stop()
            routeTimer?.stop()
            metrics.routeMeter("all").mark()
            routeMeter(requestHeader, handlerDef, result)?.mark()
            result
        }
    }

    companion object : KLogging()
}
