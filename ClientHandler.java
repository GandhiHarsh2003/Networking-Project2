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
    private DataOutputStream dos;
    private BufferedReader in;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        try {
            dos = new DataOutputStream(clientSocket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            System.err.println("Error setting up streams: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        handlers.add(this);
        try {
            while (true) {
                synchronized (handlers) {
                    if (currentQuestionIndex > 20) {
                        break;
                    }
                    sendCurrentQuestion();
                }

                String feedback = in.readLine();
                if (feedback == null) break; 
                synchronized (handlers) {
                    System.out.println("Feedback from client: " + feedback);
                    currentQuestionIndex++; 
                    for (ClientHandler handler : handlers) {
                        handler.sendCurrentQuestion();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error in communication: " + e.getMessage());
        } finally {
            try {
                handlers.remove(this);
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing the client socket: " + e.getMessage());
            }
        }
    }

    private void sendCurrentQuestion() throws IOException {
        String questionFilePath = "Question" + currentQuestionIndex + ".txt";
        byte[] fileContent = Files.readAllBytes(Paths.get(questionFilePath));
        dos.writeInt(fileContent.length);
        dos.write(fileContent);
        dos.flush();
    }
}
