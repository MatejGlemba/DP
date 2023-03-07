package io.dpm.dropmenote.ws.services;

import java.sql.Timestamp;
import java.util.List;

import javax.transaction.Transactional;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.dpm.dropmenote.db.entity.UserEntity;
import io.dpm.dropmenote.db.enums.ProfileIconEnum;
import io.dpm.dropmenote.db.repository.QRCodeRepository;
import io.dpm.dropmenote.db.repository.UserRepository;
import io.dpm.dropmenote.ws.bean.UserBean;
import io.dpm.dropmenote.ws.constants.ConfigurationConstant;
import io.dpm.dropmenote.ws.dto.UserDto;
import io.dpm.dropmenote.ws.exception.EmailException;
import io.dpm.dropmenote.ws.exception.EntityDoesntExistException;
import io.dpm.dropmenote.ws.exception.MatrixException;
import io.dpm.dropmenote.ws.exception.NonValidLoginInformationException;
import io.dpm.dropmenote.ws.exception.UserAlreadyExistsException;
import io.dpm.dropmenote.ws.utils.MatrixUtil;

/**
 * 
 * @author Martin Jurek (Starbug s.r.o. | https://www.strabug.eu)
 *
 */
@Service
public class UserService {

	private static Logger LOG = LoggerFactory.getLogger(UserService.class);

	{
		LOG.info("{} initialisation.", UserService.class.getName());
	}

	@Value("${image.file.url}")
    private String imageFileUrl;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private QRCodeRepository qrCodeRepository;

	@Autowired
	private EmailService emailService;

	@Autowired
	private MatrixService matrixService;

	/**
	 * 
	 * @param userBean
	 * @return
	 * @throws EmailException
	 */
	public void register(UserBean userBean) throws EmailException, UserAlreadyExistsException, MatrixException {
		if (userRepository.findByLogin(userBean.getLogin()) != null) {
			throw new UserAlreadyExistsException("User already exists.");
		}
		
		userBean.setMatrixPassword(MatrixUtil.generateMatrixPassword());
		userBean.setMatrixUsername(MatrixUtil.generateMatrixUsername());
		userBean.setChatIcon(ProfileIconEnum.P1);
		userBean.setPushNotification(true);
		userBean.setEmailNotification(true);
		
		UserBean storedUserBean = save(userBean);

		try {
			matrixService.registerMatrixUser(ConfigurationConstant.MATRIX_SERVER, userBean.getMatrixUsername(), userBean.getMatrixPassword());
		
			emailService.sendRegistration(storedUserBean);
		}catch(Exception e) {

			// TODO: ak tu nastala chyba, tak mame v DB fantomoveho uzivatela!!!
			
			throw e;
		}
	}

	/**
	 * 
	 * @param login
	 * @param password
	 * @param deviceId
	 * @return
	 */
	public UserBean login(UserBean userBean) throws NonValidLoginInformationException {
		UserEntity userEntity = userRepository.findByLoginAndPassword(userBean.getLogin(), userBean.getPassword());
		if (userEntity == null) {
			throw new NonValidLoginInformationException("Wrong login or password information");
		}
		return UserDto.convertToBean(userEntity);
	}

	/**
	 * 
	 * @param userBean
	 * @return
	 */
	@Transactional
	public synchronized UserBean save(UserBean userBean) {
		UserEntity userEntity = UserDto.convertToEntity(userBean);

		// TODO toto sa musi generovat krajsie
		if (userEntity.getId() == 0) {
			userEntity.setUuid(RandomStringUtils.random(50, true, true));
		}

		userEntity = userRepository.save(userEntity);
		return UserDto.convertToBean(userEntity);
	}

	/**
	 * 
	 * @param userLogin
	 * @throws EmailException
	 * @throws EntityDoesntExistException 
	 */
	public void forgotPassword(String userLogin) throws EmailException, EntityDoesntExistException {
		UserEntity userEntity = userRepository.findByLogin(userLogin);
		if (userEntity == null) {
			throw new EntityDoesntExistException("User login is not valid.");
		}
		userEntity.setRecoveryToken(RandomStringUtils.random(50, true, true));
		userEntity.setRecoveryTokenCreated(new Timestamp(System.currentTimeMillis()));
		userEntity = userRepository.save(userEntity);
		emailService.sendForgotPassword(UserDto.convertToBean(userEntity));
	}

	/**
	 * 
	 * @param userBean
	 * @return
	 */
	public UserBean load(long userId) {
		UserEntity userEntity = userRepository.findOne(userId);
		UserBean bean = UserDto.convertToBean(userEntity);
		//set img url
		bean.setAlias(bean.getAlias() == null ? "Anonymous" : bean.getAlias());
		LOG.info("USER service : load {}, {}",imageFileUrl, bean.getPhoto());
		bean.setPhoto(imageFileUrl + bean.getPhoto());
		return bean;
	}

	/**
	 * 
	 * @param userBean
	 * @return
	 */
	public UserBean loadByRecoveryToken(String emailRecoveryToken) {
		UserEntity userEntity = userRepository.findByRecoveryToken(emailRecoveryToken);
		return UserDto.convertToBean(userEntity);
	}

	/**
	 * 
	 * @param userBean
	 * @return
	 */
	public UserBean loadByLogin(String login) {
		UserEntity userEntity = userRepository.findByLogin(login);
		return UserDto.convertToBean(userEntity);
	}
	
	/**
	 * 
	 * @param userBean
	 * @return
	 */
	public UserBean loadByMatrixUsername(String matrixUsername) {
		UserEntity userEntity = userRepository.findByMatrixUsername(matrixUsername);
		return UserDto.convertToBean(userEntity);
	}

	/**
	 * save username+password to UserEntity with certain userUuid
	 * @param userUuid
	 * @param matrixUsername
	 * @param matrixPassword
	 */
	public void saveMatrixInfo(String userUuid, String matrixUsername, String matrixPassword) {
		userRepository.findByUuid(userUuid).stream().forEach(u -> {
			u.setMatrixUsername(matrixUsername);
			u.setMatrixPassword(matrixPassword);
			userRepository.save(u);
		});
	}

	/**
	 * delete user
	 * @param userId
	 */
	public void delete(long userId) {
//		qrCodeRepository;
//		deviceRepository;
//		blacklistRepository;
//		matrixRepository.deleteByUserUuid();
//		sharedUserRepository.deleteByOwnerUserIdOrSharedUserId(userId, userId);
//		sessionRepository.deleteByUserId(userId);
		userRepository.delete(userId);
	}

	/**
	 * return user Chat icon
	 * @param userUuid
	 * @return
	 */
	public ProfileIconEnum loadUserIcon(String userUuid) {
		return userRepository.findChatIconByUuid(userUuid);
	}

	/**
	 * find alias in user table
	 * @param matrixRoomId
	 * @return
	 */
	public String findAliasByMatrixRoomId(String username, String matrixRoomId) {
		return userRepository.findAliasByMatrixRoomId(username, matrixRoomId);
	}

	public List<UserEntity> loadAll() {
		return userRepository.findAll();
	}
}
