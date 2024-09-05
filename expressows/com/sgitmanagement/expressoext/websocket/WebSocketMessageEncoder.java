package com.sgitmanagement.expressoext.websocket;

import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

public class WebSocketMessageEncoder implements Encoder.Text<String> {

	@Override
	public String encode(String message) throws EncodeException {
		return message;
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