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
import io.dpm.dropmenote.ws.websocket.session.ChatSessionInfo;
import io.dpm.dropmenote.ws.websocket.websocketObject.utils.RequestMappingUtil;
import io.dpm.dropmenote.ws.websocket.websocketObject.websocketRequest.LoginRequest;
import io.dpm.dropmenote.ws.websocket.websocketObject.websocketRequest.WebsocketRequestTypeEnum;
import io.dpm.dropmenote.ws.websocket.websocketObject.websocketRequest.WebsocketTextMessageRequest;
import io.dpm.dropmenote.ws.websocket.websocketObject.websocketResponse.ErrorResponse;
import io.dpm.dropmenote.ws.websocket.websocketObject.websocketResponse.SuccessResponse;

/**
 * @author phu (Starbug s.r.o. | https://www.starbug.eu)
 */
@Component
public class WebSocketChatHandler extends TextWebSocketHandler {
    private static Logger LOG = LoggerFactory.getLogger(WebSocketChatHandler.class);

    {
        LOG.debug("WebSocketHandler initialisation.");
    }

    @Autowired
    private RequestMappingUtil requestMappingUtil;

    @Autowired
    private WebSocketChatHandlerService webSocketChatHandlerService;
    
    @Autowired
	private ChatSessionInfo chatSessionInfo;

    @Override
    public void handleTextMessage(WebSocketSession session, final TextMessage message) throws Exception {
		LOG.debug("handle text message WS {}, message {}", session, message);
    	// Prihlasenie
        try {
        	// {"type":"LOGIN", "token":"<token>", "qr": "<QR code UUID>", "room":<roomId>}
        	// alebo
        	// TOTO asi nepodporujeme
        	// {"type":"LOGIN", "fingerprint":"<fingerprint>", "qr": "<QR code UUID>"}
        	LoginRequest request = requestMappingUtil._mapObjectFromString(LoginRequest.class, message.getPayload());
            
        	if(
        		WebsocketRequestTypeEnum.LOGIN.equals(request.getType())
        		&&
    			(
					(request.getToken() != null && !request.getToken().isBlank())
					|| 
					(request.getFingerprint() != null && !request.getFingerprint().isBlank())
				)
    			&&
    			(
    				request.getQrCodeUuid() != null && !request.getQrCodeUuid().isBlank()
    			)
    		) {
        		ChatSessionInfo.Info sessionInfo = chatSessionInfo.get(session.getId());
            	if(sessionInfo != null) {
            		try {
                        session.sendMessage(WebSocketUtil.createWebSocketTextMessage(new ErrorResponse("Already logged in")));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
            		return;
            	}
        		
        		try {
        			if((request.getToken() != null && !request.getToken().isBlank())) {
        				webSocketChatHandlerService.handleUserLogin(session, request);
        			}else{
        				webSocketChatHandlerService.handleAnonymousLogin(session, request);
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
        
        // Text messagge
        try {
        	// TODO: tu nema co byt token, mal sa pri logine ulozit do session a potom tahat odtial
        	// {"type": "TEXT_MESSAGE", "message": "text spravy", "token": "token alebo fingerprint" }
        	WebsocketTextMessageRequest request = requestMappingUtil._mapObjectFromString(WebsocketTextMessageRequest.class, message.getPayload());
            
    	if(										
            		WebsocketRequestTypeEnum.TEXT_MESSAGE.equals(request.getType())
        			&&
        			(request.getMessage() != null && !request.getMessage().isBlank())
        		) {
    				ChatSessionInfo.Info sessionInfo = chatSessionInfo.get(session.getId());
		    		if(sessionInfo == null) {
		        		try {
		                    session.sendMessage(WebSocketUtil.createWebSocketTextMessage(new ErrorResponse("Unauthorized")));
		                } catch (IOException ex) {
		                    ex.printStackTrace();
		                }
		        		return;
		        	}
    		
            		try {
        				webSocketChatHandlerService.handleTextMessage(session, request);
            		}catch(Exception e) {
            			try {
                            session.sendMessage(WebSocketUtil.createWebSocketTextMessage(new ErrorResponse(e.getMessage())));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                		return;
            		}
            		try {
                        session.sendMessage(WebSocketUtil.createWebSocketTextMessage(new SuccessResponse("Sent OK")));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
            		return;
            	}
        } catch (IOException e) {
        	// Neprisiel nam message request ale nieco ine
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
    	LOG.debug("CONNECTED: " + session.toString());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    	LOG.info("DISCONNECTED: " + session.toString() + " " + status.toString());
    	
    	// Upratat po sebe
    	ChatSessionInfo.Info sessionInfo = chatSessionInfo.get(session.getId());
    	if(sessionInfo != null) {
    		if(sessionInfo.getMatrixClient() != null) {
    			try {
    				sessionInfo.getMatrixClient().logout();
    				LOG.info("Client Logged out from Chat");
    			}catch(Exception e) {
    				
    			}
    		}
    		sessionInfo = null;
    		chatSessionInfo.remove(session.getId());
    	}
    }
}
