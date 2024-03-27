import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Queue;

public class UDPThread implements Runnable {
    private DatagramSocket socket;
    private final static Queue<String> queue = new LinkedList<>();

    public UDPThread(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];

        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String clientId = new String(packet.getData(), 0, packet.getLength()).trim();
                System.out.println(clientId);
                queue.offer(clientId);
                //System.out.println("client id is " + clientId);
            } catch (IOException e) {
                System.out.println("UDP Thread Error: " + e.getMessage());
            }
        }
    }

    public String firstInLine() {
        int i = 0;
        while(i < queue.size()) {
            String clientId = queue.poll();
            System.out.println("in queue are this people" + clientId);
            queue.add(clientId);
            i++;
        } 
       return queue.peek();
    }

    public void removeClients() {
        while(!queue.isEmpty()) {
            queue.poll();
        }
    }
}
