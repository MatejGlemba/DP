package io.dpm.dropmenote.ws.handler;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.dpm.dropmenote.db.enums.ProfileIconEnum;
import io.dpm.dropmenote.ws.bean.SessionBean;
import io.dpm.dropmenote.ws.bean.UserBean;
import io.dpm.dropmenote.ws.constants.ControllerConstant;
import io.dpm.dropmenote.ws.controller.rrbean.AuthInfoRequest;
import io.dpm.dropmenote.ws.controller.rrbean.UserBeanRequest;
import io.dpm.dropmenote.ws.controller.rrbean.UserBeanResponse;
import io.dpm.dropmenote.ws.controller.rrbean.UserInfoResponse;
import io.dpm.dropmenote.ws.dto.UserDto;
import io.dpm.dropmenote.ws.exception.EmailException;
import io.dpm.dropmenote.ws.exception.EntityDoesntExistException;
import io.dpm.dropmenote.ws.exception.MatrixException;
import io.dpm.dropmenote.ws.exception.NonValidLoginInformationException;
import io.dpm.dropmenote.ws.exception.NotValidRecoverPswdTokenException;
import io.dpm.dropmenote.ws.exception.NotValidSessionException;
import io.dpm.dropmenote.ws.exception.PermissionDeniedException;
import io.dpm.dropmenote.ws.exception.UserAlreadyExistsException;
import io.dpm.dropmenote.ws.handler.interfaces.SessionPermissionValidatorInterface;
import io.dpm.dropmenote.ws.services.DeviceService;
import io.dpm.dropmenote.ws.services.UserService;

@Service
public class UserHandler extends AbstractHandler implements SessionPermissionValidatorInterface {

	private static Logger LOG = LoggerFactory.getLogger(UserHandler.class);

	{
		LOG.info("{} initialisation.", UserHandler.class.getName());
	}

	@Autowired
	private UserService userService;

	@Autowired
	private DeviceService deviceService;
	
	/**
	 * 
	 * @param authInfoRequest
	 * @throws UserAlreadyExistsException
	 * @throws EmailException
	 * @throws MatrixException 
	 */
	public void register(AuthInfoRequest authInfoRequest) throws UserAlreadyExistsException, EmailException, MatrixException {
		userService.register(UserDto.convertToBean(authInfoRequest));
	}

	/**
	 * 
	 * @param authInfoRequest
	 * @param httpResposne
	 * @return
	 * @throws NonValidLoginInformationException
	 */
	public UserInfoResponse login(AuthInfoRequest authInfoRequest, HttpServletResponse httpResposne) throws NonValidLoginInformationException {
		UserBean userBean = userService.login(UserDto.convertToBean(authInfoRequest));
		String newToken = sessionService.save(userBean, authInfoRequest.getDeviceId());

		// save deviceId
		String devId = authInfoRequest.getDeviceId();
		if (devId != null && !devId.isBlank() && !devId.equals("null")) {
			deviceService.save(userBean, authInfoRequest.getDeviceId());
		}
		
		// TODO ak je lognuty tak tuto metodu rejectni, aby sa to nedalo hackovat cez
		// tuto metodu
		// TODO ak je lognuty tak tuto metodu rejectni, aby sa to nedalo hackovat cez
		// tuto metodu
		// TODO ak je lognuty tak tuto metodu rejectni, aby sa to nedalo hackovat cez
		// tuto metodu
		// Response token
		httpResposne.setHeader(ControllerConstant.TOKEN_HEADER, newToken);

		String chatIcon = userBean.getChatIcon() == null ? ProfileIconEnum.P1.toString() : userBean.getChatIcon().toString();
		return new UserInfoResponse(userBean.getLogin(), userBean.getAlias(), chatIcon);
	}

	/**
	 * 
	 * @param token
	 * @param userBeanRequest
	 * @param httpResposne
	 * @return
	 * @throws NotValidSessionException
	 * @throws PermissionDeniedException
	 */
	public UserInfoResponse update(String token, UserBeanRequest userBeanRequest, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, null);
		if (sessionBean == null) {
			throw new NotValidSessionException("User's session not valid.");
		}
		
		// Login change is not allowed
		if (sessionBean.getUser() == null) {
			throw new NotValidSessionException("User's session not valid.");
		}
		if (!sessionBean.getUser().getLogin().equals(userBeanRequest.getLogin())) {
			throw new PermissionDeniedException("Change login information is not allowed.");
		}
		
		sessionBean.getUser().setAlias(userBeanRequest.getAlias());
		sessionBean.getUser().setChatIcon(userBeanRequest.getChatIcon());
		sessionBean.getUser().setPushNotification(userBeanRequest.isPushNotification());
		sessionBean.getUser().setEmailNotification(userBeanRequest.isEmailNotification());
		sessionBean.getUser().setDmnAI(userBeanRequest.isDmnAI());
		
		userService.save(sessionBean.getUser());
		
		// Response token
		httpResposne.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
		return new UserInfoResponse(sessionBean.getUser().getLogin(), sessionBean.getUser().getAlias(), sessionBean.getUser().getChatIcon().toString());
	}

	/**
	 * 
	 * @param emailPswToken
	 * @param authInfoRequest
	 * @return
	 * @throws NotValidRecoverPswdTokenException
	 * @throws PermissionDeniedException
	 */
	public void updatePswdRecovery(String emailPswToken, String password) throws NotValidRecoverPswdTokenException {
		UserBean userBean = userService.loadByRecoveryToken(emailPswToken);
		
		if (userBean == null) {
			throw new NotValidRecoverPswdTokenException("Not valid Email Recovery Token.");
		}

		userBean.setPassword(password);
		userBean.setRecoveryToken(null);
		userService.save(userBean);
	}

	/**
	 * 
	 * @param token
	 * @param httpResposne
	 * @return
	 * @throws NotValidSessionException
	 * @throws PermissionDeniedException
	 */
	public UserBeanResponse load(String token, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, null);

		UserBeanResponse resp = UserDto.mapInto(new UserBeanResponse(), userService.load(sessionBean.getUser().getId()));
		
		// Response token
		httpResposne.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));

		return resp;
	}

	/**
	 * 
	 * @param login
	 * @throws EmailException
	 * @throws EntityDoesntExistException
	 */
	public void forgotPassword(String login) throws EmailException, EntityDoesntExistException {
		UserBean userBean = userService.loadByLogin(login);

		// Validation
		if (userBean == null) {
			throw new EntityDoesntExistException("User login is not valid.");
		}

		userService.forgotPassword(login);
	}

	/**
	 * 
	 * @param recoverPswdToken
	 * @return login
	 * @throws NotValidRecoverPswdTokenException
	 */
	public void validateResetPasswordToken(String recoverPswdToken) throws NotValidRecoverPswdTokenException {
		UserBean userBean = userService.loadByRecoveryToken(recoverPswdToken);
		userBean.setCanChangePassword(true);
		userBean = userService.save(userBean);
		// Validation
		if (userBean == null) {
			throw new NotValidRecoverPswdTokenException("Email recovery token is not valid.");
		}
	}

	@Override
	public SessionBean validateSessionAndPermission(String token, Long userId) throws NotValidSessionException, PermissionDeniedException {
		SessionBean sessionBean = sessionService.validate(token);
		return sessionBean;
	}

	/**
	 * 
	 * OBSOLETE
	 * 
	 * save matrix username+password to UserEntity
	 * @param token
	 * @param userUuid
	 * @param matrixUsername
	 * @param matrixPassword
	 * @param httpResponse
	 * @throws PermissionDeniedException
	 * @throws NotValidSessionException
	 */
//	public void saveMatrixInfo(String token, String userUuid, String matrixUsername, String matrixPassword, HttpServletResponse httpResponse) throws NotValidSessionException, PermissionDeniedException {
//		// Session validation
//		SessionBean sessionBean = validateSessionAndPermission(token, null);
//		// TODO daco s tokenom?
//		userService.saveMatrixInfo(userUuid, matrixUsername, matrixPassword);
//		// Response token
//		httpResponse.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
//	}

	/**
	 * generate new token and don't send it to user in header
	 * @param token
	 * @param httpResposne
	 * @throws PermissionDeniedException 
	 * @throws NotValidSessionException 
	 */
	public void logout(String deviceId, String token, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		SessionBean sessionBean = validateSessionAndPermission(token, null);
		if (sessionBean == null) {
			throw new NotValidSessionException("No session to delete");
		}
		deviceService.delete(sessionBean.getUser(), deviceId);
		sessionService.remove(token);
		sessionService.delete(token);
	}

	/**
	 * update password, nothing else
	 * @param token
	 * @param password
	 * @param httpResposne
	 * @throws PermissionDeniedException 
	 * @throws NotValidSessionException 
	 */
	public void updatePassword(String token, String password, HttpServletResponse httpResponse) throws NotValidSessionException, PermissionDeniedException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, null);
		// Change password
		sessionBean.getUser().setPassword(password);
		userService.save(sessionBean.getUser());
		// Response token
		httpResponse.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
		
	}

	/**
	 * zmaze usera
	 * @param token
	 * @param userId
	 * @param httpResposne
	 * @throws PermissionDeniedException 
	 * @throws NotValidSessionException 
	 */
	public void delete(String token, long userId, HttpServletResponse httpResponse) throws NotValidSessionException, PermissionDeniedException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, null);
		
		userService.delete(userId);
		
		httpResponse.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
	}

	/**
	 * just validate token, throw exception or return 200 success
	 * @param token
	 * @throws PermissionDeniedException 
	 * @throws NotValidSessionException 
	 */
	public void validateToken(String token, HttpServletResponse httpResponse) throws NotValidSessionException, PermissionDeniedException {
		SessionBean sessionBean = validateSessionAndPermission(token, null);
		httpResponse.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
	}
}
