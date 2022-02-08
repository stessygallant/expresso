package com.sgitmanagement.expresso.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPFile;

public class FTPSClient extends FTPClient {
	org.apache.commons.net.ftp.FTPSClient ftps;

	public FTPSClient(String host, int port, String username, String password) throws Exception {
		ftps = new org.apache.commons.net.ftp.FTPSClient("ssl");
		ftps.connect(host, port);
		ftps.login(username, password);

		// Use passive mode as default because most of us are
		// behind firewalls these days.
		ftps.enterLocalPassiveMode();
	}

	public FTPSClient(String host, String username, String password) throws Exception {
		this(host, 22, username, password);
	}

	@Override
	public void downloadFile(String remoteFileName, String localFileName) throws Exception {
		// if (binaryTransfer) ftps.setFileType(FTP.BINARY_FILE_TYPE);
		OutputStream output = new FileOutputStream(localFileName);
		ftps.retrieveFile(remoteFileName, output);
		output.close();
	}

	@Override
	public byte[] downloadFile(String remoteFileName) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ftps.retrieveFile(remoteFileName, output);
		output.close();
		return output.toByteArray();
	}

	@Override
	public void uploadFile(String remoteDir, String remoteFileName, File file) throws Exception {
		// if (binaryTransfer) ftps.setFileType(FTP.BINARY_FILE_TYPE);
		InputStream input = new FileInputStream(file);
		ftps.changeWorkingDirectory(remoteDir);
		ftps.storeFile(remoteFileName, input);
		input.close();
	}

	@Override
	public void close() {
		try {
			ftps.logout();
		} catch (IOException e) {
			// ignore
		}
		try {
			if (ftps.isConnected()) {
				ftps.disconnect();
			}
		} catch (IOException e) {
			// ignore
		}
	}

	@Override
	public List<String> listFiles(String remoteDir) throws Exception {
		ftps.changeWorkingDirectory(remoteDir);
		FTPFile[] ftpFiles = ftps.listFiles();
		List<String> files = new ArrayList<>();
		for (FTPFile ftpFile : ftpFiles) {
			files.add(ftpFile.getName());
		}
		return files;
	}

	@Override
	public void renameFile(String oldFileName, String newFileName) throws Exception {
		ftps.rename(oldFileName, newFileName);
	}

	@Override
	public void deleteFile(String fileName) throws Exception {
		ftps.deleteFile(fileName);
	}
}
