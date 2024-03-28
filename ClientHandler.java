import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ClientHandler implements Runnable {
    private static final Set<ClientHandler> handlers = Collections.synchronizedSet(new HashSet<>());
    private static int currentQuestionIndex = 1;
    private final Socket clientSocket;
    private final UDPThread udpThread;
    private PrintWriter out;
    private String receivedID;
    private DataOutputStream dos;
	private DataInputStream dis;
    private BufferedReader in;
    private int clientId;
    private String correctAnswer;

    public ClientHandler(Socket socket, int clientId, UDPThread udpThread) {
        this.clientSocket = socket;
        this.udpThread = udpThread;
        this.clientId = clientId;
        try {
            dos = new DataOutputStream(clientSocket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Error setting up streams: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            dos.writeUTF("ClientID: " + clientId);
            dos.flush();
            synchronized (handlers) {
                if (!handlers.contains(this)) {
                    handlers.add(this);
                }
                System.out.println("Next");

                sendCurrentQuestion();
            }

            String feedback;
            while ((feedback = dis.readUTF()) != null) {
                System.out.println("Feedback from client " + clientId + ": " + feedback);
                synchronized (handlers) {
                    if ("buzz".equals(feedback.trim())) {
                        receivedID = dis.readUTF();
                        System.out.println(receivedID);
                        handleBuzz();
                    } else if ("Next".equals(feedback.trim())) {
                        udpThread.removeClients();
                        System.out.println("sent the notification");
                        handleNext();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error in communication with client " + clientId + ": " + e.getMessage());
        } finally {
            try {
                handlers.remove(this);
                if (clientSocket != null)
                    clientSocket.close();
                if (out != null)
                    out.close();
                if (dos != null)
                    dos.close();
                if (in != null)
                    in.close();
            } catch (IOException e) {
                System.err.println("Error closing resources for client " + clientId + ": " + e.getMessage());
            }
        }
    }

    private void handleNext() throws IOException {
        synchronized (ClientHandler.class) {
            currentQuestionIndex++;
            findAnswer();
            for (ClientHandler handler : handlers) {

                handler.sendCurrentQuestion();
            }
        }
    }

    private void findAnswer() {
        String questionFilePath = "Question" + currentQuestionIndex + ".txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(questionFilePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("Correct: ")) {
					correctAnswer = line.replace("Correct: ", "");
				}
			}
		} catch (IOException e) {
			System.err.println("Error reading the question file: " + e.getMessage());
		}
    }

    private void sendCurrentQuestion() throws IOException {
        String questionFilePath = "Question" + currentQuestionIndex + ".txt";
        byte[] fileContent = Files.readAllBytes(Paths.get(questionFilePath));
        System.out.println(fileContent.length);
        dos.writeUTF("Next Question");
        dos.writeInt(fileContent.length);
        dos.write(fileContent);
        dos.flush();
    }

    private void handleBuzz() throws IOException {
        String firstClientId = udpThread.firstInLine();
        if (receivedID.equals(firstClientId)) {
            System.out.println("sending ack");
            dos.writeUTF("ack");
            dos.flush();
        } else {
            dos.writeUTF("nack");
            dos.flush();
        }
    }
}