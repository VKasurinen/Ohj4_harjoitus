package oy.tol.chatclient;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.json.JSONException;
import org.json.JSONObject;

import oy.tol.chat.ChangeTopicMessage;
import oy.tol.chat.ChatMessage;
import oy.tol.chat.ErrorMessage;
import oy.tol.chat.JoinMessage;
import oy.tol.chat.ListChannelsMessage;
import oy.tol.chat.Message;
import oy.tol.chat.MessageFactory;

public class ChatTCPClient implements Runnable {

	private ChatClientDataProvider dataProvider = null;

	private Socket socket;
	private boolean running = true;

	private PrintWriter out;
	private BufferedReader in;
	private String nick;

	ChatTCPClient(ChatClientDataProvider provider) {
		dataProvider = provider;
		nick = dataProvider.getNick();
	}

	public boolean isConnected() {
		return running;
	}

	//Added changeNick method for chaning nickname
	
	public synchronized void changeNick(String newNick) {
		String currentNick = nick;
		nick = newNick; // Update the local nickname
	
		
		JSONObject nickChangeRequest = new JSONObject();
		try {
			nickChangeRequest.put("type", "NICK_CHANGE");
			nickChangeRequest.put("newNick", newNick);
			nickChangeRequest.put("oldNick", currentNick);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	
		// Convert the JSON object to a string and send it to the server
		write(nickChangeRequest.toString());
	}


	public synchronized void postChatMessage(String message) {
		String userName = dataProvider.getNick();
		String recipientNick = null;
		String actualMessage = message;
		if (message.startsWith("@")) {
			int firstSpace = message.indexOf(' ', 0);
			if (firstSpace > 0 && firstSpace < message.length()) {
				recipientNick = message.substring(1, firstSpace);
				actualMessage = message.substring(firstSpace+1);
			}
		}
		ChatMessage msg = new ChatMessage(LocalDateTime.now(), userName, actualMessage);
		if (null != recipientNick) {
			msg.setRecipient(recipientNick);
		}
		String jsonObjectString = msg.toJSON();
		write(jsonObjectString);
	}

	private synchronized void write(String message) {
		// ChatClient.println("DEBUG OUT: " + message, ChatClient.colorError);
		//out.write(message + "\n");
		if (out != null){
			out.println(message);
			out.flush();
		}
		
	}

	@Override
	public void run() {
		while (running) {
			try {
				if (socket == null) {
					connect();
				}
				String data;
				while ((data = in.readLine()) != null) {
					// ChatClient.println("DEBUG IN: " + data, ChatClient.colorInfo);
					boolean continueReading = handleMessage(data);
					if (!continueReading) {
						break;
					}
				}
			} catch (EOFException e) {
				// ChatClient.println("ChatSession: EOFException", ChatClient.colorError);
			} catch (IOException e) {
				// ChatClient.println("ChatSession: IOException", ChatClient.colorError);
				ErrorMessage msg = new ErrorMessage("Cannot connect: " + e.getLocalizedMessage(), true);
				dataProvider.handleReceived(msg);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				close();
			}
		}
	}

	private void connect() throws IOException, UnknownHostException {
		String hostName = dataProvider.getServer();
		int port = dataProvider.getPort();
		InetAddress hostAddress = InetAddress.getByName(hostName);
		socket = new Socket(hostAddress, port);
		out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
		// ChatClient.println("Connecting to server " + address, ChatClient.colorError);
	}

	

	private boolean handleMessage(String data) {
		Message received = null;
		try {
			JSONObject jsonObject = new JSONObject(data);
			received = MessageFactory.fromJSON(jsonObject);
		} catch (JSONException e) {
			e.printStackTrace();
			received = new ErrorMessage("Invalid JSON message from client");
		}
		return dataProvider.handleReceived(received);
	}

	public void close() {
		running = false;
		if (null != socket) {
			try {
				socket.close();
				if (null != in) in.close();
				if (null != out) out.close();
			} catch (IOException e) {
				// nada
			} finally {
				in = null;
				out = null;
				socket = null;
				dataProvider.connectionClosed();
			}
		}
	}

	public void changeChannelTo(String channel) {
		JoinMessage msg = new JoinMessage(channel);
		String jsonObjectString = msg.toJSON();
		write(jsonObjectString);
	}

	public void changeTopicTo(String topic) {
		ChangeTopicMessage newTopic = new ChangeTopicMessage(topic);
		String jsonString = newTopic.toJSON();
		write(jsonString);
	}

	public void listChannels() {
		ListChannelsMessage listChannels = new ListChannelsMessage();
		String msg = listChannels.toJSON();
		write(msg);
	}
}
