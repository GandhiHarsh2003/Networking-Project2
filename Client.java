import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class Client {
    private Socket socket;
    private final String serverAddress;
    private final int serverPort;
    private ClientWindow clientWindow;
    private PrintWriter out;

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
            while (true) {
                int fileLength = dis.readInt();
                if (fileLength > 0) {
                    byte[] fileContent = new byte[fileLength];
                    dis.readFully(fileContent, 0, fileContent.length);
                    String fileName = "clientQuestion.txt";
                    saveToFile(fileName, fileContent);
                    displayQuestionFromFile(fileName);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not connect to the server or receive the file: " + e.getMessage());
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
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
            System.out.println(feedback);
            out.println(feedback);
            out.flush();
        }
    }

    public static void main(String[] args) {
        Client client = new Client("127.0.0.1", 1234);
        client.connectToServer();
    }
}
