/**
 * 
 */
package io.dpm.dropmenote.ws.utils.email;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.annotation.PostConstruct;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.dpm.dropmenote.ws.exception.EmailException;
import io.dpm.dropmenote.ws.utils.email.EmailData.Attachement;

/**
 * 
 * @author martinjurek
 *
 */
@Component
public class EmailUtil {

	private static Logger LOG = LoggerFactory.getLogger(EmailUtil.class);

	private String MAIL_CONTENT_TYPE = "text/html; charset=utf-8";

	@Value("${mail.configuration.username}")
	private String MAIL_CONFIGURATION_USERNAME;

	@Value("${mail.configuration.password}")
	private String MAIL_CONFIGURATION_PASSWORD;

	@Value("${mail.configuration.mail.smtp.host}")
	private String MAIL_CONFIGURATION_MAIL_SMTP_HOST;

	@Value("${mail.configuration.mail.smtp_socketFactory.port}")
	private String MAIL_CONFIGURATION_MAIL_SMTP_SOCKETFACTORY_PORT;

	@Value("${mail.configuration.mail.smtp.socketFactory.class}")
	private String MAIL_CONFIGURATION_MAIL_SMTP_SOCKETFACTORY_CLASS;

	@Value("${mail.configuration.mail.smtp.auth}")
	private String MAIL_CONFIGURATION_MAIL_SMTP_AUTH;

	@Value("${mail.configuration.mail.smtp.port}")
	private String MAIL_CONFIGURATION_MAIL_SMTP_PORT;

	private Session session;

	@PostConstruct
	private void init() {
		Properties props = new Properties();
		props.put("mail.smtp.host", MAIL_CONFIGURATION_MAIL_SMTP_HOST);
		props.put("mail.smtp.socketFactory.port", MAIL_CONFIGURATION_MAIL_SMTP_SOCKETFACTORY_PORT);
		props.put("mail.smtp.socketFactory.class", MAIL_CONFIGURATION_MAIL_SMTP_SOCKETFACTORY_CLASS);
		props.put("mail.smtp.auth", MAIL_CONFIGURATION_MAIL_SMTP_AUTH);
		props.put("mail.smtp.port", MAIL_CONFIGURATION_MAIL_SMTP_PORT);
		props.put("mail.smtp.ssl.enable", MAIL_CONFIGURATION_MAIL_SMTP_AUTH);

		// TODO trochu na picu , lebo stale sa robi connection na server je to
		// pomale a moze to padat
		session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(MAIL_CONFIGURATION_USERNAME, MAIL_CONFIGURATION_PASSWORD);
			}
		});
	}

	/**
	 * TODO neposiela sa body ak sa posle priloha. neviem preco to robi TODO TODO
	 * 
	 * @param emailData
	 * @throws EmailException
	 */
	public void sendEmail(EmailData emailData) throws EmailException {
//		LOG.debug("Call sendEmail [EmailData:" + emailData.toString() + "]");
		InternetAddress me;

		try {
			me = new InternetAddress(MAIL_CONFIGURATION_USERNAME);
			Message message = new MimeMessage(session);
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailData.getSendTo()));
			
			if (emailData.getBcc() != null && !emailData.getBcc().contains("")) {
				message.addRecipients(Message.RecipientType.BCC, InternetAddress.parse(emailData.getBcc()));
			}
			message.setSubject(emailData.getSubject());
			message.setFrom(me);
			// message.setContent(emailData.getBody(),
			// emailData.getEmailType());

			/*
			 * Toto je spravne zlozitejsie ale da sa do body part davat viacej sprav. my to
			 * nepotrebujeme ale nechavam to tu
			 */
			Multipart multipart = new MimeMultipart();
			BodyPart messageBodyPartText = new MimeBodyPart();
			messageBodyPartText.setContent(emailData.getBody(), MAIL_CONTENT_TYPE);
			multipart.addBodyPart(messageBodyPartText);

			if (emailData.getAttachements() != null) {
				for (Attachement attachementIter : emailData.getAttachements()) {
					BodyPart messageBodyPartAtachemtn = new MimeBodyPart();

					// Put file into email | attachement with specified name
					// this constructor takes byte[] as input
					// TODO tuna to je spravne mimo file ale mysliet na to ze mye type treba pekne
					// posielat do tohoto helpera
					// TODO ako preposlielat mime type do classy
					// TODO ako preposlielat mime type do classy
					// TODO ako preposlielat mime type do classy
					// TODO ako preposlielat mime type do classy
					ByteArrayDataSource rawData = new ByteArrayDataSource(attachementIter.getContent(), "image/png");

					messageBodyPartAtachemtn.setDataHandler(new DataHandler(rawData));
					messageBodyPartAtachemtn.setFileName(attachementIter.getName());
					multipart.addBodyPart(messageBodyPartAtachemtn);
				}
			}

			// Send the complete message parts
			message.setContent(multipart);
			LOG.debug("Sending email : {}, {}, {}, {}", message.getFrom(), message.getAllRecipients(), message.getSubject(), message.getSession().getTransport().getURLName());
			Transport.send(message);

			LOG.debug("Email sending Done");
		} catch (Exception e) {
			e.printStackTrace();
			throw new EmailException("Problem to send email.", e);
		}
	}
}