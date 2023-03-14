package io.dpm.dropmenote.ws.services;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.opencsv.CSVWriter;
import io.dpm.dropmenote.db.entity.UserEntity;
import io.dpm.dropmenote.ws.dto.UserDto;
import io.dpm.matrix.client.regular.MatrixHttpClient;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dpm.dropmenote.db.entity.MatrixEntity;
import io.dpm.dropmenote.db.objects.QrUuidRoomCountPair;
import io.dpm.dropmenote.db.repository.MatrixRepository;
import io.dpm.dropmenote.ws.bean.MatrixBean;
import io.dpm.dropmenote.ws.bean.QRCodeBean;
import io.dpm.dropmenote.ws.bean.SharedUser;
import io.dpm.dropmenote.ws.bean.UserBean;
import io.dpm.dropmenote.ws.constants.ConfigurationConstant;
import io.dpm.dropmenote.ws.controller.rrbean.MatrixResponseObject;
import io.dpm.dropmenote.ws.controller.rrbean.QRCodeBeanResponse;
import io.dpm.dropmenote.ws.dto.MatrixDto;
import io.dpm.dropmenote.ws.dto.QRCodeDto;
import io.dpm.dropmenote.ws.enums.ChatIconPositionEnum;
import io.dpm.dropmenote.ws.enums.UserTypeEnum;
import io.dpm.dropmenote.ws.exception.MatrixException;
import io.dpm.dropmenote.ws.services.helpers.NameGenerationHelper;
import io.dpm.dropmenote.ws.utils.AESCipher;
import io.dpm.dropmenote.ws.utils.MatrixUtil;
import io.dpm.dropmenote.ws.utils.WebSocketUtil;
import io.dpm.dropmenote.ws.websocket.session.ChatSessionInfo;
import io.dpm.dropmenote.ws.websocket.websocketObject.websocketResponse.NotificationTextMessageResponse;
import io.dpm.dropmenote.ws.websocket.websocketObject.websocketResponse.TextMessageResponse;
import io.dpm.matrix.api.MatrixID;
import io.dpm.matrix.client.api.MatrixClient;
import io.dpm.matrix.client.api.SyncData;
import io.dpm.matrix.client.api.SyncData.InvitedRoom;
import io.dpm.matrix.client.api.SyncData.JoinedRoom;
import io.dpm.matrix.client.regular.SyncOptions;
import io.dpm.matrix.event.api.MatrixPersistentEvent;
import io.dpm.matrix.hs.api.MatrixRoom;
import io.dpm.matrix.json.event.MatrixJsonRoomMessageEvent;
import lombok.Data;

/**
 * 
 * @author Husarik (Starbug s.r.o. | https://www.strabug.eu)
 *
 */
@Service
public class MatrixService {

	private static Logger LOG = LoggerFactory.getLogger(MatrixService.class);

	{
		LOG.debug("{} initialisation.", MatrixService.class.getName());
	}

	private static final long THREAD_SLEEP = 200; // polling interval

	@Value("${secret.crypting.key}")
	private String CRYPTING_KEY;
	
    @Value("${image.file.url}")
    private String imageFileUrl;

	@Value("${dump.data}")
	private Boolean dumpdata;

//    @Autowired
//    private QRCodeService qrCodeService;

	@Autowired
	private MatrixRepository matrixRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private ChatSessionInfo chatSessionInfo;

	@Autowired
	private SharedUserService sharedUserService;

	@Autowired
	private BlacklistService blacklistService;

	@Autowired
	private QRCodeService qrcodeService;

	@Autowired
	private KafkaService kafkaService;

	/**
	 * save/update matrix entity
	 * 
	 * @param matrixBean
	 * @return saved entity converted to bean
	 */
	public MatrixBean save(MatrixBean matrixBean) {
		MatrixEntity matrixEntity = MatrixDto.convertToEntity(matrixBean);
		if (matrixEntity == null) {
			return null;
		}
		return MatrixDto.convertToBean(matrixRepository.save(matrixEntity));

	}

	/**
	 * get matrix room id
	 * 
	 * @param userUuid
	 * @param qrUuid
	 * @return
	 */
	public String getMatrixRoomId(String userUuid, String qrUuid) {
		return matrixRepository.findMatrixRoomIdByUserUuidAndQrCodeUuid(userUuid, qrUuid);
	}

	/**
	 * get matrix room id
	 * 
	 * @param userUuid
	 * @param qrUuid
	 * @return
	 */
	public MatrixBean findOneByUserUuidAndQrCodeUuid(String userUuid, String qrUuid) {
		return MatrixDto.convertToBean(matrixRepository.findOneByUserUuidAndQrCodeUuid(userUuid, qrUuid));
	}

	/**
	 * 
	 * @param username
	 * @param password
	 * @return user_id from response
	 * @throws Exception
	 */
	public String registerMatrixUser(String server, String username, String password) throws MatrixException {
		/**
		 * SDK je stare a nema spravnu registraciu preto volame servisu rucne
		 */

		final String uri = "https://" + server + "/_matrix/client/r0/register";
		RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
		// Step1
		MatrixRegisterRequest req = new MatrixRegisterRequest(username, password, null, "m.login.terms");

		MatrixRegisterResponse response = null;
		try {
			LOG.debug("uri {}, server {} , username {}, req {}", uri, server, username, req);
			response = restTemplate.postForObject(uri, req, MatrixRegisterResponse.class);
		} catch (HttpClientErrorException e) {
			String responseBody = e.getResponseBodyAsString();
			if (responseBody.contains("M_USER_IN_USE")) {
				throw new MatrixException("Username already exists", null);
			} else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
				try {
					response = objectMapper.readValue(responseBody, MatrixRegisterResponse.class);
					if (response.getSession() == null && response.getSession().isBlank()) {
						throw new MatrixException("Invalid register flow response step1", null);
					}
					// Step2
					req = new MatrixRegisterRequest(username, password, response.getSession(), "m.login.dummy");
					try {
						response = restTemplate.postForObject(uri, req, MatrixRegisterResponse.class);
						if (response.getUser_id() != null && !response.getUser_id().isBlank()) {
							// Finally OK
							return response.user_id;
						} else {
							LOG.error("Response: " + response.toString());
							throw new MatrixException("Invalid register flow response step2", null);
						}
					} catch (HttpClientErrorException e2) {
						LOG.error("Response: " + e2.getResponseBodyAsString());
						e2.printStackTrace();
						throw new MatrixException("Invalid register flow response step2", e2);
					}
				} catch (IOException e1) {
					LOG.error("Response: " + responseBody);
					e1.printStackTrace();
					throw new MatrixException("Invalid register flow response step1", e1);
				}
			} else {
				LOG.error("Response: " + responseBody);
				e.printStackTrace();
				throw new MatrixException("Invalid register flow response step1", e);
			}
		}

		//
		throw new MatrixException("Invalid register flow!", null);

	}

	public String invite(String qrcode, String userName) throws Exception {
		// Ziskaj moje matrix udaje z DB
		final String server = ConfigurationConstant.MATRIX_SERVER;
		final String user = "peter-s";
		final String pass = "5$24TgJckgk$";

		// Login
		MatrixClient client = MatrixUtil.login(server, user, pass);

		// String roomId = "id z DB podla QR";
		String roomId = qrcode;
		MatrixRoom room = client.getJoinedRooms().stream().filter(r -> r.getAddress().equals(roomId)).findFirst()
				.orElse(null);

		if (room == null) {
			throw new Exception("Nenasla sa rooma, na pozvanie");
		}

		room.invite(MatrixUtil.getMatrixIdObject(userName, server));

		return "ok";
	}

	/**
	 * 
	 * @param qrcode
	 * @param message
	 * @param createRoom
	 * @return
	 * @throws Exception
	 */
	public String sendMsg(String qrcode, String message, boolean createRoom) throws Exception {
		// Ziskaj moje matrix udaje z DB
		final String server = ConfigurationConstant.MATRIX_SERVER;
		final String user = "peter-s";
		final String pass = "5$24TgJckgk$";

		// Login
		MatrixClient client = MatrixUtil.login(server, user, pass);

		// Zisti ci je konverzacia uz vytvorena
		MatrixRoom room;
		if (createRoom) {
			// zisti kto su admini ku qrkodu aby boli automaticky pridany do roomy
			Set<MatrixID> admini = new HashSet<MatrixID>();
			admini.add(MatrixUtil.getMatrixIdObject("peter-s2", server));

			room = MatrixUtil.createRoom(client, admini);
		} else {
			// String roomId = "id z DB podla QR";
			String roomId = qrcode;
			room = client.getJoinedRooms().stream().filter(r -> r.getAddress().equals(roomId)).findFirst().orElse(null);

			if (room == null) {
				throw new Exception("Nenasla sa rooma, ktora uz ma byt vytvorena");
			}
		}

		// Posli spravu
		String eventId = room.sendText(message);

		client.logout();

		return eventId;
	}

	/**
	 * Zisti pocet neprecitanych sprav uzivatela a poslednu prijatu spravu pre
	 * jednotlive matrix roomy
	 * 
	 * @param server   matrix server (ConfigurationConstant.MATRIX_SERVER)
	 * @param user     matrix login
	 * @param password matrix password
	 * @return hashmap of room information for room matrix ID
	 * @throws Exception
	 */
	public HashMap<String, InboxRoomInfo> getUnreadMessaggesInfo(String server, String user, String password)
			throws Exception {
		HashMap<String, InboxRoomInfo> data = new HashMap<>();

		// Login
		MatrixClient client;
		try {
			client = MatrixUtil.login(server, user, password);
		} catch (Exception e) {
			throw new MatrixException("Communication error", e);
		}

		// Filter sa da pouzit na lepsi performance, teraz nam chodia zbitocne data
		String filter = "{" + " \"presence\": { \"types\": [\"none\"] }"
				+ ", \"account_data\": { \"types\": [\"none\"] }" + ", \"room\": {"
				+ " \"state\": { \"types\": [\"none\"] }" + ", \"account_data\": { \"types\": [\"none\"] }"
				+ ", \"ephemeral\": { \"types\": [\"none\"] }"
				+ ", \"timeline\": { \"types\": [\"m.room.message\"], \"limit\": 1 }" + " } " + "}";
		SyncData syncData = client.sync(SyncOptions.build().setFilter(filter).get());
		// System.out.println(syncData.getJson());

		// We check the invited rooms
		try {
			for (InvitedRoom invitedRoom : syncData.getRooms().getInvited()) {
				// We auto-join rooms we are invited to
				client.getRoom(invitedRoom.getId()).join();
			}
		} catch (Exception e) {
			// unknown error
		}

		// We check the joined rooms
		for (JoinedRoom room : syncData.getRooms().getJoined()) {
			List<MatrixPersistentEvent> msgs = room.getTimeline().getEvents().stream()
					.filter(ev -> "m.room.message".contentEquals(ev.getType())).collect(Collectors.toList());
			if (msgs.size() <= 0) {
				// no messages
				continue;
			}

			MatrixJsonRoomMessageEvent lastMsg = new MatrixJsonRoomMessageEvent(msgs.get(msgs.size() - 1).getJson());
			String fromMatrixUsername = lastMsg.getSender().getLocalPart();

			InboxRoomInfo info = new InboxRoomInfo();
			String alias = findAliasByMatrixName(fromMatrixUsername);
			String uuid = findUserUuidByMatrixName(fromMatrixUsername);
			String counterpartName = findCounterpartAlias(user, room.getId());
			String decryptedMsg = AESCipher.decrypt(CRYPTING_KEY, lastMsg.getBody()).getData();
			info.setCounterpartName(counterpartName);
			info.setAlias(alias == null ? "Anonymous" : alias);
			info.setUuid_fingerprint(uuid);
			info.setDate(new Date(lastMsg.getTime()));
			info.setMessage(decryptedMsg);
			info.setUnreadMsgCount((int) room.getUnreadNotifications().getNotificationCount());

			data.put(room.getId(), info);
		}

		return data;
	}

	/**
	 * find alias in matrix table or user table
	 * 
	 * @param matrixRoomId
	 * @param username
	 * @return
	 */
	private String findCounterpartAlias(String username, String matrixRoomId) {
		String matrixName = matrixRepository.findAliasByMatrixRoomId(matrixRoomId);
		if (matrixName != null && !matrixName.equals("")) {
			return matrixName;
		}
		String userName = userService.findAliasByMatrixRoomId(username, matrixRoomId);
		if (userName != null && !userName.equals("")) {
			return userName;
		}
		String adminName = qrcodeService.loadAdminAliasByMatrixRoomId(matrixRoomId);
		if (adminName != null && !adminName.equals("")) {
			return adminName;
		}
		return null;
	}

	@Data
	public class InboxRoomInfo {
		private Date date;
		private String alias;
		/**
		 * user uuid or 'plebs' fingerprint
		 */
		private String uuid_fingerprint;
		private Integer unreadMsgCount;
		private String message;
		private String counterpartName;
	}

	/**
	 * list of rooms by userUuid
	 * 
	 * @param userUuid
	 * @return
	 */
	public List<MatrixBean> loadUserRoomsFromDB(String userUuid) {
		return MatrixDto.convertToBean(matrixRepository.findByUserUuid(userUuid));
	}

	/**
	 * find room by ID
	 * 
	 * @param id
	 * @return
	 */
	public MatrixBean load(long id) {
		return MatrixDto.convertToBean(matrixRepository.findOne(id));
	}

	/**
	 * list of rooms by qrcodeUuid or userUuid
	 * 
	 * @param qrcodeUuidList
	 * @param userUuid
	 * @return
	 */
	public List<MatrixBean> loadRoomsByListOfQrCodeUuidOrUserUuid(List<String> qrcodeUuidList, String userUuid) {
		if (qrcodeUuidList == null || qrcodeUuidList.size() == 0) {
			return MatrixDto.convertToBean(new ArrayList<MatrixEntity>());
		}

		return MatrixDto.convertToBean(matrixRepository.findByQrCodeUuidInOrUserUuid(qrcodeUuidList, userUuid));
	}

	@Data
	static class MatrixRegisterResponse {
		String session;
		Set<String> completed;
		String user_id;
		String home_server;
		String access_token;
		String device_id;

		public MatrixRegisterResponse() {
		}

		@Override
		public String toString() {
			return "Res [session=" + session + ", completed=" + completed + ", user_id=" + user_id + ", home_server="
					+ home_server + ", access_token=" + access_token + ", device_id=" + device_id + "]";
		}
	}

	@Data
	class MatrixRegisterRequest {
		@Data
		class Auth {
			String session;
			String type;

			public Auth(String session, String type) {
				this.session = session;
				this.type = type;
			}
		}

		Auth auth;
		String username;
		String password;
		boolean inhibit_login = false;
		boolean x_show_msisdn = true;

		public MatrixRegisterRequest() {
		}

		public MatrixRegisterRequest(String username, String password, String session, String type) {
			this.username = username;
			this.password = password;
			this.auth = new Auth(session, type);
		}
	}

	/**
	 * Create matrix thread, listen for events and send new messages to notification
	 * websocket session
	 * 
	 * @param matrixUsername
	 * @param matrixPassword
	 * @param matrixServer
	 * @param session
	 * @throws Exception
	 */
	public void createClientAndSendNotificationsToWS(String matrixUsername, String matrixPassword, String matrixServer,
			WebSocketSession session) throws Exception {
		if (!session.isOpen() || matrixUsername.isBlank() || matrixPassword.isBlank() || matrixServer.isBlank()) {
			throw new Exception("invalid params");
		}

		// Login
		MatrixClient client = MatrixUtil.login(matrixServer, matrixUsername, matrixPassword);

		// Filter sa da pouzit na lepsi performance, teraz nam chodia zbitocne data
		String filter = "{" + " \"presence\": { \"types\": [\"none\"] }"
				+ ", \"account_data\": { \"types\": [\"none\"] }" + ", \"room\": {"
				+ " \"state\": { \"types\": [\"none\"] }" + ", \"account_data\": { \"types\": [\"none\"] }"
				+ ", \"ephemeral\": { \"types\": [\"none\"] }"
				+ ", \"timeline\": { \"types\": [\"m.room.message\"], \"limit\": 1 }" + " } " + "}";

		Thread t = new Thread(() -> {
			LOG.debug("Client Notifications Sync thread is running");
			String syncToken = null;
			// hashmap<matrixUsername, UserInfo>
			HashMap<String, UserInfo> matrixUsersMap = new HashMap<>();
			// Musim kontrolovat ci stale je WS session otvorena
			while (!Thread.currentThread().isInterrupted() && session.isOpen()) {
				try {
					SyncData data = client.sync(SyncOptions.build().setFilter(filter).setSince(syncToken).get());

					// We check the invited rooms
					try {
						for (InvitedRoom invitedRoom : data.getRooms().getInvited()) {
							// We auto-join rooms we are invited to
							client.getRoom(invitedRoom.getId()).join();
						}
					} catch (Exception e) {
						// unknown error
					}

					// We check the joined rooms
					for (JoinedRoom joinedRoom : data.getRooms().getJoined()) {
						if (joinedRoom.getUnreadNotifications().getNotificationCount() == 0) {
							continue;
						}

						/**
						 * Pre jednu davku notifikujem len poslednu neprecitanu spravu pre konkretnu
						 * room
						 */

						List<MatrixPersistentEvent> msgs = joinedRoom.getTimeline().getEvents().stream()
								.filter(ev -> "m.room.message".contentEquals(ev.getType())
										&& !matrixUsername.equals(ev.getSender().getLocalPart()))
								.collect(Collectors.toList());
						if (msgs.size() <= 0) {
							// no messages daco je zle
							LOG.warn("Daco je zle, asi nam usla notification");
							continue;
						}

						MatrixRoom matrixRoom = client.getRoom(joinedRoom.getId());
						MatrixJsonRoomMessageEvent msg = new MatrixJsonRoomMessageEvent(
								msgs.get(msgs.size() - 1).getJson());

						MatrixBean matrixBean = loadByMatrixRoomId(matrixRoom.getId());
						UserInfo senderInfo = null;
						String hashmapKeyUsername = msg.getSender().getLocalPart();
						if (matrixUsersMap.containsKey(hashmapKeyUsername)) {
							senderInfo = matrixUsersMap.get(hashmapKeyUsername);
						} else {
							senderInfo = getChatUserInfo(hashmapKeyUsername, null);
							matrixUsersMap.put(hashmapKeyUsername, senderInfo);
						}
						if (senderInfo != null && matrixBean != null) {
							NotificationTextMessageResponse notification = new NotificationTextMessageResponse();
							if (senderInfo.getChatUsername() == null || senderInfo.getChatUsername().equals("")) {
								senderInfo.setChatUsername("Anonymous");
							}
							notification.setFrom(senderInfo.getChatUsername());
							notification.setDate(new Date(msg.getTime()));
							
							if (senderInfo.getUserType() == UserTypeEnum.GUEST) {
								if (senderInfo.userUuid == null) {
									notification.setImage(null);
								} else {
									notification.setImage(userService.loadUserIcon(senderInfo.userUuid).toString());
								}
							} else {
								String qrcodePhoto = qrcodeService.loadPhotoByUuid(matrixBean.getQrCodeUuid());
								if (qrcodePhoto != null && !qrcodePhoto.isBlank()) {
									notification.setImage(imageFileUrl + qrcodePhoto);
								}
							}
							
							String decryptedMsg = AESCipher.decrypt(CRYPTING_KEY, msg.getBody()).getData();
							notification.setMsg(decryptedMsg);
							notification.setPosition(ChatIconPositionEnum.LEFT);
							notification.setQrCodeUuid(matrixBean.getQrCodeUuid());
							notification.setRoomId(matrixBean.getId());
							notification.setUserType(senderInfo.getUserType());
							try {
								session.sendMessage(WebSocketUtil.createWebSocketTextMessage(notification));
								// NEPOSIELAME receipt, lebo potom nikdy neuvidime pocet neprecitanych
								// Send read receipt
								// matrixRoom.sendReadReceipt(msg.getId());
							} catch (IOException ex) {
								ex.printStackTrace();
							}
						} else {
							// daco je zle
						}
					}

					syncToken = data.nextBatchToken();
				} catch (RuntimeException e) {
					LOG.warn("Error during sync", e.getMessage(), e);
				} finally {
					try {
						Thread.sleep(THREAD_SLEEP);
					} catch (InterruptedException e) {
						LOG.debug("Interrupted while waiting for next sync");
					}
				}
			}

			client.logout();
			LOG.debug("Exiting Notifications Client Sync thread");
		});

		t.setName("matrixNotificationsClientSync");
		t.start();
	}

	public MatrixBean loadByMatrixRoomId(String matrixRoomId) {
		return MatrixDto.convertToBean(matrixRepository.findOneByMatrixRoomId(matrixRoomId));
	}

	public List<MatrixBean> loadAllMatrixRoomIds() {
		return MatrixDto.convertToBean(matrixRepository.findAll());
	}

	/**
	 * Find information for matrixUser
	 * 
	 * @param matrixUsername
	 * @return user info object with user chat displayed name and type
	 */
	public UserInfo getChatUserInfo(String matrixUsername, QRCodeBean qrcode) {
		UserInfo userInfo = new UserInfo();
		userInfo.setMatrixUsername(matrixUsername);

		UserBean userBean = userService.loadByMatrixUsername(matrixUsername);
		if (userBean != null) {
			if (userBean.getAlias() != null && !userBean.getAlias().isBlank()) {
				userInfo.setChatUsername(userBean.getAlias());
			} else {
//				userInfo.setChatUsername(userBean.getLogin().hashCode()+"");
				userInfo.setChatUsername("Anonymous");
			}
			if (qrcode != null && qrcode.getOwner().getUuid().equals(userBean.getUuid())) {
				userInfo.setUserType(UserTypeEnum.ADMIN);
			} else {
				userInfo.setUserType(sharedUserService.getSharedOrGuestUserType(qrcode, userBean));
			}
			userInfo.setUserUuid(userBean.getUuid());
			return userInfo;
		}

		MatrixBean matrixBean = MatrixDto.convertToBean(matrixRepository.findOneByMatrixUsername(matrixUsername));
		if (matrixBean != null) {
			userInfo.userType = UserTypeEnum.GUEST;
//			if(matrixBean.getAlias() != null && !matrixBean.getAlias().isBlank()) {
//				userInfo.setChatUsername(matrixBean.getAlias());
//			}else {
			userInfo.setFingerprint(matrixBean.getUserUuid());
//				userInfo.setChatUsername(matrixBean.getMatrixUsername().hashCode()+"");
			userInfo.setChatUsername(matrixBean.getAlias());
//			}
			return userInfo;
		}

		// Toto by nemalo nikdy nastat
		LOG.warn("Nenasiel sa uzivatel s matrix loginom: " + matrixUsername);
		return null;
	}

	/**
	 * Helper class with chat user info
	 * 
	 * @author Peter
	 *
	 */
	@Data
	public static class UserInfo {
		private String matrixUsername;
		private String chatUsername;
		private UserTypeEnum userType;
		private String userUuid;
		private String fingerprint;
	}

	/**
	 * Create matrix room for registered user and save to matrix table
	 * 
	 * @param matrixUsername
	 * @param matrixPassword
	 * @param matrixServer
	 * @param qrCodeBean
	 * @param userBean
	 * @return created matrixBean saved in DB
	 * @throws Exception
	 */
	public synchronized MatrixBean createAndSaveUserMatrixRoom(String matrixUsername, String matrixPassword, String matrixServer,
			QRCodeBean qrCodeBean, UserBean userBean) throws Exception {
		
		// if matrix room exists, return it
		MatrixBean matrixBean = findOneByUserUuidAndQrCodeUuid(userBean.getUuid(), qrCodeBean.getUuid());
		
		if (matrixBean != null) {
			return matrixBean;
		} else {
			// Is UserBean SharedUser?
			if (qrCodeBean.getSharedUsers().stream().anyMatch(user -> user.getId() == userBean.getId())) {
				
				// Get List of SharedUsers Uuids for one QrCodeBean
				List<String> sharedUserUuids = new ArrayList<>();
				qrCodeBean.getSharedUsers().stream().forEach(user -> sharedUserUuids.add(user.getUuid()));
				
				// Find one matrix room shared by all shared users of this qrCodeBean
				if (!sharedUserUuids.isEmpty()) {
					matrixBean = findOneByQrCodeUuidAndListOfUserUuid(qrCodeBean.getUuid(), sharedUserUuids);
				}
				if (matrixBean != null) {
					return matrixBean;
				}
			}
		}
		// Login
		MatrixClient client = MatrixUtil.login(matrixServer, matrixUsername, matrixPassword);

		// Pridaj adminov QR kodu
		Set<MatrixID> admins = new HashSet<MatrixID>();


		qrCodeBean.getSharedUsers().forEach((user) -> {
			if (user.getId() != userBean.getId()) {
				admins.add(MatrixUtil.getMatrixIdObject(user.getMatrixUsername(), matrixServer));
			}
		});
		admins.add((MatrixUtil.getMatrixIdObject(qrCodeBean.getOwner().getMatrixUsername(), matrixServer)));

		// Vytvor
		MatrixRoom room = MatrixUtil.createRoom(client, admins);
		client.logout();

		// Sparuj u nas roomId uzivatela a QR a uloz do DB
		matrixBean = new MatrixBean();
		//
		matrixBean.setMatrixPassword(null);
		matrixBean.setMatrixUsername(null);
		//
		matrixBean.setMatrixRoomId(room.getId());
		matrixBean.setQrCodeUuid(qrCodeBean.getUuid());
		matrixBean.setUserUuid(userBean.getUuid());
		return save(matrixBean);
	}

	/**
	 * find one matrix room searching any matrix room that has user_uuid equal to some uuid from List;
	 * @param sharedUserUuids
	 * @return
	 */
	private MatrixBean findOneByQrCodeUuidAndListOfUserUuid(String qrCodeUuid, List<String> sharedUserUuids) {
		return MatrixDto.convertToBean(matrixRepository.findOneByListOfUserUuid(qrCodeUuid, sharedUserUuids));
	}

	/**
	 * Create matrix room for anonymous user (fingerprint) and save to matrix table
	 *
	 * @param matrixServer
	 * @param fingerprint
	 * @param qrCodeBean
	 * @return created matrixBean saved in DB
	 * @throws Exception
	 */
	public synchronized MatrixBean createAndSaveAnonymousMatrixRoom(String matrixServer, String fingerprint, QRCodeBean qrCodeBean) throws Exception {
		
		// if matrix room exists, return it;
		MatrixBean matrixBean = findOneByUserUuidAndQrCodeUuid(fingerprint, qrCodeBean.getUuid());
    	if(matrixBean != null) {
			return matrixBean;
    	}
    	
    	// Register
    	String pass = MatrixUtil.generateMatrixPassword();
		String login = MatrixUtil.generateMatrixUsername();
		registerMatrixUser(ConfigurationConstant.MATRIX_SERVER, login, pass);
    	
		// Login
		MatrixClient client = MatrixUtil.login(matrixServer, login, pass);

		// Pridaj adminov QR kodu
		Set<MatrixID> admins = new HashSet<MatrixID>();
		qrCodeBean.getSharedUsers().forEach((user) -> {
			admins.add(MatrixUtil.getMatrixIdObject(user.getMatrixUsername(), matrixServer));
		});
		admins.add((MatrixUtil.getMatrixIdObject(qrCodeBean.getOwner().getMatrixUsername(), matrixServer)));

		// Vytvor
		MatrixRoom room = MatrixUtil.createRoom(client, admins);
		client.logout();

		// Sparuj u nas roomId uzivatela a QR a uloz do DB
		matrixBean = new MatrixBean();
		//
		matrixBean.setMatrixPassword(pass);
		matrixBean.setMatrixUsername(login);
		//
		matrixBean.setAlias(NameGenerationHelper.getRandomName(3));
		//
		matrixBean.setMatrixRoomId(room.getId());
		matrixBean.setQrCodeUuid(qrCodeBean.getUuid());
		matrixBean.setUserUuid(fingerprint);
		return save(matrixBean);
	}

	/**
	 * Create matrix thread, listen for events and send new messages to chat
	 * websocket session
	 *
	 * @param matrixServer
	 * @param matrixBean
	 * @param qrCodeBean
	 * @param session
	 * @throws Exception
	 */
	public void createClientAndSendChatToWS(String matrixUsername, String matrixPassword, String matrixServer,
			MatrixBean matrixBean, QRCodeBean qrCodeBean, WebSocketSession session) throws Exception {
		if (!session.isOpen() || matrixUsername == null || matrixUsername.isBlank() || matrixPassword == null
				|| matrixPassword.isBlank() || matrixBean == null || qrCodeBean == null) {
			throw new Exception("invalid params");
		}

		String matrixRoomId = matrixBean.getMatrixRoomId();
		if (dumpdata) {
			new Thread(() -> {
				List<MatrixBean> matrixBeans = loadAllMatrixRoomIds();
				Map<MatrixBean, ImmutablePair<String, String>> credentials = new HashMap<>();
				List<MatrixBean> noExistingCredentials = new ArrayList<>();
				for (MatrixBean mBean : matrixBeans) {
					if (mBean.getMatrixUsername() == null || mBean.getMatrixPassword() == null) {
						List<UserEntity> allById = userService.findAllById(mBean.getUserUuid());
						if (allById.isEmpty()) {
							noExistingCredentials.add(mBean);
						} else {
							credentials.put(mBean, ImmutablePair.of(allById.get(0).getMatrixUsername(), allById.get(0).getMatrixPassword()));
						}
					} else {
						credentials.put(mBean, ImmutablePair.of(mBean.getMatrixUsername(), mBean.getMatrixPassword()));
					}
				}

				List<List<List<String>>> matrixList = new ArrayList<>();
				int numOfEntry = 0;
				for (Map.Entry<MatrixBean, ImmutablePair<String, String>> entry : credentials.entrySet()) {
					LOG.info("------------------------------- Entry num {}", numOfEntry++);
					/*if (numOfEntry == 10) {
						break;
					}*/
					MatrixClient client = null;
					try {
						client = MatrixUtil.login(matrixServer, entry.getValue().getLeft(), entry.getValue().getRight());
					} catch (Exception e) {
						continue;
					}

					String syncToken = null;
					while (!Thread.currentThread().isInterrupted()) {
						try {
							String filter = "{" + " \"presence\": { \"types\": [\"none\"] }"
									+ ", \"account_data\": { \"types\": [\"none\"] }" + ", \"room\": {" + " \"rooms\":[\"" + entry.getKey().getMatrixRoomId()
									+ "\"]" + ", \"state\": { \"types\": [\"none\"] }" + ", \"account_data\": { \"types\": [\"none\"] }"
									+ ", \"ephemeral\": { \"types\": [\"none\"] }"
									+ ", \"timeline\": { \"types\": [\"m.room.message\"] }" + " } " + "}";
							SyncData data = client.sync(SyncOptions.build().setFilter(filter).setSince(syncToken).get());
							JoinedRoom joinedRoom = data.getRooms().getJoined().stream()
									.filter(r -> r.getId().equals(entry.getKey().getMatrixRoomId())).findFirst().orElse(null);
							if (joinedRoom == null || joinedRoom.getTimeline() == null) {
								break;
							}
							List<List<String>> eventList = new ArrayList<>();
							for (MatrixPersistentEvent event : joinedRoom.getTimeline().getEvents()) {
								if ("m.room.message".contentEquals(event.getType())) {
									MatrixJsonRoomMessageEvent msg = new MatrixJsonRoomMessageEvent(event.getJson());
									String hashmapKeyUsername = msg.getSender().getLocalPart();
									var codeBean = qrcodeService.load(entry.getKey().getQrCodeUuid());
									UserInfo senderInfo = getChatUserInfo(hashmapKeyUsername, codeBean);
									String decryptedMsg = AESCipher.decrypt(CRYPTING_KEY, msg.getBody()).getData();
									var userUuid = senderInfo.getUserUuid() == null ? entry.getKey().getUserUuid() : null;
									var qrCodeUuid = codeBean.getUuid() == null ? entry.getKey().getQrCodeUuid() : null;
									var roomId = entry.getKey().getMatrixRoomId();
									List<String> roomData = new ArrayList<>();
									roomData.add(roomId);
									roomData.add(userUuid);
									roomData.add(qrCodeUuid);
									roomData.add(decryptedMsg);

									eventList.add(roomData);
								}
							}
							matrixList.add(eventList);
							syncToken = data.nextBatchToken();
						//break;
						} catch (RuntimeException e) {
							LOG.warn("Error during sync", e.getMessage(), e);
						} finally {
							try {
								Thread.sleep(THREAD_SLEEP);
							} catch (InterruptedException e) {
								LOG.info("Interrupted while waiting for next sync");
							}
						}
					}
					client.logout();
				}

				Set<String[]> collect = matrixList.stream()
						.flatMap(Collection::stream)
						.map(e -> e.toArray(new String[0]))
						.collect(Collectors.toSet());
				try (CSVWriter writer = new CSVWriter(new FileWriter("roomMessages.csv"), ',',
						CSVWriter.NO_QUOTE_CHARACTER,
						CSVWriter.DEFAULT_ESCAPE_CHARACTER,
						CSVWriter.DEFAULT_LINE_END)) {
					String[] header = { "RoomId", "UserUUID", "QRcodeID", "message"};
					writer.writeNext(header);
					for (String[] s: collect) {
						writer.writeNext(s);
					}
				} catch (IOException e) {
					LOG.info("file output exc {}", e);
					throw new RuntimeException(e);
				}
			}).start();
		}
		// Login
		LOG.debug("Logging " + matrixUsername + " to matrix");
		MatrixClient client = MatrixUtil.login(matrixServer, matrixUsername, matrixPassword);
		chatSessionInfo.set(session.getId(), client);
		chatSessionInfo.set(session.getId(), matrixRoomId);
		MatrixRoom matrixRoom = client.getRoom(matrixRoomId);

		// Filter sa da pouzit na lepsi performance, teraz nam chodia zbitocne data
		final int messagesLimit = 2000; // Kolko poslednych sprav sa zobrazi v chate
		String filter = "{" + " \"presence\": { \"types\": [\"none\"] }"
				+ ", \"account_data\": { \"types\": [\"none\"] }" + ", \"room\": {" + " \"rooms\":[\"" + matrixRoomId
				+ "\"]" + ", \"state\": { \"types\": [\"none\"] }" + ", \"account_data\": { \"types\": [\"none\"] }"
				+ ", \"ephemeral\": { \"types\": [\"none\"] }"
				+ ", \"timeline\": { \"types\": [\"m.room.message\"], \"limit\": " + messagesLimit + " }" + " } " + "}";

		Thread t = new Thread(() -> {
			LOG.debug("Client Chat Sync thread is running " + matrixUsername.hashCode());
			String syncToken = null;
			List<KafkaService.INPUT_DATA> inputDataList = new ArrayList<>();
			// Musim kontrolovat ci stale je WS session otvorena
			while (!Thread.currentThread().isInterrupted() && session.isOpen()) {
				try {
					SyncData data = client.sync(SyncOptions.build().setFilter(filter).setSince(syncToken).get());

					// We check the invited rooms
					try {
						for (InvitedRoom invitedRoom : data.getRooms().getInvited()) {
							// We auto-join rooms we are invited to
							client.getRoom(invitedRoom.getId()).join();
						}
					} catch (Exception e) {
						// unknown error
						LOG.debug("try-catch check invited rooms error");
					}

					JoinedRoom joinedRoom = data.getRooms().getJoined().stream()
							.filter(r -> r.getId().equals(matrixRoomId)).findFirst().orElse(null);

					if (joinedRoom != null && matrixRoom != null) {
						// HashMap<matrixUsername, UserInfo>
						HashMap<String, UserInfo> matrixUsersMap = new HashMap<>();
						// We check events in room
						for (MatrixPersistentEvent event : joinedRoom.getTimeline().getEvents()) {
							// We only want to notify room messages
							if ("m.room.message".contentEquals(event.getType())) {
								MatrixJsonRoomMessageEvent msg = new MatrixJsonRoomMessageEvent(event.getJson());
								UserInfo senderInfo = null;
								String hashmapKeyUsername = msg.getSender().getLocalPart();
								if (matrixUsersMap.containsKey(hashmapKeyUsername)) {
									senderInfo = matrixUsersMap.get(hashmapKeyUsername);
								} else {
									senderInfo = getChatUserInfo(hashmapKeyUsername, qrCodeBean);
									matrixUsersMap.put(hashmapKeyUsername, senderInfo);
								}
								if (senderInfo != null && matrixBean != null) {
									TextMessageResponse textMessage = new TextMessageResponse();
									if (senderInfo.getChatUsername() == null
											|| senderInfo.getChatUsername().equals("")) {
										senderInfo.setChatUsername("Anonymous");
									}
									textMessage.setAlias(senderInfo.getChatUsername());
									if (senderInfo.getUserUuid() != null) {
										textMessage.setFrom(senderInfo.getUserUuid());
									} else if (senderInfo.getFingerprint() != null) {
										textMessage.setFrom(senderInfo.getFingerprint());
									}

									textMessage.setDate(new Date(msg.getTime()));
									if (senderInfo.getUserType() == UserTypeEnum.GUEST) {
										if (senderInfo.userUuid == null) {
											textMessage.setImage(null);
										} else {
											textMessage.setImage(userService.loadUserIcon(senderInfo.userUuid).toString());
										}
									} else {
										if (qrCodeBean.getPhoto() != null && !qrCodeBean.getPhoto().isBlank()) {
											textMessage.setImage(qrCodeBean.getPhoto());
										}
									}
									String decryptedMsg = AESCipher.decrypt(CRYPTING_KEY, msg.getBody()).getData();
									textMessage.setMessage(decryptedMsg);
									textMessage.setPosition(msg.getSender().getLocalPart().equals(matrixUsername)
											? ChatIconPositionEnum.LEFT
											: ChatIconPositionEnum.RIGHT);
									textMessage.setUserType(senderInfo.getUserType());
									textMessage.setQrName(qrCodeBean.getName());

									//qrCodeBean.getId();
									var qrCodeUuid = matrixBean.getQrCodeUuid() == null ? qrCodeBean.getUuid() : matrixBean.getQrCodeUuid();
									var description = qrCodeBean.getDescription();
									var roomTopic = matrixRoom.getTopic();
									var roomTags = matrixRoom.getAllTags();
									var photo = qrCodeBean.getPhoto();

									inputDataList.add(new KafkaService.MESSAGE_DATA(matrixRoomId,
											qrCodeUuid, senderInfo.getUserUuid(), decryptedMsg));
									// send msg
									try {
										// System.out.println(chatSessionInfo.getAll());
										session.sendMessage(WebSocketUtil.createWebSocketTextMessage(textMessage));
										LOG.debug("msg sent to client: " + textMessage.getMessage());

										
										
										// inform matrix server that msg was read by user
										if (!msg.getSender().getLocalPart().equals(matrixUsername)) {
											matrixRoom.sendReadReceipt(event.getId());
										}

									} catch (IOException ex) {
										ex.printStackTrace();
									}
								} else {
									// daco je zle 1
									LOG.debug("no sender and/or matrix data");
								}
							}
						}
					} else {
						// daco je zle 3
						LOG.debug("no joined and/or matrix room");
					}

					syncToken = data.nextBatchToken();
				} catch (RuntimeException e) {
					LOG.warn("Error during sync", e.getMessage(), e);
				} finally {
					try {
						Thread.sleep(THREAD_SLEEP);
					} catch (InterruptedException e) {
						LOG.debug("Interrupted while waiting for next sync");
					}
				}

				kafkaService.startProducer(KafkaService.TOPIC.MESSAGE_DATA, inputDataList);
			}

			try {
				client.logout();
				LOG.debug("Logged out of Matrix (chat) " + matrixUsername.hashCode());
			} catch (Exception e) {
				// e.printStackTrace();
			}

			LOG.debug("Exiting Chat Client Sync thread " + matrixUsername.hashCode());
		});

		t.setName("matrixChatClientSync" + matrixUsername.hashCode());
		t.start();

	}

	/**
	 * count matrix rooms by list of qr uuids
	 * 
	 * @param qrList
	 * @return
	 */
	public HashMap<String, Long> getRoomsCountMapByListOfQr(List<QRCodeBean> qrList) {
		HashMap<String, Long> map = new HashMap<>();

		if (qrList == null || qrList.size() == 0) {
			return map;
		}

		// qr uuid list from bean list
		List<String> qrUuidList = new ArrayList<>();
		for (QRCodeBean qr : qrList) {
			qrUuidList.add(qr.getUuid());
		}

		// get list of { qrCodeUuid, count(matrix_rooms) }
		List<QrUuidRoomCountPair> qrPairs = new ArrayList<>();
		if (qrUuidList != null && !qrUuidList.isEmpty()) {
			qrPairs = matrixRepository.countRoomsByQrCodeUuidIn(qrUuidList);
		}

		// get hashmap<qrUuid, roomsCount> from qrUuid list;
		qrPairs.stream().forEach(obj -> map.put(obj.getQrUuid(), obj.getRoomsCount()));

		return map;
	}

	/**
	 * get List of emails of users that communicated in matrixRoomId and has enabled
	 * emailNotification
	 * 
	 * @param matrixRoomId
	 * @return
	 */
	public List<String> getEmailsForEmailNotifications(String matrixRoomId, String userEmail) {
		List<String> response = new ArrayList<>();
		response.addAll(matrixRepository.getEmailsForEmailNotifications1(matrixRoomId, userEmail));
		response.addAll(matrixRepository.getEmailsForEmailNotifications2(matrixRoomId, userEmail));
		response.addAll(matrixRepository.getEmailsForEmailNotifications3(matrixRoomId, userEmail));
		return response;
	}

	/**
	 * get List of emails of users that communicated in matrixRoomId and has enabled
	 * pushNotification
	 * 
	 * @param matrixRoomId
	 * @param userEmail
	 * @return
	 */
	public List<String> getEmailsForPushNotifications(String matrixRoomId, String userEmail) {
		List<String> response = new ArrayList<>();
		response.addAll(matrixRepository.getEmailsForPushNotifications1(matrixRoomId, userEmail));
		response.addAll(matrixRepository.getEmailsForPushNotifications2(matrixRoomId, userEmail));
		response.addAll(matrixRepository.getEmailsForPushNotifications3(matrixRoomId, userEmail));
		return response;
	}

	/**
	 * zoznam roomov kde user pisal + zoznam roomov kde je user ako owner/shared
	 * user
	 * 
	 * @param userBean
	 * @return
	 * @throws Exception
	 */
	public List<MatrixResponseObject> loadAdminRooms(UserBean userBean) throws Exception {
		// TODO ziska zoznam qr kodov kde je owner/shared user a vytiahne room
		// qrcodeuuid=matrixqruuid
		List<MatrixResponseObject> response = new ArrayList<>();

		// load shared qr codes
		List<SharedUser> sharedQrList = qrcodeService.loadAllBySharedUserId(userBean.getId());

		// load owned qr codes
		List<QRCodeBean> ownedQrList = qrcodeService.loadAllByUserId(userBean.getId());

		// load qr codes from matrix table by user_uuid
		List<MatrixBean> pomMatrixList = loadUserRoomsFromDB(userBean.getUuid());
		List<String> pomMatrixQrUuidList = new ArrayList<>();
		pomMatrixList.stream().forEach(m -> pomMatrixQrUuidList.add(m.getQrCodeUuid()));
		List<QRCodeBean> matrixQrList = qrcodeService.loadAllByListOfUuid(pomMatrixQrUuidList);

		// mix owned and shared and qrcodes from matrix (by used_uuid) together;
		List<QRCodeBean> ownedAndSharedQrList = ownedQrList;
		List<QRCodeBean> sharedQrBeanList = new ArrayList<>();
		sharedQrList.stream().forEach(shared -> {
			sharedQrBeanList.add(shared.getQrCode());
			if (!ownedAndSharedQrList.contains(shared.getQrCode())) {
				ownedAndSharedQrList.add(shared.getQrCode());
			}
		});
		matrixQrList.stream().forEach(matrixQr -> {
			if (!ownedAndSharedQrList.contains(matrixQr)) {
				ownedAndSharedQrList.add(matrixQr);
			}
		});

		// get list of qrcodeUuids to select from DB
		List<String> qrcodeUuidList = new ArrayList<>();
		for (QRCodeBean b : ownedAndSharedQrList) {
			qrcodeUuidList.add(b.getUuid());
		}

		// load matrix rooms by qrCodeUuid
		List<MatrixBean> matrixList = loadRoomsByListOfQrCodeUuidOrUserUuid(qrcodeUuidList, userBean.getUuid());
		if (matrixList == null || matrixList.isEmpty()) {
			return response;
		}

		// map all selected qrcodes from DB to qrRRbeans
		List<QRCodeBeanResponse> qrcodeResponseList = new ArrayList<>();
		QRCodeDto.mapInto(qrcodeResponseList, ownedAndSharedQrList);

		// get and set qr that are shared
		qrcodeResponseList.stream().forEach(q -> {
			if (sharedQrBeanList.stream().anyMatch(s -> s.getId() == q.getId())) {
				q.setUserType(UserTypeEnum.SHARED);
			}
			if (ownedQrList.stream().anyMatch(o -> 
				o.getId() == q.getId() && o.getOwner().getId() == userBean.getId())) {
				q.setUserType(UserTypeEnum.ADMIN);
			}
		});

		// get count of unread msgs, last msg, sender info, etc. for all matrix rooms in
		// HashMap<matrixRoomId,
		String server = ConfigurationConstant.MATRIX_SERVER;
		String user = userBean.getMatrixUsername();
		String password = userBean.getMatrixPassword();
		HashMap<String, InboxRoomInfo> newMsgMap = getUnreadMessaggesInfo(server, user, password);

		// for every matrix room create new MatrixResponseObject, that will be added to
		// list
		for (MatrixBean m : matrixList) {
			MatrixResponseObject responseObject = new MatrixResponseObject();

			// set matrixRoomId
			// responseObject.setRoomId(m.getMatrixRoomId()); //TODO citlivy udaj, nema sa
			// posielat, zatial posli len ID
			responseObject.setRoomId(String.valueOf(m.getId()));

			// set newMsgsCount, last msg and date, sender alias and uuid/fingerprint
			if (newMsgMap != null && newMsgMap.containsKey(m.getMatrixRoomId())) {
				responseObject.setNewMsgCount(newMsgMap.get(m.getMatrixRoomId()).getUnreadMsgCount());
				responseObject.setLastMsg(newMsgMap.get(m.getMatrixRoomId()).getMessage());
				responseObject.setLastMsgDate(newMsgMap.get(m.getMatrixRoomId()).getDate());
				responseObject.setSenderAlias(newMsgMap.get(m.getMatrixRoomId()).getAlias());
				responseObject.setSenderUuid(newMsgMap.get(m.getMatrixRoomId()).getUuid_fingerprint());
				responseObject.setCounterpartName(newMsgMap.get(m.getMatrixRoomId()).getCounterpartName());
			} else {
				continue;
			}

			// set qrCodeBean that match qrcodeBean.uuid == matrixRoom.qrcodeuuid
			QRCodeBeanResponse qrCodeResponseBean = new QRCodeBeanResponse();
			for (QRCodeBeanResponse q : qrcodeResponseList) {
				if (q.getUuid().equals(m.getQrCodeUuid())) {
					qrCodeResponseBean = q;
					break;
				}
			}
			responseObject.setQrCodeBean(qrCodeResponseBean);
			if (m.getUserUuid() != null && userBean != null) {
				responseObject.setBlocked(blacklistService.existsByUuidAndOwnerId(m.getUserUuid(), userBean.getId()));
			}
			response.add(responseObject);
		}

		return response;
	}

	/**
	 * get matrix ID by matrix_room_id
	 * 
	 * @param matrixRoomId
	 * @return
	 */
	public String getIdByMatrixRoomId(String matrixRoomId) {
		Long id = matrixRepository.findIdByMatrixRoomId(matrixRoomId);
		if (id == null) {
			return "";
		}
		return Long.toString(id);
	}

	/**
	 * get qrcode UUID by matrix_room_id
	 * 
	 * @param matrixRoomId
	 * @return
	 */
	public String getQrCodeUuidByMatrixRoomId(String matrixRoomId) {
		return matrixRepository.findQrCodeUuidByMatrixRoomId(matrixRoomId);
	}

	/**
	 * get Alias of msg sender by matrix_room_id
	 * 
	 * @param matrixRoomId
	 * @return
	 */
	public String findAliasByMatrixRoomId(String matrixRoomId) {
		return matrixRepository.findAliasByMatrixRoomId(matrixRoomId);
	}

	/**
	 * get alias of sender. Checks matrix table and user table
	 * 
	 * @param fromMatrixId
	 * @return
	 */
	private String findAliasByMatrixName(String fromMatrixId) {
		String userAlias = matrixRepository.findUserAliasByMatrixName(fromMatrixId);
		if (userAlias != null && !userAlias.equals("")) {
			return userAlias;
		}
		String matrixAlias = matrixRepository.findMatrixAliasByMatrixName(fromMatrixId);
		if (matrixAlias != null && !matrixAlias.equals("")) {
			return matrixAlias;
		}
		return null;
	}

	/**
	 * get user uuid or fingerprint from matrix table
	 * 
	 * @param fromMatrixId
	 * @return
	 */
	private String findUserUuidByMatrixName(String fromMatrixId) {
		String userUuid = matrixRepository.findUserUuidByMatrixName(fromMatrixId);
		String matrixUuid = matrixRepository.findUuidByMatrixName(fromMatrixId);
		if (matrixUuid != null && !matrixUuid.equals("")) {
			return matrixUuid;
		}
		if (userUuid != null && !userUuid.equals("")) {
			return userUuid;
		}
		return null;
	}

	/**
	 * Add shared user to QR code Gets all qrcode already created rooms and invites
	 * new user to these rooms
	 * 
	 * @param sharedUser
	 * @throws Exception
	 */
	public void shareQRcode(SharedUser sharedUser) throws Exception {
		QRCodeBean qr = sharedUser.getQrCode();
		UserBean owner = sharedUser.getOwner();
		UserBean newShare = sharedUser.getShareUser();

		List<MatrixEntity> matrixRooms = matrixRepository.findByQrCodeUuid(qr.getUuid());

		if (matrixRooms.size() < 1) {
			// no rooms to join
			return;
		}

		// Login
		MatrixClient client = MatrixUtil.login(ConfigurationConstant.MATRIX_SERVER, owner.getMatrixUsername(),
				owner.getMatrixPassword());

		// Invite user to rooms
		matrixRooms.forEach((matrixRoom) -> {
			MatrixRoom room = client.getRoom(matrixRoom.getMatrixRoomId());
			try {
				room.invite(MatrixUtil.getMatrixIdObject(newShare.getMatrixUsername(), ConfigurationConstant.MATRIX_SERVER));
			} catch (Exception e) {
				LOG.warn("Exception while inviting share user to room! Room id: " + room.getId() + " share user: " + newShare.getMatrixUsername(), e);
			}
		});
	}
	
	/**
	 * remove shared user from matrix server
	 * @param sharedUser
	 * @throws Exception
	 */
	public void unshareQRcode(SharedUser sharedUser) throws Exception {
		UserBean newShare = sharedUser.getShareUser();

		List<MatrixEntity> matrixRooms = matrixRepository.findByQrCodeUuid(sharedUser.getQrCode().getUuid());

		if (matrixRooms.size() < 1) {
			// no rooms to join
			return;
		}

		// Login
		MatrixClient client = MatrixUtil.login(ConfigurationConstant.MATRIX_SERVER, newShare.getMatrixUsername(),
				newShare.getMatrixPassword());

		// Invite user to rooms
		matrixRooms.forEach((matrixRoom) -> {
			MatrixRoom room = client.getRoom(matrixRoom.getMatrixRoomId());
			try {
				room.leave();
			} catch (Exception e) {
				LOG.warn("Exception when leaving room " + matrixRoom.getMatrixRoomId() + " as shared user id: " + newShare.getId(), e);
			}
		});
	}

	/**
	 * update column matrix.empty to false; to know if chat has any msgs
	 * @param matrixRoomId
	 */
	public void setNotEmpty(String matrixRoomId) {
		matrixRepository.setNotEmpty(matrixRoomId);
		
	}

	/**
	 * find qr name by matrix room id
	 * @param matrixRoomId
	 * @return
	 */
	public String findQrNameByMatrixRoomId(String matrixRoomId) {
		return matrixRepository.findQrNameByMatrixRoomId(matrixRoomId);
	}
}
