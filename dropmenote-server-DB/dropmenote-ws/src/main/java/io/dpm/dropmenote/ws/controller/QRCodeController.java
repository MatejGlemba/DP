package io.dpm.dropmenote.ws.controller;

import java.util.List;

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
import org.springframework.web.multipart.MultipartFile;

import io.dpm.dropmenote.ws.bean.QRCodeListBean;
import io.dpm.dropmenote.ws.constants.ControllerConstant;
import io.dpm.dropmenote.ws.controller.rrbean.QRCodeBeanResponse;
import io.dpm.dropmenote.ws.controller.rrbean.QRCodeBeanScanResponse;
import io.dpm.dropmenote.ws.controller.rrbean.QRCodeListWrapperResponse;
import io.dpm.dropmenote.ws.exception.EmailException;
import io.dpm.dropmenote.ws.exception.EntityDoesntExistException;
import io.dpm.dropmenote.ws.exception.NotValidSessionException;
import io.dpm.dropmenote.ws.exception.PermissionDeniedException;
import io.dpm.dropmenote.ws.handler.QRCodeHandler;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 
 * @author Martin Jurek (Starbug s.r.o. | https://www.strabug.eu)
 *
 */
@Api(value = "QRCodeController", description = "QRCodeController")
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", maxAge = 3600)
@RequestMapping("/ws/qrcode")
public class QRCodeController extends AbstractController {

	private static Logger LOG = LoggerFactory.getLogger(QRCodeController.class);

	{
		LOG.debug("{} initialisation.", QRCodeController.class.getName());
	}

	@Autowired
	private QRCodeHandler qrCodeHandler;

	// ------------------------
	// ------------------------
	// DB STORAGE OPERRATION
	// ------------------------
	// ------------------------
	@ApiOperation(value = "User register", notes = "pri RegistrationUser screene", responseContainer = "Object")
	@RequestMapping(value = "/save", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public QRCodeBeanResponse save(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, @RequestBody QRCodeBeanResponse request, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		LOG.debug("QRCodeController - save - token {}", token);
		return qrCodeHandler.save(token, request, httpResposne);
	}

	@ApiOperation(value = "Servisa zmeni usera", notes = "pri ChangeProfile screene", responseContainer = "Object")
	@RequestMapping(value = "/load", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public QRCodeBeanResponse load(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, long qrCodeId, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		LOG.debug("QRCodeController - load - token {} , qrCodeId- {}", token, qrCodeId);
		return qrCodeHandler.load(token, qrCodeId, httpResposne);
	}

	@ApiOperation(value = "Servisa zmeni usera", notes = "pri ChangeProfile screene", responseContainer = "Object")
	@RequestMapping(value = "/scan", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public QRCodeBeanScanResponse scan(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = false) String token, String qrCodeToken, HttpServletResponse httpResposne) throws EntityDoesntExistException, NotValidSessionException {
		LOG.debug("QRCodeController - scan - token {}, qrCodeToken - {}", token, qrCodeToken);
		return qrCodeHandler.scan(qrCodeToken, token, httpResposne);
	}
	
	@ApiOperation(value = "Servisa vrati meno qr kodu", notes = "pri chat screene", responseContainer = "Object")
	@RequestMapping(value = "/loadName", method = RequestMethod.POST, produces = ControllerConstant.TEXT_PLAIN, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public String loadName(String qrCodeId, HttpServletResponse httpResposne) throws EntityDoesntExistException, NotValidSessionException {
		LOG.debug("QRCodeController - loadName - qrCodeId - {}", qrCodeId);
		return qrCodeHandler.loadName(qrCodeId, httpResposne);
	}

	@ApiOperation(value = "Servisa zmeni usera", notes = "pri ChangeProfile screene", responseContainer = "Object")
	@RequestMapping(value = "/loadAll", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public QRCodeListWrapperResponse loadAll(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, HttpServletResponse httpResposne) throws Exception {
		LOG.debug("QRCodeController - loadAll - token {}", token);
		return qrCodeHandler.loadAll(token, httpResposne);
	}

	@ApiOperation(value = "Servisa loadne moje qr kody a k tomu qr kody kde som chatoval", notes = "pri ChangeProfile screene", responseContainer = "Object")
	@RequestMapping(value = "/loadAllForInbox", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public List<QRCodeBeanResponse> loadAllForInbox(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		LOG.debug("QRCodeController - loadAllForInbox - token {}", token);
		return qrCodeHandler.loadAllForInboxFilter(token, httpResposne);
	}

	@ApiOperation(value = "Servisa zmeni usera", notes = "pri ChangeProfile screene", responseContainer = "Object")
	@RequestMapping(value = "/remove", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public void remove(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, long qrCodeId, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		LOG.debug("QRCodeController - remove - token {}, qrCodeId {}", token, qrCodeId);
		qrCodeHandler.remove(token, qrCodeId, httpResposne);
	}

	// ------------------------
	// ------------------------
	// QR CODE SHARE
	// ------------------------
	// ------------------------
	@ApiOperation(value = "Servisa zmeni usera", notes = "pri ChangeProfile screene", responseContainer = "Object")
	@RequestMapping(value = "/addShare", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public void addShare(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, long qrCodeId, String shareUserLogin, HttpServletResponse httpResposne) throws Exception {
		LOG.debug("QRCodeController - addShare - token {}, qrCodeId {}, shareUserLogin {}", token, qrCodeId, shareUserLogin);
		qrCodeHandler.addShare(token, qrCodeId, shareUserLogin, httpResposne);

	}

	@ApiOperation(value = "Servisa zmeni usera", notes = "pri ChangeProfile screene", responseContainer = "Object")
	@RequestMapping(value = "/loadShares", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public List<String> loadShares(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, long qrCodeId, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		LOG.debug("QRCodeController - loadShares - token {}, qrCodeId {}", token, qrCodeId);

		return qrCodeHandler.loadShares(token, qrCodeId, httpResposne);
	}

	@ApiOperation(value = "Servisa zmeni usera", notes = "pri ChangeProfile screene", responseContainer = "Object")
	@RequestMapping(value = "/removeShare", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public void removeShare(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, long qrCodeId, String shareUserLogin, HttpServletResponse httpResposne) throws Exception {
		LOG.debug("QRCodeController - removeShare - token {}, qrCodeId {}, shareUserLogin {}", token, qrCodeId, shareUserLogin);
		qrCodeHandler.removeShare(token, qrCodeId, shareUserLogin, httpResposne);
	}
	
	// ------------------------
	// ------------------------
	// QR CODE GENERATOR
	// ------------------------
	// ------------------------
	@ApiOperation(value = "Servisa zmeni usera", notes = "pri ChangeProfile screene", responseContainer = "Object")
	@RequestMapping(value = "/generate", method = RequestMethod.POST, produces = ControllerConstant.ALL, consumes = ControllerConstant.ALL)
	@ResponseBody
	public byte[] generate(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, long qrCodeId, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException, EmailException {
		LOG.debug("QRCodeController - generate - token {}, qrCodeId {}", token, qrCodeId);
		return qrCodeHandler.generate(token, qrCodeId, httpResposne);
	}

	@ApiOperation(value = "import qrcode from CSV", notes = "import qrcode from CSV", responseContainer = "Object")
	@RequestMapping(value = "/importFromCSV", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.ALL)
	@ResponseBody
	public List<QRCodeListBean> importFromCSV(@RequestParam("file") MultipartFile file, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		LOG.debug("QRCodeController - importFromCSV - file {}", file);
		return qrCodeHandler.importFromCSV(file);
	}
}
