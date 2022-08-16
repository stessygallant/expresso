package com.sgitmanagement.expresso.util.mail;

public class Message {
	private String id;
	private String subject;
	private String senderEmail;
	private String content;
	private boolean hasAttachments;

	public Message() {
		super();
	}

	public Message(String id, String subject, String senderEmail, String content, boolean hasAttachments) {
		this();
		this.id = id;
		this.subject = subject;
		this.senderEmail = senderEmail;
		this.content = content;
		this.hasAttachments = hasAttachments;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getSenderEmail() {
		return senderEmail;
	}

	public void setSenderEmail(String senderEmail) {
		this.senderEmail = senderEmail;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean hasAttachments() {
		return hasAttachments;
	}

	public void setHasAttachments(boolean hasAttachments) {
		this.hasAttachments = hasAttachments;
	}

	@Override
	public String toString() {
		return "Message [subject=" + subject + ", senderEmail=" + senderEmail + ", hasAttachments=" + hasAttachments + "]";
	}
}
