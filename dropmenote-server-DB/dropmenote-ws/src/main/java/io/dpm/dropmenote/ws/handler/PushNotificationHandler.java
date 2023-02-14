package io.dpm.dropmenote.ws.handler;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.dpm.dropmenote.ws.bean.SessionBean;
import io.dpm.dropmenote.ws.constants.ControllerConstant;
import io.dpm.dropmenote.ws.exception.NotValidSessionException;
import io.dpm.dropmenote.ws.exception.PermissionDeniedException;
import io.dpm.dropmenote.ws.handler.interfaces.SessionPermissionValidatorInterface;
import io.dpm.dropmenote.ws.services.PushNotificationService;

@Service
public class PushNotificationHandler extends AbstractHandler implements SessionPermissionValidatorInterface {

	private static Logger LOG = LoggerFactory.getLogger(PushNotificationHandler.class);

	{
		LOG.debug("{} initialisation.", PushNotificationHandler.class.getName());
	}

	@Autowired
	private PushNotificationService pushService;

	@Override
	public SessionBean validateSessionAndPermission(String token, Long userId) throws NotValidSessionException, PermissionDeniedException {
		SessionBean sessionBean = sessionService.validate(token);
		return sessionBean;
	}
	
	/**
	 * send push notification to devices
	 * @param token
	 * @param message
	 * @param deviceIds
	 * @param httpResponse
	 * @throws NotValidSessionException
	 * @throws PermissionDeniedException
	 * @throws IOException 
	 */
	public void sendPush(String token, String message, String[] deviceIds, HttpServletResponse httpResponse) throws NotValidSessionException, PermissionDeniedException, IOException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, null);
		LOG.debug("success validation of session {}", sessionBean);
		pushService.sendPush(message, deviceIds);
		LOG.debug("success sending of message to device");
		// Response token
		httpResponse.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
		LOG.info("success sending http response header");
	}
}
