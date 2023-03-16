package com.sgitmanagement.expresso.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.sgitmanagement.expresso.base.KeyField;
import com.sgitmanagement.expresso.base.Sortable;
import com.sgitmanagement.expresso.exception.BaseException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class Util {
	private static final Logger logger = LoggerFactory.getLogger(Util.class);
	public static final String[] internalSubnets;

	static {
		if (SystemEnv.INSTANCE.getDefaultProperties().getProperty("internal_ipaddresses") != null) {
			internalSubnets = SystemEnv.INSTANCE.getDefaultProperties().getProperty("internal_ipaddresses").split("[ \\,]");
		} else {
			internalSubnets = null;
		}
	}

	static public boolean isNull(String s) {
		if (s == null || s.trim().length() == 0 || s.equals("null") || s.equals("undefined")) {
			return true;
		} else {
			return false;
		}
	}

	static public String nullifyIfNeeded(String s) {
		if (s == null || s.trim().length() == 0 || s.equals("null") || s.equals("undefined")) {
			return null;
		} else {
			return s.trim();
		}
	}

	static public String urlEncodeUTF8(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	static public String mapToUrlParams(Map<?, ?> map) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (entry.getKey() != null) {
				if (sb.length() > 0) {
					sb.append("&");
				}
				if (entry.getValue() != null) {
					sb.append(String.format("%s=%s", entry.getKey().toString(), urlEncodeUTF8(entry.getValue().toString())));
				} else {
					sb.append(entry.getKey());
				}
			}
		}
		return sb.toString();
	}

	static public String replaceNewlineByBR(String s) {
		if (s != null) {
			return s.replaceAll("\\R", "<br/>");
		} else {
			return null;
		}
	}

	static public String mapToFormEncoded(Map<?, ?> map) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (sb.length() > 0) {
				sb.append("&");
			}
			sb.append(String.format("%s=%s", (entry.getKey() != null ? entry.getKey().toString() : ""), entry.getValue() != null ? entry.getValue().toString() : ""));
		}
		return sb.toString();
	}

	/**
	 * Replace the place holders from the map in the string
	 *
	 * @param s   string to replace
	 * @param map map of placeholders <key, value>
	 * @return
	 */
	static public String replacePlaceHolders(String s, Map<String, String> map) {
		return replacePlaceHolders(s, map, false);
	}

	/**
	 * Replace the place holders from the map in the string
	 *
	 * @param s                      string to replace
	 * @param map                    map of placeholders <key, value>
	 * @param encodeBaseHTMLEntities true to replace base HTML entities
	 * @return
	 */

	static public String replacePlaceHolders(String s, Map<String, String> map, boolean encodeBaseHTMLEntities) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			String key = "{" + entry.getKey() + "}";
			String backwardKey = "%" + entry.getKey() + "%";
			String value = entry.getValue();
			if (encodeBaseHTMLEntities) {
				value = encodeBaseHTMLEntities(value);
			}
			if (s.indexOf(key) != -1) {
				s = s.replace(key, value == null ? "" : value);
			} else if (s.indexOf(backwardKey) != -1) {
				// backward compatibility %DESCRIPTION%
				s = s.replace(backwardKey, value == null ? "" : value);
			}
		}
		return s;
	}

	static public String encodeBaseHTMLEntities(String s) {
		if (s != null) {
			s = s.replaceAll("&", "&amp;"); // & - ampersand
			s = s.replaceAll("\"", "&quot;"); // " - double-quote
			s = s.replaceAll("<", "&lt;"); // < - less-than
			s = s.replaceAll(">", "&gt;"); // > - greater-than
			return replaceNewlineByBR(s);
		} else {
			return null;
		}
	}

	/**
	 * Remove from the string all invalid characters
	 *
	 * @param s
	 * @return
	 */
	static public String purgeInvalidFileNameCharacters(String s) {
		if (s != null) {
			StringBuilder sb = new StringBuilder(s.length());
			for (int i = 0, n = s.length(); i < n; i++) {
				char c = s.charAt(i);
				// keep only the first 255 characters (same code in ISO-8859-1 and UTF-8)
				// 47 = /
				// 92 = \
				// 58 = :
				// 63 = ?
				// 42 = *
				// 43 = +
				// 37 = %
				// 44 = ,
				// 39 = '
				// 59 = ;
				// 35 = #
				if (c >= 32 && c < 255 && c != 47 && c != 92 && c != 58 && c != 63 && c != 42 && c != 43 && c != 37 && c != 44 && c != 39 && c != 59 && c != 35) {
					sb.append(c);
				} else {
					// System.out.println("Skip [" + (int) c + "]: " + c);
				}
			}
			s = sb.toString();
		}
		return s;
	}

	/**
	 * Remove from the string all invalid characters
	 *
	 * @param s
	 * @return
	 */
	static public String purgeInvalidCharacters(String s) {
		if (s != null) {
			StringBuilder sb = new StringBuilder(s.length());
			for (int i = 0, n = s.length(); i < n; i++) {
				char c = s.charAt(i);
				// keep only the first 255 characters (same code in ISO-8859-1 and UTF-8)
				if (c >= 32 && c < 255) {
					sb.append(c);
				} else {
					// System.out.println("Skip [" + (int) c + "]: " + c);
				}
			}
			s = sb.toString();
		}
		return s;
	}

	/**
	 * Get the content of a resource file
	 *
	 * @param filePath relative path from the resources directory
	 * @return content of a resource file
	 */
	public static String getResourceFileContent(String filePath) throws Exception {
		InputStream inputStream = null;
		try {
			inputStream = SystemEnv.class.getClassLoader().getResourceAsStream(filePath);
			if (inputStream == null) {
				throw new Exception("Cannot find ressource file [" + filePath + "]");
			}

			return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	public static URL getResourceFile(String filePath) throws Exception {
		return SystemEnv.class.getClassLoader().getResource(filePath);
	}

	public static boolean equals(Object a, Object b) {
		if (a instanceof Date) {
			return (a != null && b != null && ((Date) a).getTime() == ((Date) b).getTime()) || (a == null && b == null);
		} else {
			return (a != null && a.equals(b)) || (a == null && b == null);
		}
	}

	public static String stripAccents(String input) {
		return input == null ? null : Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

	/**
	 * Get a field from a class going up to the super class if needed
	 *
	 * @param e
	 * @param fieldName
	 * @return
	 * @throws Exception
	 */
	public static Field getField(Object e, String fieldName) throws Exception {
		// handle sub field. Ex: equipment.equipmentNo
		if (fieldName.indexOf('.') != -1) {
			String topLevelFieldName = fieldName.substring(0, fieldName.indexOf('.'));
			Field topLevelField = getField(e, topLevelFieldName);

			Class<?> fieldTypeClass = topLevelField.getType();
			if (Collection.class.isAssignableFrom(fieldTypeClass)) {
				// we need to get the generic type
				ParameterizedType geneticType = (ParameterizedType) topLevelField.getGenericType();
				fieldTypeClass = (Class<?>) geneticType.getActualTypeArguments()[0];
			}

			String subFieldName = fieldName.substring(fieldName.indexOf('.') + 1);
			return getField(fieldTypeClass, subFieldName);
		} else {
			Class<?> clazz;
			if (e instanceof Class) {
				clazz = (Class<?>) e;
			} else {
				clazz = e.getClass();
			}
			while (clazz != null) {
				try {
					Field field = clazz.getDeclaredField(fieldName);
					field.setAccessible(true);
					return field;
				} catch (NoSuchFieldException ex) {
					clazz = clazz.getSuperclass();
				}
			}
		}
		return null;
	}

	/**
	 * If the keyField needs a padding, do it before the search
	 *
	 * @param keyField
	 * @param keyValue
	 * @return
	 * @throws Exception
	 */
	public static String formatKeyField(Field field, Object keyValue) {
		// always trim
		if (keyValue == null) {
			return null;
		} else {
			String key = ("" + keyValue).trim();
			try {
				KeyField keyFieldAnnotation = field.getAnnotation(KeyField.class);

				if (keyFieldAnnotation != null) {
					// format if needed
					if (keyFieldAnnotation.format().length() > 0) {
						key = Util.formatKey(keyFieldAnnotation.format(), key);
					}

					// add padding
					if (keyFieldAnnotation.padding() > 0) {
						// key = String.format("%1$" + keyFieldAnnotation.padding() + "s",
						// key).replace(' ', '0');
						key = StringUtils.leftPad(key, keyFieldAnnotation.padding(), '0');
					}

					// add a space padding
					if (keyFieldAnnotation.rightSpacePadding()) {
						key = StringUtils.rightPad(key, keyFieldAnnotation.length());
					}

					// add prefix
					if (keyFieldAnnotation.prefix().length() > 0) {
						if (!key.startsWith(keyFieldAnnotation.prefix())) {

							// add the prefix only if the key is complete
							if (keyFieldAnnotation.length() != 0 && (key.length() + keyFieldAnnotation.prefix().length()) != keyFieldAnnotation.length()) {
								// this means the key is not complete, do not add the prefix
							} else {
								key = keyFieldAnnotation.prefix() + key;
							}
						}
					}

					// verify the key total length
					if (keyFieldAnnotation.length() != 0) {
						if (key.length() != keyFieldAnnotation.length()) {
							// this means the key is not complete, we cannot search by EQUALS
							// should we search by CONTAINS?
						}
					}
				}
			} catch (Exception ex) {
				logger.warn("Cannot format keyfield: " + ex);
			}
			return key;
		}
	}

	/**
	 *
	 * @param format
	 * @param key
	 * @return
	 */
	private static String formatKey(String format, String key) {
		// 0: means digits (it will automatically left pad with 0)
		// x: letter lowercase
		// X: letter uppercase
		// -: mandatory dash
		// {}: enclosed format char are optional

		if (format == null || key == null || format.length() == 0) {
			return key;
		}

		// between dash, if digits, pad them as necessary
		StringBuilder sbKey = new StringBuilder(format.length());
		int keyIndex = 0;
		boolean optional = false;
		for (int i = 0; i < format.length(); i++) {
			char cFormat = format.charAt(i);

			if (cFormat == '{' || cFormat == '}') {
				optional = cFormat == '{';
			} else {

				if (keyIndex >= key.length()) {
					// no more key char
					break;
				}
				char cKey = key.charAt(keyIndex);

				// if we find a dash and it is not in the format, remove it
				if (cKey == '-' && cFormat != '-') {
					if (keyIndex + 1 >= key.length()) {
						// no more key char
						break;
					}
					cKey = key.charAt(++keyIndex);
				}

				switch (cFormat) {
				case '0':
					// count the number of digit needed
					int digitCount = 0;
					while (cFormat == '0') {
						digitCount++;
						if ((i + 1) == format.length()) {
							i++;
							break;
						}
						cFormat = format.charAt(++i);
					}
					// go back to the previous char in the format
					i--;

					// get all digits from the key
					// keep the digits in a buffer to pad them if needed
					StringBuilder sbDigit = new StringBuilder();
					while (Character.isDigit(cKey)) {
						sbDigit.append(cKey);
						if ((keyIndex + 1) == key.length()) {
							break;
						}
						cKey = key.charAt(++keyIndex);
					}

					if (sbDigit.length() != digitCount) {
						// pad with 0
						sbKey.append(StringUtils.leftPad(sbDigit.toString(), digitCount, '0'));
					} else {
						sbKey.append(sbDigit.toString());
					}
					break;
				case 'x':
					sbKey.append(Character.toLowerCase(cKey));
					keyIndex++;
					break;
				case 'X':
					sbKey.append(Character.toUpperCase(cKey));
					keyIndex++;
					break;
				case '-':
					// add "-" if needed
					if (cKey != '-') {
						sbKey.append('-');
					} else {
						if (!optional || keyIndex < key.length() - 1) {
							sbKey.append(cKey);
						}
						keyIndex++;
					}
					break;

				default:
					// invalid character
					break;
				}
			}
		}

		return sbKey.toString();

	}

	/**
	 * 
	 * @param typeClassName
	 * @return
	 */
	private static String getBasicObjectType(String typeClassName) {
		if (typeClassName.indexOf('.') != -1) {
			typeClassName = typeClassName.substring(typeClassName.lastIndexOf('.') + 1);
		}
		switch (typeClassName) {
		case "int":
		case "Integer":
			typeClassName = "Integer";
			break;
		case "long":
		case "Long":
			typeClassName = "Long";
			break;
		case "short":
		case "Short":
			typeClassName = "Short";
			break;

		case "float":
		case "Float":
			typeClassName = "Float";
			break;
		case "double":
		case "Double":
			typeClassName = "Double";
			break;

		case "boolean":
		case "Boolean":
			typeClassName = "Boolean";
			break;
		}
		return typeClassName;
	}

	/**
	 *
	 * @param value
	 * @param typeClassName
	 * @return
	 * @throws Exception
	 */
	public static Object convertValue(Object value, String expectedTypeClassName) throws Exception {
		Object result = null;
		if (value instanceof String) {
			value = nullifyIfNeeded((String) value);
		}
		if (value != null) {
			String valueType = getBasicObjectType(value.getClass().getSimpleName());
			expectedTypeClassName = getBasicObjectType(expectedTypeClassName);
			// System.out.println("Convert [" + value + "] from [" + valueType + "] to [" + expectedTypeClassName + "]");

			switch (expectedTypeClassName) {
			case "Short":
				if (valueType.equals("Short")) {
					result = value;
				} else if (valueType.equals("Long")) {
					result = ((Long) value).shortValue();
				} else if (valueType.equals("Integer")) {
					result = ((Integer) value).shortValue();
				} else if (valueType.equals("Float")) {
					result = ((Float) value).shortValue();
				} else if (valueType.equals("Double")) {
					result = ((Double) value).shortValue();
				} else {
					result = Short.parseShort((String) value);
				}
				break;
			case "Long":
				if (valueType.equals("Long")) {
					result = value;
				} else if (valueType.equals("Short")) {
					result = ((Short) value).longValue();
				} else if (valueType.equals("Integer")) {
					result = ((Integer) value).longValue();
				} else if (valueType.equals("Float")) {
					result = ((Float) value).longValue();
				} else if (valueType.equals("Double")) {
					result = ((Double) value).longValue();
				} else {
					result = Long.parseLong((String) value);
				}
				break;
			case "Integer":
				if (valueType.equals("Integer")) {
					result = value;
				} else if (valueType.equals("Short")) {
					result = ((Short) value).intValue();
				} else if (valueType.equals("Long")) {
					result = ((Long) value).intValue();
				} else if (valueType.equals("Float")) {
					result = ((Float) value).intValue();
				} else if (valueType.equals("Double")) {
					result = ((Double) value).intValue();
				} else {
					result = Integer.parseInt((String) value);
				}
				break;

			case "Float":
				if (valueType.equals("Float")) {
					result = value;
				} else if (valueType.equals("Short")) {
					result = ((Short) value).floatValue();
				} else if (valueType.equals("Long")) {
					result = ((Long) value).floatValue();
				} else if (valueType.equals("Integer")) {
					result = ((Integer) value).floatValue();
				} else if (valueType.equals("Double")) {
					result = ((Double) value).floatValue();
				} else {
					result = Float.parseFloat((String) value);
				}
				break;
			case "Double":
				if (valueType.equals("Double")) {
					result = value;
				} else if (valueType.equals("Short")) {
					result = ((Short) value).doubleValue();
				} else if (valueType.equals("Long")) {
					result = ((Long) value).doubleValue();
				} else if (valueType.equals("Integer")) {
					result = ((Integer) value).doubleValue();
				} else if (valueType.equals("Float")) {
					result = ((Float) value).doubleValue();
				} else {
					result = Double.parseDouble((String) value);
				}
				break;

			case "Date":
				if (valueType.equals("Date")) {
					result = value;
				} else if (valueType.equals("Timestamp")) {
					result = value;
				} else {
					result = DateUtil.parseDate(value);
				}
				break;

			case "String":
				if (valueType.equals("String")) {
					result = value;
				} else {
					result = "" + value;
				}
				break;

			case "Boolean":
				if (valueType.equals("Boolean")) {
					result = value;
				} else {
					result = Boolean.parseBoolean((String) value);
				}
				break;

			case "Array":
			case "Set":
			case "List":
				String stringValue = (String) value;
				// this assume a list of Integer: Ex 1,2,3
				if (stringValue.indexOf('[') == 0 && stringValue.indexOf(']') == (stringValue.length() - 1)) {
					stringValue = stringValue.substring(1, stringValue.length() - 1);
				}

				ArrayList<Integer> a = new ArrayList<>();
				for (String s : stringValue.split(",")) {
					if (s != null && s.trim().length() > 0) {
						a.add(Integer.parseInt(s.trim()));
					}
				}

				switch (expectedTypeClassName) {
				case "Array":
					result = a.toArray(new Integer[0]);
					break;
				case "Set":
					result = new HashSet<>(a);
					break;
				case "List":
					result = a;
					break;
				}
				break;

			default:
				throw new Exception("Expected Type not supported: " + expectedTypeClassName);
			}
		}

		return result;
	}

	/**
	 * Split a string into multiple string using the separator, but not if the separator is inside quote
	 *
	 * @param s
	 * @param sep
	 * @param removeQuotes
	 * @return
	 */
	static public String[] splitAvoidQuotes(String s, char sep, boolean removeQuotes) {
		int count = 0;
		int lastIndex = 0;
		List<String> list = new ArrayList<>();
		for (int i = 0, n = s.length(); i < n; i++) {
			char c = s.charAt(i);
			if (c == 34) { // quote "
				count++;
			} else if (c == sep) {
				if (count % 2 == 0) {
					if (removeQuotes && count == 2) {
						list.add(s.substring(++lastIndex, i - 1));

					} else {
						list.add(s.substring(lastIndex, i));
					}
					lastIndex = i + 1; // skip separator
					count = 0;
				}
			}
		}

		// last \n
		if (s.length() > lastIndex) {
			if (removeQuotes && count == 2) {
				list.add(s.substring(++lastIndex, s.length() - 1));

			} else {
				list.add(s.substring(lastIndex));
			}
		}

		return list.toArray(new String[0]);
	}

	/**
	 * Utility method to write on the HTTP response the stream of the file
	 *
	 * @param response
	 * @param e
	 * @throws Exception
	 */
	static public void downloadFile(HttpServletResponse response, File file) throws Exception {
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(file);

			response.setHeader("Content-disposition", "inline; filename=" + file.getName());
			// response.setContentType(contentType.getMimeType());

			os = response.getOutputStream();
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			os.flush();
		} catch (Exception ex) {
			throw new BaseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file [" + file.getAbsolutePath(), ex);
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

	/**
	 * Utility method to write on the HTTP response the stream of the file
	 *
	 * @param response
	 * @param e
	 * @throws Exception
	 */
	static public void downloadStream(HttpServletResponse response, InputStream is) throws Exception {
		OutputStream os = response.getOutputStream();
		try {
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			os.flush();
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

	static public boolean isInternalIpAddress(String ipAddress) {
		boolean internal = false;
		if (ipAddress != null && ipAddress.equals("0:0:0:0:0:0:0:1")) {
			// SubnetUtils does not support IPV6 for now
			ipAddress = "127.0.0.1";
		}
		if (ipAddress != null && internalSubnets != null) {
			for (String internalSubnet : internalSubnets) {
				SubnetInfo subnet = (new SubnetUtils(internalSubnet)).getInfo();
				internal = subnet.isInRange(ipAddress);
				if (internal) {
					break;
				}
			}
		}

		return internal;
	}

	static public String getIpAddress(HttpServletRequest req) {
		String ip = null;
		try {
			ip = req.getRemoteAddr();
			if (req.getHeader("X-Forwarded-For") != null) {
				ip = req.getHeader("X-Forwarded-For").split(",")[0];
			}
		} catch (Exception e) {
			// ignore
		}
		return ip;
	}

	static public String generateRandomPassword() {
		return generateRandomToken(15);
	}

	static public String generateRandomToken(int tokenLength) {
		String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		String password = RandomStringUtils.random(tokenLength, characters);
		return password;
	}

	static public String hashPassword(String password) throws Exception {
		if (password != null) {
			return DigestUtils.sha256Hex(password);
		} else {
			return null;
		}
	}

	/**
	 * // DO NOT USE request.getParameter("action") because you will not be able to read the body later
	 *
	 * @param param
	 * @return
	 */
	static public String getParameterValue(HttpServletRequest req, String param) {
		String value = null;
		String qs = req.getQueryString();
		String key = (param + "=");
		if (qs != null) {
			if (qs.indexOf(key) != -1) {
				value = qs.substring(qs.indexOf(key) + key.length());
				if (value.indexOf("&") != -1) {
					value = value.substring(0, value.indexOf("&"));
				}
				if (value.indexOf("#") != -1) {
					value = value.substring(0, value.indexOf("#"));
				}
			}
		}
		return value;
	}

	static public Map<String, String> getParameters(HttpServletRequest req) {
		Map<String, String> params = new HashMap<>();
		String qs = req.getQueryString();
		if (qs != null) {
			String[] a = qs.split("&");
			for (String s : a) {
				if (s.indexOf('#') != -1) {
					s = s.substring(0, s.indexOf('#'));
				}
				String[] v = s.split("=");
				String param = v[0];
				String value = null;
				if (v.length > 1) {
					value = v[1];
				}
				params.put(param, value);
			}
		}
		return params;
	}

	/**
	 *
	 * @param part
	 * @return
	 */
	static public String getFileName(jakarta.servlet.http.Part part) {
		String filename = null;
		String contentDisposition = part.getHeader("content-disposition");
		if (contentDisposition == null) {
			contentDisposition = part.getHeader("Content-Disposition");
		}
		if (contentDisposition != null) {
			for (String content : contentDisposition.split(";")) {
				if (content.trim().startsWith("filename")) {
					filename = content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
					filename = Util.purgeInvalidFileNameCharacters(filename);
					break;
				}
			}
		}
		return filename;
	}

	/**
	 * 
	 * @param directoryPath
	 * @return
	 * @throws IOException
	 */
	static public Set<String> listFiles(String directoryPath) throws IOException {
		try (Stream<Path> stream = Files.list(Paths.get(directoryPath))) {
			return stream.filter(file -> !Files.isDirectory(file)).map(Path::getFileName).map(Path::toString).collect(Collectors.toSet());
		}
	}

	/**
	 * Method to write a CSV
	 *
	 * @param separator
	 * @param headerRow
	 * @param rows      and rows values
	 *
	 */
	public static String writeCSVContent(String separator, List<String> headerRow, List<List<String>> rows, String enclosingChar) {
		String content = "";

		if (headerRow != null) {

			// 1 - enclose header
			if (enclosingChar != null) {
				for (int i = 0; i < headerRow.size(); i++) {
					headerRow.set(i, StringUtils.wrap(headerRow.get(i), enclosingChar));
				}
			}
			// 2 - separator header
			content = String.join(separator, headerRow) + "\r\n";
		}

		for (List<String> column : rows) {
			// 1 - enclose
			if (enclosingChar != null) {
				for (int i = 0; i < column.size(); i++) {
					column.set(i, StringUtils.wrap(column.get(i), enclosingChar));
				}
			}

			// 2 - separator
			String data = String.join(separator, column);

			// 3 - new line
			content += data + "\r\n";
		}

		content = content.replace("null", "");

		return content;
	}

	public static String writeCSVContent(String separator, List<String> headerRow, List<List<String>> rows) {
		return writeCSVContent(separator, headerRow, rows, null);
	}

	/**
	 *
	 * @param contentFile
	 * @param separator
	 * @param skipHeader
	 * @param charset
	 * @return
	 * @throws Exception
	 */
	public static List<String[]> parseCSVContent(InputStream inputStream, char separator, boolean skipHeader, Charset charset) throws Exception {
		if (charset == null) {
			charset = StandardCharsets.UTF_8;
		}
		try {
			return Util.parseCSVContent(IOUtils.toString(inputStream, charset), separator, skipHeader);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	/**
	 *
	 * @param contentFile
	 * @param separator
	 * @param skipHeader
	 * @param charset
	 * @return
	 * @throws Exception
	 */
	public static List<String[]> parseCSVContent(File contentFile, char separator, boolean skipHeader, Charset charset) throws Exception {
		if (charset == null) {
			charset = StandardCharsets.UTF_8;
		}
		StringBuilder contentBuilder = new StringBuilder();
		try (Stream<String> stream = Files.lines(contentFile.toPath(), charset)) {
			stream.forEach(s -> contentBuilder.append(s).append("\n"));
		}
		return Util.parseCSVContent(contentBuilder.toString(), separator, skipHeader);
	}

	/**
	 * Method to read a CSV
	 *
	 * @param reader
	 * @return
	 * @throws Exception
	 */
	public static List<String[]> parseCSVContent(String content, char separator, boolean skipHeader) throws Exception {
		final char WHITESPACE_CHAR = ' ';
		final char QUOTE_CHAR = '"';
		final char ESCAPE_CHAR = '\\';
		final int BOM_CHAR = 65279; // 0xEF, 0xBB, 0xBF

		List<String[]> lines = new ArrayList<>();

		// split the content into lines
		String[] contentLines = content.split("\\r?\\n");

		// for each lines
		for (int l = (skipHeader ? 1 : 0); l < contentLines.length; l++) {
			String s = contentLines[l];

			boolean isWithinQuote = false;
			boolean isIgnoreWhiteSpace = true;
			StringBuffer currentValue = new StringBuffer(1024);
			List<String> resultList = new ArrayList<>();
			while (s != null) {
				char cArray[] = s.toCharArray();

				for (int i = 0; i < cArray.length; i++) {
					if (cArray[i] == BOM_CHAR) {
						// skip the BOM char
						continue;
					} else if (cArray[i] == WHITESPACE_CHAR) {
						if (isIgnoreWhiteSpace) {
							// do nothing
						} else {
							currentValue.append(cArray[i]);
						}
					} else if (cArray[i] == separator) {
						if (isWithinQuote) {
							currentValue.append(cArray[i]);
						} else {
							// add to list and start capture next value
							resultList.add(currentValue.toString());
							currentValue.setLength(0);
							isWithinQuote = false;
							isIgnoreWhiteSpace = true;
						}
					} else if (cArray[i] == QUOTE_CHAR) {
						if (isWithinQuote) {
							if (cArray.length > i + 1) {
								if (cArray[i + 1] == QUOTE_CHAR) {
									// special char "" for "
									currentValue.append(QUOTE_CHAR);
									i++;
								} else {
									// close of quote
									isWithinQuote = false;
									isIgnoreWhiteSpace = true;
								}
							} else {
								isWithinQuote = false;
								isIgnoreWhiteSpace = true;
							}

						} else {

							if (currentValue.length() > 0) {
								// quote in the middle: a,bc"d"ef,g
								currentValue.append(QUOTE_CHAR);
							} else {
								// start quote
								isWithinQuote = true;
								isIgnoreWhiteSpace = false;
							}

						}
					} else if (cArray[i] == ESCAPE_CHAR) {
						if (cArray.length > i + 1) {
							// special char \\ for \
							if (isWithinQuote && cArray[i + 1] == ESCAPE_CHAR) {
								currentValue.append(ESCAPE_CHAR);
								i++;
							} else {
								currentValue.append(ESCAPE_CHAR);
							}
						} else {
							currentValue.append(ESCAPE_CHAR);
						}
					} else {
						currentValue.append(cArray[i]);
						isIgnoreWhiteSpace = false;
					}
				}

				if (currentValue.length() > 0) {
					if (isWithinQuote) {
						// end of line read.. but still within quote... append back the new line
						currentValue.append("\n");
						// and read next line
						s = contentLines[++l];
						if (s == null) {
							throw new Exception("Unclosed quote. Expecting char '\"' ");
						}
					} else {
						resultList.add(currentValue.toString());
						s = null;
					}
				} else {
					// empty String
					resultList.add(currentValue.toString());
					s = null;
				}
			}

			if (resultList.size() > 0) {
				String[] result = new String[resultList.size()];
				resultList.toArray(result);
				resultList.clear();
				lines.add(result);
			}
		}

		return lines;
	}

	/**
	 * Convert a comma string separated value to a list of ID
	 *
	 * @param ids
	 * @return
	 */
	public static Set<Integer> stringIdsToIntegers(String stringIds) {
		if (stringIds != null && stringIds.trim().length() > 0) {
			try {
				stringIds = URLDecoder.decode(stringIds, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// ignore
			}
			String[] arrayIds = stringIds.split(",");
			Set<Integer> ids = new HashSet<>(arrayIds.length);
			for (String s : arrayIds) {
				if (s != null && s.trim().length() > 0) {
					ids.add(Integer.parseInt(s.trim()));
				}
			}
			return ids;
		} else {
			return new HashSet<>();
		}
	}

	public static String getMemoryStats() {
		StringBuilder sb = new StringBuilder();
		int mb = 1024 * 1024;

		// Getting the runtime reference from system
		Runtime runtime = Runtime.getRuntime();

		// sb.append("Memory (MB)");

		// Print total available memory
		sb.append("Total:" + runtime.totalMemory() / mb);

		// Print used memory
		sb.append(" Use:" + (runtime.totalMemory() - runtime.freeMemory()) / mb);

		// Print free memory
		// sb.append(" Free:" + runtime.freeMemory() / mb);

		// Print Maximum available memory
		// sb.append(" Max:" + runtime.maxMemory() / mb);

		return sb.toString();

	}

	/**
	 * Reset the sortOrder from 1 to N
	 *
	 * @param sortables
	 */
	static public void resetSortOrder(List<? extends Sortable> sortables, Sortable newSortable) {
		if (newSortable != null) {
			// increase value for all sortOrder > new
			for (Sortable s : sortables) {
				if (s.getSortOrder() >= newSortable.getSortOrder() && s != newSortable) {
					s.setSortOrder(s.getSortOrder() + 1);
				}
			}
		}

		// then reset the sortOrder
		Comparator<Sortable> sortOrderSorter = (a, b) -> ((Integer) a.getSortOrder()).compareTo(b.getSortOrder());
		Collections.sort(sortables, sortOrderSorter);

		int index = 1;
		for (Sortable s : sortables) {
			s.setSortOrder(index++);
		}
	}

	static public void setHttpServletResponse(HttpServletResponse response, Throwable ex) {
		Map<String, Object> map = new HashMap<>();

		int code;
		if (ex instanceof BaseException) {
			code = ((BaseException) ex).getCode();
			map.put("description", ((BaseException) ex).getDescription());
			if (((BaseException) ex).getParams() != null) {
				map.put("params", ((BaseException) ex).getParams());
			}
		} else {
			code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			StringBuilder sb = new StringBuilder();
			while (ex != null) {
				sb.append("Caused by: " + ex.getClass().getSimpleName() + " - " + ex.getMessage() + "<br>\n");
				ex = ex.getCause();
			}
			map.put("description", sb.toString());
		}

		map.put("code", code);

		response.setStatus(code);
		response.setHeader("Content-Type", MediaType.APPLICATION_JSON);
		try {
			response.getWriter().write(new Gson().toJson(map));
			response.getWriter().flush();
		} catch (Exception ex1) {
			// ignore
		}
	}

	static public Response buildReponse(Throwable ex) {
		Map<String, Object> map = new HashMap<>();

		int code;
		if (ex instanceof BaseException) {
			code = ((BaseException) ex).getCode();
			map.put("description", ((BaseException) ex).getDescription());
			if (((BaseException) ex).getParams() != null) {
				map.put("params", ((BaseException) ex).getParams());
			}
		} else {
			code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			StringBuilder sb = new StringBuilder();
			while (ex != null) {
				sb.append("Caused by: " + ex.getClass().getSimpleName() + " - " + ex.getMessage() + "<br>\n");
				ex = ex.getCause();
			}
			map.put("description", sb.toString());
		}

		map.put("code", code);
		return Response.status(code).entity(new Gson().toJson(map)).type(MediaType.APPLICATION_JSON).build();
	}

	/**
	 * Search a class inside the base package
	 *
	 * @param name
	 * @param entityBasePackage
	 * @return
	 */
	// for performance only
	private static Map<String, Class<?>> classNameCache = new HashMap<>();

	public static Class<?> findEntityClassByName(String name) {
		if (classNameCache.containsKey(name)) {
			return classNameCache.get(name);
		} else {
			String entityBasePackage = SystemEnv.INSTANCE.getDefaultProperties().getProperty("entity_base_package");
			for (Package p : Package.getPackages()) {
				if (p.getName().startsWith(entityBasePackage)) {
					try {
						Class<?> clazz = Class.forName(p.getName() + "." + name);
						classNameCache.put(name, clazz);
						return clazz;
					} catch (ClassNotFoundException e) {
						// not in this package, try another
					}
				}
			}
			return null;
		}
	}

	static public void main(String[] args) throws Exception {
		// System.out.println(purgeInvalidCharacters("éàçïôèÉ"));

		// String s = "a,b,\"c,d\"";
		// String[] lines = Util.splitAvoidQuotes(s, ',', true);
		// for (int i = 0; i < lines.length; i++) {
		// System.out.println(lines[i]);
		// }
		// Map<String, String> map = new HashMap<>();
		// map.put("DESCRIPTION", "aaaa");
		// System.out.println(Util.replacePlaceHolders("cadscsdc %DESCRIPTION% asfdsfsdf", map));

		String format = "XX-0000";
		for (String key : new String[] { "un-10", "un10", "UN-010", "UN10", "UN-100", "UN-0100", "UN-00100" }) {
			System.out.println(key + ":\t" + Util.formatKey(format, key));
		}

		format = "XX0000";
		for (String key : new String[] { "un-10", "un10", "UN-010", "UN10", "UN-100", "UN-0100", "UN-00100" }) {
			System.out.println(key + ":\t" + Util.formatKey(format, key));
		}

		format = "";
		for (String key : new String[] { "", "999999999999" }) {
			System.out.println(key + ":\t" + Util.formatKey(format, key));
		}

		format = "XX-XXX{-X}";
		for (String key : new String[] { "PL100", "pl100", "pl-100", "pl-100-a", "pl100a", "pl100-", "pl-100-" }) {
			System.out.println(key + ":\t" + Util.formatKey(format, key));
		}
	}
}
