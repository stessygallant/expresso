package com.sgitmanagement.expressoext.base;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.apache.commons.codec.binary.Base64;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.sgitmanagement.expresso.exception.InvalidCredentialsException;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expresso.util.Util;

public abstract class BaseFileResource<E extends BaseFile, S extends BaseFileService<E>> extends BaseEntityResource<E, S> {

	public BaseFileResource(Class<E> typeOfE, HttpServletRequest request, HttpServletResponse response) {
		super(typeOfE, request, response);
	}

	@GET
	@Path("file/{fileName}")
	// we ignore the filename. it is only for the browser
	public void downloadFile(@PathParam("fileName") String fileName) throws Exception {
		getService().downloadFile(getId());
	}

	/**
	 * Support file uploaded as Base64 encoded
	 *
	 * @throws Exception
	 */
	@Override
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public E performAction(MultivaluedMap<String, String> formParams) throws Exception {
		String fileName = formParams.getFirst("fileName");
		String imgBase64 = formParams.getFirst("imgBase64");

		if (imgBase64 != null && imgBase64.startsWith("data:") && imgBase64.indexOf(',') != -1) {
			// remove the header
			// data:image/png;base64,
			imgBase64 = imgBase64.substring(imgBase64.indexOf(',') + 1);
		}

		byte[] data = Base64.decodeBase64(imgBase64);
		InputStream fileInputStream = new ByteArrayInputStream(data);

		Map<String, String> params = new HashMap<>();
		for (String key : formParams.keySet()) {
			params.put(key, formParams.getFirst(key));
		}

		return uploadFile(fileInputStream, fileName, params);
	}

	/**
	 * Support file uploaded as multipart message
	 *
	 * @throws Exception
	 */
	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces(MediaType.APPLICATION_JSON)
	public E uploadFile(@FormDataParam("file") InputStream fileInputStream, @FormDataParam("file") FormDataContentDisposition fileMetaData, FormDataMultiPart formDataMultiPart) throws Exception {
		// Get file name with correct encoding, known bug in jersey : https://github.com/jersey/jersey/issues/3304
		String fileName = new String(fileMetaData.getFileName().getBytes("iso-8859-1"), "UTF-8");

		Map<String, String> params = new HashMap<>();
		for (BodyPart bodyPart : formDataMultiPart.getBodyParts()) {
			// System.out.println(bodyPart.getMediaType().getType() + "/" + bodyPart.getMediaType().getSubtype());

			if (bodyPart.getMediaType().getType().equals("text")) {
				if (bodyPart instanceof FormDataBodyPart) {
					FormDataBodyPart formDataBodyPart = (FormDataBodyPart) bodyPart;
					String value = URLDecoder.decode(formDataBodyPart.getValue(), "UTF-8");
					if (value != null && value.equals("null")) {
						value = null;
					}

					params.put(formDataBodyPart.getName(), value);
				}
			}
		}
		return uploadFile(fileInputStream, fileName, params);
	}

	private E uploadFile(InputStream fileInputStream, String fileName, Map<String, String> params) throws Exception {
		// first we must validate the token
		if (!SystemEnv.INSTANCE.isInProduction()) {
			String sessionToken = params.get("sessionToken");
			if (!Util.equals(sessionToken, getRequest().getSession().getAttribute("sessionToken"))) {
				throw new InvalidCredentialsException();
			}
		}

		try {
			getService().getPersistenceManager().startTransaction(getEntityManager());
			E e = get(getId());
			return getService().uploadFile(e, fileInputStream, fileName, params);
		} catch (Exception ex) {
			getService().getPersistenceManager().rollback(getEntityManager());
			throw ex;
		} finally {
			getService().getPersistenceManager().commit(getEntityManager());
		}
	}
}
