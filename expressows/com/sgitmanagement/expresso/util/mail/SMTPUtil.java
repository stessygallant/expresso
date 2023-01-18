package com.sgitmanagement.expresso.util.mail;

import java.util.Collection;
import java.util.Properties;

import com.sgitmanagement.expresso.util.SystemEnv;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

public class SMTPUtil implements MailSender {

	static private Session session;

	static private String username;
	static private String password;

	static private Properties sessionProperties;

	static {
		Properties props = SystemEnv.INSTANCE.getProperties("mail");
		username = props.getProperty("mail.smtp.username");
		password = props.getProperty("mail.smtp.password");

		String smtpHost = props.getProperty("mail.smtp.host");
		String smtpPort = props.getProperty("mail.smtp.port");
		boolean smtpAuth = Boolean.parseBoolean(props.getProperty("mail.smtp.auth", "false"));
		boolean smtpStartTLS = Boolean.parseBoolean(props.getProperty("mail.smtp.starttls.enable", "false"));

		sessionProperties = new Properties();
		sessionProperties.setProperty("mail.smtp.host", smtpHost);
		sessionProperties.setProperty("mail.smtp.port", smtpPort);
		sessionProperties.setProperty("mail.smtp.auth", "" + smtpAuth);
		sessionProperties.setProperty("mail.smtp.starttls.enable", "" + smtpStartTLS);

		sessionProperties.setProperty("mail.smtp.connectiontimeout", "" + (20 * 1000));
		sessionProperties.setProperty("mail.smtp.timeout", "" + (40 * 1000)); // read
		sessionProperties.setProperty("mail.smtp.writetimeout", "" + (40 * 1000));
		sessionProperties.setProperty("mail.smtp.ssl.trust", "*");

		// create the session
		session = Session.getInstance(sessionProperties, new jakarta.mail.Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});
	}

	@Override
	public void sendMail(String fromAddress, String fromName, Collection<String> tos, Collection<String> ccs, Collection<String> bccs, String replyTo, String subject, boolean importantFlag,
			String messageBody, Collection<String> attachments) throws Exception {
		MimeMessage message = new MimeMessage(session);

		if (importantFlag) {
			message.setHeader("X-Priority", "2");
		}
		message.setSubject(subject, "UTF-8");

		InternetAddress senderAddress = new InternetAddress(fromAddress);
		if (fromName != null) {
			senderAddress.setPersonal(fromName);
		}
		message.setFrom(senderAddress);

		int countRecipients = 0;
		// TO (mandatory)
		if (tos != null) {
			for (String to : tos) {
				if (to != null) {
					String[] addresses = to.split("[,; ]");
					for (String address : addresses) {
						if (address != null && address.length() > 5 && address.indexOf("@") != -1) {
							countRecipients++;
							message.addRecipient(Message.RecipientType.TO, new InternetAddress(address));
						}
					}
				}
			}
		}

		// CC (optional)
		if (ccs != null) {
			for (String cc : ccs) {
				if (cc != null) {
					String[] addresses = cc.split("[,;]");
					for (String address : addresses) {
						if (address != null && address.length() > 0 && address.indexOf("@") != -1) {
							countRecipients++;
							message.addRecipient(Message.RecipientType.CC, new InternetAddress(address));
						}
					}
				}
			}
		}

		// BCC (optional)
		if (bccs != null) {
			for (String bcc : bccs) {
				if (bcc != null) {
					String[] addresses = bcc.split("[,;]");
					for (String address : addresses) {
						if (address != null && address.length() > 0 && address.indexOf("@") != -1) {
							countRecipients++;
							message.addRecipient(Message.RecipientType.BCC, new InternetAddress(address));
						}
					}
				}
			}
		}

		// avoid sending an email to all because of a bug in the application logic
		if (countRecipients > 50) {
			throw new Exception("tooManyRecipients");
		}

		if (replyTo != null && replyTo.length() > 0 && replyTo.indexOf("@") != -1) {
			message.setReplyTo(new jakarta.mail.Address[] { new jakarta.mail.internet.InternetAddress(replyTo) });
		}

		if (attachments == null || attachments.isEmpty()) {
			message.setContent(messageBody, "text/html; charset=UTF-8");
		} else {
			Multipart multipart = new MimeMultipart();
			message.setContent(multipart);

			// add the messageBody
			BodyPart messageText = new MimeBodyPart();
			messageText.setContent(messageBody, "text/html; charset=UTF-8");
			multipart.addBodyPart(messageText);

			// add the attachments
			for (String filename : attachments) {
				BodyPart messageBodyPart = new MimeBodyPart();
				DataSource source = new FileDataSource(filename);
				if ((source.getName() == null) || (source.getName().length() <= 0)) {
					throw new Exception("Problem setting attachment");
				}
				messageBodyPart.setDataHandler(new DataHandler(source));
				messageBodyPart.setFileName(source.getName());
				multipart.addBodyPart(messageBodyPart);
			}
		}

		Transport.send(message);
	}

	@Override
	public void connect() {
	}

	@Override
	public void disconnect() {
	}
}