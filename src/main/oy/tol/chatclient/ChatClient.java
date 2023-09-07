package oy.tol.chatclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import oy.tol.chat.ChangeTopicMessage;
import oy.tol.chat.ChatMessage;
import oy.tol.chat.ErrorMessage;
import oy.tol.chat.ListChannelsMessage;
import oy.tol.chat.Message;
import oy.tol.chat.StatusMessage;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;


import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ChatClient extends JFrame implements ChatClientDataProvider {

    private static final String SERVER = "localhost:10000";
	private int serverPort = 10000;
	private String currentServer = SERVER;
    private ChatTCPClient tcpClient = null;
	private static String nick = "Väinö";
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JLabel namLabel;
    private JLabel channeltopicLabel;
    private JLabel channelNameLabel;
    private DefaultListModel<String> channelListModel;
    private Map<String, String> channelTopics = new HashMap<>();
    private Map<String, StringBuilder> channelMessages = new HashMap<>();
    private JList<String> channelList;
    private boolean running = true;


    public static void main(String[] args) {
        if (args.length == 1) {
            SwingUtilities.invokeLater(() -> {
                try {
                    new ChatClient(args[0]).initUI();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            System.out.println("Usage: java -jar chat-client-jar-file chatclient.properties");
            System.out.println("Where chatclient.properties is the client configuration file");
        }
    }


    private ChatClient(String configFile) {
        try {
            readConfiguration(configFile);
            tcpClient = new ChatTCPClient(this);
            new Thread(tcpClient).start();
        } catch (Exception e) {
            displayMessage("Failed to run the ChatClient\nReason: " + e.getLocalizedMessage());
            e.printStackTrace();
            e.getMessage();
        }
    }

	private void initUI() {
            try {
                setTitle("Swing Chat Client");
                setDefaultCloseOperation(EXIT_ON_CLOSE);
                setSize(800, 600);

                setLocationRelativeTo(null);

                channelTopics.put("Main", "Everything");
                channelTopics.put("Odysseu", "Bot Channel");

                //Creating menupanel to the left side.
                JPanel menuPanel = new JPanel();
                menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
                

                //Buttons for NAME and NEW CHANNEL and actions for them
                JButton button1 = new JButton("Name");
                JButton button2 = new JButton("New Channel");
                
                button1.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e){
                        openNameWindow();
                    }
                });

                button2.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e){
                        newChannel();
                    }
                });

                
                //Channelbox code
                //updateChannelLabels(currentChannel)
                channelListModel = new DefaultListModel<>();
                channelListModel.addElement("Main");
                channelListModel.addElement("Odysseu");


                channelList = new JList<>(channelListModel);
                channelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                JScrollPane listScrollPane = new JScrollPane(channelList);

                // if (channelList != null){
                //     channelList.setSelectedValue("Main", true);
                // }


                channelList.addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting()) {
                            int selectedIndex = channelList.getSelectedIndex();
                            if (selectedIndex >= 0 && selectedIndex < channelListModel.size()) {
                                String selectedChannel = channelListModel.getElementAt(selectedIndex);
                                updateChannelLabels(selectedChannel);
                            }
                        }
                    }
                });

                menuPanel.add(button1);
                menuPanel.add(button2);
                menuPanel.add(listScrollPane);

                add(menuPanel, BorderLayout.WEST);

                //Creating labels for channel topic and channel name. 
                channeltopicLabel = new JLabel("Channel Topic: ");
                channelNameLabel = new JLabel("Current Channel: "); 
                
                //Putting the labels in a box so they can be next to each other.
                JPanel labelsPanel = new JPanel();
                labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.X_AXIS));
                labelsPanel.add(channelNameLabel);
                labelsPanel.add(Box.createHorizontalStrut(300)); // Add some spacing between labels
                labelsPanel.add(channeltopicLabel);
                labelsPanel.setBorder(BorderFactory.createEmptyBorder(10, 150, 0, 0)); // Adding padding
                add(labelsPanel, BorderLayout.NORTH);

                //initializing chatarea for sending messages
                chatArea = new JTextArea();
                chatArea.setEditable(false);
                JScrollPane scrollPane = new JScrollPane(chatArea);
                add(scrollPane, BorderLayout.CENTER);


                inputField = new JTextField(30);
                sendButton = new JButton("Send");
                sendButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        sendMessage();
                    }
                });
                
                inputField.addKeyListener(new KeyListener() {
                    @Override
                    public void keyTyped(KeyEvent e) {
                        // No need to use, but required by the KeyListener interface
                    }
                
                    @Override
                    public void keyPressed(KeyEvent e) {
                        // In this method it checks if enter (keycode 10) is pressed 
                        if (e.getKeyCode() == 10) {
                            sendMessage();
                        }
                    }
                
                    @Override
                    public void keyReleased(KeyEvent e) {
                        // No need to use, but required by the KeyListener interface
                    }
                });


                //Putting name label next to textfield
                JPanel inputPanel = new JPanel();
                JPanel namePanel = new JPanel();

                namLabel = new JLabel(nick + " ");
                namePanel.add(namLabel);
                inputPanel.add(namePanel, BorderLayout.WEST);
                inputPanel.add(inputField, BorderLayout.CENTER);
                inputPanel.add(sendButton, BorderLayout.EAST);
                add(inputPanel, BorderLayout.SOUTH);


                setVisible(true);

                

            } catch (Exception e){
                System.out.println("Error : " + e.getMessage());
            }     
        
    }

    public void displayGUI() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    //method for creating new channel and topic
    private void newChannel(){
        JFrame nameFrame = new JFrame("Change channel name");
        nameFrame.setSize(300, 175);
    
        JPanel channelPanel = new JPanel();
        channelPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); 
    
        JLabel channelLabel = new JLabel("Enter new channel name: ");
        JTextField channelField = new JTextField(20);

        JLabel topicLabel = new JLabel("Enter new channel topic: ");
        JTextField topicField = new JTextField(20);

        JButton saveButton = new JButton("Save");
    
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newChannelName = channelField.getText();
                String newTopic = topicField.getText();
    
                // Update labels
                if (newChannelName.isEmpty()) {
                    displayMessage("Error: Channel name cannot be empty");
                } else if (newTopic.isEmpty()){
                    displayMessage("Error: Channel topic cannot be empty");
                } else {
                    channelNameLabel.setText("Current Channel: " + newChannelName);
                    tcpClient.changeChannelTo(newChannelName);
                    channeltopicLabel.setText("Channel Topic: " + newTopic);
                    tcpClient.changeTopicTo(newTopic);

                    // Update channelListModel
                    channelListModel.addElement(newChannelName);
                    channelTopics.put(newChannelName, newTopic);

                    channelMessages.put(newChannelName, new StringBuilder());
                    
                    chatArea.setText("");

    
                    nameFrame.dispose();
                }
    
            }
        });

        channelPanel.add(channelLabel);
        channelPanel.add(channelField);
        channelPanel.add(topicLabel);
        channelPanel.add(topicField);
        channelPanel.add(saveButton);
    
        nameFrame.add(channelPanel);
        nameFrame.setLocationRelativeTo(this);
        nameFrame.setVisible(true);

    }



    private void updateChannelLabels(String selectedChannel) {
        
        String selectedTopic = channelTopics.get(selectedChannel); 

        //clear the chatarea
        chatArea.setText("");
    
        //display messages for the selected channel
        StringBuilder channelMessageBuilder = channelMessages.getOrDefault(selectedChannel, new StringBuilder());
        chatArea.append(channelMessageBuilder.toString());

        channelNameLabel.setText("Current Channel: " + selectedChannel);
        channeltopicLabel.setText("Channel Topic: " + selectedTopic);
    }


    private void changeNick(String newNick) {
        // Send the new nickname to the server
        tcpClient.changeNick(newNick);
    
        // Update the local nickname
        nick = newNick;
    
        // Update the name label
        namLabel.setText(nick + ":");
    }

    //method for changing nick
    private void openNameWindow() {
        JFrame nameFrame = new JFrame("Change Name");
        nameFrame.setSize(300, 150);

        JPanel namePanel = new JPanel();
        namePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel nameLabel = new JLabel("Enter new name: ");
        JTextField nameField = new JTextField(20);
        JButton saveButton = new JButton("Save");

        saveButton.addActionListener(e -> {
            String newName = nameField.getText();
            if (!newName.isEmpty()) {
                changeNick(newName);
                nameFrame.dispose();
            } else {
                displayMessage("Error: Name cannot be empty");
            }
        });

        namePanel.add(nameLabel);
        namePanel.add(nameField);
        namePanel.add(saveButton);

        nameFrame.add(namePanel);
        nameFrame.setLocationRelativeTo(this);
        nameFrame.setVisible(true);
    }

    
    
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            if (nick != null) {
                String selectedChannel = channelList.getSelectedValue();
                if (selectedChannel == null) {
                    displayMessage("Error: No channel selected.");
                } else {
                    //String currentTimeUTC = java.time.LocalTime.now(java.time.ZoneOffset.UTC).format(timeFormatter);
                    ZoneId finnishTimeZone = ZoneId.of("Europe/Helsinki");
                    ZonedDateTime currentTimeZone = ZonedDateTime.now(finnishTimeZone);
                    String currentTimeFormatted = currentTimeZone.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                    String formattedMessage = "[" + currentTimeFormatted + "] " +nick + ": "+message; 
                    //String formattedMessage = "[" + currentTimeFormatted + "] " + message;
    
                    StringBuilder channelMessageBuilder = channelMessages.getOrDefault(selectedChannel, new StringBuilder());
                    channelMessageBuilder.append(formattedMessage).append("\n");
                    channelMessages.put(selectedChannel, channelMessageBuilder);
    
                    displayMessage(formattedMessage); // Display message with time locally
                    tcpClient.postChatMessage(message); // Send the message to the server
                }
            }
        }
        inputField.setText("");
    }
    

    private void displayMessage(String message) {
        chatArea.append(message + "\n");
        //chatArea.setForeground(color); // Set the text color
    }

    private void readConfiguration(String configFileName) throws FileNotFoundException, IOException {
		System.out.println("Using configuration: " + configFileName);
		File configFile = new File(configFileName);
		Properties config = new Properties();
		FileInputStream istream;
		istream = new FileInputStream(configFile);
		config.load(istream);
		String serverStr = config.getProperty("server", "localhost:10000");
		String [] components = serverStr.split(":");
		if (components.length == 2) {
			serverPort = Integer.parseInt(components[1]);
			currentServer = components[0];
		} else {
			System.out.println("Error");
		}
		// nick = config.getProperty("nick", "");
		// if (config.getProperty("usecolor", "false").equalsIgnoreCase("true")) {
		// 	useColorOutput = true;
		// }
		istream.close();
	}

    @Override
    public String getServer() {
        return currentServer;
    }

    @Override
    public int getPort() {
        return serverPort;
    }

    @Override
    public String getNick() {
        return nick;
    }

    @Override
    public boolean handleReceived(Message message) {
        boolean continueReceiving = true;
    
        switch (message.getType()) {
            // case Message.CHAT_MESSAGE: {
            //     if (message instanceof ChatMessage) {
            //         ChatMessage msg = (ChatMessage) message;
            //         String sender = msg.getNick();
            //         String content = msg.getMessage();
            
            //         // Get the current time in the desired format
            //         String currentTimeFormatted = LocalDateTime.now()
            //             .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            
            //         String formattedMessage;
            
            //         if (msg.isDirectMessage()) {
            //             formattedMessage = "[" + currentTimeFormatted + "] [private] " + sender + " > " + content;
            //             // Handle private messages differently (e.g., change color or display differently)
            //             displayMessage(formattedMessage);
            //         } else {
            //             formattedMessage = "[" + currentTimeFormatted + "] " + sender + " > " + content;
            //             displayMessage(formattedMessage);
            //         }
            //     }
            //     continueReceiving = true;
            //     break;
            // }
            case Message.CHAT_MESSAGE: {
                if (message instanceof ChatMessage) {
                    ChatMessage msg = (ChatMessage) message;
                    String sender = msg.getNick();
                    String content = msg.getMessage();
    
                    // Get the current time in the desired format
                    String currentTimeFormatted = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    
                    String formattedMessage = "[" + currentTimeFormatted + "] " + sender + " > " + content;
                    displayMessage(formattedMessage);
                }
                continueReceiving = true;
                break;
            }
    
            case Message.LIST_CHANNELS: {
                ListChannelsMessage msg = (ListChannelsMessage) message;
                List<String> channels = msg.getChannels();
    
                if (channels != null) {
                    StringBuilder channelListStr = new StringBuilder();
                    for (String channel : channels) {
                        channelListStr.append(channel).append(", ");
                    }
                    if (channelListStr.length() > 2) {
                        // Remove the trailing ", " from the last channel
                        channelListStr.setLength(channelListStr.length() - 2);
                    }
    
                    String formattedMessage = "[" + LocalDateTime.now() + "] SERVER > channels in server: " + channelListStr.toString();
                    displayMessage(formattedMessage);
                }
                continueReceiving = true;
                break;
            }
    
            case Message.CHANGE_TOPIC: {
                ChangeTopicMessage msg = (ChangeTopicMessage) message;
                String topic = msg.getTopic();
                String formattedMessage = "[" + LocalDateTime.now() + "] SERVER > channel topic is: " + topic;
                displayMessage(formattedMessage);
                continueReceiving = true;
                break;
            }
    
            case Message.STATUS_MESSAGE: {
                StatusMessage msg = (StatusMessage) message;
                String status = msg.getStatus();
                String formattedMessage = "[" + LocalDateTime.now() + "] SERVER > status: " + status;
                displayMessage(formattedMessage);
                continueReceiving = true;
                break;
            }
    
            case Message.ERROR_MESSAGE: {
                try {
                    ErrorMessage msg = (ErrorMessage) message;
                String error = msg.getError();
                String formattedMessage = "[" + LocalDateTime.now() + "] SERVER > " + error;
                displayMessage(formattedMessage);
                if (msg.requiresClientShutdown()) {
                    running = false;
                    continueReceiving = false;
                }
                } catch(Exception e){
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                
                break;
            }
    
            default:
                displayMessage("Unknown message type from server.");
                break;
        }
    
        return continueReceiving;
    }

    @Override
    public void connectionClosed() {
        SwingUtilities.invokeLater(() -> displayMessage("Connection closed"));
        running = false;
    }
}