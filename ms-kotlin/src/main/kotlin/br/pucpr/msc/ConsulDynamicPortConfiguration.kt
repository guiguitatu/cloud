package br.pucpr.msc

import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties

@Component
class ConsulDynamicPortConfiguration(
    private val consulDiscoveryProperties: ConsulDiscoveryProperties
) : ApplicationListener<WebServerInitializedEvent> {
    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        consulDiscoveryProperties.port = event.webServer.port
    }
}
