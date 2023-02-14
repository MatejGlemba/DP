package io.dpm.dropmenote.ws.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import io.dpm.dropmenote.ws.websocket.handler.WebSocketChatHandler;
import io.dpm.dropmenote.ws.websocket.handler.WebSocketNotificationHandler;


/**
 * @author phu (Starbug s.r.o. | https://www.starbug.eu)
 */
@Configuration
@EnableWebSocket
@ComponentScan({ "io.dpm.dropmenote.ws.websocket" })
public class WebSocketConf implements WebSocketConfigurer {

    private static Logger LOG = LoggerFactory.getLogger(WebSocketConf.class);

    {
        LOG.debug("WebSocketConfig initialisation");
    }

    @Autowired
    private WebSocketChatHandler webSocketChatHandler;

    @Autowired
    private WebSocketNotificationHandler webSocketNotificationHandler;

    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    	
    	/**
    	 * Chat Websocket url je: ws://localhost:8080/dropmenote-ws/websocket/chat
    	 */
        WebSocketHandlerRegistration chatHandler = registry.addHandler(webSocketChatHandler, "/websocket/chat");
        chatHandler.setHandshakeHandler(new DefaultHandshakeHandler(new TomcatRequestUpgradeStrategy()));
        LOG.debug("chatHandler {}", chatHandler);
        /**
         * Notification Websocket url je: ws://localhost:8080/dropmenote-ws/websocket/notification
         */
        WebSocketHandlerRegistration notificationHandler = registry.addHandler(webSocketNotificationHandler, "/websocket/notification");
        notificationHandler.setHandshakeHandler(new DefaultHandshakeHandler(new TomcatRequestUpgradeStrategy()));
        
        
        
        /**
         * SECURITY
         */
        // TODO: nastavit v produkcii asi z konstant
        chatHandler.setAllowedOrigins("*");
        notificationHandler.setAllowedOrigins("*");
        
    }
    
    
}