package com.sgitmanagement.expressoext.networkfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.sgitmanagement.expresso.exception.BaseException;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expresso.exception.ValidationException;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

public abstract class NetworkFileService extends BaseService {
	final private int chunk_size = 1024 * 1024; // 1MB chunks

	protected void verifyPermission(File file) throws Exception {
	}

	abstract protected String getRootPath();

	public File getPathFile(String path) {
		if (path == null || path.trim().equals("")) {
			path = "";
		} else {
			path = (path.startsWith("/") || path.startsWith("\\") ? "" : File.separator) + path;
		}

		File pathFile = new File(getRootPath() + path);
		return pathFile;
	}

	public List<NetworkFile> getFolderContent(String folderPath, boolean recursive) throws Exception {
		File folderPathFile = getPathFile(folderPath);

		// make sure that the user can read the folder
		verifyPermission(folderPathFile);

		if (!folderPathFile.exists() && folderPathFile.isDirectory()) {
			throw new ForbiddenException("Folder [" + folderPathFile.getAbsolutePath() + "] does not exists");
		}

		// list all files in folder
		return retrieveFiles(folderPath, folderPathFile, recursive);
	}

	private List<NetworkFile> retrieveFiles(String folderPath, File folderPathFile, boolean recursive) {
		List<NetworkFile> networkFiles = new ArrayList<>();
		File[] files = folderPathFile.listFiles();
		if (files != null) {
			for (File file : files) {
				NetworkFile networkFile = new NetworkFile(folderPath, file.getName(), file.isDirectory());
				networkFiles.add(networkFile);

				if (file.isDirectory() && recursive) {
					folderPath += File.separator + file.getName();
					networkFile.setNetworkFiles(retrieveFiles(folderPath, file, true));
				}
			}
		}
		return networkFiles;
	}

	public void deleteFile(String filePath) throws Exception {
		File filePathFile = getPathFile(filePath);

		// make sure that the user can read the file
		verifyPermission(filePathFile);

		filePathFile.delete();
	}

	public void downloadFile(String filePath) throws Exception {
		File filePathFile = getPathFile(filePath);

		// make sure that the user can read the file
		verifyPermission(filePathFile);

		InputStream is = new FileInputStream(filePathFile);
		HttpServletResponse response = getResponse();
		OutputStream os = null;
		String fileName = filePath;
		if (fileName.indexOf('\\') != -1) {
			fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);
		}
		if (fileName.indexOf('/') != -1) {
			fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
		}
		try {
			if (fileName.endsWith("pdf") || fileName.endsWith("txt") || fileName.endsWith("jpg") || fileName.endsWith("jpeg") || fileName.endsWith("png") || fileName.endsWith("bmp")) {
				response.setHeader("Content-disposition", "inline; filename=" + fileName);
			} else {
				response.setHeader("Content-disposition", "attachment; filename=" + fileName);
			}

			os = response.getOutputStream();
			IOUtils.copy(is, os);
		} catch (Exception ex) {
			throw new BaseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file [" + filePath + "]", ex);
		} finally {
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(os);
		}
	}

	public Response buildStream(String filePath, String range) throws Exception {

		File asset = getPathFile(filePath);

		// range not requested : Firefox does not send range headers
		if (range == null) {
			StreamingOutput streamer = output -> {
				try (FileChannel inputChannel = new FileInputStream(asset).getChannel(); WritableByteChannel outputChannel = Channels.newChannel(output)) {
					inputChannel.transferTo(0, inputChannel.size(), outputChannel);
				}
			};
			return Response.ok(streamer).status(200).header(HttpHeaders.CONTENT_LENGTH, asset.length()).build();
		}

		String[] ranges = range.split("=")[1].split("-");
		final int from = Integer.parseInt(ranges[0]);

		/*
		 * Chunk media if the range upper bound is unspecified. Chrome, Opera sends "bytes=0-"
		 */
		int to = chunk_size + from;
		if (to >= asset.length()) {
			to = (int) (asset.length() - 1);
		}
		if (ranges.length == 2) {
			to = Integer.parseInt(ranges[1]);
		}

		final String responseRange = String.format("bytes %d-%d/%d", from, to, asset.length());
		final RandomAccessFile raf = new RandomAccessFile(asset, "r");
		raf.seek(from);

		final int len = to - from + 1;
		final NetworkFileStreamer streamer = new NetworkFileStreamer(len, raf);
		Response.ResponseBuilder res = Response.ok(streamer).status(Response.Status.PARTIAL_CONTENT).header("Accept-Ranges", "bytes").header("Content-Range", responseRange)
				.header(HttpHeaders.CONTENT_LENGTH, streamer.getLenth()).header(HttpHeaders.LAST_MODIFIED, new Date(asset.lastModified()));
		return res.build();
	}

	/**
	 *
	 * @param fileInputStream
	 * @param fileName
	 * @param params
	 * @return
	 * @throws Exception
	 */
	protected String uploadFile(InputStream fileInputStream, String path, String fileName, Map<String, String> params) throws Exception {
		String fileExtension = null;
		if (fileName.indexOf('.') != -1) {
			fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
			verifyFileExtension(fileExtension);
		}
		fileName = purgeFileName(fileName);

		File file = getPathFile(path + File.separator + fileName);

		logger.debug("File [" + file.getAbsolutePath() + "]");
		if (file.exists()) {
			if (fileExtension != null) {
				fileName = fileName.substring(0, fileName.length() - fileExtension.length() - 1) + "-" + new Date().getTime() + "." + fileExtension;
			} else {
				fileName += "-" + new Date().getTime();
			}
			file = getPathFile(path + File.separator + fileName);
		}

		// write the file
		FileUtils.copyInputStreamToFile(fileInputStream, file);

		// change file permission
		changeFilePermission(file);

		return fileName;
	}

	private void changeFilePermission(File file) throws Exception {
		try {
			Set<PosixFilePermission> x666 = PosixFilePermissions.fromString("rw-rw-rw-");
			Files.setPosixFilePermissions(file.toPath(), x666);

			Set<PosixFilePermission> x777 = PosixFilePermissions.fromString("rwxrwxrwx");
			Files.setPosixFilePermissions(file.getParentFile().toPath(), x777);

			Files.setPosixFilePermissions(file.getParentFile().getParentFile().toPath(), x777);
		} catch (Exception e) {
			// ignore
		}
	}

	/**
	 * Verify if the file extension is allowed
	 *
	 * @param fileName
	 */
	private void verifyFileExtension(String fileExtension) {
		String[] allowedExtensions = new String[] {
				// image
				"jpg", "jpeg", "png", "bmp", "gif", "heic",
				// office
				"doc", "xls", "ppt", "docx", "xlsx", "pptx", "vsdx",
				// emails
				"eml", "msg",
				// text
				"pdf", "txt", "csv",
				// others
				"zip" };

		boolean contains = Arrays.stream(allowedExtensions).anyMatch(fileExtension::equalsIgnoreCase);
		if (!contains) {
			logger.warn("Found invalid extension [" + fileExtension + "]");

			Map<String, Object> params = new HashMap<>();
			params.put("allowedExtensions", String.join(", ", allowedExtensions));
			throw new ValidationException("invalidFileExtension", params);
		}
	}

	/**
	 * Remove invalid characters from the filename and make sure it is only a file name (no path)
	 *
	 * @param fileName
	 * @return
	 */
	private String purgeFileName(String fileName) {
		// verify the filename
		// remove the path if it contains / or \
		if (fileName.contains("\\")) {
			fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
		}
		if (fileName.contains("/")) {
			fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
		}
		fileName = Util.purgeInvalidFileNameCharacters(fileName);

		fileName = fileName.replace("\"", "");

		return fileName;
	}

	public boolean isImage(String fileName) {
		if (fileName.indexOf('.') == -1) {
			return false;
		} else {
			String[] allowedExtensions = new String[] { ".jpg", ".jpeg", ".png", ".bmp", ".gif" };
			String extension = fileName.substring(fileName.lastIndexOf('.'));
			return Arrays.stream(allowedExtensions).anyMatch(extension::equalsIgnoreCase);
		}
	}
}