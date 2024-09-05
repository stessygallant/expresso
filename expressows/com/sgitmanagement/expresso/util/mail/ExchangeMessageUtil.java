package com.sgitmanagement.expresso.util.mail;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sgitmanagement.expresso.util.MsGraphClient;
import com.sgitmanagement.expresso.util.SystemEnv;

/**
 * This class is a utilities to read and send messages
 *
 */
public class ExchangeMessageUtil implements MailSender {
	static private Logger logger = LoggerFactory.getLogger(ExchangeMessageUtil.class);
	static private Properties mailProps;
	static private String defaultUserPrincipalName;

	private MsGraphClient msGraphClient;

	static {
		mailProps = SystemEnv.INSTANCE.getProperties("mail");
		defaultUserPrincipalName = mailProps.getProperty("mail.userPrincipalName");
	}

	/**
	 * 
	 * @throws Exception
	 */
	@Override
	public void connect() throws Exception {
		this.msGraphClient = new MsGraphClient();
		this.msGraphClient.connect();
	}

	/**
	 * 
	 */
	@Override
	public void disconnect() {
		try {
			msGraphClient.disconnect();
		} catch (Exception e) {
			// not much that we can do
		}
		msGraphClient = null;
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<Message> getMessages() throws Exception {
		return getMessages(null);
	}

	/**
	 * 
	 * @param filter
	 * @return
	 * @throws Exception
	 */
	public List<Message> getMessages(String filter) throws Exception {
		return getMessages(defaultUserPrincipalName, "Inbox", filter);
	}

	/**
	 * Get all messages from the folder.
	 *
	 * @return
	 * @throws Exception
	 */
	private List<Message> getMessages(String userPrincipalName, String folder, String filter) throws Exception {
		String path = "/users/" + userPrincipalName + "/mailFolders/" + folder + "/messages?$select=id,sender,subject,body,hasAttachments&$top=100";
		JsonObject rootJonsObject = msGraphClient.callMicrosoftGraph(path);

		List<Message> messages = new ArrayList<>();
		for (JsonElement jsonElement : rootJonsObject.get("value").getAsJsonArray()) {
			JsonObject messageJsonObject = (JsonObject) jsonElement;

			String id = messageJsonObject.get("id").getAsString();
			String subject = messageJsonObject.get("subject").getAsString();

			if (isValidMessage(id, subject)) {
				if (filter == null || subject.contains(filter)) {
					boolean hasAttachments = messageJsonObject.get("hasAttachments").getAsBoolean();
					String senderEmail = messageJsonObject.get("sender").getAsJsonObject().get("emailAddress").getAsJsonObject().get("address").getAsString();
					String content = messageJsonObject.get("body").getAsJsonObject().get("content").getAsString();

					Message message = new Message(id, subject, senderEmail, content, hasAttachments);
					messages.add(message);
				}
			}
		}

		return messages;
	}

	/**
	 * 
	 * @param messageId
	 * @throws Exception
	 */
	public void deleteMessage(String messageId) throws Exception {
		deleteMessage(defaultUserPrincipalName, messageId);
	}

	/**
	 * 
	 * @param userPrincipalName
	 * @param messageId
	 * @throws Exception
	 */
	private void deleteMessage(String userPrincipalName, String messageId) throws Exception {
		if (SystemEnv.INSTANCE.isInProduction()) {
			String path = "/users/" + userPrincipalName + "/messages/" + messageId;
			msGraphClient.callMicrosoftGraph(path, "DELETE", null, null, null);
		}
	}

	/**
	 * 
	 * @param messageId
	 * @throws Exception
	 */
	public void moveMessagetoDeletedFolder(String messageId) throws Exception {
		moveMessage(defaultUserPrincipalName, messageId, "deleteditems");
	}

	/**
	 * 
	 * @param messageId
	 * @param destinationFolderId
	 * @throws Exception
	 */
	public void moveMessage(String messageId, String destinationFolderId) throws Exception {
		moveMessage(defaultUserPrincipalName, messageId, destinationFolderId);
	}

	/**
	 * 
	 * @param userPrincipalName
	 * @param messageId
	 * @param destinationFolderI
	 * @throws Exception
	 */
	private void moveMessage(String userPrincipalName, String messageId, String destinationFolderId) throws Exception {
		if (SystemEnv.INSTANCE.isInProduction()) {
			String path = "/users/" + userPrincipalName + "/messages/" + messageId + "/move";
			JsonObject rootJsonObject = new JsonObject();
			rootJsonObject.addProperty("destinationId", destinationFolderId);
			String jsonBody = new GsonBuilder().create().toJson(rootJsonObject);
			msGraphClient.callMicrosoftGraph(path, jsonBody);
		}
	}

	/**
	 * 
	 * @param tos
	 * @param ccs
	 * @param bccs
	 * @param replyTo
	 * @param subject
	 * @param importantFlag
	 * @param messageBody
	 * @param attachments
	 */
	@Override
	public void sendMail(String fromAddress, String fromName, Collection<String> tos, Collection<String> ccs, Collection<String> bccs, String replyTo, String subject, boolean importantFlag,
			String messageBody, Collection<String> attachments, boolean skipMaxRecipientsValidation) throws Exception {

		// avoid sending an email to all because of a bug in the application logic
		int countRecipients = (tos != null ? tos.size() : 0) + (ccs != null ? ccs.size() : 0) + (bccs != null ? bccs.size() : 0);
		if (!skipMaxRecipientsValidation && countRecipients > 50) {
			throw new Exception("tooManyRecipients");
		}

		try {
			String path = "/users/" + defaultUserPrincipalName + "/sendMail";

			JsonObject rootJsonObject = new JsonObject();

			// do not put in sent items
			rootJsonObject.addProperty("saveToSentItems", false);

			// message
			JsonObject messageJsonObject = new JsonObject();
			rootJsonObject.add("message", messageJsonObject);

			// importance
			messageJsonObject.addProperty("importance", importantFlag ? "high" : "normal");

			// from/sender -> must be the same as the actual defaultUserPrincipalName
			messageJsonObject.add("from", createRecipient(fromAddress));
			messageJsonObject.add("sender", createRecipient(fromAddress));

			// subject
			messageJsonObject.addProperty("subject", subject);

			// body
			JsonObject bodyJsonObject = new JsonObject();
			messageJsonObject.add("body", bodyJsonObject);
			bodyJsonObject.addProperty("contentType", "HTML");
			bodyJsonObject.addProperty("content", messageBody);
			// Base64.getEncoder().encodeToString(messageBody.getBytes(StandardCharsets.UTF_8)));

			// recipients
			messageJsonObject.add("toRecipients", createRecipients(tos));
			if (ccs != null) {
				messageJsonObject.add("ccRecipients", createRecipients(ccs));
			}
			if (bccs != null) {
				messageJsonObject.add("bccRecipients", createRecipients(bccs));
			}

			// not supported: status code: 400, reason phrase: Bad Request
			// read-only properties
			// replyTo
			// if (replyTo != null) {
			// messageJsonObject.add("replyTo", createRecipient(replyTo));
			// }

			// attachments (limited to 3M)
			if (attachments != null && !attachments.isEmpty()) {
				JsonArray attachmentsJsonArray = new JsonArray();
				messageJsonObject.add("attachments", attachmentsJsonArray);
				for (String attachment : attachments) {
					attachmentsJsonArray.add(createAttachment(attachment));
				}
			}

			// send
			String jsonBody = new GsonBuilder().create().toJson(rootJsonObject);
			// System.out.println(jsonBody);
			msGraphClient.callMicrosoftGraph(path, jsonBody);

		} catch (Exception ex) {
			logger.error("Cannot send email", ex);
		}
	}

	/**
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	private JsonObject createAttachment(String path) throws Exception {
		JsonObject attachmentJsonObject = new JsonObject();

		File file = new File(path);
		attachmentJsonObject.addProperty("@odata.type", "#microsoft.graph.fileAttachment");
		attachmentJsonObject.addProperty("name", file.getName());
		attachmentJsonObject.addProperty("contentType", "text/plain");

		byte[] content = FileUtils.readFileToByteArray(file);
		attachmentJsonObject.addProperty("contentBytes", Base64.getEncoder().encodeToString(content));

		return attachmentJsonObject;
	}

	/**
	 * 
	 * @param tos
	 * @return
	 */
	private JsonArray createRecipients(Collection<String> tos) {
		JsonArray recipientsJsonArray = new JsonArray();
		if (tos != null) {
			for (String to : tos) {
				if (to != null) {
					String[] addresses = to.split("[,; ]");
					for (String address : addresses) {
						if (address != null && address.length() > 5 && address.indexOf("@") != -1) {
							recipientsJsonArray.add(createRecipient(address));
						}
					}
				}
			}
		}
		return recipientsJsonArray;
	}

	/**
	 * 
	 * @param email
	 * @return
	 */
	private JsonObject createRecipient(String email) {
		JsonObject recipientJsonObject = new JsonObject();
		JsonObject emailJsonObject = new JsonObject();
		recipientJsonObject.add("emailAddress", emailJsonObject);
		emailJsonObject.addProperty("address", email);

		return recipientJsonObject;
	}

	/**
	 * 
	 * @param message
	 * @return
	 * @throws Exception
	 */
	public List<File> getAttachments(Message message) throws Exception {
		List<File> attachments = new ArrayList<>();

		// list attachments
		String path = "/users/" + defaultUserPrincipalName + "/messages/" + message.getId() + "/attachments";
		JsonObject rootJonsObject = msGraphClient.callMicrosoftGraph(path);
		for (JsonElement jsonElement : rootJonsObject.get("value").getAsJsonArray()) {
			JsonObject attachmentJsonObject = (JsonObject) jsonElement;

			String name = attachmentJsonObject.get("name").getAsString();
			// String contentType = attachmentJsonObject.get("contentType").getAsString();
			// int size = attachmentJsonObject.get("size").getAsInt();
			if (attachmentJsonObject.get("contentBytes") != null) {
				String contentBytes = attachmentJsonObject.get("contentBytes").getAsString();

				// System.out.println(name + " (" + contentType + "): " + size);
				File file = File.createTempFile(name, "");
				byte[] bytes = Base64.getDecoder().decode(contentBytes);
				FileUtils.writeByteArrayToFile(file, bytes);

				attachments.add(file);
			}
		}

		return attachments;
	}

	private boolean isValidMessage(String messageId, String subject) {
		boolean validEmail = true;

		// verify invalid email
		if (subject == null || subject.trim().length() == 0 || subject.startsWith("Réponse automatique") || subject.startsWith("Automatic") || subject.startsWith("Fwd:") || subject.startsWith("Re:")
				|| subject.startsWith("RE:") || subject.startsWith("Out of Office") || subject.startsWith("out of the office")) {
			validEmail = false;
		} else if (subject.startsWith("Undeliverable:") || subject.startsWith("Message undeliverable:") || subject.startsWith("Failure") || subject.startsWith("Mail Delivery Subsystem")
				|| subject.startsWith("Non remis :") || subject.startsWith("Undelivered Mail") || subject.startsWith("Delivery Status Notification (Failure)")) {
			// logger.warn("Undeliverable email Subject [" + subject + "] Sent: " + message.getSentDate());
			validEmail = false;
		} else if (subject.startsWith("Microsoft 365 Message center") || subject.startsWith("Weekly digest: Microsoft service updates") || subject.startsWith("Major update from Message center")
				|| subject.startsWith("Your Case has been submitted") || subject.startsWith("Messages mis en quaranatine")) {
			// spam
			validEmail = false;
		}

		if (validEmail) {
			// logger.debug("Valid email based on subject [" + subject + "]");
		} else {
			logger.debug("Deleting invalid email based on subject [" + subject + "]");
			try {
				deleteMessage(messageId);
			} catch (Exception ex) {
				logger.warn("Cannot delete message: " + ex);
			}
		}
		return validEmail;
	}

	/**
	 *
	 * @throws Exception
	 */
	public void cleanMailbox() throws Exception {
		List<Message> messages = getMessages();

		for (Message message : messages) {
			isValidMessage(message.getId(), message.getSubject());
		}
	}

	public static void main(String[] args) throws Exception {
		ExchangeMessageUtil mu = new ExchangeMessageUtil();
		try {
			mu.connect();

			// List<Message> messages = mu.getMessages();
			// for (Message message : messages) {
			// System.out.println(message);
			//
			// if (message.hasAttachments()) {
			// List<File> attachments = mu.getAttachments(message);
			// for (File attachment : attachments) {
			// System.out.println(attachment.getAbsolutePath());
			//
			// attachment.delete();
			// }
			// }
			// // mu.deleteMessage(message.getId());
			// }

			// mu.cleanMailbox();
		} finally {
			mu.disconnect();
		}

		System.out.println("Done");
	}
}
