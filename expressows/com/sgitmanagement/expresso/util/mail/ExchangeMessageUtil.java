package com.sgitmanagement.expresso.util.mail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
import com.sgitmanagement.expresso.util.Util;

/**
 * This class is a utilities to read and send messages
 *
 */
public class ExchangeMessageUtil {
	static private Logger logger = LoggerFactory.getLogger(ExchangeMessageUtil.class);
	static private Properties mailProps;
	static private String defaultUserPrincipalName;
	static private String supportEmail;
	static private String templateDir;

	private MsGraphClient msGraphClient;

	static {
		mailProps = SystemEnv.INSTANCE.getProperties("mail");
		defaultUserPrincipalName = mailProps.getProperty("mail.userPrincipalName");
		supportEmail = mailProps.getProperty("mail.smtp.support");
		templateDir = mailProps.getProperty("mail.smtp.template_dir");
	}

	/**
	 * 
	 * @throws Exception
	 */
	public ExchangeMessageUtil() throws Exception {
		connect();
	}

	/**
	 * 
	 * @throws Exception
	 */
	private void connect() throws Exception {
		this.msGraphClient = new MsGraphClient();
		this.msGraphClient.connect();

	}

	/**
	 * 
	 */
	public void disconnect() {
		try {
			msGraphClient.disconnect();
		} catch (Exception e) {
			// not much that we can do
		}
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

			String subject = messageJsonObject.get("subject").getAsString();
			if (filter == null || subject.contains(filter)) {
				String id = messageJsonObject.get("id").getAsString();
				boolean hasAttachments = messageJsonObject.get("hasAttachments").getAsBoolean();
				String senderEmail = messageJsonObject.get("sender").getAsJsonObject().get("emailAddress").getAsJsonObject().get("address").getAsString();
				String content = messageJsonObject.get("body").getAsJsonObject().get("content").getAsString();

				Message message = new Message(id, subject, senderEmail, content, hasAttachments);
				messages.add(message);
			}
		}

		return messages;
	}

	/**
	 * 
	 * @param id
	 * @throws Exception
	 */
	public void deleteMessage(String id) throws Exception {
		deleteMessage(defaultUserPrincipalName, id);
	}

	/**
	 * 
	 * @param userPrincipalName
	 * @param id
	 * @throws Exception
	 */
	private void deleteMessage(String userPrincipalName, String id) throws Exception {
		if (SystemEnv.INSTANCE.isInProduction()) {
			String path = "/users/" + userPrincipalName + "/messages/" + id;
			msGraphClient.callMicrosoftGraph(path, "DELETE", null, null, null);
		}
	}

	/**
	 * 
	 * @param userPrincipalName
	 * @param to
	 * @param subject
	 * @param body
	 * @throws Exception
	 */
	public void sendMessage(String to, String subject, String body) throws Exception {
		String path = "/users/" + defaultUserPrincipalName + "/sendMail";

		JsonObject topJsonObject = new JsonObject();

		// do not put in sent items
		topJsonObject.addProperty("saveToSentItems", false);

		// message
		JsonObject messageJsonObject = new JsonObject();
		topJsonObject.add("message", messageJsonObject);

		// subject
		messageJsonObject.addProperty("subject", subject);

		// body
		JsonObject bodyJsonObject = new JsonObject();
		messageJsonObject.add("body", bodyJsonObject);
		bodyJsonObject.addProperty("contentType", "HTML");
		bodyJsonObject.addProperty("content", Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8)));

		// recipients
		JsonArray recipientsJsonArray = new JsonArray();
		messageJsonObject.add("toRecipients", recipientsJsonArray);

		// ccRecipients

		// only 1 to for now
		JsonObject recipientJsonObject = new JsonObject();
		JsonObject emailJsonObject = new JsonObject();
		recipientJsonObject.add("emailAddress", emailJsonObject);
		emailJsonObject.addProperty("address", to);
		recipientsJsonArray.add(recipientJsonObject);

		// send
		String jsonBody = new GsonBuilder().setPrettyPrinting().create().toJson(topJsonObject);
		// System.out.println(jsonBody);
		msGraphClient.callMicrosoftGraph(path, jsonBody);
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
	 * @param bccSupport
	 */
	public void sendMail(Collection<String> tos, Collection<String> ccs, Collection<String> bccs, String replyTo, String subject, boolean importantFlag, String messageBody,
			Collection<String> attachments, boolean bccSupport) {
		try {
			if (!SystemEnv.INSTANCE.isInProduction()) {
				messageBody += "<br><br>---------------------------------------------------";
				messageBody += "<br>Original TO : " + tos;
				messageBody += "<br>Original CC : " + ccs;
				messageBody += "<br>Original BCC: " + bccs;
				messageBody += "<br>Reply TO : " + replyTo;
				tos = Arrays.asList(supportEmail);
				ccs = bccs = null;
				replyTo = null;
			}

			String path = "/users/" + defaultUserPrincipalName + "/sendMail";

			JsonObject rootJsonObject = new JsonObject();

			// do not put in sent items
			rootJsonObject.addProperty("saveToSentItems", false);

			// message
			JsonObject messageJsonObject = new JsonObject();
			rootJsonObject.add("message", messageJsonObject);

			// importance
			messageJsonObject.addProperty("importance", importantFlag ? "high" : "normal");

			// subject
			messageJsonObject.addProperty("subject", subject);

			// body
			JsonObject bodyJsonObject = new JsonObject();
			messageJsonObject.add("body", bodyJsonObject);
			bodyJsonObject.addProperty("contentType", "HTML");
			bodyJsonObject.addProperty("content", Base64.getEncoder().encodeToString(messageBody.getBytes(StandardCharsets.UTF_8)));

			// recipients
			messageJsonObject.add("toRecipients", createRecipients(tos));
			if (ccs != null) {
				messageJsonObject.add("ccRecipients", createRecipients(ccs));
			}
			if (bccs != null) {
				messageJsonObject.add("bccRecipients", createRecipients(bccs));
			}
			if (bccSupport) {
				JsonArray recipientsJsonArray;
				if (bccs != null) {
					recipientsJsonArray = messageJsonObject.get("bccRecipients").getAsJsonArray();
				} else {
					recipientsJsonArray = new JsonArray();
					messageJsonObject.add("bccRecipients", recipientsJsonArray);
				}
				recipientsJsonArray.add(createRecipient(supportEmail));
			}

			// replyTo
			if (replyTo != null) {
				messageJsonObject.add("replyTo", createRecipient(replyTo));
			}

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

	// ---------- sendMail
	public void sendMail(String to, String subject, String messageBody) {
		sendMail(to, null, subject, messageBody);
	}

	public void sendMail(String to, String cc, String subject, String messageBody) {
		sendMail(Arrays.asList(to), Arrays.asList(cc), subject, messageBody, null);
	}

	public void sendMail(Collection<String> tos, String subject, String messageBody) {
		sendMail(tos, null, subject, messageBody);
	}

	public void sendMail(Collection<String> tos, Collection<String> ccs, String subject, String messageBody) {
		sendMail(tos, ccs, subject, messageBody);
	}

	public void sendMail(Collection<String> tos, Collection<String> ccs, String subject, String messageBody, Collection<String> attachments) {
		sendMail(tos, ccs, null, null, subject, false, messageBody, attachments, true);
	}

	// ---------- sendMail with template
	public void sendMail(String to, String emailTemplate, Map<String, String> params) {
		sendMail(to, null, emailTemplate, params);
	}

	public void sendMail(String to, String cc, String emailTemplate, Map<String, String> params) {
		sendMail(Arrays.asList(to), Arrays.asList(cc), null, null, false, emailTemplate, params, null);
	}

	public void sendMail(Collection<String> tos, Collection<String> ccs, Collection<String> bccs, String replyTo, boolean importantFlag, String emailTemplate, Map<String, String> params,
			Collection<String> attachments) {
		// get the messageBody from the email template
		String messageBody = getMessageBody(emailTemplate, params);
		if (messageBody != null) {
			// get the subject from the title element in the HTML messageBody
			String subject = "";
			if (messageBody.indexOf("<title>") != -1 && messageBody.indexOf("</title>") != -1) {
				subject = messageBody.substring(messageBody.indexOf("<title>") + "<title>".length(), messageBody.indexOf("</title>"));
			}

			sendMail(tos, ccs, bccs, replyTo, subject, importantFlag, messageBody, attachments, true);
		}
	}

	public String getMessageBody(String emailTemplate, Map<String, String> params) {
		return getMessageBody(emailTemplate, params, false);
	}

	public String getMessageBody(String emailTemplate, Map<String, String> params, boolean encodeBaseHTMLEntities) {
		try {
			// get the email template
			String messageBody;

			// get the css template
			String css;

			try {
				if (templateDir.startsWith("/") || templateDir.startsWith("\\")) {
					// get the files from the absolute path
					messageBody = FileUtils.readFileToString(new File(templateDir + File.separator + emailTemplate + ".html"), StandardCharsets.UTF_8);
					css = FileUtils.readFileToString(new File(templateDir + File.separator + "email.css"), StandardCharsets.UTF_8);
				} else {
					// get the files from the WAR file (WEB-INF/classes)
					messageBody = Util.getResourceFileContent(templateDir + File.separator + emailTemplate + ".html");
					css = Util.getResourceFileContent(templateDir + File.separator + "email.css");
				}
			} catch (Exception e) {
				logger.error("Cannot read the template file [" + emailTemplate + "]: " + e);
				throw e;
			}

			// add css to params
			params.put("css", css);

			// replace the placeholders
			messageBody = Util.replacePlaceHolders(messageBody, params, encodeBaseHTMLEntities);

			return messageBody;

		} catch (Exception e) {
			logger.error("Cannot get Message Body from template", e);
			return null;
		}
	}

	public static void main(String[] args) throws Exception {
		ExchangeMessageUtil mu = new ExchangeMessageUtil();
		// mu.sendMessage("stessygallant@gmail.com", "test1", "allo test1");

		List<Message> messages = mu.getMessages();
		for (Message message : messages) {
			System.out.println(message);

			if (message.hasAttachments()) {
				List<File> attachments = mu.getAttachments(message);
				for (File attachment : attachments) {
					System.out.println(attachment.getAbsolutePath());

					attachment.delete();
				}
			}
			// mu.deleteMessage(message.getId());
		}

		mu.disconnect();
		System.out.println("Done");
	}
}
