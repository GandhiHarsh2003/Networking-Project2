import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.SecureRandom;
import java.util.TimerTask;
import java.util.Timer;
import javax.swing.*;

public class ClientWindow implements ActionListener {
	private static JButton poll;
	private JLabel clientID;
	private static JButton submit;
	private JRadioButton options[];
	private ButtonGroup optionGroup;
	private JLabel question;
	private static JLabel timerLabel;
	private JLabel score;
	private JLabel currScore;
	private JLabel clientIDLable;
	private TimerTask clock;
	private static Client client;
	private Timer timer;
	public static boolean canAnswer = false;

	private JFrame window;

	private static SecureRandom random = new SecureRandom();

	// write setters and getters as you need

	public ClientWindow(Client client) {
		JOptionPane.showMessageDialog(window, "This is a trivia game");
		this.client = client;
		window = new JFrame("Trivia");
		question = new JLabel("Q1. This is a sample question"); // represents the question
		window.add(question);
		question.setBounds(10, 5, 350, 100);
		

		options = new JRadioButton[4];
		optionGroup = new ButtonGroup();
		for (int index = 0; index < options.length; index++) {
			options[index] = new JRadioButton("Option " + (index + 1)); // represents an option
			// if a radio button is clicked, the event would be thrown to this class to
			// handle
			options[index].addActionListener(this);
			options[index].setBounds(10, 110 + (index * 20), 350, 20);
			window.add(options[index]);
			optionGroup.add(options[index]);
		}

		timerLabel = new JLabel("TIMER"); // represents the countdown shown on the window
		timerLabel.setBounds(250, 250, 100, 20);
		window.add(timerLabel);

		clientID = new JLabel("Client ID:"); // represents the score
		clientID.setBounds(270, 20, 100, 20);
		window.add(clientID);

		clientIDLable = new JLabel("1"); // represents the score
		clientIDLable.setBounds(335, 20, 100, 20);
		window.add(clientIDLable);

		score = new JLabel("SCORE:"); // represents the score
		score.setBounds(50, 250, 50, 20);
		window.add(score);

		currScore = new JLabel(""); 
        currScore.setBounds(100, 250, 25, 20); 
        window.add(currScore);

		poll = new JButton("Poll"); // button that use clicks/ like a buzzer
		poll.setBounds(10, 300, 100, 20);
		poll.addActionListener(this); // calls actionPerformed of this class
		window.add(poll);

		submit = new JButton("Submit"); // button to submit their answer
		submit.setBounds(200, 300, 100, 20);
		submit.addActionListener(this); // calls actionPerformed of this class
		window.add(submit);

		window.setSize(400, 400);
		window.setBounds(50, 50, 400, 400);
		window.setLayout(null);
		window.setVisible(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setResizable(false);
	}

	// this method is called when you check/uncheck any radio button
	// this method is called when you press either of the buttons- submit/poll
	@Override
	public void actionPerformed(ActionEvent e) {
		if ("Poll".equals(e.getActionCommand())) {
			client.sendBuzz();
		} else if ("Submit".equals(e.getActionCommand())) {
			timer.cancel();
            timer = null;
			String selectedOption = getSelectedOptionIndex();
			if (selectedOption != null) {
				System.out.println("picked the option " + selectedOption);
				client.sendAnswerFeedback(selectedOption);
			}
		}
		// System.out.println("You clicked " + e.getActionCommand());

		// // input refers to the radio button you selected or button you clicked
		// String input = e.getActionCommand();
		// switch(input)
		// {
		// case "Option 1": // Your code here
		// break;
		// case "Option 2": // Your code here
		// break;
		// case "Option 3": // Your code here
		// break;
		// case "Option 4": // Your code here
		// break;
		// case "Poll": // Your code here
		// break;
		// case "Submit": // Your code here
		// break;
		// default:
		// System.out.println("Incorrect Option");
		// }

		// // test code below to demo enable/disable components
		// // DELETE THE CODE BELOW FROM HERE***
		// if(poll.isEnabled())
		// {
		// poll.setEnabled(false);
		// submit.setEnabled(true);
		// }
		// else
		// {
		// poll.setEnabled(true);
		// submit.setEnabled(false);
		// }

		// // you can also enable disable radio buttons
		// // options[random.nextInt(4)].setEnabled(false);
		// // options[random.nextInt(4)].setEnabled(true);
		// // TILL HERE ***

	}


	public void updateClientID(String id) {
		clientIDLable.setText(id);
    }

	public void updateScore(String score, String correctOrWrong) {
		currScore.setText(score);
		if(correctOrWrong.equals("Correct")) {
			JOptionPane.showMessageDialog(window, "Correct Answer!!");
		} else if(correctOrWrong.equals("Wrong")){
			JOptionPane.showMessageDialog(window, "Wrong Answer!!");
		} else if(correctOrWrong.equals("Timer ran out")){
			JOptionPane.showMessageDialog(window, "Time Up!! Be Quicker Next Time!!");
		}
    }

	private String getSelectedOptionIndex() {
        for (int i = 0; i < options.length; i++) {
            if (options[i].isSelected()) {
                return String.valueOf(i + 1); 
            }
        }
        return null; 
    }

	public void startTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        clock = new TimerCode(10);
        timer.schedule(clock, 0, 1000); 
    }

	// this class is responsible for running the timer on the window
	private static class TimerCode extends TimerTask {
        private int duration;

        public TimerCode(int duration) {
            this.duration = duration;
        }

        @Override
        public void run() {
            SwingUtilities.invokeLater(() -> {
                if (duration < 0) {
					if (submit.isEnabled() && canAnswer) {
                        client.sendAnswerFeedback("Didn't answer");
                    } else {
						enablePoll(false);
						client.sendAnswerFeedback("Don't know");
					}
                    timerLabel.setText("Timer expired");
                    this.cancel(); 
                    return;
                }
                if (duration < 6) {
                    timerLabel.setForeground(Color.red);
                } else {
                    timerLabel.setForeground(Color.black);
                }
                timerLabel.setText("Time: " + duration + "s");
                duration--;
            });
        }
    }

	public void updateQuestion(String text) {
		question.setText(text);
	}

	public void setOptions(String[] optionsText) {
		for (int i = 0; i < options.length && i < optionsText.length; i++) {
			options[i].setText(optionsText[i]);
			options[i].setVisible(true);
		}
	}

	public void disableOptions() {
		canAnswer = true;
		for (int i = 0; i < options.length; i++) {
			options[i].setEnabled(false);
		}
	}

	public void enableOptions() {
		for (int i = 0; i < options.length; i++) {
			options[i].setEnabled(true);
		}
	}

	public void enableSubmit(boolean enable) {
		submit.setEnabled(enable);
	}

	public static void enablePoll(boolean enable) {
		poll.setEnabled(enable);
	}

	public void finished(String message) {
		JOptionPane.showMessageDialog(window, message);
	}
}