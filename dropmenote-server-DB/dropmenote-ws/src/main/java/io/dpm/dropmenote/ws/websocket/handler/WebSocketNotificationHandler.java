package io.dpm.dropmenote.ws.websocket.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import io.dpm.dropmenote.ws.utils.WebSocketUtil;
import io.dpm.dropmenote.ws.websocket.websocketObject.utils.RequestMappingUtil;
import io.dpm.dropmenote.ws.websocket.websocketObject.websocketRequest.LoginRequest;
import io.dpm.dropmenote.ws.websocket.websocketObject.websocketRequest.WebsocketRequestTypeEnum;
import io.dpm.dropmenote.ws.websocket.websocketObject.websocketResponse.ErrorResponse;
import io.dpm.dropmenote.ws.websocket.websocketObject.websocketResponse.SuccessResponse;

/**
 * @author phu (Starbug s.r.o. | https://www.starbug.eu)
 */
@Component
public class WebSocketNotificationHandler extends TextWebSocketHandler {
    private static Logger LOG = LoggerFactory.getLogger(WebSocketNotificationHandler.class);

    {
        LOG.info("WebSocketNotificationHandler initialisation.");
    }

    @Autowired
    private RequestMappingUtil requestMappingUtil;

    @Autowired
    private WebSocketNotificationHandlerService webSocketNotificationHandlerService;

    @Override
    public void handleTextMessage(WebSocketSession session, final TextMessage message) throws Exception {
    	LOG.info("handle text message WS {}, message {}", session, message);
    	// Prihlasenie
        try {
        	// {"type":"LOGIN", "token":"<token>"}
        	// alebo
        	// TOTO asi nepodporujeme
        	// {"type":"LOGIN", "fingerprint":"<fingerprint>"}
        	LoginRequest request = requestMappingUtil._mapObjectFromString(LoginRequest.class, message.getPayload());
            
        	if(
        		WebsocketRequestTypeEnum.LOGIN.equals(request.getType())
        		&&
    			(
					(request.getToken() != null && !request.getToken().isBlank())
					|| 
					(request.getFingerprint() != null && !request.getFingerprint().isBlank())
				)
    		) {
        		
        		
        		try {
        			if((request.getToken() != null && !request.getToken().isBlank())) {
        				webSocketNotificationHandlerService.handleUserLogin(session, request);
        			}else{
//        				webSocketNotificationHandlerService.handleAnonymousLogin(session, request);
        			}
        		}catch(Exception e) {
        			try {
                        session.sendMessage(WebSocketUtil.createWebSocketTextMessage(new ErrorResponse(e.getMessage())));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
            		return;
        		}
        		try {
                    session.sendMessage(WebSocketUtil.createWebSocketTextMessage(new SuccessResponse("Login OK")));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
        		return;
        	}
        } catch (IOException e) {
        	// Neprisiel nam login request ale nieco ine
        }

        // Neznama sprava
        try {
            session.sendMessage(WebSocketUtil.createWebSocketTextMessage(new ErrorResponse("This is not a correct message!")));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        LOG.info("CONNECTED: " + session.toString());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        LOG.info("DISCONNECTED: " + session.toString() + " " + status.toString());
    }
}
