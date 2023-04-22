package io.dpm.dropmenote.ws.bean;

import io.dpm.dropmenote.db.enums.ProfileIconEnum;
import lombok.Data;
import lombok.ToString;

/**
 * @author Peterko
 *
 */
@Data
@ToString(includeFieldNames = true)
public class UserBean extends AbstractBean {

	/**
	 * Default constructor
	 */
	public UserBean() {

	}

	private long id;

	private String alias;
	private ProfileIconEnum chatIcon;
	private String login;
	private String uuid;
	private String password;
	private String photo;
	private boolean pushNotification;
	private boolean emailNotification;
	private boolean dmnAI;
	private String recoveryToken;
	private String matrixUsername;
	private String matrixPassword;
	private boolean canChangePassword;
}
