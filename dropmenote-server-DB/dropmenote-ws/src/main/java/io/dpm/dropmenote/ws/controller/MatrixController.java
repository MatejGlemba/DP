package io.dpm.dropmenote.ws.controller;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.dpm.dropmenote.ws.constants.ControllerConstant;
import io.dpm.dropmenote.ws.controller.rrbean.MatrixResponseObject;
import io.dpm.dropmenote.ws.exception.NotValidSessionException;
import io.dpm.dropmenote.ws.exception.PermissionDeniedException;
import io.dpm.dropmenote.ws.handler.MatrixHandler;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 
 * @author Martin Jurek (Starbug s.r.o. | https://www.strabug.eu)
 *
 */
@Api(value = "MatrixController", description = "MatrixController")
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", maxAge = 3600)
@RequestMapping("/ws/chat")
public class MatrixController extends AbstractController {

	private static Logger LOG = LoggerFactory.getLogger(MatrixController.class);

	{
		LOG.info("{} initialisation.", MatrixController.class.getName());
	}

	@Autowired
	private MatrixHandler matrixHandler;
	
	/**
	 * Send message
	 * 
	 * @param qrcode
	 * @param userId
	 * @param message
	 * @param createRoom toto je len pre testovanie, NESKOR ZMAZAT Z REQUESTU
	 * @return
	 * @throws Exception
	 */
	@ApiOperation(value = "Posle spravu userovi", notes = "Posiela spravu do roomy, ktoru vytvara ak je to potrebne", responseContainer = "Object")
	@RequestMapping(value = "/sendMessage", method = RequestMethod.POST, produces = ControllerConstant.ALL, consumes = ControllerConstant.ALL)
	@ResponseBody
	public String sendMsg(String qrcode, @RequestParam(value="message") String message, @RequestParam("createRoom") boolean createRoom) throws Exception {
		LOG.info("MatrixController - sendMsg - qrcode {}, message - {}, createRoom - {}", qrcode, message, createRoom);
		return matrixHandler.sendMsg(qrcode, message, createRoom);
	}

	@ApiOperation(value = "Pozve uzivatela do chatu", notes = "", responseContainer = "Object")
	@RequestMapping(value = "/invite", method = RequestMethod.POST, produces = ControllerConstant.ALL, consumes = ControllerConstant.ALL)
	@ResponseBody
	public String invite(String qrcode, String userName) throws Exception {
		LOG.info("MatrixController - invite - qrcode {}, userName - {}", qrcode, userName);
		return matrixHandler.invite(qrcode, userName);
	}
	
	@ApiOperation(value = "Ulozi matrix room", notes = "", responseContainer = "Object")
	@RequestMapping(value = "/saveMatrixRoom", method = RequestMethod.POST, consumes = ControllerConstant.ALL)
	@ResponseBody
	public void saveMatrixRoom(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, String userUuid, String qrUuid, String matrixRoomId, HttpServletResponse httpResponse) throws NotValidSessionException, PermissionDeniedException {
		LOG.info("MatrixController - saveMatrixRoom - token {}, userUuid - {}, qrUuid - {}, matrixRoomId - {}", token, userUuid, qrUuid, matrixRoomId);
		matrixHandler.save(token, userUuid, qrUuid, matrixRoomId, httpResponse);
	}
	
	@ApiOperation(value = "Ziska matrix room id", notes = "", responseContainer = "Object")
	@RequestMapping(value = "/getMatrixRoom", method = RequestMethod.POST, produces = ControllerConstant.ALL, consumes = ControllerConstant.ALL)
	@ResponseBody
	public String getMatrixRoom(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, String userUuid, String qrUuid, HttpServletResponse httpResponse) throws NotValidSessionException, PermissionDeniedException {
		LOG.info("MatrixController - getMatrixRoom - token {}, userUuid - {}, qrUuid - {}", token, userUuid, qrUuid);
		return matrixHandler.getMatrixRoom(token, userUuid, qrUuid, httpResponse);
	}

	@ApiOperation(value = "Registruje matrix usera", notes = "", responseContainer = "Object")
	@RequestMapping(value = "/registerMatrixUser", method = RequestMethod.POST, produces = ControllerConstant.ALL, consumes = ControllerConstant.ALL)
	@ResponseBody
	public String registerMatrixUser(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, String userUuid, HttpServletResponse httpResponse) throws NotValidSessionException, PermissionDeniedException {
		LOG.info("MatrixController - registerMatrixUser - token {}, userUuid - {}", token, userUuid);
		//		return matrixHandler.registerMatrixUser(token, userUuid, httpResponse);
		return ""; //TODO
	}
	
	/**
	 * ziska zoznam qr kodov kde je owner/shared user a vytiahne room qrcodeuuid=matrixqruuid
	 * @param token
	 * @param httpResponse
	 * @return
	 * @throws Exception 
	 */
	@ApiOperation(value = "Ziska zoznam vsetkych roomov, ktore user spravuje(je shared/owner) a dalsie kde si pise", notes = "Ziska zoznam vsetkych roomov, ktore user spravuje(je shared/owner) a dalsie kde si pise", responseContainer = "Object")
	@RequestMapping(value = "/loadAdminRooms", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.ALL)
	@ResponseBody
	public List<MatrixResponseObject> loadAdminRooms(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, HttpServletResponse httpResponse) throws Exception {
		LOG.info("MatrixController - loadAdminRooms - token {}", token);
		return matrixHandler.loadAdminRooms(token, httpResponse);
	}
	
	@ApiOperation(value = "TESTING  getEmailsFromUsersOfMatrixRoomQrCode", notes = "TESTING getEmailsFromUsersOfMatrixRoomQrCode", responseContainer = "Object")
	@RequestMapping(value = "/getEmailsFromUsersOfMatrixRoomQrCode", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.ALL)
	@ResponseBody
	public List<String> getEmailsFromUsersOfMatrixRoomQrCode(String matrixRoomId, String userEmail) {
		LOG.info("MatrixController - getEmailsFromUsersOfMatrixRoomQrCode - matrixRoomId {}, userEmail - {}", matrixRoomId, userEmail);
		return matrixHandler.getEmailsFromUsersOfMatrixRoomQrCode(matrixRoomId, userEmail);
	}
	
}
