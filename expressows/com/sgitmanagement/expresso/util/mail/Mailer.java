package com.sgitmanagement.expresso.util.mail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expresso.util.Util;

public enum Mailer {
	INSTANCE;

	final private Logger logger = LoggerFactory.getLogger(Mailer.class);

	private ExecutorService executor;

	private String support;
	private boolean sendEmail;
	private boolean saveEmailOnDisk;
	private String saveEmailOnDiskPath;
	private boolean bccSupport;
	private String fromAddress;
	private String fromName;
	private String mailSenderString;

	private String templateDir;

	private boolean useThreads;
	private boolean limitThreads;
	private boolean retryOnError;

	private Mailer() {
		Properties props = SystemEnv.INSTANCE.getProperties("mail");
		support = props.getProperty("mail.smtp.support");
		sendEmail = Boolean.parseBoolean(props.getProperty("mail.smtp.send_email", "true"));
		saveEmailOnDisk = Boolean.parseBoolean(props.getProperty("mail.smtp.save_email_on_disk", "false"));
		saveEmailOnDiskPath = props.getProperty("mail.smtp.save_email_on_disk_path");

		fromAddress = props.getProperty("mail.smtp.from");
		fromName = props.getProperty("mail.smtp.from_name");
		bccSupport = Boolean.parseBoolean(props.getProperty("mail.smtp.bcc_support", "false"));

		templateDir = props.getProperty("mail.smtp.template_dir");

		useThreads = Boolean.parseBoolean(props.getProperty("mail.smtp.use_threads", "true"));
		limitThreads = Boolean.parseBoolean(props.getProperty("mail.smtp.limit_threads", "true"));
		retryOnError = Boolean.parseBoolean(props.getProperty("mail.smtp.retry_on_error", "true"));

		mailSenderString = props.getProperty("mail.mailSender");
		logger.info("MailSender [" + mailSenderString + "]");

		if (limitThreads) {
			executor = Executors.newFixedThreadPool(3);
		}
	}

	public void sendMail(String subject, String messageBody) {
		sendMail(support, subject, messageBody);
	}

	public void sendMail(String to, String subject, String messageBody) {
		sendMail(Arrays.asList(to), null, null, null, subject, false, messageBody, null, this.bccSupport);
	}

	public void sendMail(String to, String cc, String replyTo, String subject, String messageBody) {
		sendMail(Arrays.asList(to), Arrays.asList(cc), null, replyTo, subject, false, messageBody, null, this.bccSupport);
	}

	public void sendMail(String to, String emailTemplate, Map<String, String> params) {
		sendMail(Arrays.asList(to), null, null, null, false, emailTemplate, params, null);
	}

	public void sendMail(String to, String cc, String emailTemplate, Map<String, String> params) {
		sendMail(Arrays.asList(to), Arrays.asList(cc), null, null, false, emailTemplate, params, null);
	}

	public void sendMail(String to, String cc, String emailTemplate, Map<String, String> params, String attachement) {
		sendMail(Arrays.asList(to), Arrays.asList(cc), null, null, false, emailTemplate, params, Arrays.asList(attachement));
	}

	public void sendMail(Collection<String> tos, Collection<String> ccs, String emailTemplate, Map<String, String> params) {
		sendMail(tos, ccs, null, null, false, emailTemplate, params, null);
	}

	public void sendMail(Collection<String> tos, Collection<String> ccs, Collection<String> bccs, String emailTemplate, Map<String, String> params) {
		sendMail(tos, ccs, bccs, null, false, emailTemplate, params, null);
	}

	public void sendMail(String to, String cc, String replyTo, String emailTemplate, Map<String, String> params) {
		sendMail(Arrays.asList(to), Arrays.asList(cc), null, replyTo, false, emailTemplate, params, null);
	}

	public void sendMail(String to, String cc, String bcc, String replyTo, String emailTemplate, Map<String, String> params) {
		sendMail(Arrays.asList(to), Arrays.asList(cc), Arrays.asList(bcc), replyTo, false, emailTemplate, params, null);
	}

	public void sendMail(Collection<String> tos, String subject, String messageBody) {
		sendMail(tos, null, null, null, subject, false, messageBody, null, this.bccSupport);
	}

	public void sendMail(Collection<String> tos, Collection<String> ccs, Collection<String> bccs, String replyTo, boolean importantFlag, String emailTemplate, Map<String, String> params,
			Collection<String> attachments) {
		// get the messageBody from the email template
		String messageBody = getMessageBody(emailTemplate, params);
		if (messageBody != null) {
			sendMail(tos, ccs, bccs, replyTo, false, messageBody, attachments);
		}
	}

	public void sendMail(Collection<String> tos, Collection<String> ccs, Collection<String> bccs, String replyTo, boolean importantFlag, String messageBody, Collection<String> attachments) {
		// get the subject from the title element in the HTML messageBody
		String subject = "";
		if (messageBody != null && messageBody.indexOf("<title>") != -1 && messageBody.indexOf("</title>") != -1) {
			subject = messageBody.substring(messageBody.indexOf("<title>") + "<title>".length(), messageBody.indexOf("</title>"));
		}

		// send the email
		sendMail(tos, ccs, bccs, replyTo, subject, false, messageBody, attachments, this.bccSupport);
	}

	public void sendMail(Collection<String> tos, Collection<String> ccs, Collection<String> bccs, String replyTo, String subject, boolean importantFlag, String messageBody,
			Collection<String> attachments) {
		sendMail(tos, ccs, bccs, replyTo, subject, importantFlag, messageBody, attachments, bccSupport);
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
				if (this.templateDir.startsWith("/") || this.templateDir.startsWith("\\")) {
					// get the files from the absolute path
					messageBody = FileUtils.readFileToString(new File(this.templateDir + File.separator + emailTemplate + ".html"), StandardCharsets.UTF_8);
					css = FileUtils.readFileToString(new File(this.templateDir + File.separator + "email.css"), StandardCharsets.UTF_8);
				} else {
					// get the files from the WAR file (WEB-INF/classes)
					messageBody = Util.getResourceFileContent(this.templateDir + File.separator + emailTemplate + ".html");
					css = Util.getResourceFileContent(this.templateDir + File.separator + "email.css");
				}
			} catch (Exception e) {
				logger.error("Cannot read the template file [" + emailTemplate + "]: " + e);
				throw e;
			}

			if (params == null) {
				params = new HashMap<>();
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

	public void sendMail(Collection<String> tos, Collection<String> ccs, Collection<String> bccs, String replyTo, String subject, boolean importantFlag, String messageBody,
			Collection<String> attachments, boolean bccSupport) {
		sendMail(tos, ccs, bccs, replyTo, subject, importantFlag, messageBody, attachments, bccSupport, false);
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
			Collection<String> attachments, boolean bccSupport, boolean skipMaxRecipientsValidation) {
		try {
			if (!SystemEnv.INSTANCE.isInProduction()) {
				messageBody += "<br>---------------------------------------------------";
				messageBody += "<br>Original TO : " + tos;
				messageBody += "<br>Original CC : " + ccs;
				messageBody += "<br>Original BCC: " + bccs;
				messageBody += "<br>Reply TO : " + replyTo;
				tos = Arrays.asList(support);
				ccs = bccs = null;
				replyTo = null;
			} else {
				// add the support to emails
				if (bccSupport) {
					if (bccs == null) {
						bccs = new ArrayList<>();
					}
					try {
						bccs.add(support);
					} catch (UnsupportedOperationException ex) {
						// this happen when the list has been built using Arrays.asList
						bccs = new ArrayList<>(bccs);
						bccs.add(support);
					}
				}
			}

			if (tos == null || tos.isEmpty() || ((String) tos.toArray()[0]) == null || ((String) tos.toArray()[0]).trim().length() == 0) {
				throw new Exception("TO address is null or not defined");
			}

			final Map<String, Object> map = new HashMap<>();
			map.put("tos", tos);
			map.put("ccs", ccs);
			map.put("bccs", bccs);
			map.put("replyTo", replyTo);
			map.put("subject", subject);
			map.put("importantFlag", importantFlag);
			map.put("messageBody", messageBody);
			map.put("attachments", attachments);
			map.put("skipMaxRecipientsValidation", skipMaxRecipientsValidation);

			if (sendEmail) {
				if (useThreads) {
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							try {
								sendMail(map);
							} catch (Exception ex) {
								if (retryOnError) {
									try {
										// we we retry a few times
										int waitInSeconds = 60;
										for (int i = 1; i <= 3; i++) {
											try {
												logger.warn(ex + " (will retry in " + (i * waitInSeconds) + " seconds)");

												// we will retry in n minutes
												Thread.sleep(i * waitInSeconds * 1000);
												sendMail(map);
												return;
											} catch (Exception ex2) {
												// try again
											}
										}

										// ok, there is nothing to do
										logger.error(ex.toString());

									} catch (Exception ex3) {
										// ignore
									}
								}
							}
						}
					};

					if (limitThreads) {
						// logger.debug("Limiting threads");
						executor.submit(runnable);
					} else {
						// logger.debug("New thread");
						new Thread(runnable).start();
					}
				} else {
					sendMail(map);
				}
			}

			if (saveEmailOnDisk) {
				File file = new File(saveEmailOnDiskPath + "\\" + subject + "-" + System.currentTimeMillis() + ".html");
				FileUtils.write(file, messageBody, StandardCharsets.UTF_8);
				logger.warn("mail.smtp.save_email_on_disk is enbaled, saving email to : " + file.getPath());
			}

		} catch (Exception ex) {
			logger.error("Exception sending email", ex);
		}
	}

	@SuppressWarnings("unchecked")
	private void sendMail(Map<String, Object> map) throws Exception {
		Collection<String> tos = (Collection<String>) map.get("tos");
		Collection<String> ccs = (Collection<String>) map.get("ccs");
		Collection<String> bccs = (Collection<String>) map.get("bccs");
		String replyTo = (String) map.get("replyTo");
		String subject = (String) map.get("subject");
		boolean importantFlag = (boolean) map.get("importantFlag");
		String messageBody = (String) map.get("messageBody");
		Collection<String> attachments = (Collection<String>) map.get("attachments");
		boolean skipMaxRecipientsValidation = (boolean) map.get("skipMaxRecipientsValidation");

		MailSender mailSender = null;
		try {
			if (mailSenderString == null || mailSenderString.equals("smtp")) {
				mailSender = new SMTPUtil();
			} else {
				mailSender = new ExchangeMessageUtil();
			}

			mailSender.connect();

			logger.debug("Sending email [" + subject + "] to [" + tos + "]");
			long startDate = new Date().getTime();

			mailSender.sendMail(fromAddress, fromName, tos, ccs, bccs, replyTo, subject, importantFlag, messageBody, attachments, skipMaxRecipientsValidation);

			long endDate = new Date().getTime();
			logger.info("Email sent [" + subject + "] to [" + tos + "] in " + (endDate - startDate) + " ms");
		} catch (Exception ex) {
			throw new Exception("Cannot send email [" + subject + "] to [" + tos + "]: " + ex);
		} finally {
			if (mailSender != null) {
				mailSender.disconnect();
			}
		}
	}

	/**
	 * 
	 */
	public void close() {
		if (executor != null) {
			try {
				executor.shutdown();
			} catch (Exception ex) {
				// ignore
			}
			executor = null;
		}
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Start");

		String to = "maxime.lemieux@glencore.ca";
		String cc = null;
		String bcc = null;
		String replyTo = "isabelleegirard@hotmail.com";
		String subject = "test";
		String messageBody = "asfsdfsdsdfs<br><b>asdfsdfaS</b>";
		boolean importantFlag = true;
		Collection<String> attachments = null;

		int i = 0;
		// for (i = 0; i < 10; i++) {
		Mailer.INSTANCE.sendMail(Arrays.asList(to), Arrays.asList(cc), Arrays.asList(bcc), replyTo, (i + 1) + " - " + subject, importantFlag, messageBody, attachments, true);
		// Thread.sleep(30000);
		// }

		if (Mailer.INSTANCE.executor != null) {
			Mailer.INSTANCE.executor.shutdown();
		}
		System.out.println("Done");

		// String emailTemplate = "test";
		// Map<String, String> params = new HashMap<>();
		// params.put("toto", "allo");
		//
		// Mailer.INSTANCE.sendMail(to, emailTemplate, params);
	}
}