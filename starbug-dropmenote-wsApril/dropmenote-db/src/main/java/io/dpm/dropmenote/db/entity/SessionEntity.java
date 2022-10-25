package io.dpm.dropmenote.db.entity;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import io.dpm.dropmenote.db.constant.Schema;
import lombok.Data;
import lombok.ToString;

/**
 * @author Peter Diskanec
 *
 */
@Data
@ToString(includeFieldNames = true)
@Entity
@Table(schema = Schema.SCHEMA, name = "USER_SESSION")
@NamedQuery(name = "SessionEntity.findAll", query = "SELECT a FROM SessionEntity a")
public class SessionEntity extends AbstractEntity implements Serializable {
	/**
	 * Serializable
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor
	 */
	public SessionEntity() {

	}

	@Id
	@SequenceGenerator(name = "device_id_seq", sequenceName = "device_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "device_id_seq")
	@Column(name = "id", nullable = false, precision = 19)
	private long id;

	@ManyToOne(cascade = CascadeType.MERGE)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@Column(name = "token", nullable = false, unique = true)
	private String token;
	
	@Column(name = "device_id", nullable = false)
	private String deviceId;

	@Column(name = "active", nullable = false)
	private boolean active;
	
}
