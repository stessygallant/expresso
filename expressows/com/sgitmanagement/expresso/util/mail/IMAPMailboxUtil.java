package com.sgitmanagement.expresso.util.mail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.util.SystemEnv;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;

import jakarta.mail.Flags;
import jakarta.mail.Flags.Flag;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;

/**
 * This class is a utilities to read mailbox and return messages
 * 
 * @author mlemieux4
 * 
 */
public class IMAPMailboxUtil {
	static final private Logger logger = LoggerFactory.getLogger(IMAPMailboxUtil.class);

	static final private int RETRY_DELAY = 5000;
	static private Properties mailProperties;

	private Object WAIT_LOCK = new Object();
	private Session session;
	private String userName;
	private String password;
	private IMAPStore store;
	private int errorsCount;

	static {
		mailProperties = SystemEnv.INSTANCE.getProperties("mail");
	}

	/**
	 * 
	 * @param domain
	 * @param userName
	 * @param mailbox
	 * @param password
	 * @throws Exception
	 */
	public IMAPMailboxUtil(String domain, String userName, String mailbox, String password) throws Exception {
		this(domain + "\\" + userName + "\\" + mailbox, password);
	}

	/**
	 * 
	 * @param userName
	 * @param password
	 * @throws Exception
	 */
	public IMAPMailboxUtil(String domain, String userName, String password) throws Exception {
		this(domain + "\\" + userName, password);
	}

	/**
	 * 
	 * @param userName
	 * @param password
	 * @throws Exception
	 */
	public IMAPMailboxUtil(String userName, String password) throws Exception {
		this.userName = userName;
		this.password = password;
		this.errorsCount = 0;

		Properties props = new Properties();

		props.setProperty("mail.imap.auth.plain.disable", "true");
		props.setProperty("mail.imap.auth.ntlm.disable", "true");
		props.setProperty("mail.imap.partialfetch", "false");
		props.setProperty("mail.imap.ssl.trust", "*");
		props.setProperty("mail.imap.ssl.checkserveridentity", "false");
		props.setProperty("mail.imap.ssl.enable", mailProperties.getProperty("mail.imap.ssl.enable", "false"));
		props.setProperty("mail.imap.starttls.enable", mailProperties.getProperty("mail.imap.starttls.enable", "false"));
		props.setProperty("mail.imap.starttls.required", mailProperties.getProperty("mail.imap.starttls.required", "false"));

		this.session = Session.getInstance(props, null);
	}

	public void connect() throws Exception {
		logger.info("Connecting to IMAP [" + this.userName + "]");

		// Flag to use when debugging connection to IMAP Exchange
		// session.setDebug(true);

		String hostName = mailProperties.getProperty("mail.imap.host");
		String protocol = mailProperties.getProperty("mail.imap.protocol");
		String port = mailProperties.getProperty("mail.imap.port");

		try {
			this.store = (IMAPStore) session.getStore(protocol);
			store.connect(hostName, Integer.valueOf(port), this.userName, this.password);

		} catch (MessagingException e) {
			errorsCount++;

			if (errorsCount < 3) {
				logger.warn("Error trying to connect to mailbox, waiting [" + RETRY_DELAY + "] and retrying");

				synchronized (WAIT_LOCK) {
					WAIT_LOCK.wait(RETRY_DELAY);
				}

				// retry to connect
				connect();
			} else {
				logger.error("Error trying to connect to mailbox :", e);
				throw e;
			}
		}
	}

	/**
	 * Get all messages from a folder
	 * 
	 * @param folder
	 * 
	 * @return List of message or Empty list when there are no new messages.
	 * @throws MessagingException
	 * @throws InterruptedException
	 */
	public List<Message> getMessages(IMAPFolder folder) throws MessagingException {
		Message[] messages = folder.getMessages();
		return Arrays.asList(messages);
	}

	/**
	 * Get the messages from the Inbox using filters.
	 *
	 * @param subjectFilter  null for all messages
	 * @param fromDateFilter null for all messages
	 * @return
	 * @throws Exception
	 */
	public List<Message> getMessages(IMAPFolder folder, String subjectFilter, Date fromDateFilter) throws Exception {
		List<Message> msgList = new ArrayList<>();
		for (Message msg : getMessages(folder)) {
			String subject = msg.getSubject();
			Date sendDate = msg.getSentDate();

			if ((fromDateFilter == null || fromDateFilter.before(sendDate)) && (subjectFilter == null || (subject != null && subject.contains(subjectFilter)))) {
				msgList.add(msg);
			}
		}
		return msgList;
	}

	public void markAsUnread(IMAPFolder folder, List<Message> messages) throws MessagingException {
		folder.setFlags(messages.toArray(new Message[messages.size()]), new Flags(Flag.SEEN), false);
	}

	public void moveMessage(Message message, IMAPFolder sourceFolder, IMAPFolder destinationFolder) throws MessagingException, InterruptedException, Exception {
		List<Message> messages = new ArrayList<>();
		messages.add(message);
		moveMessages(messages, sourceFolder, destinationFolder);
	}

	public void moveMessages(List<Message> messages, IMAPFolder sourceFolder, IMAPFolder destinationFolder) throws MessagingException, InterruptedException, Exception {

		// Do nothing if source or destination folder is not defined.
		// Or if there is no message to move
		if (sourceFolder == null || destinationFolder == null || messages == null || messages.isEmpty()) {
			return;
		}

		// source.moveMessages(source.getMessages(), destination);
		sourceFolder.moveMessages(messages.toArray(new Message[messages.size()]), destinationFolder);
	}

	/**
	 * Once the folder is opened, if we get the messages, we get the messages at the time of the opening
	 * 
	 * @param folder
	 * @throws MessagingException
	 */
	public void refreshFolder(IMAPFolder folder) throws MessagingException {
		if (folder.isOpen()) {
			folder.close(true);
		}
		folder.open(Folder.READ_WRITE);
	}

	public IMAPFolder openFolder(String folderName) throws MessagingException {
		IMAPFolder folder = (IMAPFolder) store.getFolder(folderName);
		folder.open(Folder.READ_WRITE);
		return folder;
	}

	public void closeFolder(IMAPFolder folder) {
		try {
			if (folder != null && folder.isOpen()) {
				folder.close(true);
			}
		} catch (MessagingException e) {
			logger.error("Error while trying to close folder :", e);
		}
	}

	public void closeConnection() {
		try {
			if (store != null && store.isConnected()) {
				logger.debug("Closing current IMAP connection");
				store.close();
			} else {
				logger.debug("No connection found to close");
			}
		} catch (MessagingException e) {
			logger.error("Error while trying to close connection :", e);
		}
	}
}
