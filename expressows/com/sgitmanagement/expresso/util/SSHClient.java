package com.sgitmanagement.expresso.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class SSHClient {
	static final private Logger logger = LoggerFactory.getLogger(SSHClient.class);

	static final private int COMMAND_TIMEOUT_MS = 1 * 60 * 1000; // 1 min
	static final private int CONNECT_TIMEOUT_MS = 5 * 1000; // 5 seconds

	private Session session;

	public SSHClient() {

	}

	public SSHClient(String host, int port, String user, String password) throws Exception {
		connect(host, port, user, password);
	}

	public void connect(String host, int port, String user, String password) throws Exception {
		connect(host, port, user, password, CONNECT_TIMEOUT_MS);
	}

	/**
	 *
	 * @param host
	 * @param port
	 * @param user
	 * @param password
	 * @throws Exception
	 */
	public void connect(String host, int port, String user, String password, int timeoutInMillisecond) throws Exception {
		JSch jsch = new JSch();

		session = jsch.getSession(user, host, port);

		if (password != null && password.trim().length() > 0) {
			UserInfo ui = new UserInfo() {

				@Override
				public void showMessage(String message) {
					// System.out.println("SSH message: " + message);
				}

				@Override
				public boolean promptYesNo(String question) {
					// System.out.println("SSH question: " + question);
					return true;
				}

				@Override
				public boolean promptPassword(String arg0) {
					return password != null;
				}

				@Override
				public boolean promptPassphrase(String arg0) {
					return false;
				}

				@Override
				public String getPassword() {
					return password;
				}

				@Override
				public String getPassphrase() {
					return null;
				}
			};
			session.setUserInfo(ui);
		} else {
			session.setConfig("PreferredAuthentications", "publickey");
			jsch.setKnownHosts("~/.ssh/known_hosts");
			jsch.addIdentity("~/.ssh/id_rsa");
			session.setConfig("StrictHostKeyChecking", "no");
		}
		session.setTimeout(timeoutInMillisecond);
		session.connect();
	}

	/**
	 *
	 */
	public void close() {
		if (session != null) {
			try {
				session.disconnect();
			} catch (Exception ex) {
				// ignore
			}
			session = null;
		}
	}

	public String executeCommand(String command) throws Exception {
		return executeCommand(command, COMMAND_TIMEOUT_MS);
	}

	/**
	 *
	 * @param host
	 * @param user
	 * @param password
	 * @param command
	 * @return
	 * @throws Exception
	 */
	public String executeCommand(String command, int timeoutInMillisecond) throws Exception {
		ChannelExec channel = null;

		StringBuilder sb = new StringBuilder();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		try {
			channel = (ChannelExec) session.openChannel("exec");
			channel.setCommand(command);
			channel.setInputStream(null);
			channel.setErrStream(stderr);

			// execute the command and get the output
			InputStream in = channel.getInputStream();
			channel.connect();

			Date startDate = new Date();
			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0) {
						break;
					}
					sb.append(new String(tmp, 0, i));
				}

				if (channel.isClosed()) {
					if (in.available() > 0) {
						continue;
					}
					if (channel.getExitStatus() != 0) {
						// logger.debug("exit-status: " + channel.getExitStatus());
						throw new Exception(sb + "\n" + "Exit status [" + channel.getExitStatus() + "]: " + stderr.toString());
					}
					break;
				}
				// System.out.println("2: " + new Date());

				// after the timeout, just kill the process
				if ((new Date().getTime() - startDate.getTime()) > timeoutInMillisecond) {
					// System.out.println("Stopping process [" + command + "]");
					channel.sendSignal("2"); // CTRL + C - interrupt
					break;
				}

				try {
					Thread.sleep(200);
				} catch (Exception ee) {
					// ignore
				}
			}
		} finally {
			if (channel != null) {
				channel.disconnect();
			}
		}

		// if there is any error message
		if (stderr.size() > 0) {
			throw new Exception(sb.toString() + "\n" + stderr.toString());
		}

		return sb.toString();
	}

	/**
	 * Helper method. Useful for only 1 call
	 *
	 * @param host
	 * @param port
	 * @param user
	 * @param password
	 * @param command
	 * @return
	 * @throws Exception
	 */
	public static String executeCommand(String host, int port, String user, String password, String command, int timeoutInMillisecond) throws Exception {
		SSHClient sshClient = new SSHClient();
		try {
			sshClient.connect(host, port, user, password, CONNECT_TIMEOUT_MS);
			return sshClient.executeCommand(command, timeoutInMillisecond);
		} finally {
			sshClient.close();
		}
	}

	/**
	 * Try to reach the server.
	 *
	 * @param ipAddress
	 * @return
	 */
	public static boolean isReachable(String ipAddress) throws Exception {
		boolean reachable = false;
		Process process = null;
		try {
			// cannot use this because it needs to be privileged
			// InetAddress address = InetAddress.getByName(ipAddress);
			// reachable = address.isReachable(3000);

			String[] command = { "ping", "-c", "1", "-W", "3", "-q", ipAddress };
			process = Runtime.getRuntime().exec(command);
			process.waitFor();
			reachable = (process.exitValue() == 0);
		} catch (Exception e) {
			logger.warn("Cannot reach modem IP [" + ipAddress + "]");
			throw new Exception("Cannot reach modem IP [" + ipAddress + "]: " + e.getMessage());
		} finally {
			if (process != null) {
				try {
					process.destroy();
				} catch (Exception ex) {
					// ignore
				}
			}
		}

		return reachable;
	}
}
