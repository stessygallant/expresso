package com.sgitmanagement.expressoext.websocket;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.sgitmanagement.expressoext.base.BaseService;

import jakarta.websocket.Session;

public class WebSocketService extends BaseService {
	static private Map<String, Set<Session>> webSocketSessions = Collections.synchronizedMap(new HashMap<>());

	public void addWebSocketSession(Session session, String resourceSecurityPath) {
		if (resourceSecurityPath != null) {
			Set<Session> sessions = webSocketSessions.get(resourceSecurityPath);
			if (sessions == null) {
				sessions = Collections.synchronizedSet(new HashSet<>());
				webSocketSessions.put(resourceSecurityPath, sessions);
			}
			sessions.add(session);
		} else {
			logger.warn("resourceSecurityPath is null");
		}
	}

	public void removeWebSocketSession(Session session, String resourceSecurityPath) {
		if (resourceSecurityPath != null) {
			Set<Session> sessions = webSocketSessions.get(resourceSecurityPath);
			if (sessions != null) {
				sessions.remove(session);
			}
		} else {
			logger.warn("resourceSecurityPath is null");
		}
	}

	public void broadcast(String resourceSecurityPath, JsonElement message) throws Exception {
		if (resourceSecurityPath != null) {
			Set<Session> sessions = webSocketSessions.get(resourceSecurityPath);
			if (sessions != null) {
				sessions.forEach(s -> {
					synchronized (s) {
						try {
							s.getBasicRemote().sendObject(message);
						} catch (Exception ex) {
							logger.warn("Error sending message: " + ex);
						}
					}
				});
			}
		} else {
			logger.warn("resourceSecurityPath is null");
		}
	}
}
