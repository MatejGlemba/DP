package io.dpm.dropmenote.ws.services;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.dpm.dropmenote.db.entity.BlacklistEntity;
import io.dpm.dropmenote.db.repository.BlacklistRepository;
import io.dpm.dropmenote.ws.bean.BlacklistBean;
import io.dpm.dropmenote.ws.dto.BlackListDto;

/**
 * 
 * @author Martin Jurek (Starbug s.r.o. | https://www.strabug.eu)
 *
 */
@Service
public class BlacklistService {

	private static Logger LOG = LoggerFactory.getLogger(BlacklistService.class);

	{
		LOG.debug("{} initialisation.", BlacklistService.class.getName());
	}

	@Autowired
	private BlacklistRepository blacklistRepository;
	@Autowired
	private KafkaService kafkaService;

	/**
	 * najst Blacklist zaznamy vsetky podla id usera
	 * 
	 * @param userBean
	 * @return List<BlacklistBean>
	 */
	public List<BlacklistBean> loadAll(long userId) {
		List<BlacklistBean> blacklistBeans = BlackListDto.convertToBean(blacklistRepository.findByOwnerId(userId));
		return blacklistBeans;

		// BlacklistEntity blacklistEntity = blacklistRepository.findOne(userId);
		// return blackli.convertToBean(userEntity);
		// throw new UnsupportedOperationException();
	}

	/**
	 * vyhladat blacklist zaznam podla id
	 * 
	 * @param BlacklistRecordId
	 * @return BlacklistBean
	 */
	public BlacklistBean load(long BlacklistRecordId) {
		return BlackListDto.convertToBean(blacklistRepository.findOne(BlacklistRecordId));
	}

	/**
	 * ulozit zaznam v blackList
	 * 
	 * @param blackListRequest
	 * @return BlacklistBean
	 */
	public BlacklistBean save(BlacklistBean blackListRequest) {
		BlacklistEntity blacklistEntity = BlackListDto.convertToEntity(blackListRequest);
		BlacklistBean blacklistBean = BlackListDto.convertToBean(blacklistRepository.save(blacklistEntity));
		kafkaService.produce(KafkaService.TOPIC.USER_DATA,
				new KafkaService.USER_DATA(blacklistBean.getUuid(), blacklistBean.getNote()));
		return blacklistBean;
	}

	public void removeByUuid(String blacklistUuid) {
		blacklistRepository.deleteByUuid(blacklistUuid);
	}

	/**
	 * zmazat blacklist zazanam podla id
	 * 
	 * @param blacklistRecordId
	 */
	public void remove(long blacklistRecordId) {
		blacklistRepository.delete(blacklistRecordId);
	}

	/**
	 * load by uuid
	 * 
	 * @param uuid
	 * @return
	 */
	public BlacklistBean loadByUuid(String uuid) {
		return BlackListDto.convertToBean(blacklistRepository.findByUuid(uuid));
	}

	/**
	 * check if user is blacklisted or not
	 * 
	 * @param userToken
	 * @param id
	 * @return
	 */
	public boolean existsByUuidAndOwnerId(String userToken, long ownerId) {
		Long rows = blacklistRepository.countByUuidAndOwnerId(userToken, ownerId);
		return (rows != null && rows > 0);
	}

	/**
	 * load by matrix room id 
	 * @param matrixRoomId
	 * @return
	 */
	public BlacklistBean loadByMatrixRoomId(String matrixRoomId) {
		return BlackListDto.convertToBean(blacklistRepository.findByMatrixRoomId(matrixRoomId));
	}
}
