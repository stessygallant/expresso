package com.sgitmanagement.expresso.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum SystemEnv {
	INSTANCE;

	private Logger logger;

	private Map<String, Properties> configPropertiesMap = new HashMap<>();

	private String env;

	private SystemEnv() {
		if (env == null) {
			init();
		}
	}

	private void init() {
		env = System.getProperty("SystemEnv", "local");

		try {
			// System.out.println("Configuring Log4J");
			LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
			URL url = SystemEnv.class.getClassLoader().getResource("log4j2" + "-" + env + ".xml");
			context.setConfigLocation(url.toURI());
		} catch (Exception ex) {
			logger.error("Cannot reconfigure Log4j: " + ex);
		}

		// init logger
		logger = LoggerFactory.getLogger(SystemEnv.class);

		// print the Working directory
		logger.info("Working directory [" + (new File("").getAbsolutePath()) + "]");

		// memory
		logger.info("Max memory (MB) [" + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + "]");

		// Default printer
		boolean debugPrinter = false;
		if (debugPrinter) {
			PrintService defaultPrinter = PrintServiceLookup.lookupDefaultPrintService();
			if (defaultPrinter != null) {
				logger.debug("Default printer: " + defaultPrinter.getName());
			}

			// List printers
			PrintService[] pss = PrintServiceLookup.lookupPrintServices(null, null);
			logger.debug("Available printers");
			for (PrintService ps : pss) {
				logger.debug(" " + ps.getName());
			}
		}

		String ipAddress = null;
		try {
			ipAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception ex) {
			logger.error("Cannot get server IP Address: " + ex);
		}

		// set the default locale
		Locale.setDefault(Locale.ENGLISH);

		String msg = "System started on [" + ipAddress + "] - Environment: " + env;
		if (isInProduction()) {
			logger.error(msg);
		} else {
			logger.info(msg);
		}
	}

	public boolean isInProduction() {
		return env.equals("prod");
	}

	public void setEnv(String e) {
		env = e;
	}

	public String getEnv() {
		return env;
	}

	public Properties getDefaultProperties() {
		return getProperties("config");
	}

	private InputStream loadFile(String fileName) throws Exception {
		if (fileName.startsWith("/") || fileName.startsWith("\\") || fileName.charAt(2) == ':') {
			// this is an absolute path
			try {
				return new FileInputStream(fileName);
			} catch (FileNotFoundException e) {
				return null;
			}
		} else {
			// relative path to the application
			return SystemEnv.class.getClassLoader().getResourceAsStream(fileName);
		}
	}

	public Properties getProperties(String filePath) {

		Properties props = null;

		// verify if we have the resource already loaded
		if (configPropertiesMap.containsKey(filePath)) {
			props = configPropertiesMap.get(filePath);
		} else {

			String fileName = filePath + "-" + env + ".properties";

			InputStream input = null;
			props = new Properties();
			try {
				input = loadFile(fileName);
				if (input == null) {
					// try the default one
					fileName = filePath + ".properties";
					input = loadFile(fileName);
				}
				if (input == null) {
					throw new Exception("Cannot find ressource file [" + filePath + "]");
				}

				logger.info("Loaded properties from [" + fileName + "]");
				props.load(input);

				// put the properties in the cache
				configPropertiesMap.put(filePath, props);

			} catch (Exception e) {
				logger.error("Error getProperties", e);
			} finally {
				try {
					input.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return props;
	}
}
