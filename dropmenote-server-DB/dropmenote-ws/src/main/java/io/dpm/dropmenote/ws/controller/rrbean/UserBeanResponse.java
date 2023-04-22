package io.dpm.dropmenote.ws.controller.rrbean;

import io.dpm.dropmenote.db.enums.ProfileIconEnum;
import lombok.Data;
import lombok.ToString;

/**
 * @author Peterko
 *
 */
@Data
@ToString(includeFieldNames = true)
public class UserBeanResponse {

	private String login;
	private String alias;
	private String photo;
	private ProfileIconEnum chatIcon;
	private boolean pushNotification;
	private boolean emailNotification;
	private boolean dmnAI;

}
