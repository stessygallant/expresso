package com.sgitmanagement.expresso.util.mail;

import java.util.Collection;

public interface MailSender {
	public void connect() throws Exception;

	public void disconnect() throws Exception;

	public void sendMail(String fromAddress, String fromName, Collection<String> tos, Collection<String> ccs, Collection<String> bccs, String replyTo, String subject, boolean importantFlag,
			String messageBody, Collection<String> attachments, boolean skipMaxRecipientsValidation) throws Exception;
}
