package io.dpm.dropmenote.ws.handler;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.dpm.dropmenote.db.enums.ProfileIconEnum;
import io.dpm.dropmenote.db.repository.SessionRepository;
import io.dpm.dropmenote.db.repository.SharedUserRepository;
import io.dpm.dropmenote.ws.bean.QRCodeBean;
import io.dpm.dropmenote.ws.bean.QRCodeListBean;
import io.dpm.dropmenote.ws.bean.SessionBean;
import io.dpm.dropmenote.ws.bean.SharedUser;
import io.dpm.dropmenote.ws.constants.ConfigurationConstant;
import io.dpm.dropmenote.ws.constants.ControllerConstant;
import io.dpm.dropmenote.ws.controller.rrbean.QRCodeBeanResponse;
import io.dpm.dropmenote.ws.controller.rrbean.QRCodeBeanScanResponse;
import io.dpm.dropmenote.ws.controller.rrbean.QRCodeListWrapperResponse;
import io.dpm.dropmenote.ws.controller.rrbean.UserInfoResponse;
import io.dpm.dropmenote.ws.dto.QRCodeDto;
import io.dpm.dropmenote.ws.dto.SessionDto;
import io.dpm.dropmenote.ws.dto.SharedUserDto;
import io.dpm.dropmenote.ws.dto.UserDto;
import io.dpm.dropmenote.ws.enums.UserTypeEnum;
import io.dpm.dropmenote.ws.exception.EmailException;
import io.dpm.dropmenote.ws.exception.EntityDoesntExistException;
import io.dpm.dropmenote.ws.exception.NotValidSessionException;
import io.dpm.dropmenote.ws.exception.PermissionDeniedException;
import io.dpm.dropmenote.ws.handler.interfaces.SessionPermissionValidatorInterface;
import io.dpm.dropmenote.ws.services.BlacklistService;
import io.dpm.dropmenote.ws.services.EmailService;
import io.dpm.dropmenote.ws.services.QRCodeListService;
import io.dpm.dropmenote.ws.services.QRCodeService;

@Service
public class QRCodeHandler extends AbstractHandler implements SessionPermissionValidatorInterface {

	private static Logger LOG = LoggerFactory.getLogger(QRCodeHandler.class);

	{
		LOG.info("{} initialisation.", QRCodeHandler.class.getName());
	}

	@Autowired
	private QRCodeService qrCodeService;

	@Autowired
	private EmailService emailService;
	
	@Autowired
	private BlacklistService blacklistService;
	
	@Autowired
	private SharedUserRepository sharedUserRepo;
	
	@Autowired
	private SessionRepository sessionRepository;

	@Autowired
	private QRCodeListService qrcodeListService;
	
	// ------------------------
	// ------------------------
	// DB STORAGE OPERRATION
	// ------------------------
	// ------------------------
	/**
	 * 
	 * @param token
	 * @param request
	 * @return
	 * @throws NotValidSessionException
	 * @throws PermissionDeniedException
	 */
	public QRCodeBeanResponse save(String token, QRCodeBeanResponse request, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, null);

		// Save or update?
		QRCodeBean qrCodeBean;
		if (request.getId() == 0) {
			// Create
			qrCodeBean = new QRCodeBean();
			qrCodeBean.setOwner(sessionBean.getUser());
		} else {
			// Update
			qrCodeBean = qrCodeService.load(request.getId());
		}

		QRCodeDto.mapInto(qrCodeBean, request);
		QRCodeBeanResponse response = QRCodeDto.mapInto(new QRCodeBeanResponse(), qrCodeService.save(qrCodeBean));
		
		// Send generated QR code by email if new qr
		if (qrCodeBean.getId() == 0) {
			qrCodeBean = QRCodeDto.mapInto(qrCodeBean, response);
			QRCodeBean emailQrBean = qrCodeBean;
			new Thread(() -> {
				try {
					emailService.sendQRCode(sessionBean.getUser(), emailQrBean);
				} catch (EmailException e) {
					LOG.info("Email with generated DMN failed to send!");
				}
			}).start();
		}
		
		// Response token
		httpResposne.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
		return response;
	}

	/**
	 * 
	 * @param token
	 * @param qrCodeToken
	 * @return
	 * @throws NotValidSessionException
	 * @throws PermissionDeniedException
	 */
	public QRCodeBeanResponse load(String token, long qrCodeId, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, qrCodeId);

		QRCodeBeanResponse response = QRCodeDto.mapInto(new QRCodeBeanResponse(), qrCodeService.load(qrCodeId));
		
		// Response token
		httpResposne.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
		return response;
	}

	/**
	 * 
	 * @param qrCodeToken
	 * @return
	 * @throws EntityDoesntExistException
	 * @throws NotValidSessionException 
	 */
	public QRCodeBeanScanResponse scan(String qrCodeToken, String userToken, HttpServletResponse httpResponse) throws EntityDoesntExistException, NotValidSessionException {
		QRCodeBean qrCodeBean = qrCodeService.load(qrCodeToken);
		if (qrCodeBean == null) {
			QRCodeListBean qrCodeListBean = qrcodeListService.loadByUuid(qrCodeToken);
			if (qrCodeListBean != null) {
				QRCodeBeanScanResponse response = new QRCodeBeanScanResponse();
				response.setId(0);
				response.setUuid(qrCodeToken);
				response.setUserType(UserTypeEnum.ADMIN);
				SessionBean sessionBean = SessionDto.convertToBean(sessionRepository.findByToken(userToken)); 
				if (sessionBean == null) { 
					throw new NotValidSessionException("Not valid session.");
				}
				httpResponse.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
				return response;
			} else {
				throw new EntityDoesntExistException("Scanned DMN code (" + qrCodeToken + ") doesn't exist.");
			}
		} else {
			if (!qrCodeBean.isActive()) {
				throw new EntityDoesntExistException("Scanned DMN code (" + qrCodeToken + ") is inactive.");
			}
		}
		QRCodeBeanResponse qrcodeResponse = QRCodeDto.mapInto(new QRCodeBeanResponse(), qrCodeBean);
		QRCodeBeanScanResponse response = new QRCodeBeanScanResponse();
		response = QRCodeDto.mapInto(response, qrcodeResponse);

		// If user exist based on token, its Shared/Admin, otherwise Guest
		SessionBean sessionBean = SessionDto.convertToBean(sessionRepository.findByToken(userToken)); 
		boolean isBlacklisted = false;
		if (sessionBean != null) {
			if (qrCodeBean.getOwner().getId() == sessionBean.getUser().getId()) {
				response.setUserType(UserTypeEnum.ADMIN);
			} else {
				// Get list of shared users of qrcode
				List<SharedUser> sharedList = SharedUserDto.convertToBean(sharedUserRepo.findByQrCodeId(qrCodeBean.getId()));
				for (SharedUser u : sharedList) {
					if (u.getOwner().getId() == sessionBean.getUser().getId()) {
						response.setUserType(UserTypeEnum.ADMIN);
						break;
					} else {
						if (u.getShareUser().getId() == sessionBean.getUser().getId()) {
							response.setUserType(UserTypeEnum.SHARED);
							break;
						}
					}
				}
			}
			if (response.getUserType() == null) {
				response.setUserType(UserTypeEnum.GUEST);
			}
			isBlacklisted = blacklistService.existsByUuidAndOwnerId(sessionBean.getUser().getUuid(), qrCodeBean.getOwner().getId());
			httpResponse.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
		} else {
			isBlacklisted = blacklistService.existsByUuidAndOwnerId(userToken, qrCodeBean.getOwner().getId());
			response.setUserType(UserTypeEnum.GUEST);
		}
		response.setBlocked(isBlacklisted);
		return response;
	}

	/**
	 * 
	 * @param token
	 * @return
	 * @throws Exception 
	 */
	public QRCodeListWrapperResponse loadAll(String token, HttpServletResponse httpResposne) throws Exception {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, null);
		
		// load qr list
		List<QRCodeBeanResponse> qrBeanList = qrCodeService.loadAllQRResponseBeanByUserId(sessionBean.getUser().getId());
		qrBeanList.stream().forEach(customer->{
			String fixedDescr = customer.getDescription().replaceAll("(<([^>]+)>)"," ").replaceAll("\\s{2,}", " ").replaceAll("&#65279;","");
			customer.setDescription(fixedDescr);
			});
		// Response token
		httpResposne.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
		
		/* in case user have active token in cookies, he wont go through login service, but straight to qrcodelist screen, 
			so this will return alias, login info for webapp */
		String chatIcon = sessionBean.getUser().getChatIcon() == null ? ProfileIconEnum.P1.toString() : sessionBean.getUser().getChatIcon().toString();
		UserInfoResponse userInfo = new UserInfoResponse(sessionBean.getUser().getLogin(), sessionBean.getUser().getAlias(), chatIcon);
		return new QRCodeListWrapperResponse(userInfo, qrBeanList);
	}

	/**
	 * 
	 * @param token
	 * @param qrCodeId
	 * @throws NotValidSessionException
	 * @throws PermissionDeniedException
	 */
	public void remove(String token, long qrCodeId, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, qrCodeId);

		qrCodeService.remove(qrCodeId);
		
		// Response token
		httpResposne.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
	}

	// ------------------------
	// ------------------------
	// QR CODE SHARE
	// ------------------------
	// ------------------------
	public void addShare(String token, long qrCodeId, String userLogin, HttpServletResponse httpResposne) throws Exception {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, qrCodeId);

		qrCodeService.addShare(qrCodeId, userLogin, sessionBean.getUser().getId());
		
		// Response token
		httpResposne.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
	}

	/**
	 * 
	 * @param token
	 * @param qrCodeId
	 * @return
	 * @throws NotValidSessionException
	 * @throws PermissionDeniedException
	 */
	public List<String> loadShares(String token, long qrCodeId, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, qrCodeId);

		List<String> response =  UserDto.mapInto(new ArrayList<>(), qrCodeService.loadShares(qrCodeId));
		
		// Response token
		httpResposne.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
		return response;
	}

	/**
	 * 
	 * @param token
	 * @param qrCodeId
	 * @param sharedUDID
	 * @return
	 * @throws Exception 
	 */
	public void removeShare(String token, long qrCodeId, String shareUserLogin, HttpServletResponse httpResposne) throws Exception		 {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, qrCodeId);

		qrCodeService.removeShare(qrCodeId, shareUserLogin);
		
		// Response token
		httpResposne.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
	}

	// ------------------------
	// ------------------------
	// QR CODE GENERATOR
	// ------------------------
	// ------------------------
	public byte[] generate(String token, long qrCodeId, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException, EmailException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, qrCodeId);

		QRCodeBean qrCodeBean = qrCodeService.load(qrCodeId);

		// Send generated QR code by email
		emailService.sendQRCode(sessionBean.getUser(), qrCodeBean);

		byte[] response = qrCodeService.generate(qrCodeBean, ConfigurationConstant.QRCODE_WIDTH_MEDIUM, ConfigurationConstant.QRCODE_HEIGHT_MEDIUM);
		
		// Response token
		httpResposne.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
		return response;
	}

	@Override
	public SessionBean validateSessionAndPermission(String token, Long qrCodeId) throws NotValidSessionException, PermissionDeniedException {
		SessionBean sessionBean = sessionService.validate(token);

		if (qrCodeId != null) {
			// Permission validation
			permissionValidatorService.validate(sessionBean.getUser(), qrCodeService.load(qrCodeId));
		}
		return sessionBean;
	}

	/**
	 * loadne qr kody pre filter v inboxe
	 * @param token
	 * @param httpResposne
	 * @return
	 * @throws PermissionDeniedException 
	 * @throws NotValidSessionException 
	 */
	public List<QRCodeBeanResponse> loadAllForInboxFilter(String token, HttpServletResponse httpResposne) throws NotValidSessionException, PermissionDeniedException {
		// Session validation
		SessionBean sessionBean = validateSessionAndPermission(token, null);
		
		List<QRCodeBean> qrList = qrCodeService.loadAllForInboxFilter(sessionBean.getUser().getId(), sessionBean.getUser().getUuid());
		List<QRCodeBeanResponse> response = QRCodeDto.mapInto(new ArrayList<QRCodeBeanResponse>(), qrList);
		response.stream().forEach(customer->{
			String fixedDescr = customer.getDescription().replaceAll("(<([^>]+)>)"," ").replaceAll("\\s{2,}", " ").replaceAll("&#65279;","");
			customer.setDescription(fixedDescr);
			});

//		response.stream().forEach(q -> {
//			if (qrList.stream().anyMatch(bean -> bean.getOwner().getId() == sessionBean.getUser().getId())) {
//				q.setUserType(UserTypeEnum.ADMIN);
//			}
//		});
		// Response token
		httpResposne.setHeader(ControllerConstant.TOKEN_HEADER, sessionService.generateNewToken(sessionBean));
		return response;
	}

	/**
	 * 
	 * @param filepath
	 * @return
	 */
	public List<QRCodeListBean> importFromCSV(MultipartFile file) {
		return qrcodeListService.importFromCSV(file);
	}

	/**
	 * dont change session token, just load name, for Chat
	 * @param qrcodeId
	 * @param token
	 * @param httpResposne
	 * @return
	 * @throws NotValidSessionException 
	 */
	public String loadName(String qrcodeId, HttpServletResponse httpResposne) throws NotValidSessionException {
		return qrCodeService.loadName(qrcodeId);
	}
}
