package io.dpm.dropmenote.ws.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.dpm.dropmenote.db.entity.QRCodeEntity;
import io.dpm.dropmenote.db.entity.SharedUserEntity;
import io.dpm.dropmenote.db.entity.UserEntity;
import io.dpm.dropmenote.db.repository.QRCodeRepository;
import io.dpm.dropmenote.db.repository.SharedUserRepository;
import io.dpm.dropmenote.db.repository.UserRepository;
import io.dpm.dropmenote.ws.bean.QRCodeBean;
import io.dpm.dropmenote.ws.bean.QRCodeListBean;
import io.dpm.dropmenote.ws.bean.SharedUser;
import io.dpm.dropmenote.ws.bean.UserBean;
import io.dpm.dropmenote.ws.controller.rrbean.QRCodeBeanResponse;
import io.dpm.dropmenote.ws.dto.QRCodeDto;
import io.dpm.dropmenote.ws.dto.QRCodeListDto;
import io.dpm.dropmenote.ws.dto.SharedUserDto;
import io.dpm.dropmenote.ws.dto.UserDto;
import io.dpm.dropmenote.ws.enums.ImageTypeEnum;
import io.dpm.dropmenote.ws.enums.UserTypeEnum;
import io.dpm.dropmenote.ws.exception.EntityDoesntExistException;
import io.dpm.dropmenote.ws.exception.MatrixException;
import io.dpm.dropmenote.ws.exception.NotValidSessionException;
import io.dpm.dropmenote.ws.exception.PermissionDeniedException;
import io.dpm.dropmenote.ws.services.helpers.ImageHelper;
import io.dpm.dropmenote.ws.utils.EnumUtil;
import io.dpm.dropmenote.ws.utils.QRCodeUtil;

/**
 * 
 * @author Martin Jurek (Starbug s.r.o. | https://www.strabug.eu)
 *
 */
@Service
public class QRCodeService {

	private static Logger LOG = LoggerFactory.getLogger(QRCodeService.class);
	private static final List<String> IMAGE_TYPES = EnumUtil.contenet(ImageTypeEnum.BASE64_BMP_PREFIX, ImageTypeEnum.BASE64_JPEG_PREFIX,
			ImageTypeEnum.BASE64_JPG_PREFIX, ImageTypeEnum.BASE64_PNG_PREFIX); 

	{
		LOG.debug("{} initialisation.", QRCodeService.class.getName());
	}
	
	
	
    @Value("${image.file.path}")
    private String imageFilePath;
    
    @Value("${image.file.url}")
    private String imageFileUrl;
    
    @Value("${web.app.url}")
    private String webAppUrl;
    
    @Value("${web.app.urlparam.scan}")
    private String webAppUrlParamScan;

	@Autowired
	private QRCodeRepository qrCodeRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SharedUserRepository sharedUserRepository;
	
	@Autowired
	private QRCodeListService qrcodeListService;

	@Autowired
	private MatrixService matrixService;

	@Autowired
	private KafkaService<?, ?> kafkaService;
	
	// ------------------------
	// ------------------------
	// DB STORAGE OPERRATION
	// ------------------------
	// ------------------------
	/**
	 * 
	 * @param qrCodeBean
	 * @return
	 */
	public QRCodeBean save(QRCodeBean qrCodeBean) {
		// save base64 img, and set img's url for response
		//TODO v nutry sa sa to porovnava a robi sa replace, bolo by dobre tu vratit aku hodnotu naslo a v nutry
		//len spravit replace alebo inak zeefektivnit
		if (IMAGE_TYPES.stream().anyMatch(qrCodeBean.getPhoto()::contains)) {
			String fileName = ImageHelper.saveImage(qrCodeBean.getPhoto(), "qr_code", imageFilePath, "lastfilename");
			LOG.debug("get photo obsahuje IMAGE_TYPES : file name {}", fileName);
			qrCodeBean.setPhoto(fileName);
		} else {
			if (qrCodeBean.getPhoto().contains("http")) {
				var s = qrCodeBean.getPhoto().replaceAll(imageFileUrl, "");
				LOG.debug("FILE obsahuje HTTP , file name {}", s);
				qrCodeBean.setPhoto(s);
			} else {
				LOG.debug("FILE NEOBSAHUJE HTTP , file name");
				qrCodeBean.setPhoto(""); //set default pic because photo is NOT NULL in DB
			}
		}
	
		QRCodeEntity qrCodeEntity = QRCodeDto.convertToEntity(qrCodeBean);
			
		// TODO toto generovanie je riziko ze trafime rovnaky kluc. treba to osefovat spravne. cize random musi pozerat ci take id neexisuje v db
		// new value, generate token
		// FIXME toto sa da spravit aj cez DB dako pekne, zatial takto na tvrdo
		if (qrCodeEntity.getId() == 0) {
			if (qrCodeEntity.getUuid() == null || qrCodeEntity.getUuid().isBlank() || qrCodeEntity.getUuid().equals("undefined")) {
				qrCodeEntity.setUuid(RandomStringUtils.random(20, true, true)); //24 length of customer qr codes uuid, 20 for less dense generated QR
			}
		}
		
		// Add to QR Code list table
		QRCodeListBean qrListBean = qrcodeListService.loadByUuid(qrCodeEntity.getUuid());
		if (qrListBean == null) {
			qrListBean = new QRCodeListBean();
			qrListBean = QRCodeListDto.mapInto(qrCodeEntity);
			qrListBean.setId(0);
			qrcodeListService.save(qrListBean);
		}

		QRCodeBean codeBean = QRCodeDto.convertToBean(qrCodeRepository.save(qrCodeEntity));
		kafkaService.produce(KafkaService.TOPIC.ROOM_DATA, new KafkaService.ROOM_DATA(codeBean.getUuid(),
				codeBean.getPhoto() == null ? "" : codeBean.getPhoto(),
				codeBean.getDescription() == null ? "" : codeBean.getDescription(),
				codeBean.getName() == null ? "" : codeBean.getName()));
		return codeBean;
	}

	/**
	 * Load by ID
	 * 
	 * @param qrCodeId
	 * @return
	 */
	public QRCodeBean load(long qrCodeId) {
		QRCodeBean bean = QRCodeDto.convertToBean(qrCodeRepository.findOne(qrCodeId));
		// set img as url
		if (bean.getPhoto() != null && !bean.getPhoto().isBlank()) {
			LOG.debug("load file long qrcode, file name {}, {}",imageFileUrl, bean.getPhoto());
			bean.setPhoto(imageFileUrl + bean.getPhoto());
		}
		return bean;
	}

	/**
	 * Load by UUID
	 * 
	 * @param qrCodeUuid
	 * @return
	 */
	public QRCodeBean load(String qrCodeUuid) {
		QRCodeBean bean = QRCodeDto.convertToBean(qrCodeRepository.findByUuid(qrCodeUuid));
		// set img as url
		if (bean == null) {
			return null;
		}
		// Load shared users
		List<SharedUserEntity> shares = sharedUserRepository.findByQrCodeId(bean.getId());
		if(!shares.isEmpty()) {
			shares.forEach((share)->{
				bean.getSharedUsers().add(UserDto.convertToBean(share.getShareUser()));
			});
		}
		
		if (bean.getPhoto() != null && !bean.getPhoto().isBlank()) {
			LOG.debug("load file string qrcode, file name {}, {}",imageFileUrl, bean.getPhoto());
			bean.setPhoto(imageFileUrl + bean.getPhoto());
		}
		return bean;
	}
	
	/**
	 * load all by user id for qrcodelist screen
	 * @param userId
	 * @return
	 */
	public List<QRCodeBeanResponse> loadAllQRResponseBeanByUserId(long userId) {
		// load qr list
		List<QRCodeBean> qrList = loadAllByUserId(userId);
		List<QRCodeBeanResponse> response = QRCodeDto.mapInto(new ArrayList<QRCodeBeanResponse>(), qrList);
		response.stream().forEach(q -> q.setUserType(UserTypeEnum.ADMIN));
		
		// load shared qr list and set flag 'shared'
		List<QRCodeBean> qrSharedList = loadBySharedUserId(userId);
		List<QRCodeBeanResponse> sharedResponse = QRCodeDto.mapInto(new ArrayList<QRCodeBeanResponse>(), qrSharedList);
		sharedResponse.stream().forEach(s -> s.setUserType(UserTypeEnum.SHARED));
		response.addAll(sharedResponse);
		qrList.addAll(qrSharedList);
		
		// get hashmap < qrUuid, roomCount >
		HashMap<String, Long> roomsCountList = matrixService.getRoomsCountMapByListOfQr(qrList);

		for (QRCodeBeanResponse qr : response) {
			// set roomCount to response bean
			Long roomsCount = roomsCountList.get(qr.getUuid());
			if (roomsCount != null) {
				qr.setRoomsCount(roomsCount.intValue());
			}
		}
		return response;
	}

	/**
	 * load list of owned/shared qr codes by user id
	 * @param userId
	 * @return
	 */
	public List<QRCodeBean> loadBySharedUserId(long userId) {
		List<QRCodeBean> beanList = QRCodeDto.convertToBean(sharedUserRepository.findQrCodesBySharedUserId(userId));
		beanList.stream().forEach(qrcode -> {
			if (qrcode.getPhoto() != null && !qrcode.getPhoto().isBlank()) {
				LOG.debug("load by shared user id, file name {}, {}",imageFileUrl, qrcode.getPhoto());
				qrcode.setPhoto(imageFileUrl + qrcode.getPhoto());
			}
		});
		return beanList;
	}

	/**
	 * 
	 * @param userId
	 * @return
	 */
	public List<QRCodeBean> loadAllByUserId(long userId) {
		List<QRCodeBean> beanList = QRCodeDto.convertToBean(qrCodeRepository.findByOwnerId(userId));
		beanList.stream().forEach(qrcode -> {
			if (qrcode.getPhoto() != null && !qrcode.getPhoto().isBlank()) {
				LOG.debug("load all by user id {}, {}",imageFileUrl, qrcode.getPhoto());
				qrcode.setPhoto(imageFileUrl + qrcode.getPhoto());
			}});
		return beanList;
	}

	/**
	 * 
	 * @param qrCodeId
	 * @param userToShareLogin
	 * @throws PermissionDeniedException
	 * @throws EntityDoesntExistException 
	 */
	public void addShare(long qrCodeId, String userToShareLogin, long ownerUserId) throws PermissionDeniedException, EntityDoesntExistException, Exception {
		// kontrola existencie rovnakeho zaznamu v blackliste
		SharedUserEntity sharedUserEntity = sharedUserRepository.findByOwnerIdAndQrCodeIdAndShareUserLogin(ownerUserId, qrCodeId, userToShareLogin);

		if (sharedUserEntity != null) {
			LOG.warn("It is already shared! User.login:" + userToShareLogin);
			throw new PermissionDeniedException("Defined user is already in the share list.");
		} else {
			sharedUserEntity = new SharedUserEntity();
		}

		// User to share
		UserEntity userToShareEntity = userRepository.findByLogin(userToShareLogin);
		if(userToShareEntity == null) {
			throw new EntityDoesntExistException("User to share doesn't exist. Invalid login.");
		}
		
		
		sharedUserEntity.setShareUser(userToShareEntity);
		// Qr code
		QRCodeEntity qrCodeEntity = qrCodeRepository.findOne(qrCodeId);
		sharedUserEntity.setQrCode(qrCodeEntity);
		// kontrola ownera qr kodu
		if (qrCodeEntity.getOwner().getId() == userToShareEntity.getId()) {
			LOG.warn("Can't share to the admin! QrCode.Owner.Id:" + qrCodeEntity.getOwner().getId());
			throw new PermissionDeniedException("Can't share to the admin.");
		}

		UserEntity ownerUserEntity = userRepository.findOne(ownerUserId);
		sharedUserEntity.setOwner(ownerUserEntity);
		
		
		try {
			matrixService.shareQRcode(SharedUserDto.convertToBean(sharedUserEntity));
		} catch (Exception e) {
			LOG.warn("Matrix Error. Can't share! QrCode.Owner.Id:" + qrCodeEntity.getOwner().getId());
			throw new MatrixException("Can't share! MX Connection error!", e);
		}

		sharedUserRepository.save(sharedUserEntity);
	}

	/**
	 * 
	 * @param qrCodeId
	 * @return
	 */
	public List<UserBean> loadShares(long qrCodeId) {
		// nacitanie len user bean z SharedUserEntity
		return UserDto.convertToBean(sharedUserRepository.findByQrCodeId(qrCodeId).stream().map(p -> p.getShareUser()).collect(Collectors.toList()));
	}

	/**
	 * 
	 * @param qrCodeId
	 * @param shareUserLogin
	 * @throws Exception 
	 */
	@Transactional
	public void removeShare(long qrCodeId, String shareUserLogin) throws MatrixException {
		SharedUserEntity sharedEnt = sharedUserRepository.findByQrCodeIdAndShareUserLogin(qrCodeId, shareUserLogin);
		try {
			matrixService.unshareQRcode(SharedUserDto.convertToBean(sharedEnt));
		} catch (Exception e) {
			LOG.warn("Remove share failed for shareUser: " + sharedEnt.getId());
			throw new MatrixException("Remove of shared user failed!", e);
		}
		sharedUserRepository.deleteByQrCodeIdAndShareUserLogin(qrCodeId,  shareUserLogin);
	}

	/**
	 * 
	 * @param qrCodeId
	 * @param width
	 * @param height
	 * @return
	 * @throws NotValidSessionException
	 * @throws PermissionDeniedException
	 */
	public byte[] generate(QRCodeBean qrCodeBean, int width, int height) {
		return QRCodeUtil.generate(webAppUrl + webAppUrlParamScan + qrCodeBean.getUuid(), width, height);
	}

	/**
	 * 
	 * @param qrCodeId
	 */
	public void remove(long qrCodeId) {
		qrCodeRepository.delete(qrCodeId);
	}

	/**
	 * select all qrcodes by uuid from list
	 * @param qrcodeUuidList
	 * @return
	 */
	public List<QRCodeBean> loadAllByListOfUuid(List<String> qrcodeUuidList) {
		if (qrcodeUuidList == null || qrcodeUuidList.isEmpty()) {
			return new ArrayList<>();
		}
		List<QRCodeBean> qrBeanList = QRCodeDto.convertToBean(qrCodeRepository.findByUuidIn(qrcodeUuidList));
		// Set image url
		qrBeanList.stream().forEach(qrcode -> {
			if (qrcode.getPhoto() != null && !qrcode.getPhoto().isBlank()) {
				LOG.debug("load all by list of uuid {}, {}",imageFileUrl, qrcode.getPhoto());
				qrcode.setPhoto(imageFileUrl + qrcode.getPhoto());
			}
		});
		return qrBeanList;
	}

	/**
	 * load List<SharedUser>
	 * @param ownerId
	 * @param sharedUserId
	 * @return
	 */
	public List<SharedUser> loadAllBySharedUserId(long sharedUserId) {
		List<SharedUser> sharedList = SharedUserDto.convertToBean(sharedUserRepository.findByShareUserId(sharedUserId));
		// Set image url
		sharedList.stream().forEach(shared -> {
			if (shared.getQrCode().getPhoto() != null && !shared.getQrCode().getPhoto().isBlank()) {
				LOG.debug("load all by shared user id {}, {}",imageFileUrl, shared.getQrCode().getPhoto());
				shared.getQrCode().setPhoto(imageFileUrl + shared.getQrCode().getPhoto());
			}
		});
		return sharedList;
	}

	/**
	 * load by user uuid or user_uuid from matrix
	 * @param uuid
	 * @return
	 */
	public List<QRCodeBean> loadAllForInboxFilter(long userId, String userUuid) {
		List<QRCodeBean> qrList2 = QRCodeDto.convertToBean(qrCodeRepository.findByUserUuidFromMatrixNotEmpty(userUuid));
		List<QRCodeBean> qrList = QRCodeDto.convertToBean(qrCodeRepository.findBySharedUserUuidFromMatrixNotEmpty(userUuid));
		qrList.addAll(qrList2);
		qrList = qrList.stream().distinct().collect(Collectors.toList());
		
		// Set image url
		qrList.stream().forEach(qr -> {
			if (qr.getPhoto() != null && !qr.getPhoto().isBlank()) {
				LOG.debug("load all for inbox filter {}, {}",imageFileUrl, qr.getPhoto());
				qr.setPhoto(imageFileUrl + qr.getPhoto());
			}
		});
		return qrList;
	}

	/**
	 * load photo by uuid
	 * @param qrCodeUuid
	 * @return
	 */
	public String loadPhotoByUuid(String qrCodeUuid) {
		return qrCodeRepository.getPhotoByUuid(qrCodeUuid);
	}


	/**
	 * find qr.admin alias by matrixroomid
	 * @param matrixRoomId
	 * @return
	 */
	public String loadAdminAliasByMatrixRoomId(String matrixRoomId) {
		return qrCodeRepository.findAdminAliasByMatrixRoomId(matrixRoomId);
	}

	/**
	 * just get name of qrcode for Chat purpose
	 * @param qrcodeId
	 * @return
	 */
	public String loadName(String qrcodeId) {
		return qrCodeRepository.getNameById(qrcodeId);
	}
}
