package org.taymyr.lagom.metrics

import akka.NotUsed
import com.lightbend.lagom.javadsl.api.ServiceCall
import com.lightbend.lagom.javadsl.api.deser.ExceptionMessage
import com.lightbend.lagom.javadsl.api.transport.BadRequest
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode
import com.lightbend.lagom.javadsl.api.transport.TransportException
import java.util.concurrent.CompletableFuture.completedFuture
import javax.inject.Inject

class TestServiceImpl @Inject
constructor() : TestService {

    override fun methodWithPathParams(firstId: Long, secondId: String): ServiceCall<String, String> {
        return ServiceCall {
            throw BadRequest("")
        }
    }

    override fun methodWithQueryParams(firstId: Long, pageNo: Int, pageSize: Int): ServiceCall<NotUsed, String> {
        return ServiceCall {
            throw TransportException(TransportErrorCode.InternalServerError, ExceptionMessage("", ""))
        }
    }

    override fun simpleMethod(): ServiceCall<NotUsed, String> {
        return ServiceCall { completedFuture("") }
    }
}
