package com.sgitmanagement.expresso.util;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandLineUtil {
	static final private Logger logger = LoggerFactory.getLogger(CommandLineUtil.class);

	/**
	 * Execute a shell command and return the console output
	 * 
	 * @param command the command to execute (including its arguments)
	 * @return the console output
	 * @throws Exception
	 */
	public static String executeScript(String scriptPath, List<String> args, int timeoutInMs) throws Exception {
		CommandLine cmdLine = new CommandLine(scriptPath);

		for (String arg : args) {
			cmdLine.addArgument(arg, false);
		}
		DefaultExecutor executor = new DefaultExecutor();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
		executor.setStreamHandler(streamHandler);
		executor.setExitValue(0);
		ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutInMs);
		executor.setWatchdog(watchdog);

		Integer exitValue;
		try {
			exitValue = executor.execute(cmdLine);
		} catch (ExecuteException e) {
			exitValue = e.getExitValue();
		}
		if (watchdog.killedProcess()) {
			throw new Exception("  >> Error while executing command, timeout after " + timeoutInMs + " ms");
		}
		if (exitValue != 0) {
			throw new Exception("  >> Error while executing command : [" + outputStream.toString() + "]\n");
		}

		logger.debug("  >> Command executed : " + outputStream.toString());

		return outputStream.toString();

	}
}
