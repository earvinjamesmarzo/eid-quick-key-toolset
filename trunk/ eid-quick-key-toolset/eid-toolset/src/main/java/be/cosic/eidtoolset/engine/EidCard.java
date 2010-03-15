package be.cosic.eidtoolset.engine;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import javax.smartcardio.CardException;


import be.cosic.eidtoolset.exceptions.AIDNotFound;
import be.cosic.eidtoolset.exceptions.GeneralSecurityException;
import be.cosic.eidtoolset.exceptions.InvalidPinException;
import be.cosic.eidtoolset.exceptions.InvalidResponse;
import be.cosic.eidtoolset.exceptions.NoCardConnected;
import be.cosic.eidtoolset.exceptions.NoReadersAvailable;
import be.cosic.eidtoolset.exceptions.NoSuchFeature;
import be.cosic.eidtoolset.exceptions.PinException;
import be.cosic.eidtoolset.exceptions.SignatureGenerationException;
import be.cosic.eidtoolset.exceptions.SmartCardReaderException;
import be.cosic.eidtoolset.exceptions.UnknownCardException;
import be.cosic.eidtoolset.gui.PinPad;
import be.cosic.eidtoolset.interfaces.BelpicCommandsEngine;
import be.cosic.eidtoolset.interfaces.EidCardInterface;
import be.cosic.util.MathUtils;
import be.cosic.util.TextUtils;
import be.cosic.util.TimeUtils;


@SuppressWarnings("restriction")
public class EidCard extends SmartCard implements EidCardInterface,
		BelpicCommandsEngine {
	
	

	
	
	public final static int ApduHeaderLength = 5;

	public final static int PinBlockLength = 8;
	
	private String signatureSessionText;

	public final static String authenticationSignatureSession = "Authentication session";

	public final static String nonRepudiationSignatureSession = "Non-Repudiation session";

	private String defaultPin = "";
	
	private final static int pinPadDelayBeforeReturning = 500;
	
	//TODO: probleem: belpiccommandsengine hier als interface;
	//maar eigenlijk gaat het niet noodzakelijk om een belpiccard: 
	//andere eid implementaties zouden momenteel ook alles commandos van de 
	//belpic moeten erven, inclusief aid!
	
	//--> de vraag is wat in belpic hoort en wat in nieuwe "eidcardcommandsEngine" hoort
	//--->alles van belpic (alle functies) kunnen in belpiccard.java overschreven worden (via super of volledig nieuw)
	//---> rest kan hier blijven en gebruik maken van nieuwe commandengine
	
	public static byte[] selectMasterFile = { (byte) 0x00, (byte) 0xa4,
			(byte) 0x02, (byte) 0x0C, (byte) 0x02, (byte) 0x3f, (byte) 0x00 };

	private int myType = EidCardInterface.GENERIC_EID_CARD;
	

	public EidCard(int type, String appName) {
		
		myType = type;
	}

	public int returnMyType() {
		return myType;
	}
	
	
	public byte[] selectFile(int fileid) throws UnknownCardException,
				NoSuchFeature, SmartCardReaderException, InvalidResponse, NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		//This method end up by calling selectByFileIdentifier in the eid java card applet
		//This method is called using an APDU with specific CLA, INS, P1 and P2 and has two bytes of data
		//This two bytes contain the file id. To call the master file, the two bytes for example are 0x3700
		byte[] selectFileOnIDCommand = selectFileCommand;
		selectFileOnIDCommand[5] = (byte)(fileid >> 8 & 0xff);
		selectFileOnIDCommand[6] = (byte)(fileid & 0xff);
		lookForSmartCard();
		return myReader.sendCommand(selectFileOnIDCommand);
	}


	//TODO: check if EidCard interface is synched with methods in here
	
	//TODO: check if these methods belong more in the GUI/parsing part or not: if not then permanent synch with byte arrays is needed! (read, write, get, set)
	/*
	private X509Certificate rootCACertificate = null;

	private X509Certificate caCertificate = null;

	private X509Certificate nonRepudiationCertificate = null;

	private X509Certificate authenticationCertificate = null;

	*/
	/*public RSAPublicKey getPublicAuthenticationKey() throws NoSuchFeature {
		RSAPublicKey publicKey = null;
		try {
			publicKey = (RSAPublicKey) getAuthenticationCertificate()
					.getPublicKey();
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuchFeature();
		}
		return publicKey;
	}

	public RSAPublicKey getPublicNonRepudiationKey() throws NoSuchFeature {
		RSAPublicKey publicKey = null;
		try {
			publicKey = (RSAPublicKey) getNonRepudiationCertificate()
					.getPublicKey();
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuchFeature();
		}
		return publicKey;
	}

	public X509Certificate getAuthenticationCertificate() throws NoSuchFeature {
		if (authenticationCertificate == null)
			try {
				lookForSmartCard();
				InputStream inStream = new ByteArrayInputStream(
						getAuthCertificateBytes());
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				authenticationCertificate = (X509Certificate) cf
						.generateCertificate(inStream);
				inStream.close();
			} catch (Exception e) {
				e.printStackTrace();
				throw new NoSuchFeature();
			}
		return authenticationCertificate;
	}

	public X509Certificate getCertificationAuthorityCertificate()
			throws NoSuchFeature {
		if (caCertificate == null)
			try {
				lookForSmartCard();
				InputStream inStream = new ByteArrayInputStream(
						readCACertificateBytes());
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				X509Certificate cert = (X509Certificate) cf
						.generateCertificate(inStream);
				inStream.close();

			} catch (Exception e) {
				e.printStackTrace();
				throw new NoSuchFeature();
			}
		return caCertificate;
	}

	public X509Certificate getRootCertificationAuthorityCertificate()
			throws NoSuchFeature {
		if (rootCACertificate == null)
			try {
				lookForSmartCard();
				InputStream inStream = new ByteArrayInputStream(this
						.readRootCACertificateBytes());
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				rootCACertificate = (X509Certificate) cf
						.generateCertificate(inStream);
				inStream.close();
			} catch (Exception e) {
				e.printStackTrace();
				throw new NoSuchFeature();
			}
		return rootCACertificate;
	}

	public X509Certificate getNonRepudiationCertificate() throws NoSuchFeature {
		if (nonRepudiationCertificate == null)
			try {
				lookForSmartCard();
				InputStream inStream = new ByteArrayInputStream(
						getNonRepCertificateBytes());
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				nonRepudiationCertificate = (X509Certificate) cf
						.generateCertificate(inStream);
				inStream.close();
			} catch (Exception e) {
				e.printStackTrace();
				throw new NoSuchFeature();
			}
		return nonRepudiationCertificate;
	}*/
	/*public X509Certificate getRootCACertificate() {
	return rootCACertificate;
	}
	
	public void setRootCACertificate(X509Certificate rootCACertificate) {
		this.rootCACertificate = rootCACertificate;
	}
	
	public X509Certificate getCaCertificate() {
		return caCertificate;
	}
	
	public void setCaCertificate(X509Certificate caCertificate) {
		this.caCertificate = caCertificate;
	}
	
	public void setNonRepudiationCertificate(
			X509Certificate nonRepudiationCertificate) {
		this.nonRepudiationCertificate = nonRepudiationCertificate;
	}
	
	public void setAuthenticationCertificate(
			X509Certificate authenticationCertificate) {
		this.authenticationCertificate = authenticationCertificate;
	}*/
	
	
	

	public byte[] readAuthCertificateBytes() throws NoSuchFeature {
			try {
				lookForSmartCard();
				authenticationCertificateBytes = readBinaryFile(selectAuthenticationCertificateCommand);

			} catch (Exception e) {
				e.printStackTrace();
				throw new NoSuchFeature();
			}
		return authenticationCertificateBytes;
	}

	public byte[] readNonRepCertificateBytes() throws NoSuchFeature {
			try {
				lookForSmartCard();
				nonRepudiationCertificateBytes = readBinaryFile(selectNonRepudiationCertificateCommand);

			} catch (Exception e) {
				e.printStackTrace();
				throw new NoSuchFeature();
			}
		return nonRepudiationCertificateBytes;
	}

	public byte[] readCACertificateBytes() throws NoSuchFeature {
		try {
				lookForSmartCard();
				caCertificateBytes = readBinaryFile(selectCaCertificateCommand);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuchFeature();
		}
		return caCertificateBytes;
	}

	public byte[] readIdentityFileSignatureBytes() throws NoSuchFeature {
		try {
				lookForSmartCard();
				identityFileSignatureBytes = readBinaryFile(selectIdentityFileSignatureCommand);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuchFeature();
		}
		return identityFileSignatureBytes;
	}

	public byte[] readRRNCertificateBytes() throws NoSuchFeature {
		try {
				lookForSmartCard();
				rrnCertificateBytes = readBinaryFile(selectRrnCertificateCommand);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuchFeature();
		}
		return rrnCertificateBytes;
	}

	public byte[] readRootCACertificateBytes() throws NoSuchFeature {
		try{	
			lookForSmartCard();
			rootCACertificateBytes = readBinaryFile(selectRootCaCertificateCommand);
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuchFeature();
		}
		return rootCACertificateBytes;
	}

	public byte[] readCitizenIdentityDataBytes() throws NoSuchFeature {
		try {
			lookForSmartCard();
			citizenIdentityFileBytes = readBinaryFile(selectCitizenIdentityDataCommand);
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuchFeature();
		}
		return citizenIdentityFileBytes;
	}

	public byte[] readCitizenAddressBytes() throws NoSuchFeature {
		try{
			lookForSmartCard();
			citizenAddressBytes = readBinaryFile(selectCitizenAddressDataCommand);
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuchFeature();
		}
		return citizenAddressBytes;
	}

	public byte[] readCitizenPhotoBytes() throws NoSuchFeature {
		try{
			lookForSmartCard();
			citizenPhoto = readBinaryFile(selectCitizenPhotoCommand);
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuchFeature();
		}
		return citizenPhoto;
	}
	
	public byte[] readAddressFileSignatureBytes() throws NoSuchFeature {
		try {
			
				lookForSmartCard();
				addressFileSignatureBytes = readBinaryFile(selectAddressFileSignatureCommand);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuchFeature();
		}
		return addressFileSignatureBytes;
	}

	
	
	
	
	public byte[] generateSignature(byte[] preparationCommand,
			byte[] signatureGenerationCommand, byte[] datahash)
			throws NoSuchFeature, InvalidResponse, NoReadersAvailable,
			SignatureGenerationException, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		lookForSmartCard();
		SmartCardResponse scr = null;
		try {
			myReader.sendCommand(preparationCommand);
			pinValidationEngine();
			byte[] apdu = new byte[signatureGenerationCommand.length];
			for (int i = 0; i < signatureGenerationCommand.length; i++)
				apdu[i] = signatureGenerationCommand[i];
			for (int i = 0; i < MathUtils.min(20, datahash.length); i++)
				apdu[i + 5] = datahash[i];
			scr = new SmartCardResponse(myReader.sendCommand(apdu));
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (scr.getCommandStatus().equals("9000"))
			return scr.getData();
		if (scr.getCommandStatus().equals("6180")) {
			scr = new SmartCardResponse(retrieveSignatureBytes());
			if (scr.getCommandStatus().equals("9000"))
				return scr.getData();
		}
		throw new SignatureGenerationException();
	}

	
	
	public byte[] retrieveSignatureBytes() throws NoSuchFeature,
								NoReadersAvailable, InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, AIDNotFound {
		lookForSmartCard();
		return myReader.sendCommand(retrieveSignatureCommand);
	}
	
	public byte[] generateAuthenticationSignature(byte[] datahash)
			throws NoSuchFeature, NoReadersAvailable, InvalidResponse,
			SignatureGenerationException, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		signatureSessionText = authenticationSignatureSession;
		return generateSignature(prepareForAuthenticationSignatureCommand,
				generateSignatureCommand, datahash);
	}

	public byte[] generateNonRepudiationSignature(byte[] datahash)
			throws NoSuchFeature, NoReadersAvailable, InvalidResponse,
			SignatureGenerationException, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		signatureSessionText =nonRepudiationSignatureSession;
		return generateSignature(prepareForNonRepudiationSignatureCommand,
				generateSignatureCommand, datahash);
	}

	private byte[] citizenPhoto = null;

	private byte[] citizenAddressBytes = null;

	private byte[] rootCACertificateBytes = null;

	private byte[] citizenIdentityFileBytes = null;

	private byte[] authenticationCertificateBytes = null;

	private byte[] nonRepudiationCertificateBytes = null;

	private byte[] caCertificateBytes = null;

	private byte[] rrnCertificateBytes = null;

	
	public byte[] getCitizenPhoto() {
		return citizenPhoto;
	}

	public byte[] getCitizenAddressBytes() {
		return citizenAddressBytes;
	}

	public byte[] getRootCACertificateBytes() {
		return rootCACertificateBytes;
	}

	public byte[] getCitizenIdentityFileBytes() {
		return citizenIdentityFileBytes;
	}

	public byte[] getAuthenticationCertificateBytes() {
		return authenticationCertificateBytes;
	}

	public byte[] getNonRepudiationCertificateBytes() {
		return nonRepudiationCertificateBytes;
	}

	public byte[] getCaCertificateBytes() {
		return caCertificateBytes;
	}

	public byte[] getRrnCertificateBytes() {
		return rrnCertificateBytes;
	}

	public byte[] getAddressFileSignatureBytes() {
		return addressFileSignatureBytes;
	}

	
	
	
	public void setRootCACertificateBytes(byte[] rootCACertificateBytes) {
		this.rootCACertificateBytes = rootCACertificateBytes;
	}

	public void setAuthenticationCertificateBytes(
			byte[] authenticationCertificateBytes) {
		this.authenticationCertificateBytes = authenticationCertificateBytes;
	}

	public void setNonRepudiationCertificateBytes(
			byte[] nonRepudiationCertificateBytes) {
		this.nonRepudiationCertificateBytes = nonRepudiationCertificateBytes;
	}

	public void setCaCertificateBytes(byte[] caCertificateBytes) {
		this.caCertificateBytes = caCertificateBytes;
	}

	public void setRrnCertificateBytes(byte[] rrnCertificateBytes) {
		this.rrnCertificateBytes = rrnCertificateBytes;
	}

	public void setCitizenPhoto(byte[] citizenPhoto) {
		this.citizenPhoto = citizenPhoto;
	}

	public void setCitizenIdentityFileBytes(byte[] citizenIdentityFileBytes) {
		this.citizenIdentityFileBytes = citizenIdentityFileBytes;
	}

	public void setCitizenAddressBytes(byte[] citizenAddressBytes) {
		this.citizenAddressBytes = citizenAddressBytes;
	}

	
	
	public void setAddressFileSignatureBytes(byte[] addressFileSignatureBytes) {
		this.addressFileSignatureBytes = addressFileSignatureBytes;
	}

	
	

	private byte[] identityFileSignatureBytes = null;


	private byte[] addressFileSignatureBytes = null;

	public void clearCache() {
		citizenPhoto = null;
		citizenAddressBytes = null;
		rootCACertificateBytes = null;
		citizenIdentityFileBytes = null;
		authenticationCertificateBytes = null;
		nonRepudiationCertificateBytes = null;
		caCertificateBytes = null;
		rrnCertificateBytes = null;
		identityFileSignatureBytes = null;
		addressFileSignatureBytes = null;
		
		
//		authenticationCertificate = null;
//		rootCACertificate = null;
//		caCertificate = null;
//		nonRepudiationCertificate = null;
	}

	/**
	 * This method should be called whenever a new reader connection has to be started up
	 * or when the current connection broke down for unknown reasons.
	 */
	public void closeReader(){
		if (smartCardReaderShouldBeInitialized == false) {//If already initialized (and thus selected): not necessary to do so again
			try {
				myReader.powerOff();
			} catch (CardException e) {
				myReader = null;
			}
			myReader = null;
			smartCardReaderShouldBeInitialized = true;
			
		}
	}
	

	public void setPin(String pinValue) {
		defaultPin = pinValue;
	}

	public void pinValidationEngine() throws InvalidPinException,
			InvalidResponse, PinException, NoSuchFeature, NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		pinValidationEngine(defaultPin, "");
	}

	private byte[] verifyThisPin(String pinvalue) throws InvalidResponse, NoCardConnected, CardException {
		return myReader.sendCommand(insertPinIntoApdu(verifyPinApdu, pinvalue));
	}

	private boolean checkThisPin(String pinvalue, PinPad pinPad,
			int nofMilliSecondsBeforeReturning) throws InvalidResponse, NoCardConnected, CardException {
		boolean keepTrying = true;
		byte[] result = verifyThisPin(pinvalue);
		if (TextUtils.hexDump(result).equals("9000")) {
			pinPad.setStatusText("OK...");
			TimeUtils.sleep(nofMilliSecondsBeforeReturning);
			keepTrying = false;
		} else {
			if (TextUtils.hexDump(result).equals("63C2")) {
				pinPad.setStatusText("Invalid PIN, 2 tries left...");
				TimeUtils.sleep(nofMilliSecondsBeforeReturning);
			} else if (TextUtils.hexDump(result).equals("63C1")) {
				pinPad.setStatusText("Invalid PIN, 1 try left...");
				TimeUtils.sleep(nofMilliSecondsBeforeReturning);
			} else {
				if (TextUtils.hexDump(result).equals("63C0")) {
					pinPad.setStatusText("Invalid PIN, Card blocked...");
					TimeUtils.sleep(nofMilliSecondsBeforeReturning);
					keepTrying = false;
				}
			}
		}
		return keepTrying;
	}

	

	private void pinValidationEngine(String pinvalue, String pintext)
			throws InvalidPinException, PinException, InvalidResponse,
			NoSuchFeature, NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		lookForSmartCard();
		Frame window = null;
		PinPad pinPad = null;
		if (myReader.getMyName().toUpperCase().indexOf(
				VascoSmartCardReader.referenceName.toUpperCase()) >= 0) {
			myReader.verifyPIN();
			return;
		} else if (pinvalue != "")
			checkThisPin(pinvalue, pinPad, pinPadDelayBeforeReturning);
		else {
			try {
				pinPad = new PinPad(signatureSessionText,
						EidCardInterface.minNofPinDigits,
						EidCardInterface.maxNofPinDigits, pintext);
				window = new Frame();
				window.setTitle("Pseudo-secure PinPad");
				window.setLayout(new FlowLayout());
				window.setFont(new Font("Helvetica", Font.PLAIN, 12));
				window.setBackground(Color.white);
				window.add(pinPad);
				window.pack();
				window.setVisible(true);
				window.show();
				do {
					while (pinPad.stillBusy())
						TimeUtils.sleep(100);
					if (pinPad.cancelPressed())
						throw new PinException("User cancelled PIN entry");
					else
						pinvalue = pinPad.getPinValue();
					pinPad.setStatusText("Processing Verify PIN command...");
				} while (checkThisPin(pinvalue, pinPad,
						pinPadDelayBeforeReturning));
			} catch (PinException e) {
				e.printStackTrace();
				throw e;
			} finally {
				if (window != null) {
					window.setVisible(false);
					window.dispose();
					window = null;
				}
			}
		}
	}

	public void changeThisPin(String currentpinvalue, String newpinvalue,
			PinPad pinPad, int nofMilliSecondsBeforeReturning)
			throws InvalidResponse, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, NoCardConnected {
		
		
		byte[] result = myReader.sendCommand(insertTwoPinsIntoApdu(
				changePinApdu, currentpinvalue, newpinvalue));
		if (TextUtils.hexDump(result).equals("9000")) {
			pinPad.setStatusText("OK...");
			TimeUtils.sleep(nofMilliSecondsBeforeReturning);
		} else {
			if (TextUtils.hexDump(result).equals("63C2")) {
				pinPad.setStatusText("Invalid PIN, 2 tries left...");
				TimeUtils.sleep(nofMilliSecondsBeforeReturning);
			} else if (TextUtils.hexDump(result).equals("63C1")) {
				pinPad.setStatusText("Invalid PIN, 1 try left...");
				TimeUtils.sleep(nofMilliSecondsBeforeReturning);
			} else {
				if (TextUtils.hexDump(result).equals("63C0")) {
					pinPad.setStatusText("Invalid PIN, Card blocked...");
					TimeUtils.sleep(nofMilliSecondsBeforeReturning);
				}
			}
		}
	}

	public void changePin() throws InvalidPinException, PinException,
			InvalidResponse, NoSuchFeature, NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		lookForSmartCard();
		if (myReader.getMyName().toUpperCase().indexOf(
				VascoSmartCardReader.referenceName.toUpperCase()) >= 0) {
			myReader.changePIN();
			return;
		} else {
			Frame window = null;
			String currentPin = "";
			String newPin = "";
			PinPad pinPad = null;
			if (myReader.getMyName().toUpperCase().indexOf(
					VascoSmartCardReader.referenceName.toUpperCase()) >= 0) {
				myReader.verifyPIN();
				return;
			} else {
				try {
					pinPad = new PinPad(signatureSessionText,EidCardInterface.minNofPinDigits,
							EidCardInterface.maxNofPinDigits, "current");
					window = new Frame();
					window.setTitle("Pseudo-secure PinPad");
					window.setLayout(new FlowLayout());
					window.setFont(new Font("Helvetica", Font.PLAIN, 12));
					window.setBackground(Color.white);
					window.add(pinPad);
					window.pack();
					window.setVisible(true);
					window.show();
					while (pinPad.stillBusy())
						TimeUtils.sleep(100);
					if (pinPad.cancelPressed())
						throw new PinException("User cancelled PIN entry");
					else
						currentPin = pinPad.getPinValue();
					window.remove(pinPad);
					pinPad = new PinPad(signatureSessionText,EidCardInterface.minNofPinDigits,
							EidCardInterface.maxNofPinDigits, "new");
					window.add(pinPad);
					window.pack();
					window.setVisible(true);
					window.show();
					while (pinPad.stillBusy())
						TimeUtils.sleep(100);
					if (pinPad.cancelPressed())
						throw new PinException("User cancelled PIN entry");
					else
						newPin = pinPad.getPinValue();
					pinPad.setStatusText("Processing Change PIN command...");
					changeThisPin(currentPin, newPin, pinPad,
							5 * pinPadDelayBeforeReturning);
				} catch (PinException e) {
					e.printStackTrace();
					throw e;
				} finally {
					if (window != null) {
						window.setVisible(false);
						window.dispose();
						window = null;
					}
				}
			}
		}
	}

	
	private byte[] insertPinIntoApdu(byte[] apdu, String pinvalue) {
		return insertPinIntoApdu(apdu, ApduHeaderLength, pinvalue);
	}

	private byte[] insertTwoPinsIntoApdu(byte[] apdu, String pin1, String pin2) {
		byte[] tmpApdu = insertPinIntoApdu(apdu, ApduHeaderLength, pin1);
		return insertPinIntoApdu(tmpApdu, ApduHeaderLength + PinBlockLength,
				pin2);
	}

	private byte[] insertPinIntoApdu(byte[] apdu, int pinBlockOffset,
			String pinvalue) {
		String pinValue = "";
		if (2 * ((int) (pinvalue.length() / 2)) == pinvalue.length())
			pinValue = pinvalue;
		else
			pinValue = pinvalue + "F";
		byte[] newApdu = new byte[apdu.length];
		for (int i = 0; i < newApdu.length; i++)
			newApdu[i] = apdu[i];
		int offsetInCommand = pinBlockOffset;
		newApdu[offsetInCommand++] = (byte) (2 * 16 + pinValue.length());
		for (int i = 0; i < pinValue.length(); i += 2) {
			newApdu[offsetInCommand++] = (byte) (Integer.parseInt(pinValue
					.substring(i, i + 2), 16));
		}
		return newApdu;
	}

	public void reActivate(String puk1, String puk2)
			throws InvalidPinException, InvalidResponse, NoSuchFeature,
			NoReadersAvailable, NoCardConnected, CardException, NoSuchAlgorithmException, AIDNotFound {
		lookForSmartCard();
		String pukValue = puk2 + puk1;
		byte[] unblockCardCommand = insertPinIntoApdu(unblockCardApdu, pukValue);
		
		byte[] result = myReader.sendCommand(unblockCardCommand);
	}

	

	
	public byte[] writeCACertificateBytes(byte[] caCertificate) throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		lookForSmartCard();
		return writeBinaryFile(selectCaCertificateCommand, caCertificate);
	}
	
	public byte[] writeIdentityFileSignatureBytes(byte[] identityFileSignature) throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		lookForSmartCard();
		return writeBinaryFile(selectIdentityFileSignatureCommand, identityFileSignature);
	}

	public byte[] writeAddressFileSignatureBytes(byte[] addressFileSignature) throws NoSuchFeature, InvalidResponse, NoCardConnected, CardException, GeneralSecurityException, NoSuchAlgorithmException, NoReadersAvailable, AIDNotFound {
		lookForSmartCard();
		return writeBinaryFile(selectAddressFileSignatureCommand, addressFileSignature);
	}

	public byte[] writeRRNCertificateBytes(byte[] rrnCertificate) throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		lookForSmartCard();
		return writeBinaryFile(selectRrnCertificateCommand, rrnCertificate);
	}
	
	public byte[] writeRootCACertificateBytes(byte[] rootCACertificate) throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		lookForSmartCard();
		return writeBinaryFile(selectRootCaCertificateCommand, rootCACertificate);
	}

	public byte[] writeCitizenIdentityDataBytes(byte[] citizenIdentityFile) throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		lookForSmartCard();
		return writeBinaryFile(selectCitizenIdentityDataCommand, citizenIdentityFile);
	}

	public byte[] writeCitizenAddressBytes(byte[] citizenAddress) throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		lookForSmartCard();
		return writeBinaryFile(selectCitizenAddressDataCommand, citizenAddress);
	}

	public byte[] writeCitizenPhotoBytes(byte[] citizenPhoto) throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		lookForSmartCard();
		
		//TODO: hier eerst de hash in ID field veranderen
		
		return writeBinaryFile(selectCitizenPhotoCommand, citizenPhoto);
	}
	
	
	
	
	
	public String nameOfActiveReader() throws NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound {
		lookForSmartCard();
		return myReader.getMyName();
	}

	public byte[] fetchATR() throws NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound {
		lookForSmartCard();
		return myReader.getATR();
	}

	public SmartCardReader getSmartCardReader() throws NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound {
		lookForSmartCard();
		return myReader;
	}

	private void lookForSmartCard() throws NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound {
		if (smartCardReaderShouldBeInitialized)
			lookForSmartCard(defaultPreferredSmartCardReader,
					defaultTimeoutInMilliSecondsBeforeTryingAnotherReader);
	}

	private boolean smartCardReaderShouldBeInitialized = true;

	
	private void lookForSmartCard(String preferredReader,
			int milliSecondsBeforeTryingAnotherReader)
			throws NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound {
		if (smartCardReaderShouldBeInitialized) {//If already initialized (and thus selected): not necessary to do so again
			myReader = new SmartCardReader();
			myReader.lookForSmartCard(preferredReader,
					milliSecondsBeforeTryingAnotherReader, selectAID_APDU);
			smartCardReaderShouldBeInitialized = false;
		}
	}
}