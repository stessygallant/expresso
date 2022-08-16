package com.sgitmanagement.expresso.util.ftp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class SFTPClient extends FTPClient {
	private Session session;
	private ChannelSftp channel;

	public SFTPClient(String host, int port, String username, String password) throws Exception {
		JSch jsch = new JSch();
		session = jsch.getSession(username, host, port);
		session.setConfig("StrictHostKeyChecking", "no");
		session.setPassword(password);
		session.connect();

		channel = (ChannelSftp) session.openChannel("sftp");
		channel.connect();
	}

	public SFTPClient(String host, String username, String password) throws Exception {
		this(host, 22, username, password);
	}

	@Override
	public void downloadFile(String remoteFileName, String localFileName) throws Exception {
		channel.get(remoteFileName, localFileName);

		// InputStream in = channel.get( "remote-file" );
		// channel.rm(remoteFileName);
		// channel.rename(remoteFileName, remoteFileName + "-" + new Date().getTime() + ".processed");
	}

	@Override
	public byte[] downloadFile(String remoteFileName) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		channel.get(remoteFileName, output);
		output.close();
		return output.toByteArray();
	}

	@Override
	public void uploadFile(String remoteDir, String remoteFileName, File file) throws Exception {
		FileInputStream fis = new FileInputStream(file);
		try {
			channel.put(fis, remoteFileName, ChannelSftp.OVERWRITE);
		} finally {
			fis.close();
		}
	}

	@Override
	public void close() {
		if (channel != null) {
			try {
				channel.disconnect();
			} catch (Exception e) {
				// ignore
			}
		}
		if (session != null) {
			try {
				session.disconnect();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	@Override
	public List<String> listFiles(String remoteDir) throws Exception {
		throw new NotImplementedException("todo");
	}

	@Override
	public void renameFile(String oldFileName, String newFileName) throws Exception {
		throw new NotImplementedException("todo");
	}

	@Override
	public void deleteFile(String fileName) throws Exception {
		throw new NotImplementedException("todo");
	}

}
