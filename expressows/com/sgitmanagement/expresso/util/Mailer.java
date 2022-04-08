package com.sgitmanagement.expresso.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
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

public enum Mailer {
	INSTANCE;

	final private Logger logger = LoggerFactory.getLogger(Mailer.class);

	private ExecutorService executor;

	private Session session;
	private String username;
	private String password;
	private String from;
	private String fromName;
	private String support;
	private boolean sendEmail;
	private boolean bccSupport;

	private String smtpHost;
	private String smtpPort;
	private boolean smtpAuth;
	private boolean smtpStartTLS;

	private String templateDir;

	private boolean useThreads;
	private boolean limitThreads;
	private boolean retryOnError;

	private Mailer() {
		Properties props = SystemEnv.INSTANCE.getProperties("mail");
		username = props.getProperty("mail.smtp.username");
		password = props.getProperty("mail.smtp.password");
		from = props.getProperty("mail.smtp.from");
		fromName = props.getProperty("mail.smtp.from_name");
		support = props.getProperty("mail.smtp.support");
		sendEmail = Boolean.parseBoolean(props.getProperty("mail.smtp.send_email", "true"));
		bccSupport = Boolean.parseBoolean(props.getProperty("mail.smtp.bcc_support", "false"));

		smtpHost = props.getProperty("mail.smtp.host");
		smtpPort = props.getProperty("mail.smtp.port");
		smtpAuth = Boolean.parseBoolean(props.getProperty("mail.smtp.auth", "false"));
		smtpStartTLS = Boolean.parseBoolean(props.getProperty("mail.smtp.starttls.enable", "false"));

		Properties properties = new Properties();
		properties.setProperty("mail.smtp.host", smtpHost);
		properties.setProperty("mail.smtp.port", smtpPort);
		properties.setProperty("mail.smtp.auth", "" + smtpAuth);
		properties.setProperty("mail.smtp.starttls.enable", "" + smtpStartTLS);

		properties.setProperty("mail.smtp.connectiontimeout", "" + (20 * 1000));
		properties.setProperty("mail.smtp.timeout", "" + (40 * 1000)); // read
		properties.setProperty("mail.smtp.writetimeout", "" + (40 * 1000));
		properties.setProperty("mail.smtp.ssl.trust", "*");

		// create the session
		session = Session.getInstance(properties, new javax.mail.Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		templateDir = props.getProperty("mail.smtp.template_dir");

		useThreads = Boolean.parseBoolean(props.getProperty("mail.smtp.use_threads", "true"));
		limitThreads = Boolean.parseBoolean(props.getProperty("mail.smtp.limit_threads", "true"));
		retryOnError = Boolean.parseBoolean(props.getProperty("mail.smtp.retry_on_error", "true"));

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

	public void sendMail(Collection<String> tos, Collection<String> ccs, Collection<String> bccs, String replyTo, String subject, boolean importantFlag, String messageBody,
			Collection<String> attachments, boolean bccSupport) {

		try {

			MimeMessage message = new MimeMessage(session);

			if (!SystemEnv.INSTANCE.isInProduction()) {
				messageBody += "<br>---------------------------------------------------";
				messageBody += "<br>Original TO : " + tos;
				messageBody += "<br>Original CC : " + ccs;
				messageBody += "<br>Original BCC: " + bccs;
				messageBody += "<br>Reply TO : " + replyTo;
				tos = Arrays.asList(support);
				ccs = bccs = null;
				replyTo = null;
			}

			if (importantFlag) {
				message.setHeader("X-Priority", "2");
			}
			message.setSubject(subject, "UTF-8");

			InternetAddress senderAddress = new InternetAddress(from);
			if (fromName != null) {
				senderAddress.setPersonal(fromName);
			}
			message.setFrom(senderAddress);

			// TO (mandatory)
			boolean atLeastOneTO = false;
			if (tos != null) {
				for (String to : tos) {
					if (to != null) {
						String[] addresses = to.split("[,; ]");
						for (String address : addresses) {
							if (address != null && address.length() > 5 && address.indexOf("@") != -1) {
								// System.out.println("TO [" + address +"]");
								atLeastOneTO = true;
								message.addRecipient(Message.RecipientType.TO, new InternetAddress(address));
							}
						}
					}
				}
			}
			if (!atLeastOneTO) {
				// throw new Exception("TO address is null or not defined");
				logger.warn("Email sent with empty TO [" + subject + "]");
				return;
			}

			// CC (optional)
			if (ccs != null) {
				for (String cc : ccs) {
					if (cc != null) {
						String[] addresses = cc.split("[,;]");
						for (String address : addresses) {
							if (address != null && address.length() > 0 && address.indexOf("@") != -1) {
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
								message.addRecipient(Message.RecipientType.BCC, new InternetAddress(address));
							}
						}
					}
				}
			}

			// add the support to emails
			if (bccSupport) {
				message.addRecipient(Message.RecipientType.BCC, new InternetAddress(support));
			}

			if (replyTo != null && replyTo.length() > 0 && replyTo.indexOf("@") != -1) {
				message.setReplyTo(new javax.mail.Address[] { new javax.mail.internet.InternetAddress(replyTo) });
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

			final String toString = String.join(",", tos);
			if (sendEmail) {
				if (useThreads) {
					// logger.debug("Using thread");
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							try {
								sendMail(message, toString, retryOnError);
							} catch (Exception e) {
								if (retryOnError) {
									try {
										// we we retry a few times
										int waitInSeconds = 60;
										for (int i = 1; i <= 3; i++) {
											try {
												logger.warn("Cannot send email [" + message.getSubject() + "] to [" + toString + "] (will retry in " + (i * waitInSeconds) + " seconds):  " + e);

												// we will retry in n minutes
												Thread.sleep(i * waitInSeconds * 1000);
												sendMail(message, toString, true);
												return;
											} catch (Exception e2) {
												// try again
											}
										}

										// ok, there is nothing to do
										sendMail(message, toString, false);
									} catch (Exception e3) {
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
					sendMail(message, toString, false);
				}
			}
		} catch (Exception e) {
			logger.error("Cannot send email", e);
		}
	}

	private void sendMail(Message message, String toString, boolean rethrowException) throws Exception {
		try {
			logger.info("Sending email [" + message.getSubject() + "] to [" + toString + "]");
			long startDate = new Date().getTime();
			Transport.send(message);
			long endDate = new Date().getTime();
			logger.info("Email sent [" + message.getSubject() + "] to [" + toString + "] in " + (endDate - startDate) + " ms");
		} catch (Exception e) {
			if (rethrowException) {
				throw e;
			} else {
				logger.error("Cannot send email [" + message.getSubject() + "] to [" + toString + "]: " + e);
			}
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

		String to = "stessy.gallant@gmail.com";
		String cc = null;
		String bcc = null;
		String replyTo = "isabelleegirard@hotmail.com";
		String subject = "test";
		String messageBody = "<b>asdfsdfaS</b>";
		boolean importantFlag = true;
		Collection<String> attachments = null;

		int i = 0;
		// for (i = 0; i < 50; i++) {
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