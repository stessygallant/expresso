package com.sgitmanagement.expresso.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

public class WebSocketMessageEncoder implements Encoder.Text<JsonElement> {
	private static Gson gson = new Gson();

	@Override
	public String encode(JsonElement message) throws EncodeException {
		return gson.toJson(message);
	}

	@Override
	public void init(EndpointConfig endpointConfig) {
		// Custom initialization logic
	}

	@Override
	public void destroy() {
		// Close resources
	}
}