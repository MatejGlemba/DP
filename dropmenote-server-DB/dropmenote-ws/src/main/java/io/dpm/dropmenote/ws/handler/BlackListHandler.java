package io.dpm.dropmenote.ws.handler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import io.dpm.dropmenote.ws.services.KafkaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.dpm.dropmenote.ws.bean.BlacklistBean;
import io.dpm.dropmenote.ws.bean.SessionBean;
import io.dpm.dropmenote.ws.constants.ControllerConstant;
import io.dpm.dropmenote.ws.controller.rrbean.BlackListRequest;
import io.dpm.dropmenote.ws.controller.rrbean.BlackListResponse;
import io.dpm.dropmenote.ws.dto.BlackListDto;
import io.dpm.dropmenote.ws.exception.NotValidSessionException;
import io.dpm.dropmenote.ws.exception.PermissionDeniedException;
import io.dpm.dropmenote.ws.handler.interfaces.SessionPermissionValidatorInterface;
import io.dpm.dropmenote.ws.services.BlacklistService;

@Service
public class BlackListHandler extends AbstractHandler implements SessionPermissionValidatorInterface {

	private static Logger LOG = LoggerFactory.getLogger(BlackListHandler.class);

	{
		LOG.debug("{} initialisation.", BlackListHandler.class.getName());
	}

	@Autowired
	private BlacklistService blacklistService;

	@Autowired
	private KafkaService kafkaService;


	/**
	 * 
	 * @param token
	 * @param httpResposne
	 * @return
	 * @throws NotValidSessionException
	 * @throws PermissionDeniedException
	 */
	public List<BlackListResponse> loadAll(String token, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, null);

		List<BlackListResponse> response = BlackListDto.mapInto(new ArrayList<>(), blacklistService.loadAll(sessionBean.getUser().getId()));

		// Response token
		httpResposne.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
		return response;
	}

	/**
	 * 
	 * @param token
	 * @param blacklisedUDID
	 * @return
	 * @throws NotValidSessionException
	 * @throws PermissionDeniedException
	 */
	public BlackListResponse save(String token, BlackListRequest blackListRequest, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, null);

		// update if exists
		BlacklistBean blacklistBean = blacklistService.loadByUuid(blackListRequest.getUuid());
		if (blacklistBean != null) {
			blacklistBean.setAlias(blackListRequest.getAlias());
			blacklistBean.setUpdated(new Date());
			blacklistBean.setNote(blackListRequest.getNote());
		} else {
			blacklistBean = BlackListDto.mapInto(new BlacklistBean(), blackListRequest);
		}
		blacklistBean.setOwner(sessionBean.getUser());
		// poslat do kafky nove blacklist data
		/*
		 * blacklistbean.getOwner().getUserUiid()	:	owner uuid
		 * blacklistbean.getUiid()					:	target user uuid
		 * blacklistbean.getNote()					:	data
		 *
		 * */
		// Response
		BlackListResponse response = BlackListDto.mapInto(new BlackListResponse(), blacklistService.save(blacklistBean));
		httpResposne.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
		return response;
	}

	/**
	 * 
	 * @param token
	 * @param blacklisedUDID
	 * @return
	 * @throws NotValidSessionException
	 * @throws PermissionDeniedException
	 */
	public void remove(String token, String blacklisedUDID, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, 12L/* TODO */);

		blacklistService.removeByUuid(blacklisedUDID);

		// Response token
		httpResposne.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
	}

	@Override
	public SessionBean validateSessionAndPermission(String token, Long blacklistedUDID) throws NotValidSessionException, PermissionDeniedException {
		// SessionBean sessionBean = new SessionBean();
		// // Session validation
		// UserBean userBeanFromSession = sessionService.validateDevel(token);

		// TODO toto nie je premyslene ako budeme kukat security
		// TODO toto nie je premyslene ako budeme kukat security
		// TODO toto nie je premyslene ako budeme kukat security
		// TODO toto nie je premyslene ako budeme kukat security
		// TODO toto nie je premyslene ako budeme kukat security

		SessionBean sessionBean = sessionService.validate(token);

		if (blacklistedUDID != null) {
			// Validate Permission
			permissionValidatorService.validate(sessionBean.getUser(), blacklistService.load(blacklistedUDID));
		}
		return sessionBean;
	}
}
