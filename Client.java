import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class Client {
	private Socket socket;
	private String CLIENT_ID;
	private final String serverAddress;
	private final int serverPort;
	private ClientWindow clientWindow;
	private PrintWriter out;
	private static final int UDPport = 4445;
	public boolean read = true;
	private DataInputStream dis;
	private DataOutputStream dos;
	public static String usersIP = "127.0.0.1";

	public Client(String serverAddress, int serverPort) {
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.clientWindow = new ClientWindow(this);
	}

	public void connectToServer() {
		try {
			socket = new Socket(serverAddress, serverPort);
			dos = new DataOutputStream(socket.getOutputStream());
			out = new PrintWriter(socket.getOutputStream(), true);
			dis = new DataInputStream(socket.getInputStream());
			String clientId = dis.readUTF();
			CLIENT_ID = clientId;
			clientWindow.updateClientID(clientId);
			listenForServerMessages(dis);

		} catch (IOException e) {
			System.err.println("Could not connect to the server or receive the file: " + e.getMessage());
		}
	}

	private void listenForServerMessages(DataInputStream dis) {
	    new Thread(() -> {
	        try {
	            while (!socket.isClosed()) {
	                String response = dis.readUTF();
	                System.out.println("Response from server: " + response);
	                switch (response) {
	                    case "ack":
	                        clientWindow.enableOptions();
	                        System.out.println("I was first!");
	                        break;
	                    case "nack":
	                        System.out.println("Not first.");
	                        break;
	                    case "Next Question":
	                    	System.out.println("Curr Question is happening");
							int fileLength = dis.readInt();
							System.out.println("file length " + fileLength);
							if (fileLength > 0) {
								System.out.println("Curr Question is happening22222");
								byte[] content = new byte[fileLength];
								dis.readFully(content, 0, fileLength);
								String fileName = "clientQuestion.txt";
								saveToFile(fileName, content);
								displayQuestionFromFile(fileName);
								clientWindow.disableOptions();
								response = "";
							}	                        
							break;
	                }
	            }
	        } catch (IOException e) {
	            System.err.println("Error reading from server: " + e.getMessage());
	        }
	    }).start();
	}

	private static void saveToFile(String fileName, byte[] content) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(fileName)) {
			fos.write(content);
		}
	}

	private void displayQuestionFromFile(String fileName) {
		try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
			StringBuilder questionBuilder = new StringBuilder();
			ArrayList<String> options = new ArrayList<>();
			String correctAnswer = "";

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("Correct: ")) {
					correctAnswer = line.replace("Correct: ", "");
				} else if (!line.trim().isEmpty()) {
					if (questionBuilder.length() == 0) {
						questionBuilder.append(line);
					} else {
						options.add(line);
					}
				}
			}
			this.clientWindow.updateQuestion(questionBuilder.toString());
			this.clientWindow.setOptions(options.toArray(new String[0]), correctAnswer);

		} catch (IOException e) {
			System.err.println("Error reading the question file: " + e.getMessage());
		}
	}

	public void sendAnswerFeedback(String feedback) {
		read = true;
		System.out.println(feedback);
		if (dos != null) {
			try {
				dos.writeUTF(feedback);
				dos.flush();
			} catch (IOException e) {
				System.err.println("Error sending feedback: " + e.getMessage());
			}
		} else {
			System.out.println("DataOutputStream 'dos' is not initialized.");
		}
	}

	public void sendBuzz() {
		try (DatagramSocket socket = new DatagramSocket()) {
			String message = String.valueOf(CLIENT_ID);
			byte[] messageBytes = message.getBytes();
			InetAddress serverAddress = InetAddress.getByName(usersIP);
			DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverAddress, UDPport);
			socket.send(packet);
			sendBuzzToServer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendBuzzToServer() {
		if (dos != null) {
			try {
				dos.writeUTF("buzz");
				dos.writeUTF(CLIENT_ID);
			} catch (Exception e) {
				System.out.println("An error occurred: " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			System.out.println("DataOutputStream 'dos' is not yet initialized or socket is closed.");
		}
	}

	public static void main(String[] args) {
		Client client = new Client(usersIP, 1234);
		client.connectToServer();
	}
}