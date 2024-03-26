import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final AtomicInteger clientIdGenerator = new AtomicInteger(0);
    private static final int UDPport = 4445;
    private static UDPThread udpThread;

    public static void main(String[] args) {
        int port = 1234;
        try {
            udpThread = new UDPThread(UDPport);
            new Thread(udpThread).start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                int clientId = clientIdGenerator.incrementAndGet();
                System.out.println("Client connected with " + clientId);
                ClientHandler handler = new ClientHandler(clientSocket, clientId, udpThread);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }
}
