
package io.dpm.dropmenote.ws.controller;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.dpm.dropmenote.ws.constants.ControllerConstant;
import io.dpm.dropmenote.ws.controller.rrbean.AuthInfoRequest;
import io.dpm.dropmenote.ws.controller.rrbean.UserBeanRequest;
import io.dpm.dropmenote.ws.controller.rrbean.UserBeanResponse;
import io.dpm.dropmenote.ws.controller.rrbean.UserInfoResponse;
import io.dpm.dropmenote.ws.exception.EmailException;
import io.dpm.dropmenote.ws.exception.EntityDoesntExistException;
import io.dpm.dropmenote.ws.exception.MatrixException;
import io.dpm.dropmenote.ws.exception.NonValidLoginInformationException;
import io.dpm.dropmenote.ws.exception.NotValidRecoverPswdTokenException;
import io.dpm.dropmenote.ws.exception.NotValidSessionException;
import io.dpm.dropmenote.ws.exception.PermissionDeniedException;
import io.dpm.dropmenote.ws.exception.UserAlreadyExistsException;
import io.dpm.dropmenote.ws.handler.UserHandler;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 
 * @author Martin Jurek (Starbug s.r.o. | https://www.strabug.eu)
 *
 */
@Api(value = "UserController", description = "UserController")
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", maxAge = 3600)
@RequestMapping("/ws/user")
public class UserController extends AbstractController {

	private static Logger LOG = LoggerFactory.getLogger(UserController.class);

	{
		LOG.info("{} initialisation.", UserController.class.getName());
	}

	@Autowired
	private UserHandler userHandler;

	/**
	 *
	 * @param request
	 * @return userToken
	 * @throws EmailException
	 * @throws MatrixException 
	 */
	@ApiOperation(value = "User register", notes = "pri RegistrationUser screene", responseContainer = "Object")
	@RequestMapping(value = "/register", method = RequestMethod.POST, produces = ControllerConstant.ALL, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public void register(@RequestBody AuthInfoRequest request, HttpServletResponse httpResponse) throws UserAlreadyExistsException, EmailException, MatrixException {
		LOG.info("UserController - register - request {}", request);
		userHandler.register(request);
	}

	/**
	 * 
	 * @param request
	 * @return
	 * @throws NonValidLoginInformationException
	 */
	@ApiOperation(value = "Servisa zmeni usera", notes = "pri ChangeProfile screene", responseContainer = "Object")
	@RequestMapping(value = "/login", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public UserInfoResponse login(@RequestBody AuthInfoRequest request, HttpServletResponse httpResposne) throws NonValidLoginInformationException {
		LOG.info("UserController - login - request {}", request);
		return userHandler.login(request, httpResposne);
	}

	@ApiOperation(value = "Servisa odhlasi uzivatela", notes = "pri testovani error exceptionov", responseContainer = "Object")
	@RequestMapping(value = "/logout", method = RequestMethod.POST, produces = ControllerConstant.ALL, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public void logout(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, String deviceId, HttpServletResponse httpResposne) throws NonValidLoginInformationException, NotValidSessionException, PermissionDeniedException {
		LOG.info("UserController - logout - token {}, deviceId - {}", token, deviceId);
		userHandler.logout(deviceId, token, httpResposne);
	}
	
	/**
	 * update nezmeni password
	 * 
	 * @param token
	 * @param request
	 * @return
	 * @throws PermissionDeniedException
	 */
	@ApiOperation(value = "create driver and evidences with missing status", notes = "pouzije sa pri RegistrationDriver screene", responseContainer = "Object")
	@RequestMapping(value = "/update", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public UserInfoResponse update(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, @RequestBody UserBeanRequest request, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		LOG.info("UserController - update - token {}, request - {}", token, request);
		return userHandler.update(token, request, httpResposne);
	}
	
	@ApiOperation(value = "delete user", notes = "delete user", responseContainer = "Object")
	@RequestMapping(value = "/delete", method = RequestMethod.POST, produces = ControllerConstant.ALL, consumes = ControllerConstant.ALL)
	@ResponseBody
	public void delete(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, long userId, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		LOG.info("UserController - delete - token {} - userId - {}", token, userId);
		userHandler.delete(token, userId, httpResposne);
	}

	/**
	 * update password
	 * @param token
	 * @param password
	 * @param httpResposne
	 * @throws NotValidSessionException
	 * @throws PermissionDeniedException
	 */
	@ApiOperation(value = "update password", notes = "update len passwordu", responseContainer = "Object")
	@RequestMapping(value = "/updatePassword", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.ALL)
	@ResponseBody
	public void updatePassword(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, String password, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		LOG.info("UserController - updatePassword - token {} , password - {}", token, password);
		userHandler.updatePassword(token, password, httpResposne);
	}

	/**
	 *
	 * @return
	 * @throws PermissionDeniedException
	 * @throws NotValidRecoverPswdTokenException
	 */
	@ApiOperation(value = "change password if user is validated by pswdToken", notes = "change password if user is validated", responseContainer = "Object")
	@RequestMapping(value = "/updatePswdRecovery", method = RequestMethod.POST, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public void updateFromPswdRecovery(@RequestHeader(value = ControllerConstant.TOKEN_RECOVERY_PSWD_TOKEN, required = true) String emailToken, String password) throws NotValidSessionException, NotValidRecoverPswdTokenException {
		LOG.info("UserController - updateFromPswdRecovery - emailToken {}, password - {}", emailToken, password);
		userHandler.updatePswdRecovery(emailToken, password);
	}

	/**
	 * 
	 * @param token
	 * @return
	 * @throws PermissionDeniedException
	 */
	@ApiOperation(value = "Servisa načíta usera", notes = "pri Dashboarde, ChangeProfile screene, mozno pri OfferList screene", responseContainer = "Object")
	@RequestMapping(value = "/load", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public UserBeanResponse load(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		LOG.info("UserController - load - token {}", token);
		return userHandler.load(token, httpResposne);
	}

	/**
	 * 
	 * @param login
	 * @return
	 * @throws EmailException
	 * @throws EntityDoesntExistException
	 */
	@ApiOperation(value = "Servisa načíta usera", notes = "pri Dashboarde, ChangeProfile screene, mozno pri OfferList screene", responseContainer = "Object")
	@RequestMapping(value = "/forgotPassword", method = RequestMethod.POST, produces = ControllerConstant.ALL, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public void forgotPassword(String login) throws EmailException, EntityDoesntExistException {
		LOG.info("UserController - forgotPassword - login {}", login);
		userHandler.forgotPassword(login);
	}

	@ApiOperation(value = "Servisa načíta usera", notes = "pri Dashboarde, ChangeProfile screene, mozno pri OfferList screene", responseContainer = "Object")
	@RequestMapping(value = "/validateResetPasswordToken", method = RequestMethod.POST, produces = ControllerConstant.ALL, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public void validateResetPasswordToken(@RequestParam(required = true) String token, HttpServletResponse httpResposne) throws NotValidRecoverPswdTokenException {
		// TODO toto neviem ci je dobre
		LOG.info("UserController - validateResetPasswordToken - token {}", token);
		userHandler.validateResetPasswordToken(token);
	}
	
	@ApiOperation(value = "Servisa validuje token", notes = "po spusteni appky", responseContainer = "Object")
	@RequestMapping(value = "/validateToken", method = RequestMethod.POST, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public void validateToken(@RequestParam(required = true) String token, HttpServletResponse httpResponse) throws NotValidSessionException, PermissionDeniedException {
		LOG.info("UserController - validateToken - token {}", token);
		userHandler.validateToken(token, httpResponse);
	}

	/**
	 * OBSOLETE
	 */
//	@ApiOperation(value = "Servisa uloží matrix info", notes = "Servisa uloží matrix login/pass do usera", responseContainer = "Object")
//	@RequestMapping(value = "/saveMatrixInfo", method = RequestMethod.POST, produces = ControllerConstant.ALL, consumes = ControllerConstant.MIME_JSON)
//	@ResponseBody
//	public void saveMatrixInfo(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, String userUuid, String matrixUsername, String matrixPassword, HttpServletResponse httpResponse) throws NotValidSessionException, PermissionDeniedException {
//		userHandler.saveMatrixInfo(token, userUuid, matrixUsername, matrixPassword, httpResponse);
//	}
}
