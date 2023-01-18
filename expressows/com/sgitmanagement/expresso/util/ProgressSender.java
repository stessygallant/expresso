package com.sgitmanagement.expresso.util;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;

public class ProgressSender {
	class Level {
		int weight;
		String title;
		boolean masterLevel = false;

		public Level(String title, int weight) {
			super();
			this.title = title;
			this.weight = weight;
		}
	}

	private HttpServletResponse response;
	private Writer writer;
	private Deque<Level> levels = null;
	private double overallProgressMilestone = 0.0;
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	public ProgressSender(HttpServletResponse response) throws IOException {
		this.response = response;
		if (response != null) {
			response.setBufferSize(1); // this does not work
			// we cannot set the content type as we will send multiple chunks of JSON
			// this.response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			this.writer = response.getWriter();
		}

		// by default, add a level
		levels = new ArrayDeque<>();
		addLevel("");
	}

	/**
	 *
	 * @param msg
	 * @param count
	 * @param total
	 * @return
	 * @throws Exception
	 */
	public double send(String msg, int count, int total) {
		return send(String.format("%s (%d/%d)", msg, count, total), count * 100.0 / total);
	}

	/**
	 *
	 * @param msg
	 * @param progress progress of the current level between 0 and 100
	 * @return the overall progress
	 * @throws IOException
	 */
	public double send(String msg, double progress) {
		return send(msg, null, progress);
	}

	public double send(String msg, String params, double progress) {
		// calculate the progress of the current level
		for (Level l : levels) {
			progress *= l.weight / 100.0;
		}
		double overallProgress = this.overallProgressMilestone + progress;

		logger.debug(String.format("Progress [%s]: %.1f", msg, overallProgress));

		// notification for the UI
		String buffer = "{\"progress\":" + overallProgress + ", \"message\":\"" + msg + "\", \"params\":\"" + params + "\"}";
		sendBuffer(buffer);

		return overallProgress;
	}

	public void sendError(String errorMessage) {
		sendError(errorMessage, null);
	}

	public void sendError(String errorMessage, String params) {
		String buffer = "{\"errorMessage\":\"" + errorMessage + "\", \"params\":\"" + params + "\"}";
		sendBuffer(buffer);
	}

	public void addLevel(String title) throws IOException {
		addLevel(title, 100);
	}

	public void addLevel(String title, int weight) throws IOException {
		// System.out.println("Adding level [" + title + "]");

		// the current level is a master level (do not take into account the progress)
		if (!levels.isEmpty()) {
			Level masterLevel = levels.getLast();
			masterLevel.masterLevel = true;
		}

		Level level = new Level(title, weight);
		levels.add(level);

		// send a message to start the progress
		send(title, 0);
	}

	public void completeLevel() throws IOException {
		Level level = levels.getLast();

		// set the milestone
		double progress = send(level.title, 100);
		if (!level.masterLevel) {
			this.overallProgressMilestone = progress;
		}

		// System.out.println("Completed level [" + level.title + "]: " + this.overallProgressMilestone);

		// pop the last level
		levels.pollLast();
	}

	private void sendBuffer(String buffer) {
		try {
			if (this.writer != null) {
				this.writer.write(buffer);

				// we must fill the buffer
				// int size = this.response.getBufferSize();
				int minimumSize = 512;
				if (buffer.length() < minimumSize) {
					// System.out.println("Filling buffer up to " + size);
					int fill = minimumSize - buffer.length();
					this.writer.write(String.format("%1$-" + fill + "s", " "));
				}
				this.response.flushBuffer();
				// this.writer.flush();
			}
		} catch (Exception e) {
			// ignore
		}
	}

	static public void main(String[] args) throws IOException {
		ProgressSender progressSender = new ProgressSender(null);

		progressSender.addLevel("L1", 30);
		{

			progressSender.addLevel("L1-1", 50);
			progressSender.send("1a", 20);
			progressSender.send("1b", 100);
			progressSender.completeLevel();

			progressSender.addLevel("L1-2", 20);
			progressSender.send("2a", 50);
			progressSender.send("2b", 100);
			progressSender.completeLevel();

			progressSender.addLevel("L1-3", 30);
			progressSender.send("3a", 50);
			progressSender.send("3b", 55);
			progressSender.send("3c", 56);
			progressSender.send("3d", 59);
			progressSender.send("3e", 60);
			progressSender.send("3f", 90);
			progressSender.send("3g", 98);
			progressSender.completeLevel();
		}
		progressSender.completeLevel();

		progressSender.addLevel("L2", 70);
		{
			progressSender.addLevel("L2-1", 40);
			progressSender.send("1a", 20);
			progressSender.send("1b", 100);
			progressSender.completeLevel();

			progressSender.addLevel("L2-2", 60);
			progressSender.send("2a", 50);
			progressSender.send("2b", 100);
			progressSender.completeLevel();
		}
		progressSender.completeLevel();
	}
}
