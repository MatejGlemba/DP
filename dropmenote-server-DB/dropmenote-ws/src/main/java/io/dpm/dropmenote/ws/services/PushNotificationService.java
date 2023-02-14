package io.dpm.dropmenote.ws.services;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 
 * @author Peterko
 *
 */
@Service
public class PushNotificationService {

	private static Logger LOG = LoggerFactory.getLogger(PushNotificationService.class);

	{
		LOG.debug("{} initialisation.", PushNotificationService.class.getName());
	}
	
	private final static String CHAT_PREFIX = "#_CHAT_URL_#";
	
	@Value("${web.app.url}")
	private String webUrl;
	
    @Value("${codenameone.api.token}")
    private String pushToken;
	
    @Value("${codenameone.api.fcmServerApiKey}")
    private String fcmServerApiKey;
            
    @Value("${codenameone.api.itunesCertPassword}")
    private String itunesCertPassword;
    
    @Value("${codenameone.api.itunesCert}")
    private String itunesCert;
    
    @Value("${codenameone.api.itunesProductionPush}")
    private boolean itunesProductionPush;

    @Value("${codenameone.api.itunesCertDev}")
    private String itunesCertDev;
    	    
    @Value("${codenameone.api.itunesCertPasswordDev}")
    private String itunesCertPasswordDev;
    
    @Value("${codenameone.api.windowsClientSecret}")
    private String windowsClientSecret;
    
    @Value("${codenameone.api.windowsSid}")
    private String windowsSid;

    /**
	 * send push notification to deviceIds
	 * @param message
	 * @param deviceIds
     * @throws IOException 
	 */
	public void sendPush(String message, String[] deviceIds) throws IOException {
		sendPush(message, "3", deviceIds);
	}
	
	/**
	 * send push with message and chat url
	 * @param message
	 * @param qrcodeId
	 * @param roomId
	 * @param deviceIds
	 * @throws IOException
	 */
	public void sendPush(String message, String qrcodeId, String roomId, String[] deviceIds) throws IOException {
		LOG.debug("Sending push notification from webSocketChatHandler service");
		if (qrcodeId == null || qrcodeId.equals("")) {
			LOG.debug("Push notification not send. Missing qrcodeId param for Chat.");
			return;
		}
		if (roomId == null) {
			roomId = "";
		}
		String msgBody = message + ";" + CHAT_PREFIX + webUrl + "/#/chat?qr=" + qrcodeId + "&roomId=" + roomId;
		LOG.debug("Sending push notification from webSocketChatHandler service - mssg body {}", msgBody);
		sendPush(msgBody, "3", deviceIds);
	}
	
    /**
     * send push notifications
     * @param messageBody
     * @param pushType
     * @param deviceIds
     * @return
     * @throws IOException
     */
	private String sendPush(String messageBody, String pushType, String[] deviceIds) throws IOException {
		/* do nothing when no devices */
		if (deviceIds.length <= 0) {
			return "Push notification not sent: missing deviceIds";
		}
		
		/* Log devices */
//		for (String id : deviceIds) {
//			LOG.debug("Push notification deviceId:{}", id);
//		}
		LOG.debug("Push notification messageBody:{}", messageBody);
		
		/* connect to push notification server */
		HttpURLConnection connection = (HttpURLConnection)new URL("https://push.codenameone.com/push/push").openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

		LOG.debug("successful creation of httpurl connetion to codename service");
		/* set development push certificate for testing */
		if(!itunesProductionPush) {
		    itunesCert = itunesCertDev;
		    itunesCertPassword = itunesCertPasswordDev;
		}
		
		/* create one parameter from deviceIds */
		String paramDeviceIds = "";
		for (String id : deviceIds) {
			paramDeviceIds += "&device=" + URLEncoder.encode(id, "UTF-8");
		}
		LOG.debug("paramDeviceIds {}", paramDeviceIds);
		/* create query */
		String query = "token="  + pushToken +
			paramDeviceIds +
//		    "&device=" + URLEncoder.encode(deviceId, "UTF-8") +
//		    "&device=" + URLEncoder.encode(deviceId, "UTF-8") +
		    "&type=" + pushType +
		    "&auth=" + URLEncoder.encode(fcmServerApiKey, "UTF-8") +
		    "&certPassword=" + URLEncoder.encode(itunesCertPassword, "UTF-8") +
		    "&cert=" + URLEncoder.encode(itunesCert, "UTF-8") +
		    "&body=" + URLEncoder.encode(messageBody, "UTF-8") +
		    "&production=" + itunesProductionPush +
		    "&sid=" + URLEncoder.encode(windowsSid, "UTF-8") +
		    "&client_secret=" + URLEncoder.encode(windowsClientSecret, "UTF-8");

		LOG.debug("creation of query {}", query);
		/* send notifications */
		try (OutputStream output = connection.getOutputStream()) {
		    output.write(query.getBytes("UTF-8"));
		} catch (IOException e) {
			LOG.debug("throw exception while outpustream writing {}", e);
			throw e;
		}
		LOG.debug("Push notification response code:{}, message:{}", connection.getResponseCode(), connection.getResponseMessage());
        return connection.getResponseMessage();
	}
}
