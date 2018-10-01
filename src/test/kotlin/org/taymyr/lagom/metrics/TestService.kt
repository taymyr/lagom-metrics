package org.taymyr.lagom.metrics

import akka.NotUsed
import com.lightbend.lagom.javadsl.api.Descriptor
import com.lightbend.lagom.javadsl.api.Service
import com.lightbend.lagom.javadsl.api.Service.named
import com.lightbend.lagom.javadsl.api.Service.pathCall
import com.lightbend.lagom.javadsl.api.Service.restCall
import com.lightbend.lagom.javadsl.api.ServiceCall
import com.lightbend.lagom.javadsl.api.transport.Method.DELETE
import com.lightbend.lagom.javadsl.api.transport.Method.POST
import kotlin.reflect.jvm.javaMethod

/**
 * @author Sergey Morgunov
 */
interface TestService : Service {

    fun simpleMethod(): ServiceCall<NotUsed, String>
    fun methodWithPathParams(firstId: Long, secondId: String): ServiceCall<String, String>
    fun methodWithQueryParams(firstId: Long, pageNo: Int, pageSize: Int): ServiceCall<NotUsed, String>

    @JvmDefault
    override fun descriptor(): Descriptor {
        return named("test").withCalls(
            pathCall<NotUsed, String>("/foo/bar", TestService::simpleMethod.javaMethod),
            restCall<String, String>(POST, "/foo/:firstId/bar/:secondId", TestService::methodWithPathParams.javaMethod),
            restCall<String, String>(DELETE, "/foo/:firstId/bar?pageNo&pageSize", TestService::methodWithQueryParams.javaMethod)
        ).withAutoAcl(true)
    }
}