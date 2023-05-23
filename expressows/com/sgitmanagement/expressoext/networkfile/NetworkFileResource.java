package com.sgitmanagement.expressoext.networkfile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.sgitmanagement.expresso.exception.InvalidCredentialsException;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public abstract class NetworkFileResource<S extends NetworkFileService> extends BaseResource<S> {

	public NetworkFileResource(@Context HttpServletRequest request, @Context HttpServletResponse response, Class<S> serviceClass) {
		super(request, response, serviceClass);
	}

	@GET
	@Path("folder")
	@Produces(MediaType.APPLICATION_JSON)
	public List<NetworkFile> getFolderContent(@QueryParam("folderPath") String folderPath, @DefaultValue("false") @QueryParam("recursive") boolean recursive) throws Exception {
		folderPath = java.net.URLDecoder.decode(folderPath, StandardCharsets.UTF_8.name());
		return getService().getFolderContent(folderPath, recursive);
	}

	@GET
	@Path("file")
	public void downloadFile(@QueryParam("filePath") String filePath) throws Exception {
		if (filePath != null && filePath.length() > 0) {
			filePath = java.net.URLDecoder.decode(filePath, StandardCharsets.UTF_8.name());
			getService().downloadFile(filePath);
		} else {
			// do nothing
		}
	}

	@GET
	@Path("stream")
	@Produces("video/mp4")
	public Response streamAudio(@HeaderParam("Range") String range, @QueryParam("filePath") String filePath) throws Exception {
		try {
			return getService().buildStream(filePath, range);
		} catch (Exception ex) {
			// Client closed the connection
			logger.debug("stream closed!" + ex);
			return null;
		}
	}

	/**
	 * Support file uploaded as Base64 encoded
	 *
	 * @throws Exception
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("base64")
	public String uploadFile(MultivaluedMap<String, String> formParams) throws Exception {
		String fileName = formParams.getFirst("fileName");
		String imgBase64 = formParams.getFirst("imgBase64");

		String fileType;
		if (fileName.endsWith(".png")) {
			// backward compatibility: remove it
			fileName = fileName.substring(0, fileName.length() - ".png".length());
		}

		if (imgBase64 != null && imgBase64.startsWith("data:") && imgBase64.indexOf(',') != -1) {
			// remove the header
			// data:image/png;base64,
			fileType = imgBase64.substring("data:image/".length());
			fileType = fileType.substring(0, fileType.indexOf(';'));

			imgBase64 = imgBase64.substring(imgBase64.indexOf(',') + 1);
		} else {
			// assume png
			fileType = ".png";
		}

		// complete the fileName
		fileName += "." + fileType;
		logger.info("Upload [" + fileName + "]");

		byte[] data = Base64.decodeBase64(imgBase64);
		InputStream fileInputStream = new ByteArrayInputStream(data);

		Map<String, String> params = new HashMap<>();
		for (String key : formParams.keySet()) {
			params.put(key, formParams.getFirst(key));
		}

		String path = params.computeIfAbsent("path", x -> ".");
		return uploadFile(fileInputStream, path, fileName, params);
	}

	/**
	 * Support file uploaded as multipart message
	 *
	 * @throws Exception
	 */
	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces(MediaType.APPLICATION_JSON)
	public String uploadFile(@FormDataParam("file") InputStream fileInputStream, @FormDataParam("file") FormDataContentDisposition fileMetaData, FormDataMultiPart formDataMultiPart) throws Exception {
		// Get file name with correct encoding, known bug in jersey : https://github.com/jersey/jersey/issues/3304
		String fileName = new String(fileMetaData.getFileName().getBytes("iso-8859-1"), "UTF-8");
		Map<String, String> params = new HashMap<>();
		for (BodyPart bodyPart : formDataMultiPart.getBodyParts()) {
			// System.out.println(bodyPart.getMediaType().getType() + "/" + bodyPart.getMediaType().getSubtype());

			if (bodyPart.getMediaType().getType().equals("text")) {
				if (bodyPart instanceof FormDataBodyPart) {
					FormDataBodyPart formDataBodyPart = (FormDataBodyPart) bodyPart;
					try {
						String value = URLDecoder.decode(formDataBodyPart.getValue(), "UTF-8");
						if (value != null && value.equals("null")) {
							value = null;
						}

						params.put(formDataBodyPart.getName(), value);
					} catch (Exception e) {
						// ignore. When uploading a txt file, it tries to parse it
					}
				}
			}
		}

		String path = params.computeIfAbsent("path", x -> ".");
		fileName = uploadFile(fileInputStream, path, fileName, params);
		return "{\"fileName\"=\"" + fileName + "\"}";
	}

	private String uploadFile(InputStream fileInputStream, String path, String fileName, Map<String, String> params) throws Exception {
		if (!SystemEnv.INSTANCE.isInProduction()) {
			// first we must validate the token
			String sessionToken = params.get("sessionToken");
			if (sessionToken == null) {
				sessionToken = getRequest().getHeader("X-Session-Token");
			}

			if (!Util.equals(sessionToken, getRequest().getSession().getAttribute("sessionToken"))) {
				throw new InvalidCredentialsException();
			}
		}

		return getService().uploadFile(fileInputStream, path, fileName, params);
	}
}