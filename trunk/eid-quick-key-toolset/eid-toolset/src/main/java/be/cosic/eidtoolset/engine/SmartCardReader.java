package be.cosic.eidtoolset.engine;

import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.smartcardio.*;

import be.cosic.eidtoolset.exceptions.InvalidResponse;
import be.cosic.eidtoolset.exceptions.NoReadersAvailable;
import be.cosic.eidtoolset.exceptions.NoCardConnected;
import be.cosic.eidtoolset.exceptions.AIDNotFound;
import be.cosic.eidtoolset.interfaces.SmartCardInterface;
import be.cosic.eidtoolset.interfaces.SmartCardReaderCommandsInterface;
import be.cosic.eidtoolset.interfaces.SmartCardReaderInterface;
import be.cosic.util.TextUtils;


@SuppressWarnings("restriction")
public class SmartCardReader implements SmartCardReaderCommandsInterface,
		SmartCardReaderInterface {

	private SmartCardInterface smartcard;

	private List readers; 
	private CardTerminal reader;
	private Card card;
	private CardChannel conn;

	private int usingReaderNr = -1;
	private String myName = "";
	
	/**
	 * debugLevel = 0: no debug information; 1: minimal debug information
	 * (reader and card information); 2: maximal debug information (apdus)
	 */
	private int debugLevel = 0;

	public void setDebugLevel(int dbg) {
		debugLevel = dbg;
	}

	
	public byte[] sendCommand(byte[] command) throws InvalidResponse, NoCardConnected, CardException {
		if (card == null) {
			//should not occur if reader/card was selected and card/reader not withdrawn
			//TODO: check this see also connectToCard method that is commented out
			throw new NoCardConnected();
		}
		if (debugLevel > 1)
			System.err.println("Sending <" + TextUtils.hexDump(command)
					+ "> to card in reader <" + ((CardTerminal)readers.get(usingReaderNr)).getName() + ">");
		
		ResponseAPDU response = conn.transmit(new CommandAPDU(command));
		
		if (debugLevel > 1)
			System.err.println("Receiving data <" + TextUtils.hexDump(response.getBytes())
					+ ">");
		
//		if (response.getSW() != (Integer)0x9000)
//			throw new InvalidResponse();
		
		return response.getBytes();
	}

	//TODO: check if necessary
//	private Card connectToCard() {
////		return ctx.Connect(state.szReader, PCSC.SHARE_SHARED, PCSC.PROTOCOL_T0
////				| PCSC.PROTOCOL_T1);
//		return reader.connect("T=0");
//	}

	public byte[] getATR() {
		return card.getATR().getBytes();
	}

	public String getMyName() {
		return myName;
	}

	public void lookForSmartCard(String preferredReader,
			int milliSecondsBeforeTryingAnotherReader, byte[] AID_APDU)
			throws NoReadersAvailable, CardException, NoSuchAlgorithmException, AIDNotFound {
		
		//TODO: check if working
		//Comment is code from danny: TODO: remove
		
//		ctx = new Context();
//		ctx.EstablishContext(PCSC.SCOPE_SYSTEM, null, null);
//		readerNames = ctx.ListReaders();
		readers = TerminalFactory.getInstance("PC/SC", null).terminals().list();
		
		
//		if (readerNames.length == 0)
//			throw new NoReadersAvailable();
		if (readers.isEmpty()){
			throw new NoReadersAvailable();
		}
		
		
//		for (int i = 0; i < readerNames.length; i++)
//			if (debugLevel > 0)
//				System.err.println("Discovered smart card reader <"
//						+ readerNames[i] + "> as reader <" + i + ">");
		if (debugLevel > 0){
			for(int i = 0; i < readers.size(); i++)
					System.err.println("Discovered smart card reader <"
							+ ((CardTerminal)readers.get(i)).getName() + "> as reader <" + i + ">");
		}
		
//		usingReaderNr = 0;
//		preferredReader = preferredReader.toUpperCase();
//		for (int i = 0; i < readers.size(); i++)
//			if (readerNames[i].toUpperCase().indexOf(preferredReader) >= 0)
//				usingReaderNr = i;
		usingReaderNr = 0;
		preferredReader = preferredReader.toUpperCase();
		for (int i = 0; i < readers.size(); i++)
			if (((CardTerminal)readers.get(i)).getName().toUpperCase().indexOf(preferredReader) >= 0)
				usingReaderNr = i;
		
		
		
//		if (debugLevel > 0)
//			System.err.println("Using smart card reader <" + usingReaderNr
//					+ ">, <" + readerNames[usingReaderNr]
//					+ ">, preferred reader was <" + preferredReader + ">");
		if (debugLevel > 0)
			System.err.println("Using smart card reader <" + usingReaderNr
					+ ">, <" + ((CardTerminal)readers.get(usingReaderNr)).getName()
					+ ">, preferred reader was <" + preferredReader + ">");
		
		
//		state = null;
//		do {
//			if (debugLevel > 0)
//				System.err
//						.println("Waiting for a card to be inserted into smart card reader <"
//								+ usingReaderNr
//								+ ">, <"
//								+ readerNames[usingReaderNr]
//								+ ">, will timeout in <"
//								+ milliSecondsBeforeTryingAnotherReader
//								+ "> milliseconds");
//			State[] rsa = new State[1];
//			rsa[0] = new State(readerNames[usingReaderNr]);
//			ctx.GetStatusChange(250, rsa);
//			if ((rsa[0].dwEventState & PCSC.STATE_PRESENT) == PCSC.STATE_PRESENT) {
//				state = rsa[0];
//			} else {
//				state = null;
//			}
//
//			/*
//			 * the following line works for jpcsc 0.8.0, but not for 0.7 //
//			 * state =
//			 * ctx.WaitForCard(readerNames[usingReaderNr],milliSecondsBeforeTryingAnotherReader);
//			 */
//
//			if (state == null) {
//				usingReaderNr++;
//				if (usingReaderNr >= readerNames.length)
//					usingReaderNr = 0;
//				if (debugLevel > 0)
//					System.err.println("Trying again with smart card reader <"
//							+ usingReaderNr + ">, <"
//							+ readerNames[usingReaderNr]
//							+ "> as no card was detected within <"
//							+ milliSecondsBeforeTryingAnotherReader
//							+ "> milliseconds");
//			}
//		} while (state == null);

		card = null;
		do {
			if (debugLevel > 0)
				System.err
						.println("Waiting for a card to be inserted into smart card reader <"
								+ usingReaderNr
								+ ">, <"
								+ ((CardTerminal)readers.get(usingReaderNr)).getName()
								+ ">, will timeout in <"
								+ milliSecondsBeforeTryingAnotherReader
								+ "> milliseconds");
			
			reader = (CardTerminal)readers.get(usingReaderNr);
			
			if (reader.isCardPresent() || reader.waitForCardPresent(milliSecondsBeforeTryingAnotherReader)){
				//Always connect using T=0
				card = reader.connect("T=0");
				conn = card.getBasicChannel();
				
				selectApplet(AID_APDU);
				
			}else card = null;

			if (card == null) {
				usingReaderNr++;
				if (usingReaderNr >= readers.size())
					usingReaderNr = 0;
				if (debugLevel > 0)
					System.err.println("Trying again with smart card reader <"
							+ usingReaderNr + ">, <"
							+ ((CardTerminal)readers.get(usingReaderNr)).getName()
							+ "> as no card was detected within <"
							+ milliSecondsBeforeTryingAnotherReader
							+ "> milliseconds");
			}
		} while (card == null);
		
		
		
		
//		if (debugLevel > 0)
//			System.err.println("Discovered card to <" + state.szReader + ">");
//		if (debugLevel > 0)
//			System.err.println("Card ATR is <"
//					+ TextUtils.hexDump(state.rgbAtr) + ">");
//		myName = readerNames[usingReaderNr];
		
		if (debugLevel > 0)
			System.err.println("Discovered card in <" + ((CardTerminal)readers.get(usingReaderNr)).getName() + ">");
		if (debugLevel > 0)
			System.err.println("Card ATR is <"
					+ TextUtils.hexDump(card.getATR().getBytes()) + ">");
		myName = ((CardTerminal)readers.get(usingReaderNr)).getName();
	}

	
	private void selectApplet(byte[] AID_APDU) throws AIDNotFound, CardException {

		
		ResponseAPDU response = conn.transmit(new CommandAPDU(AID_APDU));
		
		if (response.getSW() != (Integer)0x9000)
			throw new AIDNotFound();
		
	}

	
	public void powerOff() throws CardException{
		//ctx.ReleaseContext();
		try {
			conn.close();
			card.disconnect(true);//boolean true will reset card: a select command is needed again after this
			card = null;
		} catch (CardException e) {
			if (debugLevel > 0){
				e.printStackTrace();
				System.err.println("Try to disconnect card form reader: " + ((CardTerminal)readers.get(usingReaderNr)).getName() + "\n Card already disconnected.");
			}
			card = null;
			throw new CardException("Card already disconnected.");
		}
	}

	public void finalize(){
		try {
			powerOff();
		} catch (CardException e) {
			e.printStackTrace();
			System.err.println("Try to disconnect card form reader: " + ((CardTerminal)readers.get(usingReaderNr)).getName() + "\n Card already disconnected.");
		}
	}

	private byte[] _ChangePINCommand = {
		// change pin vasco dp850 -- begin
		 (byte) 0xf1, // cla: reader commands
		 (byte) 0x95, // ins: local pin change
		 (byte) 0x70, //  p1: local pin change
		 (byte) 0xf1, //  p2: full mask is present
		 (byte) 0x18, //  p3: length of data sent to the smartcard reader
		 (byte) 0x46, // between 4 and 6 digits are allowed
		 (byte) 0x00, // cla: change pin command
		 (byte) 0x24, // ins: change pin command
		 (byte) 0x00, //  p1: change pin command
		 (byte) 0x01, //  p2: change pin command
		 (byte) 0x10, //  p3: change pin command, 16 bytes will follow
		 (byte) 0x03, // pin is in bcd format
		 (byte) 0x00, // no watchdog
		 (byte) 0x2f, // header of first pin block
		 (byte) 0xff, // pin block byte 2
		 (byte) 0xff, // pin block byte 3
		 (byte) 0xFF, // pin block byte 4
		 (byte) 0xFF, // pin block byte 5
		 (byte) 0xFF, // pin block byte 6
		 (byte) 0xFF, // pin block byte 7
		 (byte) 0xFF, // pin block byte 8
		 (byte) 0x2f, // header of second pin block
		 (byte) 0xff, // pin block byte 2
		 (byte) 0xff, // pin block byte 3
		 (byte) 0xFF, // pin block byte 4
		 (byte) 0xFF, // pin block byte 5
		 (byte) 0xFF, // pin block byte 6
		 (byte) 0xFF, // pin block byte 7
		 (byte) 0xFF, // pin block byte 8
	};

	private byte[] _VerifyPINCommand = {
		// verify pin vasco dp850 -- begin
		 (byte) 0xf1, // cla: verify pin for reader
		 (byte) 0x95, // ins: local verify pin
		 (byte) 0x30, //  p1: local pin entry
		 (byte) 0xf1, //  p2: full mask follows
		 (byte) 0x10, //  p3: 12 data bytes follow
		 (byte) 0x46, // between 4 and 6 digits are allowed
		 (byte) 0x00, // cla: verify pin for smartcard
		 (byte) 0x20, // ins: verify pin
		 (byte) 0x00, //  p1: verify pin
		 (byte) 0x01, //  p2: verify pin
		 (byte) 0x08, //  p3: 8 pin block bytes follow
		 (byte) 0x03, // pin is in bcd format
		 (byte) 0x00, // no watchdog
		 (byte) 0x24, // pin block byte 1
		 (byte) 0xFF, // pin block byte 2
		 (byte) 0xFF, // pin block byte 3
		 (byte) 0xFF, // pin block byte 4
		 (byte) 0xFF, // pin block byte 5
		 (byte) 0xFF, // pin block byte 6
		 (byte) 0xFF, // pin block byte 7
		 (byte) 0xFF, // pin block byte 8
	};

	private byte[] _PollingCommand = {
		// polling end of verify pin vasco dp850 -- end
		 (byte) 0xf1, (byte) 0x61, (byte) 0x00, (byte) 0x00, (byte) 0x04 };

	private byte[] _GetReaderResultCommand = {
		// result vasco dp850 reader command -- begin
		 (byte) 0xf1, (byte) 0x93, (byte) 0x30, (byte) 0x00, (byte) 0x02 };

	private byte[] _ResetReaderCommand =
		{(byte) 0xf1, (byte) 0x63, (byte) 0x00, (byte) 0x00, (byte) 0x04 };

	public final void verifyPIN() {
		byte[] result = new byte[2];
		System.err.println(
			"Secure PIN Entry -- Please enter your PIN on the Digipass 850 reader");
		boolean tryAgain = true;
		while (tryAgain)
			try {
				result = sendCommand(_VerifyPINCommand);

				byte[] pollResult = null;
				int timeoutCounter = 100;

				do {
					Thread.sleep(250);
					pollResult = sendCommand(_PollingCommand);
				} while (
					timeoutCounter-- > 0
						&& TextUtils.hexDump(pollResult).equals("3635B0009000"));
				// loop while we are in local mode or have timed out

				if (0 == timeoutCounter) {
					System.err.println(
						"Error -- The Digipass 850 reader did not respond to the PIN verify command within the predefined timeframe");
					sendCommand(_ResetReaderCommand);
					return;
				}

				if (!TextUtils.hexDump(pollResult).equals("363560009000")) {
					System.err.println(
						"Error -- The Digipass 850 reader did not respond to the PIN verify command within the predefined timeframe");
					sendCommand(_ResetReaderCommand);
					return;
				}
				result = sendCommand(this._GetReaderResultCommand);
				tryAgain = analyzePINVerifyResult(result);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
	}

	private boolean analyzePINVerifyResult(byte[] result) {
		boolean tryAgain = true;
		String caption = "Error Secure PIN verification";
		String text = "";
		//
		String resultString = TextUtils.hexDump(result, 0, 4);
		if (resultString.equals("63C29000")) {
			text = "You provided a WRONG PIN. You have 2 PIN tries left";
		} else if (resultString.equals("63C19000")) {
			text = "You provided a WRONG PIN. You have 1 PIN try left";
		} else if (resultString.equals("63C09000")) {
			text =
				"You provided a WRONG PIN. Your eID card has now been blocked.\n"
					+ "Call the eID Helpdesk (or go to your municipality) to get instructions on how to deblock your eID card.";
			tryAgain = false;
		} else if (resultString.equals("FFFF9000")) {
			text = "Secure PIN verification has been cancelled.";
			tryAgain = false;
		} else if (resultString.equals("90009000")) {
			text = "Secure PIN verification succeeded";
			caption = "Info";
			tryAgain = false;
		} else {
			text =
				"The eID card provided an unknown returncode ("
					+ resultString
					+ ") to the PIN verification command";
			tryAgain = false;
		}
		System.err.println(caption + " -- " + text);
		return tryAgain;
	}

	public final void changePIN() {
		byte[] result = new byte[2];
		System.err.println(
			"Secure PIN Change -- Please enter your current PIN on the Digipass 850 reader");
		boolean tryAgain = true;
		while (tryAgain)
			try {
				result = sendCommand(_ChangePINCommand);
System.err.println("result after change pin command <" +TextUtils.hexDump(result)+	">");
				byte[] pollResult = null;
				int timeoutCounter = 100;

				do {
					Thread.sleep(250);
					pollResult = sendCommand(_PollingCommand);
					System.err.println("polling result <" +TextUtils.hexDump(pollResult)+
						">");
				} while (
					timeoutCounter-- > 0
						&& TextUtils.hexDump(pollResult).equals("3635B0009000"));
				// loop while we are in local mode or have timed out

				System.err.println("result after polling <" +TextUtils.hexDump(result)+
					">");
				if (0 == timeoutCounter) {
					System.err.println(
						"Error -- The Digipass 850 reader did not respond to the PIN change command within the predefined timeframe");
result=					sendCommand(_ResetReaderCommand);
	System.err.println("result after reset reader, due to timeout counter = 0 <" +TextUtils.hexDump(result)+
">");
			return;
				}

				if (!TextUtils.hexDump(pollResult).equals("363560009000")) {
					System.err.println(
						"Error -- The Digipass 850 reader did not respond to the PIN change command within the predefined timeframe");
result=					sendCommand(_ResetReaderCommand);
	System.err.println("result after reset reader, due to timeout exceeded <" +TextUtils.hexDump(result)+
">");
			return;
				}
				result = sendCommand(this._GetReaderResultCommand);
				System.err.println("result after get reader result <" +TextUtils.hexDump(result)+
					">");
				tryAgain = analyzePINChangeResult(result);
				System.err.println("tryagain <" +tryAgain+
					">");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
	}

	private boolean analyzePINChangeResult(byte[] result) {
		boolean tryAgain = true;
		String caption = "Error in Secure PIN change";
		String text = "";
		//
		String resultString = TextUtils.hexDump(result, 0, 4);
		if (resultString.equals("63C29000")) {
			text = "You provided a WRONG PIN. You have 2 PIN tries left";
		} else if (resultString.equals("63C19000")) {
			text = "You provided a WRONG PIN. You have 1 PIN try left";
		} else if (resultString.equals("63C09000")) {
			text =
				"You provided a WRONG PIN. Your eID card has now been blocked.\n"
					+ "Call the eID Helpdesk (or go to your municipality) to get instructions on how to deblock your eID card.";
			tryAgain = false;
		} else if (resultString.equals("FFFF9000")) {
			text = "Secure PIN change has been cancelled.";
			tryAgain = false;
		} else if (resultString.equals("90009000")) {
			text = "Secure PIN change succeeded";
			caption = "Info";
			tryAgain = false;
		} else {
			text =
				"The eID card provided an unknown returncode ("
					+ resultString
					+ ") to the PIN change command";
			tryAgain = false;
		}
		System.err.println(caption + " -- " + text);
		return tryAgain;
	}

}
