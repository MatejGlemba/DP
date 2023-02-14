package io.dpm.dropmenote.ws.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.dpm.dropmenote.ws.constants.ControllerConstant;
import io.dpm.dropmenote.ws.exception.NotValidSessionException;
import io.dpm.dropmenote.ws.exception.PermissionDeniedException;
import io.dpm.dropmenote.ws.handler.PushNotificationHandler;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 
 * @author Peterko
 *
 */
@Api(value = "PushNotificationController", description = "PushNotificationController")
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", maxAge = 3600)
@RequestMapping("/ws/push")
public class PushNotificationController extends AbstractController {

	private static Logger LOG = LoggerFactory.getLogger(MatrixController.class);

	{
		LOG.info("{} initialisation.", MatrixController.class.getName());
	}

	private PushNotificationHandler pushHandler;
	
	@ApiOperation(value = "Odosle push notifikaciu", notes = "", responseContainer = "Object")
	@RequestMapping(value = "/sendPush", method = RequestMethod.POST, consumes = ControllerConstant.ALL)
	@ResponseBody
	public void sendPush(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, String message, String[] deviceIds, HttpServletResponse httpResponse) throws NotValidSessionException, PermissionDeniedException, IOException {
		LOG.info("PushNotificationController - sendPush - token {}, message - {}, deviceIds - {}", token, message, deviceIds);
		pushHandler.sendPush(token, message, deviceIds, httpResponse);
	}
}
