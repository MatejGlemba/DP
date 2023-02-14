package io.dpm.dropmenote.ws.services;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.dpm.dropmenote.db.entity.SessionEntity;
import io.dpm.dropmenote.db.repository.SessionRepository;
import io.dpm.dropmenote.ws.bean.SessionBean;
import io.dpm.dropmenote.ws.bean.UserBean;
import io.dpm.dropmenote.ws.constants.ConfigurationConstant;
import io.dpm.dropmenote.ws.dto.SessionDto;
import io.dpm.dropmenote.ws.dto.UserDto;
import io.dpm.dropmenote.ws.exception.NotValidSessionException;

/**
 * 
 * @author Martin Jurek (Starbug s.r.o. | https://www.strabug.eu)
 *
 */
@Service
public class SessionService {

	private static Logger LOG = LoggerFactory.getLogger(SessionService.class);

	{
		LOG.info("Sessions initialisation.");
	}

	private static final int TOKEN_LENGTH = 232; // length of token string
	private static final long SESSION_SCHEDULER_RATE = 1000 * 60 * 10; // how often old sessions are deleted
	private static final long SESSION_SCHEDULER_INITIAL_DELAY = SESSION_SCHEDULER_RATE; // initial delay before session
																						// invalidation scheduler starts
	private static final long SESSION_LENGTH = 1000 * 60 * 60 * 2; // how long user session is kept
	private static final String TIMESTAMP_PREFIX = "#STARBUG#"; 

	private static HashMap<String, SessionObject> sessionsMap;
	
	@Autowired
	private SessionRepository sessionRepository;

	public SessionService() {
		sessionsMap = new HashMap<>();
	}

	public String generateToken() {
		return RandomStringUtils.random(TOKEN_LENGTH, true, true);
	}

	@Scheduled(initialDelay = SESSION_SCHEDULER_INITIAL_DELAY, fixedRate = SESSION_SCHEDULER_RATE)
	private void deleteInvalidSessions() {
		LOG.info("Deleting invalid sessions");

		Date currentDate = new Date();
		int sessionsCount = sessionsMap.size();
		AtomicInteger removedCount = new AtomicInteger(0);

		// Using iterator to prevent concurent modification exception
		Iterator<Map.Entry<String, SessionObject>> it = sessionsMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, SessionObject> entry = it.next();
			if (entry.getValue().getValidUntil().after(currentDate)) {
				LOG.info("Deleting session {}", entry.getKey());
				it.remove();
				removedCount.getAndIncrement();
			}
		}
		LOG.info("{}/{} sessions deleted", removedCount.get(), sessionsCount);
	}

	/**
	 * @param token
	 * @return true if this map contains a mapping for the specified key
	 */
	public boolean contains(String token) {
		return sessionsMap.containsKey(token);
	}

	/**
	 * @param token
	 * @return the value to which the specified key is mapped, or null if this map
	 *         contains no mapping for the key (session expired)
	 */
	private UserBean get(String token) {
		SessionObject val = sessionsMap.get(token);
		if (val == null) {
			return null;
		} else if (val.getValidUntil().before(new Date())) {
			// session expired
			sessionsMap.remove(token);
			return null;
		} else {
			return val.getUser();
		}
	}
	
	/**
	 * find userBean by token
	 * @param token
	 * @return
	 */
	public UserBean loadUserByToken(String token) {
		return UserDto.convertToBean(sessionRepository.findUserByToken(token));
	}

//	/**
//	 * 
//	 * @param token
//	 * @return
//	 * @throws NotValidSessionException
//	 */
//	public UserBean validateDevel(String token) throws NotValidSessionException {
//		
////		UserBean userBean = get(token);
//		// TODO toto je fake resposne
//		UserBean userBean = userService.load(Long.valueOf(token));
//		if (userBean == null) {
//			throw new NotValidSessionException("User doesn't exist in the session.");
//		}
//		return userBean;
//	}
	
	/**
	 * vyhlada a vrati SessionBean podla tokenu
	 * @param token
	 * @return
	 * @throws NotValidSessionException
	 */
	public SessionBean validate(String token) throws NotValidSessionException {
		//sessionRepository.findAll().forEach(e -> LOG.info("{}, {}", e.getDeviceId(), e.getToken()));
		SessionBean sessionBean = SessionDto.convertToBean(sessionRepository.findByToken(token)); 
		if (sessionBean == null || sessionBean.getId() == 0) {
			throw new NotValidSessionException("User doesn't exist in the session.");
		}
		return sessionBean;
	}
	
	/**
	 * zmeni token v DB
	 * @param sessionBean
	 * @return
	 */
	@Transactional
	public synchronized String generateNewToken(SessionBean sessionBean) throws NotValidSessionException {
		if (sessionBean == null) {
			throw new NotValidSessionException("User doesn't exist in the session.");
		}
		String newToken = generateToken();
		sessionBean.setToken(newToken);
		SessionEntity sessionEntity = SessionDto.convertToEntity(sessionBean);
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		sessionEntity.setToken(sessionEntity.getToken() +  TIMESTAMP_PREFIX + timestamp.getTime());
		sessionRepository.save(sessionEntity);
		return sessionBean.getToken() + TIMESTAMP_PREFIX + timestamp.getTime();
		
	}

	/**
	 * @param value
	 * @return new key token value is stored with
	 */
//	public String add(String sysUserEmail, String sysUserPassword) throws SecurityException {
//		UserBean userBean = UserDto.convertToBean(userRepository.findByLogin(sysUserEmail));
//
//		if (userBean == null) {
//			throw new SecurityException("Login failed! No user found.");
//		}
//		if (!(userBean.getPassword().equals(sysUserPassword))) {
//			throw new SecurityException("Login failed! Password mismatch.");
//		}
//		String newToken = generateToken();
//		sessionsMap.put(newToken, new SessionObject(userBean));
//		return newToken;
//	}
	@Transactional
	public synchronized String save(UserBean userBean, String deviceId) {
		SessionEntity sessionEntity = sessionRepository.findByUserIdAndDeviceId(userBean.getId(), deviceId);
		String newToken = generateToken();
		if (sessionEntity == null) {
			sessionEntity = new SessionEntity();
			sessionEntity.setUser(UserDto.convertToEntity(userBean));
			sessionEntity.setDeviceId(deviceId);
			sessionEntity.setToken(newToken);
		} else {
			sessionEntity.setToken(newToken);
		}
		sessionRepository.save(sessionEntity);
		return newToken;
	}
	
	

	/**
	 * Update user in session. Session is not increased automatically
	 *
	 * @param token
	 * @param value
	 * @return the previous value associated with the specified key, or null if
	 *         there was no mapping for the key.
	 */
	public UserBean update(String token, UserBean value) {
		SessionObject sessionObject = sessionsMap.get(token);
		if (sessionObject == null) {
			return null;
		}
		sessionObject.setUser(value);

		return sessionsMap.replace(token, sessionObject).getUser();
	}

	/**
	 * Update user and extend session length
	 *
	 * @param token
	 * @param value
	 * @return the previous value associated with the specified key, or null if
	 *         there was no mapping for the key.
	 */
	public UserBean updateAndExtendSession(String token, UserBean value) {
		SessionObject sessionObject = sessionsMap.get(token);
		if (sessionObject == null) {
			return null;
		}
		sessionObject.setUser(value);
		extendSessionTime(sessionObject, null);

		return sessionsMap.replace(token, sessionObject).getUser();
	}

	/**
	 * @param token
	 * @param time
	 *            optional time in milliseconds to extend with or null to extend by
	 *            default session length
	 * @return updated user
	 */
	public UserBean extendValidity(String token, Long time) {
		SessionObject user = sessionsMap.get(token);
		if (user == null) {
			return null;
		} else {
			return extendSessionTime(user, time).getUser();
		}
	}

	/**
	 * Increase session validity by desired milliseconds or default session length
	 *
	 * @param sessionObject
	 * @param time
	 *            optional time in milliseconds to extend with or null to extend by
	 *            default session length
	 * @return updated sessionObject
	 */
	private SessionObject extendSessionTime(SessionObject sessionObject, Long time) {
		if (time == null)
			time = SESSION_LENGTH;

		sessionObject.setValidUntil(new Date(sessionObject.getValidUntil().getTime() + time));

		return sessionObject;
	}

	/**
	 * @param token
	 * @return the previous value associated with key, or null if there was no
	 *         mapping for key.
	 */
	public UserBean remove(String token) {
		SessionObject val = sessionsMap.remove(token);
		return val == null ? null : val.getUser();
	}
	
	/**
	 * delete session by token
	 * @param token
	 */
	public void delete(String token) {
		sessionRepository.deleteByToken(token);
	}

	/**
	 * FOR TESTING ONLY
	 * 
	 * @return sessions hashmap
	 * @throws UnsupportedOperationException
	 *             in IS_PRODUCTION
	 */
	public HashMap<String, SessionObject> getAll() throws UnsupportedOperationException {
		if (ConfigurationConstant.IS_PRODUCTION) {
			throw new UnsupportedOperationException("Unavailable");
		}
		return sessionsMap;
	}

	/**
	 * 
	 * @author martinjurek
	 *
	 */
	public static class SessionObject {
		private UserBean user;
		private Date validUntil;

		public SessionObject(UserBean user) {
			this.user = user;
			this.validUntil = new Date(new Date().getTime() + SESSION_LENGTH);
		}

		public UserBean getUser() {
			return user;
		}

		public void setUser(UserBean user) {
			this.user = user;
		}

		public Date getValidUntil() {
			return validUntil;
		}

		public void setValidUntil(Date validUntil) {
			this.validUntil = validUntil;
		}

		@Override
		public String toString() {
			return "SessionObject{" + "user=" + user + ", validUntil=" + validUntil + '}';
		}
	}

}
