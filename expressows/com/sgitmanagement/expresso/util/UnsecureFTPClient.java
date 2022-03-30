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

public class UnsecureFTPClient extends FTPClient {

	org.apache.commons.net.ftp.FTPClient ftp;

	public UnsecureFTPClient(String host, int port, String username, String password) throws Exception {
		ftp = new org.apache.commons.net.ftp.FTPClient();
		ftp.setDefaultTimeout(TIMEOUT_IN_SECONDS * 1000);

		ftp.connect(host, port);
		ftp.login(username, password);

		// Use passive mode as default because most of us are
		// behind firewalls these days.
		ftp.enterLocalPassiveMode();
	}

	public UnsecureFTPClient(String host, String username, String password) throws Exception {
		this(host, 22, username, password);
	}

	@Override
	public void downloadFile(String remoteFileName, String localFileName) throws Exception {
		// if (binaryTransfer) ftps.setFileType(FTP.BINARY_FILE_TYPE);
		OutputStream output = new FileOutputStream(localFileName);
		ftp.retrieveFile(remoteFileName, output);
		output.close();
	}

	@Override
	public byte[] downloadFile(String remoteFileName) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ftp.retrieveFile(remoteFileName, output);
		output.close();
		return output.toByteArray();
	}

	@Override
	public void uploadFile(String remoteDir, String remoteFileName, File file) throws Exception {
		// if (binaryTransfer) ftps.setFileType(FTP.BINARY_FILE_TYPE);
		InputStream input = new FileInputStream(file);
		ftp.changeWorkingDirectory(remoteDir);
		ftp.storeFile(remoteFileName, input);
		input.close();
	}

	@Override
	public void close() {
		try {
			ftp.logout();
		} catch (IOException e) {
			// ignore
		}
		try {
			if (ftp.isConnected()) {
				ftp.disconnect();
			}
		} catch (IOException e) {
			// ignore
		}
	}

	@Override
	public List<String> listFiles(String remoteDir) throws Exception {
		ftp.changeWorkingDirectory(remoteDir);
		FTPFile[] ftpFiles = ftp.listFiles();
		List<String> files = new ArrayList<>();
		for (FTPFile ftpFile : ftpFiles) {
			files.add(ftpFile.getName());
		}
		return files;
	}

	@Override
	public void renameFile(String oldFileName, String newFileName) throws Exception {
		ftp.rename(oldFileName, newFileName);
	}

	@Override
	public void deleteFile(String fileName) throws Exception {
		ftp.deleteFile(fileName);
	}

}
