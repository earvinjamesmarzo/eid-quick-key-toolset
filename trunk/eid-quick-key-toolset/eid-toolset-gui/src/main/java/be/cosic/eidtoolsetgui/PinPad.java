package be.cosic.eidtoolsetgui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Event;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.ArrayList;
import java.util.Random;

import be.cosic.eidtoolset.interfaces.EidCardInterface;

public class PinPad extends Panel {
	TextField display;

	Panel keys;

	boolean maskOn = true;
	boolean leftPaddingWithDashes=false;

	String output = "";

	private Label statusLabel = null;

	private boolean enterPressed = false;

	private boolean cancelPressed = false;

	public boolean enterPressed() {
		return enterPressed;
	}

	public boolean cancelPressed() {
		return cancelPressed;
	}

	private String statusText = "";

	public void setStatusText(String text) {
		statusText = text;
		enterPressed = false;
		cancelPressed = false;
		statusLabel.setText(statusText);
		statusLabel.setVisible(true);
		updateDisplay();
	}

	boolean finished = false;

	Random rnd = new Random(System.currentTimeMillis());

	public final static String maskChar = "*";

	public final static String backspaceStr = "Backspace";

	public static String maskOffStr = "Mask Off";

	public static String maskOnStr = "Mask On";

	public final static String enterStr = "Enter";

	public final static String cancelStr = "Cancel";

	private String key1 = "", key2 = "", key3 = "", key4 = "", key5 = "",
			key6 = "", key7 = "", key8 = "", key9 = "", key0 = "",
			keyplus = "", keymin = "", keydiv = "", keymult = "",
			keyequal = "";;

	private String[] result;

	public boolean stillBusy() {
		return !(enterPressed | cancelPressed);
	}

	Button digitOne, digitTwo, digitThree, digitFour, digitFive, digitSix,
			digitSeven, digitEight, digitNine, digitZero, backspaceButton,
			maskButton, cancelButton, enterButton, shuffleButton, calcPlus,
			calcEqual, calcMin, calcDiv, calcMult;

	ArrayList buttons = new ArrayList();

	public int maxFieldLength = 50;

	private int maxLength = EidCardInterface.maxNofPinDigits;

	private int minLength = EidCardInterface.minNofPinDigits;

	public PinPad(String signatureSessionText, int min, int max, String pintext) {
		enterPressed = false;
		cancelPressed = false;
		minLength = min;
		maxLength = max;
		setLayout(new BorderLayout());
		setFont(new Font("Helvetica", Font.PLAIN, 12));
		setBackground(Color.lightGray);

		display = new TextField(maxFieldLength);
		display.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		display.setEditable(false);
		display.setFont(new Font("monospaced", Font.PLAIN, 12));
		display.setBackground(Color.white);

		keys = new Panel();
		keys.setLayout(new GridLayout(5, 4));
		keys.setFont(new Font("Helvetica", Font.PLAIN, 12));
		keys.setBackground(Color.lightGray);
		int ctr = 0;

		key1 = " 1 st  ";
		key2 = " 2 uv  ";
		key3 = " 3 wx  ";
		key4 = " 4 jkl ";
		key5 = " 5 mno ";
		key6 = " 6 pqr ";
		key7 = " 7 abc ";
		key8 = " 8 def ";
		key9 = " 9 ghi ";
		key0 = " 0 yz  ";

		buttons.add(digitOne = new Button(key1));
		buttons.add(digitTwo = new Button(key2));
		buttons.add(digitThree = new Button(key3));
		buttons.add(digitFour = new Button(key4));
		buttons.add(digitFive = new Button(key5));
		buttons.add(digitSix = new Button(key6));
		buttons.add(digitSeven = new Button(key7));
		buttons.add(digitEight = new Button(key8));
		buttons.add(digitNine = new Button(key9));
		buttons.add(digitZero = new Button(key0));
		buttons.add(backspaceButton = new Button(backspaceStr));
		buttons.add(maskButton = new Button(maskOffStr));
		buttons.add(cancelButton = new Button(cancelStr));
		buttons.add(enterButton = new Button(enterStr));

		keys.add(digitSeven);
		keys.add(digitEight);
		keys.add(digitNine);
		keys.add(digitFour);
		keys.add(digitFive);
		keys.add(digitSix);
		keys.add(digitOne);
		keys.add(digitTwo);
		keys.add(digitThree);
		keys.add(cancelButton);
		keys.add(digitZero);
		keys.add(backspaceButton);
		keys.add(maskButton);
		keys.add(enterButton);

		add("North", display);
		statusLabel = new Label(statusText, 1);

		while ((pintext.length() > 0) & (pintext.indexOf(" ") == 0))
			pintext = pintext.substring(1, pintext.length());
		while ((pintext.length() > 0)
				& (pintext.lastIndexOf(" ") == (pintext.length() - 1)))
			pintext = pintext.substring(0, pintext.length() - 2);

		setStatusText(((signatureSessionText != null ? (signatureSessionText + ", enter")
				: "Enter")
				+ " your "
				+ pintext
				+ " PIN ("
				+ minLength
				+ " to "
				+ maxLength + " digits)").replaceAll("  ", " "));

		add("Center", statusLabel);
		add("South", keys);
		updateDisplay();
		setVisible(true);
	}

	public void updateDisplay() {
		String output_right = "";
if (leftPaddingWithDashes)		for (int i = 1; i <= (maxLength - output.length()); i++)
			output_right = output_right + "-";
		if (maskOn)
			for (int i = 0; i < output.length(); i++)
				output_right += maskChar;
		else
			output_right = output_right + output;
		display.setText(output_right);
	}

	public void appendDigit(String new_d) {
		if (output.length() < maxFieldLength) {
			output = output + new_d;
			updateDisplay();
		}
	}

	public String getPinValue() {
		String result = output;
		output = "";
		updateDisplay();
		return result;
	}

	public boolean handleEvent(Event evt) {
		if (evt.arg == backspaceStr) {
			if (output.length() > 0)
				output = output.substring(0, output.length() - 1);
			updateDisplay();
		} else if (evt.arg == key1)
			appendDigit("1");
		else if (evt.arg == key2)
			appendDigit("2");
		else if (evt.arg == key3)
			appendDigit("3");
		else if (evt.arg == key4)
			appendDigit("4");
		else if (evt.arg == key5)
			appendDigit("5");
		else if (evt.arg == key6)
			appendDigit("6");
		else if (evt.arg == key7)
			appendDigit("7");
		else if (evt.arg == key8)
			appendDigit("8");
		else if (evt.arg == key9)
			appendDigit("9");
		else if (evt.arg == key0)
			appendDigit("0");
		else if ((evt.arg == maskOnStr) | (evt.arg == maskOffStr)) {
			Button target = null;
			int i = 0;
			if (maskOn)
				for (i = 0; i < buttons.size(); i++) {
					if (((Button) buttons.get(i)).getLabel() == maskOffStr)
						target = (Button) buttons.get(i);
				}
			else {
				for (i = 0; i < buttons.size(); i++) {
					if (((Button) buttons.get(i)).getLabel() == maskOnStr)
						target = (Button) buttons.get(i);
				}
			}
			maskOn = !maskOn;
			if (maskOn)
				target.setLabel(maskOffStr);
			else
				target.setLabel(maskOnStr);
			target.setVisible(true);
			updateDisplay();
		} else if (evt.arg == cancelStr)
			cancelPressed = true;
		else if (evt.arg == enterStr) {
			if ((minLength <= output.length())
					&& (output.length() <= maxLength))
				enterPressed = true;
		}
		return false;
	}
}