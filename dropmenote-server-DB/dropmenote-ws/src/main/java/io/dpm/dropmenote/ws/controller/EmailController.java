package io.dpm.dropmenote.ws.controller;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.dpm.dropmenote.ws.bean.QRCodeBean;
import io.dpm.dropmenote.ws.bean.UserBean;
import io.dpm.dropmenote.ws.constants.ControllerConstant;
import io.dpm.dropmenote.ws.services.EmailService;
import io.dpm.dropmenote.ws.services.QRCodeService;
import io.dpm.dropmenote.ws.services.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 
 * @author Martin Jurek (Starbug s.r.o. | https://www.strabug.eu)
 *
 */
@Api(value = "EmailController", description = "EmailController")
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", maxAge = 3600)
@RequestMapping("/ws/blacklist")
public class EmailController extends AbstractController {

    private static Logger LOG = LoggerFactory.getLogger(EmailController.class);

    {
        LOG.debug("{} initialisation.", EmailController.class.getName());
    }
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private QRCodeService qrService;

    @ApiOperation(value = "sendRegistration", notes = "sendRegistration", responseContainer = "Object")
	@RequestMapping(value = "/sendRegistration", method = RequestMethod.POST, produces = ControllerConstant.ALL, consumes = ControllerConstant.ALL)
	@ResponseBody
	public void sendRegistration(String loginEmail) throws Exception {
		LOG.debug("EmailController - sendRegistration - loginEmail {}", loginEmail);
		UserBean userBean = userService.loadByLogin(loginEmail);
    	if (userBean != null) {
    		emailService.sendRegistration(userBean);
    	}
	}
    
    @ApiOperation(value = "sendForgotpassword", notes = "sendForgotpassword", responseContainer = "Object")
	@RequestMapping(value = "/sendForgotpassword", method = RequestMethod.POST, produces = ControllerConstant.ALL, consumes = ControllerConstant.ALL)
	@ResponseBody
	public void sendForgotpassword(String loginEmail) throws Exception {
		LOG.debug("EmailController - sendForgotpassword - loginEmail {}", loginEmail);
		UserBean userBean = userService.loadByLogin(loginEmail);
    	if (userBean != null) {
    		emailService.sendForgotPassword(userBean);
    	}
	}
    
    @ApiOperation(value = "sendNotification", notes = "sendNotification", responseContainer = "Object")
	@RequestMapping(value = "/sendNotification", method = RequestMethod.POST, produces = ControllerConstant.ALL, consumes = ControllerConstant.ALL)
	@ResponseBody
	public void sendNotification(String loginEmail, long qrcodeId, String message, List<String> emails) throws Exception {
		LOG.debug("EmailController - sendNotification - loginEmail {}, qrcodeId - {}, emails - {}", loginEmail, qrcodeId, emails.toString());
		UserBean userBean = userService.loadByLogin(loginEmail);
    	QRCodeBean qrCodeBean = qrService.load(qrcodeId);
    	if (userBean != null && qrCodeBean != null) {
    		emailService.sendNotification(emails, userBean.getAlias(), qrCodeBean.getName()	, message);
    	}
	}
    
    @ApiOperation(value = "sendQrcode", notes = "sendQrcode", responseContainer = "Object")
	@RequestMapping(value = "/sendQrcode", method = RequestMethod.POST, produces = ControllerConstant.ALL, consumes = ControllerConstant.ALL)
	@ResponseBody
	public void sendQrcode(long userId, long qrId, HttpServletResponse response) throws Exception {
		LOG.debug("EmailController - sendQrcode - userId {}, qrId - {}", userId, qrId);
		UserBean userBean = userService.load(userId);
    	QRCodeBean qrCodebean = qrService.load(qrId);
    	if (userBean != null && qrCodebean != null) {
    		emailService.sendQRCode(userBean, qrCodebean);
    	}
	}
}
