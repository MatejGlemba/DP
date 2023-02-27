/**
 * 
 */
package io.dpm.dropmenote.ws.scheduler;

import io.dpm.dropmenote.db.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Martin Jurek (Starbug s.r.o. | https://www.strabug.eu)
 *
 */
@EnableScheduling
@Transactional
@Component
public class UserRecoverTokenCleanerScheduler {

	private static Logger LOG = LoggerFactory.getLogger(UserRecoverTokenCleanerScheduler.class);

	{
		LOG.info("{} initialisation.", UserRecoverTokenCleanerScheduler.class.getName());
	}

	@Autowired
	private UserRepository userRepository;

	/**
	 * Clear old recovery token from user table. Call it every 2 hours
	 */
	@Transactional
	@Scheduled(cron = "0 0 0/2 * * ?")
	public void removeOldGpsData() {
		 LOG.info("Clen User revocerToken.");
		 userRepository.deleteOldRecoveryTokens();
	}
}
