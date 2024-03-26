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
    private BufferedReader in;
    private int clientId;

    public ClientHandler(Socket socket, int clientId, UDPThread udpThread) {
        this.clientSocket = socket;
        this.udpThread = udpThread;
        this.clientId = clientId;
        try {
            dos = new DataOutputStream(clientSocket.getOutputStream());
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
                sendCurrentQuestion();
            }

            String feedback;
            while ((feedback = in.readLine()) != null) {
                //System.out.println("Feedback from client " + clientId + ": " + feedback);
                synchronized (handlers) {
                    if ("buzz".equals(feedback.trim())) {
                        receivedID = in.readLine();
                        handleBuzz();
                    } else if ("Next".equals(feedback.trim())) {
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
            for (ClientHandler handler : handlers) {
                handler.sendCurrentQuestion();
            }
        }
    }

    private void sendCurrentQuestion() throws IOException {
        String questionFilePath = "Question" + currentQuestionIndex + ".txt";
        //System.out.println(questionFilePath);
        byte[] fileContent = Files.readAllBytes(Paths.get(questionFilePath));
        //System.out.println(fileContent.length);
        dos.writeInt(fileContent.length);
        dos.write(fileContent);
        dos.flush();
    }

    private void handleBuzz() {
        String firstClientId = udpThread.firstInLine();
        if (receivedID.equals(firstClientId)) {
            System.out.println("sending ack");
            out.println("ack");
            out.flush();
        } else {
            out.println("nack");
            out.flush();
        }
    }
}
