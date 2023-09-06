package oy.tol.chatclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import oy.tol.chat.Message;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ChatClient extends JFrame implements ChatClientDataProvider {

    private static final String SERVER = "localhost:10000";
	private int serverPort = 10000;
	private String currentChannel = "Main";
	private String currentServer = SERVER;
	private boolean running = true;

    private ChatTCPClient tcpClient = null;
	private static final String nick = "Väinö";
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

    private static final DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder()
			.appendValue(HOUR_OF_DAY, 2)
			.appendLiteral(':')
			.appendValue(MINUTE_OF_HOUR, 2)
			.optionalStart()
			.appendLiteral(':')
			.appendValue(SECOND_OF_MINUTE, 2)
			.toFormatter();


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
            displayMessage("Failed to run the ChatClient\nReason: " + e.getLocalizedMessage(), Color.RED);
            e.printStackTrace();
            e.getMessage();
        }
    }

	private void initUI() {


        //while (tcpClient.isConnected()) {
            //System.out.println("whilen sisäl");
            try {
                setTitle("Swing Chat Client");
                setDefaultCloseOperation(EXIT_ON_CLOSE);
                setSize(800, 600);
                channelTopics.put("Main", "Everything");

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
                //updateChannelLabels(currentChannel);
                channelListModel = new DefaultListModel<>();
                channelListModel.addElement(currentChannel);
                channelList = new JList<>(channelListModel);
                channelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                JScrollPane listScrollPane = new JScrollPane(channelList);
                

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
                channelNameLabel = new JLabel("Current Channel: "); // Replace with actual channel name
                
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

                namLabel = new JLabel("Name: ");
                namePanel.add(namLabel);
                inputPanel.add(namePanel, BorderLayout.WEST);
                inputPanel.add(inputField, BorderLayout.CENTER);
                inputPanel.add(sendButton, BorderLayout.EAST);
                add(inputPanel, BorderLayout.SOUTH);

                setVisible(true);

                

            } catch (Exception e){
                System.out.println("Error : " + e.getMessage());
            }

        //}    

        // if (tcpClient != null){
        //         tcpClient.close();
        // }

        //System.out.println("Bye!");
        
        
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
                    displayMessage("Error: Channel name cannot be empty", Color.RED);
                } else if (newTopic.isEmpty()){
                    displayMessage("Error: Channel topic cannot be empty", Color.RED);
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
                //updateChannelLabels(newChannelName);
    
            }
        });

        channelPanel.add(channelLabel);
        channelPanel.add(channelField);
        channelPanel.add(topicLabel);
        channelPanel.add(topicField);
        channelPanel.add(saveButton);
    
        nameFrame.add(channelPanel);
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


    private void changeNick(String newNick){
		namLabel.setText(newNick + ":");
        
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
    
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newName = nameField.getText();
                if (!newName.isEmpty()){
                    changeNick(newName); 
                    nameFrame.dispose();
                } else {
                    displayMessage("Error: Name cannot be empty", Color.RED);
                }
                    
            } 
        });
    
        namePanel.add(nameLabel);
        namePanel.add(nameField);
        namePanel.add(saveButton);
    
        nameFrame.add(namePanel);
        nameFrame.setVisible(true);
    }

    // private void sendMessage() {
    //     String message = inputField.getText().trim();
    //     if (!message.isEmpty()) {
    //         if (nick != null) {
    //             String selectedChannel = channelList.getSelectedValue();
    //             if (selectedChannel == null){
    //                 displayMessage("Error: No channel selected.", Color.RED);
    //             } else {

    //                 String currentTimeUTC = java.time.LocalTime.now(java.time.ZoneOffset.UTC).format(timeFormatter);
    //                 //String formattedMessage = "[" + currentTimeUTC + "] " + message;
    //                 String formattedMessage = message;


    //                 StringBuilder channelMessageBuilder = channelMessages.getOrDefault(selectedChannel, new StringBuilder());
    //                 channelMessageBuilder.append(formattedMessage).append("\n");
    //                 channelMessages.put(selectedChannel, channelMessageBuilder);

    //                 tcpClient.postChatMessage(formattedMessage);
    //                 displayMessage(" " + formattedMessage, Color.BLACK);
    //             }
				
    //         } 
    //     } 
    //     inputField.setText("");
    // }


    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            if (nick != null) {
                String selectedChannel = channelList.getSelectedValue();
                if (selectedChannel == null) {
                    displayMessage("Error: No channel selected.", Color.RED);
                } else {
                    //String currentTimeUTC = java.time.LocalTime.now(java.time.ZoneOffset.UTC).format(timeFormatter);
                    ZoneId finnishTimeZone = ZoneId.of("Europe/Helsinki");
                    ZonedDateTime currentTimeZone = ZonedDateTime.now(finnishTimeZone);
                    String currentTimeFormatted = currentTimeZone.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                    String formattedMessage = "[" + currentTimeFormatted + "] " + message; // Include time locally
    
                    StringBuilder channelMessageBuilder = channelMessages.getOrDefault(selectedChannel, new StringBuilder());
                    channelMessageBuilder.append(formattedMessage).append("\n");
                    channelMessages.put(selectedChannel, channelMessageBuilder);
    
                    displayMessage(formattedMessage, Color.BLACK); // Display message with time locally
                    tcpClient.postChatMessage(message); // Send the message to the server
                }
            }
        }
        inputField.setText("");
    }
    




    private void displayMessage(String message, Color color) {
        chatArea.append(message + "\n");
		
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
        // Handle received messages and update GUI accordingly
        return true;
    }

    @Override
    public void connectionClosed() {
        SwingUtilities.invokeLater(() -> displayMessage("Connection closed", Color.RED));
        running = false;
    }
}