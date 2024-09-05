package com.sgitmanagement.expresso.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ServerTimingUtil {
	// static final private Logger logger = LoggerFactory.getLogger(ServerTimingUtil.class);

	public static ThreadLocal<Map<String, Long>> serverTiming = new ThreadLocal<>();
	private static ThreadLocal<Deque<Node>> nodeQueue = new ThreadLocal<>();

	private static class Node {
		private String key;
		private List<Node> children;
		private Node parent;
		private long startTime;

		public Node(String key) {
			children = new ArrayList<>();
			this.key = key;
			this.startTime = new Date().getTime();
		}

		public Node addChild(Node node) {
			children.add(node);
			node.parent = this;
			return this;
		}

		public void remove() {
			if (parent != null) {
				parent.children.remove(this);
				parent = null;
			}
		}

		public String getLevel() {
			String level;
			if (parent == null) {
				level = "";
			} else {
				level = parent.getLevel() + (parent.children.indexOf(this) + 1) + ".";
			}
			return level;
		}

		public String getName() {
			String level = getLevel();
			if (level.length() > 0) {
				return level.substring(0, level.length() - 1) + "-" + key;
			} else {
				return key;
			}
		}
	}

	public static void startTiming(String key) {
		Deque<Node> nodes = nodeQueue.get();
		if (nodes == null) {
			nodes = new ArrayDeque<>();
			nodeQueue.set(nodes);
			Node topNode = new Node("Total");
			nodes.add(topNode);
			// logger.debug("INSERT Total");
		}

		// get the current node and add a new child node
		Node currentNode = nodes.getLast();
		Node newNode = new Node(key);
		currentNode.addChild(newNode);
		nodes.add(newNode);
		// logger.debug("INSERT " + newNode.key);
	}

	public static void endTiming() {
		Deque<Node> nodes = nodeQueue.get();
		// because the response is written before the end of the filter chain, it may happen that
		// a filter will try to end the timing but the response is already written
		if (nodes != null && !nodes.isEmpty()) {
			Node currentNode = nodes.pollLast();
			// logger.debug("REMOVE " + currentNode.key);

			long delay = new Date().getTime() - currentNode.startTime;
			String key = currentNode.getName();
			if (delay > 1) {
				Map<String, Long> map = serverTiming.get();
				if (map == null) {
					map = new LinkedHashMap<>();
					serverTiming.set(map);
				}
				if (!map.containsKey(key)) {
					map.put(key, delay);
				} else {
					// do not overwrite key
					map.put(key + "-" + new Random().nextInt(10000), delay);
				}
			} else {
				// do not keep it in the nodes
				currentNode.remove();
			}
		}
	}

	public static void complete() {
		// close all remaining nodes
		Deque<Node> nodes = nodeQueue.get();
		while (!nodes.isEmpty()) {
			endTiming();
		}
	}

	public static void clear() {
		serverTiming.remove();
		nodeQueue.remove();
	}
}
