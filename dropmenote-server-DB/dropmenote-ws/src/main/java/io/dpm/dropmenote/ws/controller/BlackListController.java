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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.dpm.dropmenote.ws.constants.ControllerConstant;
import io.dpm.dropmenote.ws.controller.rrbean.BlackListRequest;
import io.dpm.dropmenote.ws.controller.rrbean.BlackListResponse;
import io.dpm.dropmenote.ws.exception.NotValidSessionException;
import io.dpm.dropmenote.ws.exception.PermissionDeniedException;
import io.dpm.dropmenote.ws.handler.BlackListHandler;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 
 * @author Martin Jurek (Starbug s.r.o. | https://www.strabug.eu)
 *
 */
@Api(value = "BlackListController", description = "BlackListController")
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", maxAge = 3600)
@RequestMapping("/ws/blacklist")
public class BlackListController extends AbstractController{

	private static Logger LOG = LoggerFactory.getLogger(BlackListController.class);

	{
		LOG.debug("{} initialisation.", BlackListController.class.getName());
	}

	@Autowired
	private BlackListHandler blackListHandler;

	@ApiOperation(value = "Servisa nacita blacklist podla uzivatela", notes = "pri ChangeProfile screene", responseContainer = "Object")
	@RequestMapping(value = "/loadBlacklist", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public List<BlackListResponse> loadAll(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		LOG.debug("BlackListController - loadAll - token {}", token);
		return blackListHandler.loadAll(token, httpResposne);
	}

	@ApiOperation(value = "Servisa prida usera do blacklistu", notes = "pri ChangeProfile screene", responseContainer = "Object")
	@RequestMapping(value = "/addToBlacklist", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public BlackListResponse add(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, @RequestBody BlackListRequest blackListRequest, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		LOG.debug("BlackListController - add - token {}", token);
		return blackListHandler.save(token, blackListRequest, httpResposne);
	}

	@ApiOperation(value = "Servisa odstrani usera z blacklistu", notes = "pri ChangeProfile screene", responseContainer = "Object")
	@RequestMapping(value = "/removeFromBlacklist", method = RequestMethod.POST, produces = ControllerConstant.MIME_JSON, consumes = ControllerConstant.MIME_JSON)
	@ResponseBody
	public void remove(@RequestHeader(value = ControllerConstant.TOKEN_HEADER, required = true) String token, String blacklisedUDID, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		LOG.debug("BlackListController - remove - token {}, blacklisedUDID - {}", token, blacklisedUDID);
		blackListHandler.remove(token, blacklisedUDID, httpResposne);
	}

}
