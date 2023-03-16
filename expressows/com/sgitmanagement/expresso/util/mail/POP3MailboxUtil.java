package com.sgitmanagement.expresso.util.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.util.MsGraphClient;
import com.sgitmanagement.expresso.util.SystemEnv;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeUtility;

/**
 * This class is a utilities to read mailbox and return messages
 *
 */
public class POP3MailboxUtil {
	static final private Logger logger = LoggerFactory.getLogger(POP3MailboxUtil.class);

	static private Properties mailProps;
	private Store store;
	private Folder inbox;

	static {
		mailProps = SystemEnv.INSTANCE.getProperties("mail");
	}

	public POP3MailboxUtil() throws Exception {
		connectStore();
	}

	private void connectStore() throws Exception {
		String protocol = mailProps.getProperty("mail.pop3.protocol");
		String host = mailProps.getProperty("mail.pop3.host");
		String port = mailProps.getProperty("mail.pop3.port");
		String username = mailProps.getProperty("mail.pop3.username");
		String password = mailProps.getProperty("mail.pop3.password");
		String auth = mailProps.getProperty("mail.pop3.auth.mechanisms");

		// add all mail.pop3 properties
		Properties properties = new Properties();
		mailProps.forEach((p, v) -> {
			if (((String) p).startsWith("mail.pop3."))
				properties.put(p, v);
		});

		// set timeouts
		properties.put("mail.pop3.connectiontimeout", 5000);
		properties.put("mail.pop3.timeout", 60000);

		// properties.put("mail.debug", "true");
		// properties.put("mail.debug.auth", "true");

		try {
			Session emailSession;
			if (auth != null) {
				MsGraphClient msGraphClient = new MsGraphClient();
				String scope = "https://outlook.office365.com/.default";
				String oauth2AccessToken = msGraphClient.connect(scope);
				// System.out.println(oauth2AccessToken);
				password = oauth2AccessToken;
			}
			emailSession = Session.getInstance(properties);

			// create the POP3 store object and connect with the pop server
			store = emailSession.getStore(protocol);
			if (!store.isConnected()) {
				store.connect(host, Integer.parseInt(port), username, password);
			}

			logger.info("Connected to the mailbox [" + username + "]");
		} catch (Exception e) {
			// logger.error("Cannot open the mail session Host:[" + mailProps.getProperty("mail.pop3.host") + "] Port:["
			// + mailProps.getProperty("mail.pop3.port") + "] Username:[" + username + "]", e);
			throw e;
		}

		inbox = store.getFolder("INBOX");
		if (inbox == null) {
			throw new Exception("Inbox not found");
		}
		inbox.open(Folder.READ_WRITE);
	}

	/**
	 * This method has to be called at the end of the email processing. It will delete the emails in the mailbox that has been marked as deleted
	 */
	public void close() {
		try {
			// true means it will expunge all emails marked as deleted
			inbox.close(true);
		} catch (Exception e) {
			// not much that we can do
		}
		try {
			store.close();
		} catch (Exception e) {
			// not much that we can do
		}
	}

	/**
	 * Get all messages from the Inbox.
	 *
	 * @return
	 * @throws Exception
	 */
	public List<Message> getMessages() throws Exception {
		return getMessages(null, null);
	}

	/**
	 * Get the messages from the Inbox using filters.
	 *
	 * @param subjectFilter  null for all messages
	 * @param fromDateFilter null for all messages
	 * @return
	 * @throws Exception
	 */
	public List<Message> getMessages(String subjectFilter, Date fromDateFilter) throws Exception {
		List<Message> msgList = new ArrayList<>();
		try {
			for (Message msg : inbox.getMessages()) {
				String subject = msg.getSubject();
				Date sendDate = msg.getSentDate();

				if ((fromDateFilter == null || fromDateFilter.before(sendDate)) && (subjectFilter == null || (subject != null && subject.contains(subjectFilter)))) {
					msgList.add(msg);
				}
			}
		} catch (MessagingException ex) {
			logger.warn("Cannot get messages: " + ex);
		}
		return msgList;
	}

	/**
	 *
	 * @param message
	 * @return
	 */
	public String getFirstAttachmentContent(Message message) throws Exception {
		return getFirstAttachmentContent(message, null);
	}

	/**
	 *
	 * @param message
	 * @param charset
	 * @return
	 */
	public String getFirstAttachmentContent(Message message, Charset charset) throws Exception {
		Map<String, InputStream> attachments = new HashMap<>();
		getAttachments(message, attachments);

		if (charset == null) {
			charset = StandardCharsets.UTF_8;
		}

		String content = null;
		for (Map.Entry<String, InputStream> attachment : attachments.entrySet()) {
			// String fileName = attachment.getKey();
			InputStream inputStream = attachment.getValue();

			if (content == null) {
				content = IOUtils.toString(inputStream, charset.name());
			}
			try {
				inputStream.close();
			} catch (Exception e) {
			}
		}

		return content;
	}

	/*
	 * This method checks for content-type based on which, it processes and fetches the content of the message
	 */
	public void getAttachments(Part part, Map<String, InputStream> attachments) throws Exception {

		// logger.debug("CONTENT-TYPE: " + part.getContentType());

		// check if the content is plain text
		if (part.isMimeType("text/plain")) {
			// logger.debug("This is plain text");
			// logger.debug((String) part.getContent());
		}
		// check if the content has attachment
		else if (part.isMimeType("multipart/*")) {
			// logger.debug("This is a Multipart");
			Multipart mp = (Multipart) part.getContent();
			int count = mp.getCount();
			for (int i = 0; i < count; i++) {
				getAttachments(mp.getBodyPart(i), attachments);
			}
		}
		// check if the content is a nested message
		else if (part.isMimeType("message/rfc822")) {
			// logger.debug("This is a Nested Message");
			getAttachments((Part) part.getContent(), attachments);
		}
		// check if the content is an inline image
		// else if (part.isMimeType("image/jpeg")) {
		// logger.debug("--------> image/jpeg");
		// Object o = part.getContent();
		//
		// InputStream x = (InputStream) o;
		// // Construct the required byte array
		// logger.debug("x.length = " + x.available());
		// byte[] bArray = new byte[x.available()];
		//
		// int i;
		// while ((i = x.available()) > 0) {
		// int result = (x.read(bArray));
		// if (result == -1) {
		// break;
		// }
		// }
		// FileOutputStream f2 = new FileOutputStream("/tmp/image.jpg");
		// f2.write(bArray);
		// } else if (part.getContentType().contains("image/")) {
		// logger.debug("content type" + part.getContentType());
		// File f = new File("image" + new Date().getTime() + ".jpg");
		// DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
		// com.sun.mail.util.BASE64DecoderStream test = (com.sun.mail.util.BASE64DecoderStream) part.getContent();
		// byte[] buffer = new byte[1024];
		// int bytesRead;
		// while ((bytesRead = test.read(buffer)) != -1) {
		// output.write(buffer, 0, bytesRead);
		// }
		// }

		else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
			// this part is attachment
			MimeBodyPart mimeBodyPart = (MimeBodyPart) part;
			String fileName = Normalizer.normalize(MimeUtility.decodeText(mimeBodyPart.getFileName()), Normalizer.Form.NFC);
			// logger.debug("Got attachment [" + fileName + "]: " + mimeBodyPart.getEncoding());

			File file = File.createTempFile(fileName, "");
			mimeBodyPart.saveFile(file);
			attachments.put(fileName, new FileInputStream(file));
			// attachments.put(fileName, MimeUtility.decode(mimeBodyPart.getInputStream(), mimeBodyPart.getEncoding()));
		} else {
			Object o = part.getContent();
			if (o instanceof String) {
				// logger.debug("This is a string");
				// logger.debug((String) o);
			} else if (o instanceof InputStream) {
				// logger.debug("This is just an input stream");
				String name = part.getContentType();
				String nameToken = "name=\"";
				if (name.contains(nameToken)) {
					name = name.substring(name.indexOf(nameToken) + nameToken.length());
					name = name.substring(0, name.indexOf("\""));
					InputStream is = (InputStream) o;
					attachments.put(name, is);
				}

				// int c;
				// while ((c = is.read()) != -1) {
				// System.out.write(c);
				// }
			} else {
				// logger.warn("This is an unknown type");
				// logger.debug(o.toString());
			}
		}

	}

	/**
	 *
	 * @throws Exception
	 */
	public void cleanMailbox() throws Exception {
		List<Message> messages = getMessages();

		for (Message message : messages) {
			String subject = message.getSubject();
			boolean deleteEmail = false;

			// verify invalid email
			if (subject.startsWith("Réponse automatique :") || subject.startsWith("Réponse automatique :") || subject.startsWith("Automatic") || subject.startsWith("Fwd:") || subject.startsWith("Re:")
					|| subject.startsWith("RE:") || subject.startsWith("Out of Office")) {
				deleteEmail = true;
			} else if (subject.startsWith("Undeliverable:") || subject.startsWith("Message undeliverable:") || subject.startsWith("Failure") || subject.startsWith("Mail Delivery Subsystem")
					|| subject.startsWith("Non remis :") || subject.startsWith("Undelivered Mail") || subject.startsWith("Delivery Status Notification (Failure)")) {
				// logger.warn("Undeliverable email Subject [" + subject + "] Sent: " + message.getSentDate());
				deleteEmail = true;
			} else if (subject.startsWith("Microsoft 365 Message center") || subject.startsWith("Weekly digest: Microsoft service updates") || subject.startsWith("Major update from Message center")) {
				// spam
				deleteEmail = true;

			} else {
				// valid emails
			}

			if (deleteEmail) {
				logger.info("Deleting invalid email based on subject [" + subject + "]");
				if (SystemEnv.INSTANCE.isInProduction()) {
					message.setFlag(Flags.Flag.DELETED, true);
				}
			} else {
				logger.debug("Valid email based on subject [" + subject + "]");
			}
		}
	}

	public static void main(String[] args) throws Exception {
		POP3MailboxUtil mu = new POP3MailboxUtil();
		// List<Message> messages = mu.getMessages();
		// System.out.println("Got " + messages.size() + " messages in INBOX");
		// for (Message message : messages) {
		// System.out.println(
		// "------------------------------------------------------------------------------------------------");
		// System.out.println("Subject: " + message.getSubject());
		// // System.out.println("From: " + message.getFrom()[0]);
		// // System.out.println("Text: " + message.getContent().toString());
		// // message.getRecipients(Message.RecipientType.TO)
		// // message.setFlag(Flags.Flag.DELETED, true);
		// }

		mu.cleanMailbox();

		mu.close();
		System.out.println("Done");
	}
}
