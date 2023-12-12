package com.sgitmanagement.expresso.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EndpointConfig;

public class WebSocketMessageDecoder implements Decoder.Text<JsonElement> {

	private static Gson gson = new Gson();

	@Override
	public JsonElement decode(String s) throws DecodeException {
		return gson.fromJson(s, JsonElement.class);
	}

	@Override
	public boolean willDecode(String s) {
		return (s != null);
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