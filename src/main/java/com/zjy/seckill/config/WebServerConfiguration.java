package com.zjy.seckill.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

//当Spring容器内没有TomcatEmbeddedServletContainerFactory这个Bean时，会把此Bean加载进来
@Component
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        //使用对应工厂类提供的接口，定制化tomcat connector
        TomcatServletWebServerFactory serverFactory = (TomcatServletWebServerFactory) factory;
        serverFactory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                Http11NioProtocol protocolHandler = (Http11NioProtocol) connector.getProtocolHandler();

                //定制化keepAliveTimeOut，设置30s内没有请求，服务端自动断开keep alive链接
                protocolHandler.setKeepAliveTimeout(30000);
                //当客户端发送超过10000个请求，则自动断开keep alive链接
                protocolHandler.setMaxKeepAliveRequests(10000);
            }
        });
    }
}
