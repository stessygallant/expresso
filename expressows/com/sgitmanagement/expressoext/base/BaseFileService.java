package com.sgitmanagement.expressoext.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.sgitmanagement.expresso.exception.BaseException;
import com.sgitmanagement.expresso.exception.ValidationException;
import com.sgitmanagement.expresso.util.ImageUtil;
import com.sgitmanagement.expresso.util.SSHClient;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expresso.util.Util;

import jakarta.servlet.http.HttpServletResponse;

abstract public class BaseFileService<E extends BaseFile> extends BaseEntityService<E> {
	static public String rootPath;
	static {
		rootPath = SystemEnv.INSTANCE.getProperties("config").getProperty("documents_dir");
	}

	/**
	 * Must be implemented by subclass to create the entity based on the parameters
	 *
	 * @param e
	 * @param params
	 * @return
	 * @throws Exception
	 */
	abstract protected E merge(E e, Map<String, String> params) throws Exception;

	@Override
	public void delete(Integer id) throws Exception {
		E e = get(id);

		// delete the file
		deleteFile(e);

		// then delete the entity
		super.delete(id);
	}

	/**
	 * Get the folder for the entity
	 *
	 * @param e
	 * @return
	 */
	protected String getFolder(E e) {
		return StringUtils.uncapitalize(getTypeOfE().getSimpleName()) + File.separator + e.getId();
	}

	public void downloadFile(int id) throws Exception {
		E e = get(id);
		downloadFile(getResponse(), e);
	}

	/**
	 *
	 * @param filePath
	 * @param filename
	 * @return
	 */
	protected File getFile(String filePath, String fileName) {
		File file;
		if (new File(filePath).isAbsolute()) {
			file = new File(filePath + File.separator + fileName);
		} else {
			file = new File(rootPath + File.separator + filePath + File.separator + fileName);
		}
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		return file;
	}

	/**
	 *
	 * @param request
	 * @param filePath
	 * @throws Exception
	 */
	public void deleteFile(E e) throws Exception {
		String filePath = getFolder(e);
		String fileName = e.getFileName();
		if (fileName != null && fileName.length() > 0 && !fileName.equals("/") && !fileName.equals("\\")) {
			File file = getFile(e);
			if (file.isDirectory()) {
				throw new Exception("Cannot delete a directory: " + filePath);
			} else {
				logger.info("Deleting file [" + file.getCanonicalPath() + "]");
				file.delete();
			}
		}
	}

	/**
	 * 
	 * @param e
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public E uploadFile(E e, File file) throws Exception {
		try (InputStream inputStream = new FileInputStream(file)) {
			uploadFile(e, inputStream, file.getName(), null);
		}
		return e;
	}

	/**
	 *
	 * @param e
	 * @param fileInputStream
	 * @param fileName
	 * @param params
	 * @return
	 * @throws Exception
	 */
	protected E uploadFile(E e, InputStream fileInputStream, String fileName, Map<String, String> params) throws Exception {
		String fileExtension = null;
		if (fileName.indexOf('.') != -1) {
			fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
			verifyFileExtension(fileExtension);
		}

		fileName = purgeFileName(fileName);

		// now build the entity
		if (e == null) {
			e = getTypeOfE().getDeclaredConstructor().newInstance();
		}
		e.setFileName(fileName);

		// set the creationUSer
		if (params != null && params.get("creationUserId") != null) {
			Integer creationUserId = Integer.parseInt(params.get("creationUserId"));
			e.setCreationUserId(creationUserId);
		}

		e = merge(e, params);

		String filePath = getFolder(e);
		File file = getFile(filePath, fileName);

		if (file.exists()) {
			if (fileExtension != null) {
				fileName = fileName.substring(0, fileName.length() - fileExtension.length() - 1) + "-" + new Date().getTime() + "." + fileExtension;
			} else {
				fileName += "-" + new Date().getTime();
			}
			e.setFileName(fileName);
			file = getFile(filePath, fileName);
		}

		// write the file
		FileUtils.copyInputStreamToFile(fileInputStream, file);

		// if file is an image an there is a maxWidth, resize it
		if (isImage(fileName)) {
			if (params.get("maxWidth") != null) {
				int maxWidth = Integer.parseInt(params.get("maxWidth"));
				File targetImageFile = File.createTempFile(e.getFileNameNoSuffix(), "." + e.getSuffix());
				ImageUtil.resizeImage(file, targetImageFile, maxWidth, null);

				// replace the file with the new shrink file
				FileUtils.copyFile(targetImageFile, file);
				targetImageFile.delete();
			}

			// if the file is an image, create a thumbnail
			try {
				ImageUtil.createThumbnailImage(file);
			} catch (Exception ex) {
				// if we cannot create the thumbnail, it is not dramatic
				logger.warn("Cannot create thumbnail for file [ " + file.getAbsolutePath() + "]: " + ex);
			}
		}

		// change file permission
		changeFilePermission(file);

		// scan for virus
		try {
			scanAntiVirus(file.getAbsolutePath());
		} catch (ValidationException ex1) {
			// if there is a virus, remove the file
			try {
				file.delete();
			} catch (Exception ex2) {
				// ignore
			}
			throw ex1;
		}

		return e;
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

	protected void scanAntiVirus(String filePath) throws Exception {
		String command = SystemEnv.INSTANCE.getDefaultProperties().getProperty("clamav_command");
		if (command != null && command.trim().length() > 0) {
			String user = SystemEnv.INSTANCE.getDefaultProperties().getProperty("clamav_user");
			String password = SystemEnv.INSTANCE.getDefaultProperties().getProperty("clamav_password");
			int port = Integer.parseInt(SystemEnv.INSTANCE.getDefaultProperties().getProperty("clamav_port"));
			String host = SystemEnv.INSTANCE.getDefaultProperties().getProperty("clamav_host");

			command += " '" + filePath + "'";
			logger.info("Anti virus Scan command [" + command + "]");
			SSHClient sshClient = new SSHClient();
			try {
				sshClient.connect(host, port, user, password);
				sshClient.executeCommand(command);
			} catch (Exception ex) {
				logger.error("File infected: " + ex.getMessage());
				throw new ValidationException("infectedFile");
			} finally {
				sshClient.close();
			}
		}
	}

	/**
	 *
	 * @param response
	 * @param filePath
	 * @throws Exception
	 */
	public void downloadFile(HttpServletResponse response, E e) throws Exception {

		File file = getFile(e);
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(file);

			response.setHeader("Content-disposition", "inline; filename=" + e.getFileName());
			// response.setContentType(contentType.getMimeType());

			os = response.getOutputStream();
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			os.flush();
		} catch (Exception ex) {
			throw new BaseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file [" + file.getAbsolutePath() + "]", ex);
		} finally {
			try {
				is.close();
			} catch (Exception ex) {
			}
			try {
				os.close();
			} catch (Exception ex) {
			}
		}
	}

	public File getFile(E e) {
		String filePath = getFolder(e);
		String fileName = e.getFileName();
		return getFile(filePath, fileName);
	}

	/**
	 * Remove invalid charaters from the filename and make sure it is only a file name (no path)
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
		String[] allowedExtensions = new String[] { ".jpg", ".jpeg", ".png", ".bmp", ".gif" };
		String extension = fileName.substring(fileName.lastIndexOf('.'));
		return Arrays.stream(allowedExtensions).anyMatch(extension::equalsIgnoreCase);
	}

	/**
	 * Verify if the file extension is allowed
	 *
	 * @param fileName
	 */
	private void verifyFileExtension(String fileExtension) {
		String[] allowedExtensions = new String[] {
				// image
				"jpg", "jpeg", "png", "bmp", "gif",
				// office
				"doc", "xls", "ppt", "docx", "xlsx", "pptx",
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
}
