
package io.dpm.dropmenote.db.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.dpm.dropmenote.db.entity.UserEntity;
import io.dpm.dropmenote.db.enums.ProfileIconEnum;

/**
 * 
 * @author martinjurek
 *
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

	public UserEntity findByLoginAndPassword(String login, String password);

	public UserEntity findByLogin(String login);

	public UserEntity findByMatrixUsername(String login);

	public UserEntity findByRecoveryToken(String receoveryToken);

	//	//@Query("DELETE FROM #{#entityName} e WHERE e.recoveryTokenCreated > DATEADD(hh, -1, GETDATE())")
	@Modifying
	@Query(value = "DELETE FROM #{#entityName} e WHERE e.recoveryTokenCreated > CURRENT_DATE - interval ' 1 hour'", nativeQuery = true)
	public void deleteOldRecoveryTokens();

	public List<UserEntity> findByUuid(String userUuid);

	@Query("SELECT u.chatIcon FROM UserEntity u WHERE u.uuid = :userUuid")
	public ProfileIconEnum findChatIconByUuid(@Param("userUuid") String userUuid);

	@Query("SELECT u.alias FROM UserEntity u WHERE u.uuid = (SELECT m.userUuid FROM MatrixEntity m WHERE m.matrixRoomId = :matrixRoomId)"
			+ " AND u.matrixUsername != :username")
	public String findAliasByMatrixRoomId(@Param("username") String username, @Param("matrixRoomId") String matrixRoomId);

}
