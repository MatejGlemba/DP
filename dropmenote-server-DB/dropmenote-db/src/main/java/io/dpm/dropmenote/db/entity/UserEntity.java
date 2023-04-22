package io.dpm.dropmenote.db.entity;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import io.dpm.dropmenote.db.constant.Schema;
import io.dpm.dropmenote.db.converter.AESAttributeConverter;
import io.dpm.dropmenote.db.enums.ProfileIconEnum;
import lombok.Data;
import lombok.ToString;

/**
 * @author Peterko
 *
 */
@Data
@ToString(includeFieldNames = true)
@Entity
@Table(schema = Schema.SCHEMA, name = "USER")
@NamedQuery(name = "UserEntity.findAll", query = "SELECT a FROM UserEntity a")
public class UserEntity extends AbstractEntity implements Serializable {

	/**
	 * Serializable
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor
	 */
	public UserEntity() {

	}

	@Id
	@SequenceGenerator(name = "user_id_seq", sequenceName = "user_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_seq")
	@Column(name = "id", nullable = false, precision = 19)
	private long id;

	@Column(name = "alias", nullable = true)
	private String alias;

	@Column(name = "login", nullable = false, unique = true)
	@Convert(converter = AESAttributeConverter.class)
	private String login;

	@Column(name = "uuid", nullable = false, unique = true)
	private String uuid;

	@Column(name = "password", nullable = false)
	@Convert(converter = AESAttributeConverter.class)
	private String password;

	@Column(name = "photo", nullable = true)
	private String photo;

	@Column(name = "chat_icon")
	@Enumerated(EnumType.STRING)
	private ProfileIconEnum chatIcon = ProfileIconEnum.P1;
	
	@Column(name = "push_notification", nullable = false)
	private boolean pushNotification;

	@Column(name = "email_notification", nullable = false)
	private boolean emailNotification;

	@Column(name = "dmn_ai", nullable = false)
	private boolean dmnAI;

	@Column(name = "recovery_token", nullable = true)
	private String recoveryToken;

	@Column(name = "recovery_token_created", nullable = true)
	private Timestamp recoveryTokenCreated;

	@Column(name = "matrix_username", nullable = false)
	@Convert(converter = AESAttributeConverter.class)
	private String matrixUsername;

	@Column(name = "matrix_password", nullable = false)
	@Convert(converter = AESAttributeConverter.class)
	private String matrixPassword;

	@Column(name = "can_change_password")
	private boolean canChangePassword;
	
//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
//	@JoinTable(name = "USER_DEVICE", schema = Schema.SCHEMA)
//	private List<DeviceEntity> devices;
//	
//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
//	@JoinTable(name = "USER_SESSION", schema = Schema.SCHEMA)
//	private List<DeviceEntity> sessions;
}
