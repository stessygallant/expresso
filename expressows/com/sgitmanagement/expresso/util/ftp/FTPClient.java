package com.sgitmanagement.expresso.util.ftp;

import java.io.File;
import java.util.List;

public abstract class FTPClient {
	public static final int TIMEOUT_IN_SECONDS = 10;

	public static FTPClient connect(String protocol, String host, int port, String username, String password) throws Exception {
		if (protocol.equals("sftp")) {
			return new SFTPClient(host, port, username, password);
		} else if (protocol.equals("ftps")) {
			return new FTPSClient(host, port, username, password);
		} else if (protocol.equals("ftp")) {
			return new UnsecureFTPClient(host, port, username, password);
		} else {
			throw new Exception("FTP protocol not supported [" + protocol + "]");
		}
	}

	public abstract List<String> listFiles(String remoteDir) throws Exception;

	public abstract void downloadFile(String remoteFileName, String localFileName) throws Exception;

	public abstract byte[] downloadFile(String remoteFileName) throws Exception;

	public abstract void uploadFile(String remoteDir, String remoteFileName, File file) throws Exception;

	public abstract void renameFile(String oldFileName, String newFileName) throws Exception;

	public abstract void deleteFile(String fileName) throws Exception;

	public abstract void close();
}
