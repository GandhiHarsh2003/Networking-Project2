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

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.clientWindow = new ClientWindow(this);
    }

    public void connectToServer() {
        try {
            socket = new Socket(serverAddress, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            DataInputStream dis = new DataInputStream(socket.getInputStream());
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
                    if (read) {
                        read = false;
                        int fileLength = dis.readInt();
                        if (fileLength > 0) {
                            //System.out.println(fileLength);
                            byte[] content = new byte[fileLength];
                            dis.readFully(content, 0, fileLength);
                            String fileName = "clientQuestion.txt";
                            saveToFile(fileName, content);
                            displayQuestionFromFile(fileName);
                            clientWindow.disableOptions();
                        }
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
        if (out != null) {
            read = true;
            //System.out.println(feedback);
            out.println(feedback);
            out.flush();
        }
    }

    public void sendBuzz() {
        try (DatagramSocket socket = new DatagramSocket()) {
            String message = String.valueOf(CLIENT_ID);
            byte[] messageBytes = message.getBytes();
            InetAddress serverAddress = InetAddress.getByName("127.0.0.1");
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverAddress, UDPport);
            socket.send(packet);
            sendBuzzToServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendBuzzToServer() {
        if (out != null) {
            try {
                out.println("buzz");
                out.println(CLIENT_ID);
                out.flush();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String response = reader.readLine();

                System.out.println(response);
                if ("ack".equals(response)) {
                    clientWindow.enableOptions();
                    System.out.println("I was first!");
                } else {
                    System.out.println("Not first.");
                }
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("PrintWriter 'out' is not yet initialized or socket is closed.");
        }
    }

    public static void main(String[] args) {
        Client client = new Client("127.0.0.1", 1234);
        client.connectToServer();
    }
}
