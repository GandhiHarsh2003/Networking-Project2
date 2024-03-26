import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.SecureRandom;
import java.util.TimerTask;
import java.util.Timer;
import javax.swing.*;

public class ClientWindow implements ActionListener {
	private JButton poll;
	private JLabel clientID;
	private JButton submit;
	private JRadioButton options[];
	private ButtonGroup optionGroup;
	private JLabel question;
	private JLabel timer;
	private JLabel score;
	private JLabel currScore;
	private int currentScore = 0;
	private TimerTask clock;
	private String answer;
	private Client client;

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

		timer = new JLabel("TIMER"); // represents the countdown shown on the window
		timer.setBounds(250, 250, 100, 20);
		clock = new TimerCode(30); // represents clocked task that should run after X seconds
		Timer t = new Timer(); // event generator
		t.schedule(clock, 0, 1000); // clock is called every second
		window.add(timer);

		clientID = new JLabel("Client ID:"); // represents the score
		clientID.setBounds(280, 20, 100, 20);
		window.add(clientID);

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
			client.sendBuzz(); // Call the method when Poll button is clicked
		} else if ("Submit".equals(e.getActionCommand())) {
			// Existing submit logic
			String selectedOption = getSelectedOptionIndex();
			if (selectedOption != null && selectedOption.equals(answer)) {
				updateScore(true);
				client.sendAnswerFeedback("Next");
				JOptionPane.showMessageDialog(window, "Correct Answer!");
			} else {
				updateScore(false);
				client.sendAnswerFeedback("Next");
				JOptionPane.showMessageDialog(window, "Wrong Answer!");
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
		clientID.setText(id);
    }

	private void updateScore(boolean correct) {
		if (correct) {
			currentScore += 10; 
		} else {
			currentScore -= 10; 
		}
		currScore.setText(String.valueOf(currentScore));
    }

	private String getSelectedOptionIndex() {
        for (int i = 0; i < options.length; i++) {
            if (options[i].isSelected()) {
                return String.valueOf(i + 1); 
            }
        }
        return null; 
    }

	// this class is responsible for running the timer on the window
	public class TimerCode extends TimerTask {
		private int duration; // write setters and getters as you need

		public TimerCode(int duration) {
			this.duration = duration;
		}

		@Override
		public void run() {
			if (duration < 0) {
				timer.setText("Timer expired");
				window.repaint();
				this.cancel(); // cancel the timed task
				return;
				// you can enable/disable your buttons for poll/submit here as needed
			}

			if (duration < 6)
				timer.setForeground(Color.red);
			else
				timer.setForeground(Color.black);

			timer.setText(duration + "");
			duration--;
			window.repaint();
		}
	}

	public void updateQuestion(String text) {
		question.setText(text);
	}

	public void setOptions(String[] optionsText, String correctAnswer) {
		answer = correctAnswer;
		for (int i = 0; i < options.length && i < optionsText.length; i++) {
			options[i].setText(optionsText[i]);
			options[i].setVisible(true);
		}
	}

	public void disableOptions() {
		for (int i = 0; i < options.length; i++) {
			options[i].setEnabled(false);
		}
	}

	public void enableOptions() {
		for (int i = 0; i < options.length; i++) {
			options[i].setEnabled(true);
		}
	}
}