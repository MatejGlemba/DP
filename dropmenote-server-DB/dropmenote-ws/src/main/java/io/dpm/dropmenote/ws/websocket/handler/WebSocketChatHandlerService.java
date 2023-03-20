package io.dpm.dropmenote.ws.websocket.handler;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import io.dpm.dropmenote.ws.services.*;
import io.dpm.dropmenote.ws.utils.WebSocketUtil;
import io.dpm.dropmenote.ws.websocket.websocketObject.websocketResponse.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import io.dpm.dropmenote.ws.bean.BlacklistBean;
import io.dpm.dropmenote.ws.bean.MatrixBean;
import io.dpm.dropmenote.ws.bean.QRCodeBean;
import io.dpm.dropmenote.ws.bean.SessionBean;
import io.dpm.dropmenote.ws.bean.UserBean;
import io.dpm.dropmenote.ws.constants.ConfigurationConstant;
import io.dpm.dropmenote.ws.exception.EmailException;
import io.dpm.dropmenote.ws.utils.AESCipher;
import io.dpm.dropmenote.ws.websocket.session.ChatSessionInfo;
import io.dpm.dropmenote.ws.websocket.websocketObject.websocketRequest.LoginRequest;
import io.dpm.dropmenote.ws.websocket.websocketObject.websocketRequest.WebsocketTextMessageRequest;
import io.dpm.matrix.hs.api.MatrixRoom;

@Service
public class WebSocketChatHandlerService {
	

	private static Logger LOG = LoggerFactory.getLogger(WebSocketChatHandlerService.class);

	{
		LOG.debug("{} initialisation.", WebSocketChatHandlerService.class.getName());
	}
	
	@Value("${secret.crypting.key}")
	private String CRYPTING_KEY;

	@Autowired
	SessionService sessionService;

    @Autowired
    private MatrixService matrixService;
    
    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private ChatSessionInfo chatSessionInfo;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PushNotificationService pushNotificationService;

    @Autowired
	private DeviceService deviceService;
    
    @Autowired
    private BlacklistService blacklistService;

	@Autowired
	private KafkaService<?, ?> kafkaService;

	public void handleUserLogin(WebSocketSession session, final LoginRequest request) throws Exception {
    	if(!session.isOpen()) {
    		throw new Exception("Session closed");
    	}
    	
    	// Session validation
		SessionBean sessionBean = sessionService.validate(request.getToken());
		UserBean userBean = sessionBean.getUser();
		
		// Check QR code
		QRCodeBean qrCodeBean = qrCodeService.load(request.getQrCodeUuid());
        if(qrCodeBean == null) {
        	throw new Exception("Incorrect DMN code!");
        }
        
        MatrixBean matrixBean = null;

        boolean isShared = qrCodeBean.getSharedUsers().stream().filter(user->user.getId()==userBean.getId()).findFirst().isPresent(); // is Shared?
        // Is QR admin or QR User?
        if (qrCodeBean.getOwner().getId() == userBean.getId() /*Is Owner?*/ || isShared) {
        	
        	/* Shared user */
        	if (isShared) {
            	if (request.getRoomId() == null) {
            		matrixBean = matrixService.createAndSaveUserMatrixRoom(userBean.getMatrixUsername(), userBean.getMatrixPassword(), ConfigurationConstant.MATRIX_SERVER, qrCodeBean, userBean);
            	} else {
            		matrixBean = matrixService.load(request.getRoomId());
            		if(matrixBean == null) {
                		throw new Exception("Invalid room");
                	}
            	}
            	
            /* Owner user */
            } else if (qrCodeBean.getOwner().getId() == userBean.getId()) {
        		if (request.getRoomId() == null) {
            		throw new Exception("Invalid room");
            	}
            	matrixBean = matrixService.load(request.getRoomId());
            	if(matrixBean == null) {
            		throw new Exception("Invalid room");
            	}
            }
        	
        /* Guest user or plebs */
        }else {
        	// Get existing or create matrix room
        	matrixBean = matrixService.createAndSaveUserMatrixRoom(userBean.getMatrixUsername(), userBean.getMatrixPassword(), ConfigurationConstant.MATRIX_SERVER, qrCodeBean, userBean);
        }
		
		matrixService.createClientAndSendChatToWS(userBean.getMatrixUsername(), userBean.getMatrixPassword(), ConfigurationConstant.MATRIX_SERVER, matrixBean, qrCodeBean, session);
        
    }

    public void handleAnonymousLogin(WebSocketSession session, LoginRequest request) throws Exception {
    	if(!session.isOpen()) {
    		throw new Exception("Session closed");
    	}
    	
		// Check QR code
		QRCodeBean qrCodeBean = qrCodeService.load(request.getQrCodeUuid());
        if(qrCodeBean == null) {
        	throw new Exception("Incorrect DMN code!");
        }

    	MatrixBean matrixBean = matrixService.createAndSaveAnonymousMatrixRoom(ConfigurationConstant.MATRIX_SERVER, request.getFingerprint(), qrCodeBean);
    	
    	matrixService.createClientAndSendChatToWS(matrixBean.getMatrixUsername(), matrixBean.getMatrixPassword(), ConfigurationConstant.MATRIX_SERVER, matrixBean, qrCodeBean, session);
        
    }

    public void handleTextMessage(WebSocketSession session, WebsocketTextMessageRequest request) throws Exception {
    	UserBean userBean = sessionService.loadUserByToken(request.getToken());
    	
		ChatSessionInfo.Info sessionInfo = chatSessionInfo.get(session.getId());
		
		if(sessionInfo == null) {
			throw new Exception("Unauthorized");
		}
		if(sessionInfo.getMatrixClient() == null || sessionInfo.getMatrixRoomId() == null || sessionInfo.getMatrixRoomId().isBlank()) {
			throw new Exception("Invalid session data");
		}
		
		MatrixRoom room = sessionInfo.getMatrixClient().getRoom(sessionInfo.getMatrixRoomId());
		if(room == null) {
			throw new Exception("Invalid room");
		}

		BlacklistBean blacklist = blacklistService.loadByMatrixRoomId(sessionInfo.getMatrixRoomId());
		if (blacklist != null) {
			return;
		}
		
		//otvori sa websocket, treba nastavit active v session
		// ked zanikne websocket, nastavit na false active
		
		
		// send message via email if users had turned on email_notifications TODO2 malo by posielat len offline uzivatelom?
		new Thread(() -> {
			String userEmail = userBean == null ? "" : userBean.getLogin();
			List<String> emails = matrixService.getEmailsForEmailNotifications(sessionInfo.getMatrixRoomId(), userEmail);
			if (emails != null && !emails.isEmpty()) {
				for (int i = 0; i < emails.size(); i++) {
					if (emails.get(i) == null || emails.get(i).equals("null") || emails.get(i).equals("")) {
						emails.remove(i);
					}
				}
				
				try {
					String senderAlias = matrixService.findAliasByMatrixRoomId(sessionInfo.getMatrixRoomId());
					senderAlias = (senderAlias == null || senderAlias.equals("")) ? "Anonymous" : senderAlias;
					String qrName = matrixService.findQrNameByMatrixRoomId(sessionInfo.getMatrixRoomId());
					emailService.sendNotification(emails, senderAlias, qrName, request.getMessage());
				} catch (EmailException e) {
					e.printStackTrace(); //TODO custom exception?
				}
			}
		}).start();
		
		// send push notifications if users had turned on push_notifications
		new Thread(() -> {
			String userEmail = userBean == null ? "" : userBean.getLogin();
			List<String> emailsForPush = matrixService.getEmailsForPushNotifications(sessionInfo.getMatrixRoomId(), userEmail);
			if (emailsForPush != null && !emailsForPush.isEmpty()) {		
				for (int i = 0; i < emailsForPush.size(); i++) {
					if (emailsForPush.get(i) == null || emailsForPush.get(i).equals("null") || emailsForPush.get(i).equals("")) {
						emailsForPush.remove(i);
					}
				}
				
				List<String> deviceIds = deviceService.loadDeviceIds(emailsForPush);
				if (deviceIds != null && !deviceIds.isEmpty()) {
					String[] deviceIdsArray = deviceIds.toArray(new String[0]);
//						for (int i = 0; i < deviceIdsArray.length; i++) {
//							LOG.debug("deviceId: " + deviceIdsArray[i]);
//						}

					try {
						String matrixId = matrixService.getIdByMatrixRoomId(sessionInfo.getMatrixRoomId());
						String qrCodeUuid = matrixService.getQrCodeUuidByMatrixRoomId(sessionInfo.getMatrixRoomId());
						pushNotificationService.sendPush(request.getMessage(), qrCodeUuid, matrixId, deviceIdsArray);
					} catch (IOException e) {
						LOG.debug("Exception while pushing notification {}", e);
						e.printStackTrace(); 
					} 
				}
			}
		}).start();

		// poslat novu spravu do kafky
		new Thread(() -> {
			/*
			 * request.getMessage()			: data
			 * userBean.getUuid()			: user_id
			 * loginRequest.getRoomId()		: room_id
			 * loginRequest.getQrCodeUiid()	: qrcode_id
			 * */
			var qrCodeUUID = matrixService.getQrCodeUuidByMatrixRoomId(sessionInfo.getMatrixRoomId());
			KafkaService.INPUT_DATA inputData = new KafkaService.MESSAGE_DATA(sessionInfo.getMatrixRoomId(),
					qrCodeUUID, userBean.getUuid(), request.getMessage());
			kafkaService.produce(KafkaService.TOPIC.MESSAGE_DATA, inputData);
		}).start();

		String encryptedMsg = AESCipher.encrypt(CRYPTING_KEY, request.getMessage()).getData();
		room.sendText(encryptedMsg);

		//kafkaService.startConsumerLoop(outputConsumer(session));

		// set chat room as not empty anymore. Note takes around 60 ms to execute. When loading matrixBean, setEmpty and save takes 480ms
		matrixService.setNotEmpty(sessionInfo.getMatrixRoomId());
	}

	private Consumer<KafkaService.MESSAGE_OUTPUT> outputConsumer(WebSocketSession session) {
		return new Consumer<KafkaService.MESSAGE_OUTPUT>() {
			@Override
			public void accept(KafkaService.MESSAGE_OUTPUT messageOutput) {
				if (messageOutput.getNotification_type().equals(KafkaService.NOTIFICATION_TYPE.HATE)) {
					try {
						session.sendMessage(WebSocketUtil.createWebSocketTextMessage(new ErrorResponse("There was hate message detected")));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		};
	}
}
