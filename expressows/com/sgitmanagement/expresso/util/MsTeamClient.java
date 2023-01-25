package com.sgitmanagement.expresso.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Permissions<br>
 * // send a message in a Team Channel<br>
 * ChannelMessage.Send, Group.ReadWrite.All<br>
 * <br>
 * // send a message in a Team Chat<br>
 * ChatMessage.Send, Chat.ReadWrite<br>
 *
 */
public class MsTeamClient {
	static private Logger logger = LoggerFactory.getLogger(MsTeamClient.class);
	private MsGraphClient msGraphClient;

	public static void main(String args[]) throws Exception {
		MsTeamClient msTeamClient = new MsTeamClient();
		try {
			msTeamClient.connect();

			// String chatId = "19:1a7fc775-f0b6-4489-bb27-cfbaec9cac0d_8a8e33c7-95c5-41f8-ad07-f7cb67c0d0e4@unq.gbl.spaces";
			// msTeamClient.sendChatMessage(chatId, "Allo from Sherpa");

			// https://learn.microsoft.com/en-us/answers/questions/1154651/send-messages-in-channel-apis-we-have-added-the-ap

			// teamDashboard/Sherpa Test
			// https://teams.microsoft.com/l/channel/19%3aSsrdSs7i08f-bw36Sa-ShZCc1_Gm2YZ-XoMDlY0yYIQ1%40thread.tacv2/General?groupId=ac235c9e-8b95-4b01-a6e1-673369f5a70d&tenantId=cffed910-a6da-4704-819d-7fb0ab7fa079
			String teamId = "ac235c9e-8b95-4b01-a6e1-673369f5a70d";
			String channelId = "19:SsrdSs7i08f-bw36Sa-ShZCc1_Gm2YZ-XoMDlY0yYIQ1@thread.tacv2";
			msTeamClient.sendChannelMessage(teamId, channelId, "Allo from Sherpa");
			System.out.println("Done");
		} finally {
			msTeamClient.disconnect();
		}
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void connect() throws Exception {
		this.msGraphClient = new MsGraphClient();
		this.msGraphClient.connect();
	}

	/**
	 * 
	 */
	public void disconnect() {
		try {
			msGraphClient.disconnect();
		} catch (Exception e) {
			// not much that we can do
		}
		msGraphClient = null;
	}

	public void sendChatMessage(String chatId, String message) throws Exception {
		// Sending message in a chat
		// POST /chats/{chat-id}/messages
		JsonObject rootJsonObject = new JsonObject();

		// body
		JsonObject bodyJsonObject = new JsonObject();
		rootJsonObject.add("body", bodyJsonObject);
		// bodyJsonObject.addProperty("contentType", "HTML");
		bodyJsonObject.addProperty("content", message);
		// Base64.getEncoder().encodeToString(messageBody.getBytes(StandardCharsets.UTF_8)));

		String jsonBody = new GsonBuilder().create().toJson(rootJsonObject);
		// System.out.println(jsonBody);
		JsonObject rootJonsObject = msGraphClient.callMicrosoftGraph("/chats/" + chatId + "/messages", jsonBody);
		logger.debug(new GsonBuilder().create().toJson(rootJonsObject));
	}

	public void sendChannelMessage(String teamId, String channelId, String message) throws Exception {
		// Sending message in a channel
		// POST /teams/{team-id}/channels/{channel-id}/messages

		// POST https://graph.microsoft.com/v1.0/teams/fbe2bf47-16c8-47cf-b4a5-4b9b187c508b/channels/19:4a95f7d8db4c4e7fae857bcebe0623e6@thread.tacv2/messages
		// Content-type: application/json
		//
		// {
		// "body": {
		// "content": "Hello World"
		// }
		// }
		JsonObject rootJsonObject = new JsonObject();

		// body
		JsonObject bodyJsonObject = new JsonObject();
		rootJsonObject.add("body", bodyJsonObject);
		bodyJsonObject.addProperty("content", message);

		String jsonBody = new GsonBuilder().create().toJson(rootJsonObject);
		System.out.println(jsonBody);
		JsonObject rootJonsObject = msGraphClient.callMicrosoftGraph("/teams/" + teamId + "/channels/" + channelId + "/messages", jsonBody);
		logger.debug(new GsonBuilder().create().toJson(rootJonsObject));
	}
}
