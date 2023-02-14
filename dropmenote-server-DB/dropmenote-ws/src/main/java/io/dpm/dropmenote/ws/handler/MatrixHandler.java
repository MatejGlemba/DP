package io.dpm.dropmenote.ws.handler;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.dpm.dropmenote.ws.bean.MatrixBean;
import io.dpm.dropmenote.ws.bean.SessionBean;
import io.dpm.dropmenote.ws.constants.ControllerConstant;
import io.dpm.dropmenote.ws.controller.rrbean.MatrixResponseObject;
import io.dpm.dropmenote.ws.exception.NotValidSessionException;
import io.dpm.dropmenote.ws.exception.PermissionDeniedException;
import io.dpm.dropmenote.ws.handler.interfaces.SessionPermissionValidatorInterface;
import io.dpm.dropmenote.ws.services.MatrixService;

@Service
public class MatrixHandler extends AbstractHandler implements SessionPermissionValidatorInterface {

	private static Logger LOG = LoggerFactory.getLogger(MatrixHandler.class);

	{
		LOG.info("{} initialisation.", MatrixHandler.class.getName());
	}

	@Autowired
	private MatrixService matrixService;

	@Override
	public SessionBean validateSessionAndPermission(String token, Long userId) throws NotValidSessionException, PermissionDeniedException {
		SessionBean sessionBean = sessionService.validate(token);
		return sessionBean;
	}

	/**
	 * send message
	 * 
	 * @param qrcode
	 * @param message
	 * @param createRoom
	 * @return
	 * @throws Exception
	 */
	public String sendMsg(String qrcode, String message, boolean createRoom) throws Exception {
		return matrixService.sendMsg(qrcode, message, createRoom);
	}

	/**
	 * save matrix room
	 * 
	 * @param token
	 * @param userUuid
	 * @param qrUuid
	 * @param matrixRoomId
	 * @throws NotValidSessionException
	 * @throws PermissionDeniedException
	 */
	public void save(String token, String userUuid, String qrUuid, String matrixRoomId, HttpServletResponse httpResponse) throws NotValidSessionException, PermissionDeniedException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, null);
		// Set matrix bean
		MatrixBean matrixBean = new MatrixBean();
		matrixBean.setUserUuid(userUuid);
		matrixBean.setQrCodeUuid(qrUuid);
		matrixBean.setMatrixRoomId(matrixRoomId);
		matrixService.save(matrixBean);
		// Response token
		httpResponse.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
	}

	/**
	 * get ?
	 * 
	 * @param token
	 * @param userUuid
	 * @param qrUuid
	 * @param httpResponse
	 * @return
	 * @throws PermissionDeniedException
	 * @throws NotValidSessionException
	 */
	public String getMatrixRoom(String token, String userUuid, String qrUuid, HttpServletResponse httpResponse) throws NotValidSessionException, PermissionDeniedException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, null);
		// Get matrixRoomdId
		String matrixRoomId = matrixService.getMatrixRoomId(userUuid, qrUuid);
		// Response token
		httpResponse.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));

		return matrixRoomId;
	}

	// /**
	// * get ?
	// * @param token
	// * @param userUuid
	// * @param httpResponse
	// * @return
	// * @throws PermissionDeniedException
	// * @throws NotValidSessionException
	// */
	// public String registerMatrixUser(String token, String userUuid,
	// HttpServletResponse httpResponse) throws NotValidSessionException,
	// PermissionDeniedException {
	// // Session validation
	// SessionBean sessionBean = validateSessionAndPermission(token, null);
	// // Register matrix user
	// String registerMatrixUserResponse = null;
	// try {
	// registerMatrixUserResponse = matrixService.registerMatrixUser(userUuid);
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// // Response token
	// httpResponse.setHeader(ControllerConstant.TOKEN_HEADER,
	// sessionService.generateNewToken(sessionBean));
	//
	// return registerMatrixUserResponse;
	// }

	/**
	 * 
	 * @param qrcode
	 * @param userName
	 * @return
	 * @throws Exception
	 */
	public String invite(String qrcode, String userName) throws Exception {
		return matrixService.invite(qrcode, userName);
	}

	/**
	 * zoznam roomov kde user pisal + zoznam roomov kde je user ako owner/shared
	 * user
	 * 
	 * @param token
	 * @param httpResponse
	 * @return
	 * @throws Exception
	 */
	public List<MatrixResponseObject> loadAdminRooms(String token, HttpServletResponse httpResponse) throws Exception {
		// validate session
		SessionBean sessionBean = validateSessionAndPermission(token, null);
			
		// TODO ziska zoznam qr kodov kde je owner/shared user a vytiahne room
		// qrcodeuuid=matrixqruuid
		List<MatrixResponseObject> response = matrixService.loadAdminRooms(sessionBean.getUser());
		
		// set response
		httpResponse.setHeader(ControllerConstant.TOKEN_HEADER, sessionBean.getToken());
		return response;
	}

	/**
	 * get user emails to send message notifications to
	 * @param matrixRoomId
	 * @return
	 */
	public List<String> getEmailsFromUsersOfMatrixRoomQrCode(String matrixRoomId, String userEmail) {
		return matrixService.getEmailsForEmailNotifications(matrixRoomId, userEmail);
	}
}
