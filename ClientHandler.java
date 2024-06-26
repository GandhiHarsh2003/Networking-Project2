import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {
    private static final Set<ClientHandler> handlers = Collections.synchronizedSet(new HashSet<>());
    private static int currentQuestionIndex = 1;
    public static int totalQuestions = 20;
    private final Socket clientSocket;
    private final UDPThread udpThread;
    private PrintWriter out;
    private DataOutputStream dos;
    private DataInputStream dis;
    private BufferedReader in;
    private int clientId;
    private static String correctAnswer = "";
    private static HashMap<String, Integer> scores = new HashMap<>();
    private static Set<String> respondedClients = new HashSet<String>();
    private static boolean sentFinal = false;
    private static int answeringQuestion = 0;
    public static boolean answeringClientLeft = false;

    public ClientHandler(Socket socket, int clientId, UDPThread udpThread) {
        this.clientSocket = socket;
        this.udpThread = udpThread;
        this.clientId = clientId;
        scores.putIfAbsent(String.valueOf(clientId), 0);
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
            dos.writeUTF(String.valueOf(clientId));
            dos.flush();
            synchronized (handlers) {
                if (!handlers.contains(this)) {
                    handlers.add(this);
                }
                sendCurrentQuestion();
            }

            String feedback;
            while ((feedback = dis.readUTF()) != null) {
                System.out.println("feedback from client " + feedback);
                System.out.println("Correct answer is " + correctAnswer);
                if (correctAnswer.equals("")) {
                    correctAnswer = findAnswer();
                }
                synchronized (handlers) {
                    if ("Buzz".equals(feedback.trim())) {
                        if (udpThread.checkIfEmpty() == false) {
                            System.out.println("not waiting");
                        } else {
                            System.out.println("waiting");
                        }
                        while (true) {
                            if (udpThread.checkIfEmpty() == false) {
                                handleBuzz(clientId);
                                break;
                            }
                        }
                    } else if (feedback.trim().equals("Didn't answer")) {
                        System.out.println("Didn't answer so get penalized");
                        updateScore(String.valueOf(clientId), "Penalize");
                        respondedClients.add(String.valueOf(clientId));
                        if (currentQuestionIndex == totalQuestions && respondedClients.size() == handlers.size()) {
                            sendFinishMessage();
                            break;
                        }
                        udpThread.removeID(clientId);
                        if (udpThread.checkIfEmpty() == false) {
                            giveSecondChanceToNextClient();
                        } else {
                            checkIfAllResponded();
                        }
                    } else if (correctAnswer.equals(feedback.trim())) {
                        System.out.println("This is the correct answer so moving on to the next one");
                        updateScore(String.valueOf(clientId), "Correct");
                        if (currentQuestionIndex == totalQuestions) {
                            respondedClients.add(String.valueOf(clientId));
                            answeringClientLeft = true;
                            sendFinishMessage();
                            break;
                        }
                        handleNext();
                    } else if ("Don't know".equals(feedback.trim())) {
                        respondedClients.add(String.valueOf(clientId));
                        if (currentQuestionIndex == totalQuestions && respondedClients.size() == handlers.size()) {
                            sendFinishMessage();
                            break;
                        }
                        System.out.println("didn't answer " + clientId);
                        checkIfAllResponded();
                    } else if (!correctAnswer.equals(feedback.trim())) {
                        System.out.println("WRONG ANSWER!!!!! so moving on to the next one");
                        updateScore(String.valueOf(clientId), "Wrong");
                        respondedClients.add(String.valueOf(clientId));
                        if (currentQuestionIndex == totalQuestions && respondedClients.size() == handlers.size()) {
                            sendFinishMessage();
                            break;
                        }
                        udpThread.removeID(clientId);
                        if (udpThread.checkIfEmpty() == false) {
                            giveSecondChanceToNextClient();
                        } else {
                            checkIfAllResponded();
                        }
                    }
                }
            }
        } catch (IOException e) {
            Iterator<ClientHandler> iterator = handlers.iterator();
            while (iterator.hasNext()) {
                ClientHandler handler = iterator.next();
                if (handler.clientId == clientId) {
                    udpThread.removeID(clientId);
                    respondedClients.remove(String.valueOf(clientId));
                    scores.remove(String.valueOf(clientId));
                    iterator.remove();
                    if (currentQuestionIndex == totalQuestions) {
                        try {
                            sendFinishMessage();
                        } catch (IOException e1) {
                            System.out.println("Error occured in sending win message " + e1);
                        }
                    }
                    if (respondedClients.size() == handlers.size()) {
                        try {
                            handleNext();
                        } catch (IOException e1) {
                            System.out.println("Error occured in next question " + e1);
                        }
                    }
                    break;
                }
            }
            if (answeringQuestion == clientId) {
                try {
                    scores.remove(String.valueOf(clientId));
                    if (udpThread.checkIfEmpty() == false) {
                        udpThread.removeID(clientId);
                        giveSecondChanceToNextClient();
                    } else {
                        if (currentQuestionIndex == totalQuestions) {
                            sendFinishMessage();
                        } else {
                            checkIfAllResponded();
                        }
                    }
                } catch (IOException e1) {
                    System.out.println("Error occured in next question " + e1);
                }
            }
            System.err.println("Error in communication with client " + clientId + ": " + e.getMessage());
        } finally {
            if (sentFinal == true) {
                killSwitch();
            }
        }
    }


    private void giveSecondChanceToNextClient() throws IOException {
        String nextClientId = udpThread.firstInLine();
        if (nextClientId != null) {
            System.out.println("Giving a second chance to client: " + nextClientId);
            sendAckToSecondClient(nextClientId);
        } else {
            System.out.println("No more clients in line.");
            handleNext();
        }
    }

    private void sendAckToSecondClient(String clientId) throws IOException {
        ClientHandler clientHandler = findClientHandlerById(clientId);
        answeringQuestion = Integer.parseInt(clientId);
        if (clientHandler != null) {
            clientHandler.dos.writeUTF("ack");
            clientHandler.dos.flush();
        } else {
            System.err.println("ClientHandler not found for ID: " + clientId);
        }
    }

    private ClientHandler findClientHandlerById(String clientId) {
        synchronized (handlers) {
            for (ClientHandler handler : handlers) {
                if (String.valueOf(handler.clientId).equals(clientId)) {
                    return handler;
                }
            }
        }
        return null;
    }

    private void killSwitch() {
        try {
            if (sentFinal == true) {
                terminate();
                handlers.clear();
                if (clientSocket != null)
                    clientSocket.close();
                if (out != null)
                    out.close();
                if (dos != null)
                    dos.close();
                if (in != null)
                    in.close();
                System.exit(0);
            }
        } catch (IOException e) {
            System.err.println("Error closing resources for client " + clientId + ": " + e.getMessage());
        }
    }

    private void terminate() throws IOException {
        synchronized (ClientHandler.class) {
            for (ClientHandler handler : handlers) {
                DataOutputStream handlerDos = handler.dos;
                handlerDos.writeUTF("TERMINATE");
            }
        }
    }

    private void checkIfAllResponded() throws IOException {
        System.out.println("res: " + respondedClients.size());
        System.out.println("han: " + handlers.size());
        if (respondedClients.size() == handlers.size() && currentQuestionIndex != 20) {
            correctAnswer = "";
            respondedClients.clear();
            handleNext();
        }
    }

    private void updateScore(String clientID, String check) throws IOException {
        if ("Correct".equals(check)) {
            int currentScore = scores.get(clientID) + 10;
            scores.put(clientID, currentScore);
            dos.writeUTF("UPDATE");
            dos.writeUTF(String.valueOf(currentScore));
            dos.writeUTF("Correct");
            dos.flush();
        } else if ("Wrong".equals(check)) {
            int currentScore = scores.get(clientID) - 10;
            scores.put(clientID, currentScore);
            dos.writeUTF("UPDATE");
            dos.writeUTF(String.valueOf(currentScore));
            dos.writeUTF("Wrong");
            dos.flush();
        } else if ("Penalize".equals(check)) {
            int currentScore = scores.get(clientID) - 20;
            scores.put(clientID, currentScore);
            dos.writeUTF("UPDATE");
            dos.writeUTF(String.valueOf(currentScore));
            dos.writeUTF("Timer ran out");
            dos.flush();
        }
    }

    private void handleNext() throws IOException {
        synchronized (ClientHandler.class) {
            correctAnswer = "";
            answeringQuestion = 0;
            respondedClients.clear();
            udpThread.removeClients();
            currentQuestionIndex++;
            for (ClientHandler handler : handlers) {
                handler.sendCurrentQuestion();
            }
        }
    }

    private void sendFinishMessage() throws IOException {
        int highestScore = scores.values().stream().max(Integer::compare).orElse(0);
        synchronized (ClientHandler.class) {
            System.out.println("Sending finish message");
            if (respondedClients.size() >= handlers.size() || answeringClientLeft) {
                Set<String> winners = scores.entrySet().stream()
                        .filter(entry -> entry.getValue().equals(highestScore))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());

                for (ClientHandler handler : handlers) {
                    DataOutputStream handlerDos = handler.dos;
                    handlerDos.writeUTF("FINISHED");
                    if (winners.contains(String.valueOf(handler.clientId))) {
                        handlerDos.writeUTF("Game Finished, YOU WON!!");
                    } else {
                        handlerDos.writeUTF("Game Finished, YOU LOST!!");
                    }
                    handlerDos.flush();
                }
                printScoresInDescendingOrder();
                sentFinal = true;
            }
        }
    }

    private String findAnswer() {
        String questionFilePath = "Questions/Question" + currentQuestionIndex + ".txt";
        String answer = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(questionFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Correct: ")) {
                    answer = line.replace("Correct: ", "");
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the question file: " + e.getMessage());
        }
        return answer;
    }

    private void sendCurrentQuestion() throws IOException {
        correctAnswer = "";
        String questionFilePath = "Questions/Question" + currentQuestionIndex + ".txt";
        byte[] fileContent = Files.readAllBytes(Paths.get(questionFilePath));
        dos.writeUTF("Next Question");
        dos.writeInt(fileContent.length);
        dos.write(fileContent);
        dos.flush();
    }

    private void handleBuzz(int clientID) throws IOException {
        String firstClientId = udpThread.firstInLine();
        if (String.valueOf(clientID).equals(firstClientId)) {
            System.out.println("sending ack to " + firstClientId);
            answeringQuestion = clientId;
            dos.writeUTF("ack");
            dos.flush();
        } else {
            System.out.println("sending nack to " + clientID);
            dos.writeUTF("nack");
            dos.flush();
        }
    }

    public void printScoresInDescendingOrder() {
        scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> System.out
                        .println("Client " + entry.getKey() + " scored " + entry.getValue() + " points"));
    }
}