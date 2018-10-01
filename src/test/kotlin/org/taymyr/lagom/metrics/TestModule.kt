package org.taymyr.lagom.metrics

import com.google.inject.AbstractModule
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport

/**
 * @author Sergey Morgunov
 */
class TestModule : AbstractModule(), ServiceGuiceSupport {

    override fun configure() {
        bindService(TestService::class.java, TestServiceImpl::class.java)
    }
}