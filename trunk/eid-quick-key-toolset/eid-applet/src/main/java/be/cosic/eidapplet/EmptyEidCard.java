package be.cosic.eidapplet;


import javacard.framework.*;
import javacard.security.*;

public class EmptyEidCard extends EidCard {

	// these are identical for all eid card applet so share these between subclasses
	static byte[] dirData, tokenInfoData, odfData, aodfData, prkdfData, cdfData;
	static byte[] citizenCaCert, rrnCert, rootCaCert;
	// save some more memory by making the photo static as well
	static byte[] photoData;

	/**
	 * called by the JCRE to create an applet instance
	 */
	public static void install(byte[] bArray, short bOffset, byte bLength) {

		// create a sample eID card applet instance
		new EmptyEidCard();

	}

	/**
	 * private constructor - called by the install method to instantiate a 
	 * SampleEidCard instance
	 * 
	 * needs to be protected so that it can be invoked by subclasses
	 */
	protected EmptyEidCard() {

		super();

		// initialize PINs to fixed value
		initializePins();

		// initialize file system
		initializeFileSystem();
		// initialize place holders for large files (certificates + photo)
		initializeEmptyLargeFiles();

		// initialize basic keys pair
		initializeKeyPairs();

	}

	/**
	 * initialize all the PINs
	 * 
	 * PINs are set to the same values as the sample eID card
	 */
	private void initializePins() {

		/*
		 * initialize cardholder PIN (hardcoded to fixed value) 
		 * 
		 * PIN header is "24" (length of PIN = 4)
		 * PIN itself is "1234" (4 digits)
		 * fill rest of PIN data with F
		 */
		byte[] cardhold =
			{
				(byte)0x24,
				(byte)0x12,
				(byte)0x34,
				(byte)0xFF,
				(byte)0xFF,
				(byte)0xFF,
				(byte)0xFF,
				(byte)0xFF };
		cardholderPin = new OwnerPIN(CARDHOLDER_PIN_TRY_LIMIT, PIN_SIZE);
		cardholderPin.update(cardhold, (short)0, PIN_SIZE);

		/*
		 * initialize unblock PUK (hardcoded to fixed value) 
		 * 
		 * PUK header is "2c" (length of PUK = 12)
		 * PUK itself consists of 2 parts
		 * 		PUK2 is "222222" (6 digits)
		 * 		PUK1 is "111111" (6 digits)
		 * so in total the PUK is "222222111111" (12 digits)
		 * fill last bye of PUK data with "FF"
		 */
		byte[] unblock =
			{
				(byte)0x2c,
				(byte)0x22,
				(byte)0x22,
				(byte)0x22,
				(byte)0x11,
				(byte)0x11,
				(byte)0x11,
				(byte)0xFF };
		unblockPin = new OwnerPIN(UNBLOCK_PIN_TRY_LIMIT, PIN_SIZE);
		unblockPin.update(unblock, (short)0, PIN_SIZE);

		/*
		 * activation PIN is same as PUK
		 */
		activationPin = new OwnerPIN(ACTIVATE_PIN_TRY_LIMIT, PIN_SIZE);
		activationPin.update(unblock, (short)0, PIN_SIZE);

		// TODO: is this the correct value?
		/*
		 * initialize reset PIN (hardcoded to fixed value)
		 *
		 * PUK header is "2c" (length of PUK = 12)
		 * PIN itself consists of 2 parts
		 * 		PUK3 is "333333" (6 digits)
		 * 		PUK1 is "111111" (6 digits)
		 * so in total the PIN is "333333111111" (12 digits)
		 * fill last bye of PIN data with "FF"
		 */
		byte[] reset =
			{
				(byte)0x2c,
				(byte)0x33,
				(byte)0x33,
				(byte)0x33,
				(byte)0x11,
				(byte)0x11,
				(byte)0x11,
				(byte)0xFF };
		resetPin = new OwnerPIN(RESET_PIN_TRY_LIMIT, PIN_SIZE);
		resetPin.update(reset, (short)0, PIN_SIZE);

	}

	/**
	 * initialize all files on the card as empty with max size
	 * 
	 * see "Belgian Electronic Identity Card content" (version 2.2)
	 * 
	 * TODO: check all max lengths and fix if necessary: check when key for example rsa 2048: both public key and signature double in length!
	 * 
	 * e.g. //TODO: depending on the edi card version, the address is of different length (current: 117)
	 */
	private void initializeFileSystem() {

		masterFile = new MasterFile();

		/*
		 * initialize PKCS#15 data structures
		 * see "5. PKCS#15 information details" for more info
		 */

		/*if (EmptyEidCard.dirData == null)
			EmptyEidCard.dirData = new byte[] {
			 Belpic (One entry per application) 
			// [APPLICATION 1] IMPLICIT SEQUENCE
			 (byte)0x61, (byte)0x23,
			 Application ID 
			// [APPLICATION 15] IMPLICIT OCTET STRING
			 (byte)0x4F, (byte)0x0C,
			// AID = RID || "PKCS-15"
			(byte)0xA0,
				(byte)0x00,
				(byte)0x00,
				(byte)0x01,
				(byte)0x77,
				(byte)0x50,
				(byte)0x4B,
				(byte)0x43,
				(byte)0x53,
				(byte)0x2D,
				(byte)0x31,
				(byte)0x35,
			 Label 
			// [APPLICATION 16] IMPLICIT UTF8 STRING
			 (byte)0x50, (byte)0x06,
			// "BELPIC"
			(byte)0x42,
				(byte)0x45,
				(byte)0x4C,
				(byte)0x50,
				(byte)0x49,
				(byte)0x43,
			 Path 
			// [APPLICATION 17] IMPLICIT OCTET STRING
			 (byte)0x51, (byte)0x04,
			// MF/Belpic
			 (byte)0x3F, (byte)0x00, (byte)0xDF, (byte)0x00,
			 Discretionary Data Object 
			// [APPLICATION 19] IMPLICIT SEQUENCE
			 (byte)0x73, (byte)0x05,
			 Object ID 
			// oject identifier
			 (byte)0x06, (byte)0x03,
			// belgian citizen (2.16.56.2)
			 (byte)0x60, (byte)0x38, (byte)0x02 };*/
		dirFile = new ElementaryFile(EF_DIR, masterFile, (short)0x25);

		belpicDirectory = new DedicatedFile(DF_BELPIC, masterFile);

		/*if (EmptyEidCard.tokenInfoData == null)
			EmptyEidCard.tokenInfoData = new byte[] {
			// SEQUENCE
			 (byte)0x30, (byte)0x27,
			 Version 
			// INTEGER
			 (byte)0x02, (byte)0x01,
			// "0"
			 (byte)0x00,
			 Serial Number 
			// OCTET STRING
			 (byte)0x04, (byte)0x10,
			// chip serial number (16 bytes)
			(byte)0x53,
				(byte)0x4C,
				(byte)0x49,
				(byte)0x4E,
				(byte)0x33,
				(byte)0x66,
				(byte)0x00,
				(byte)0x29,
				(byte)0x6C,
				(byte)0xFF,
				(byte)0x23,
				(byte)0x2C,
				(byte)0xF6,
				(byte)0x12,
				(byte)0x12,
				(byte)0x26,
			 Application Label 
			 [0] Label 
			// IMPLICIT UTF8 String
			 (byte)0x80, (byte)0x06,
			// "BELPIC"
			(byte)0x42,
				(byte)0x45,
				(byte)0x4C,
				(byte)0x50,
				(byte)0x49,
				(byte)0x43,
			 Token Flags 
			// BIT STRING
			 (byte)0x03, (byte)0x02,
			// prnGeneration(2), eidCompliant (3)
			 (byte)0x04, (byte)0x30,
			 [30] BELPIC Application 
			// IMPLICIT INTEGER
			 (byte)0x9E, (byte)0x04,
			// version (4 bytes)
			// also see "Description of the Belpic EID-version numbering"
			 (byte)0x01, (byte)0x01, (byte)0x00, (byte)0x00 };*/
		tokenInfo =
			new ElementaryFile(
				TOKENINFO,
				belpicDirectory,
				(short)0x30);

		/*if (EmptyEidCard.odfData == null)
			EmptyEidCard.odfData = new byte[] {
			
			 * [0] Private Keys 
			 
			 (byte)0xA0, (byte)0x0A,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x08,
			// OCTECT STRING
			 (byte)0x04, (byte)0x06,
			// MF/Belpic/PrKDF
			(byte)0x3F,
				(byte)0x00,
				(byte)0xDF,
				(byte)0x00,
				(byte)0x50,
				(byte)0x35,
			
			 * [4] Certificates 
			 
			 (byte)0xA4, (byte)0x0A,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x08,
			// OCTECT STRING
			 (byte)0x04, (byte)0x06,
			// MF/Belpic/CDF
			(byte)0x3F,
				(byte)0x00,
				(byte)0xDF,
				(byte)0x00,
				(byte)0x50,
				(byte)0x37,
			 
			 * [8] Authentication Objects
			 
			 (byte)0xA8, (byte)0x0A,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x08,
			// OCTECT STRING
			 (byte)0x04, (byte)0x06,
			// MF/Belpic/AODF
			(byte)0x3F,
				(byte)0x00,
				(byte)0xDF,
				(byte)0x00,
				(byte)0x50,
				(byte)0x34 };*/
		objectDirectoryFile =
			new ElementaryFile(ODF, belpicDirectory, (short)40);

		/*if (EmptyEidCard.aodfData == null)
			EmptyEidCard.aodfData = new byte[] {
			 
			 * PIN Cardholder (One entry per PIN)
			 
			// SEQUENCE
			 (byte)0x30, (byte)0x33,
			 Common Object Attributes 
			// SEQUENCE
			 (byte)0x30, (byte)0x0F,
			 Label 
			// UTF8 STRING
			 (byte)0x0C, (byte)0x09,
			// "Basic PIN"
			(byte)0x42,
				(byte)0x61,
				(byte)0x73,
				(byte)0x69,
				(byte)0x63,
				(byte)0x20,
				(byte)0x50,
				(byte)0x49,
				(byte)0x4E,
			 Common Object Flags 
			// BIT STRING
			 (byte)0x03, (byte)0x02,
			// private(0), modifiable (1)
			 (byte)0x06, (byte)0xC0,
			 Common Authentication Object Attributes 
			// SEQUENCE
			 (byte)0x30, (byte)0x03,
			 Authority ID 
			// OCTECT STRING
			 (byte)0x04, (byte)0x01,
			// "01"
			 (byte)0x01,
			 [1] Pin Attributes 
			 (byte)0xA1, (byte)0x1B,
			// SEQUENCE
			 (byte)0x30, (byte)0x19,
			 Pin Flags 
			// BIT STRING
			 (byte)0x03, (byte)0x02,
			// initialized(4), needs-padding(5)
			 (byte)0x02, (byte)0x0C,
			 Pin Type 
			// ENUMERATED
			 (byte)0x0A, (byte)0x01,
			// bcd(0)
			 (byte)0x00,
			 Min Length 
			// INTEGER
			 (byte)0x02, (byte)0x01,
			// "4"
			 (byte)0x04,
			 Stored Length 
			// INTEGER
			 (byte)0x02, (byte)0x01,
			// "8"
			 (byte)0x08,
			 [0] Pin Reference 
			// IMPLICIT INTEGER
			 (byte)0x80, (byte)0x01,
			// "1"
			 (byte)0x01,
			 Pad Char 
			// OCTET STRING
			 (byte)0x04, (byte)0x01,
			// "FF"
			 (byte)0xFF,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x04,
			// OCTET STRING
			 (byte)0x04, (byte)0x02,
			// MF
			 (byte)0x3F, (byte)0x00 };*/
		authenticationObjectDirectoryFile =
			new ElementaryFile(AODF, belpicDirectory, (short)0x40);

		/*if (EmptyEidCard.prkdfData == null)
			EmptyEidCard.prkdfData = new byte[] {
			
			 *  Private Authentication Key 
			 
			// SEQUENCE
			 (byte)0x30, (byte)0x3A,
			 Common Object Attributes 
			// SEQUENCE
			 (byte)0x30, (byte)0x17,
			 Label 
			// UTF8 STRING
			 (byte)0x0C, (byte)0x0E,
			// "Authentication"
			(byte)0x41,
				(byte)0x75,
				(byte)0x74,
				(byte)0x68,
				(byte)0x65,
				(byte)0x6E,
				(byte)0x74,
				(byte)0x69,
				(byte)0x63,
				(byte)0x61,
				(byte)0x74,
				(byte)0x69,
				(byte)0x6F,
				(byte)0x6E,
			 Common Object Flags 
			// BIT STRING
			 (byte)0x03, (byte)0x02,
			// private(0), modifiable(1)
			 (byte)0x06, (byte)0xC0,
			 Authority ID 
			// OCTET STRING
			 (byte)0x04, (byte)0x01,
			// "01"
			 (byte)0x01,
			 Common Key Attributes 
			// SEQUENCE
			 (byte)0x30, (byte)0x0F,
			 Identifier 
			// OCTET STRING
			 (byte)0x04, (byte)0x01,
			// "02"
			 (byte)0x02,
			 Key Usage Flags 
			// BIT STRING
			 (byte)0x03, (byte)0x02,
			// Sign(2)
			 (byte)0x05, (byte)0x20,
			 Key Access Flags 
			// BIT STRING
			 (byte)0x03, (byte)0x02,
			// sensitive(0), always sensitive(2), never extractable(3), local(4)
			 (byte)0x03, (byte)0xB8,
			 Key Reference 
			// INTEGER
			 (byte)0x02, (byte)0x02,
			// "82"
			 (byte)0x00, (byte)0x82,
			 [1] Private RSA Key Attributes 
			 (byte)0xA1, (byte)0x0E,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x0C,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x06,
			// OCTET STRING
			 (byte)0x04, (byte)0x04,
			// MF/Belpic
			 (byte)0x3F, (byte)0x00, (byte)0xDF, (byte)0x00,
			 Modulus Length 
			 (byte)0x02, (byte)0x02,
			// "1024"
			 (byte)0x04, (byte)0x00,
			 
			 * Private Non-repudiation Key
			 
			// SEQUENCE
			 (byte)0x30, (byte)0x39,
			 Common Object Attributes 
			// SEQUENCE
			 (byte)0x30, (byte)0x15,
			 Label 
			// UTF8 STRING
			 (byte)0x0C, (byte)0x09,
			// "Signature"
			(byte)0x53,
				(byte)0x69,
				(byte)0x67,
				(byte)0x6E,
				(byte)0x61,
				(byte)0x74,
				(byte)0x75,
				(byte)0x72,
				(byte)0x65,
			 Common Object Flags 
			// BIT STRING
			 (byte)0x03, (byte)0x02,
			// private(0), modifiable(1)
			 (byte)0x06, (byte)0xC0,
			 Authority ID 
			// OCTET STRING
			 (byte)0x04, (byte)0x01,
			// "01"
			 (byte)0x01,
			 User Consent 
			// INTEGER
			 (byte)0x02, (byte)0x01,
			// "1"
			 (byte)0x01,
			 Common Key Attributes 
			// SEQUENCE
			 (byte)0x30, (byte)0x10,
			 Identifier 
			// OCTET STRING
			 (byte)0x04, (byte)0x01,
			// "03"
			 (byte)0x03,
			 Key Usage Flags 
			// BIT STRING
			 (byte)0x03, (byte)0x03,
			// NonRepudiation(9)
			 (byte)0x06, (byte)0x00, (byte)0x40,
			 Key Access Flags 
			// BIT STRING
			 (byte)0x03, (byte)0x02,
			// sensitive(0), always sensitive(2), never extractable(3), local(4)
			 (byte)0x03, (byte)0xB8,
			 Key Reference 
			// INTEGER
			 (byte)0x02, (byte)0x02,
			// "83"
			 (byte)0x00, (byte)0x83,
			 [1] Private RSA Key Attributes 
			 (byte)0xA1, (byte)0x0E,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x0C,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x06,
			// OCTET STRING
			 (byte)0x04, (byte)0x04,
			// MF/Belpic
			 (byte)0x3F, (byte)0x00, (byte)0xDF, (byte)0x00,
			 Modulus Length 
			// INTEGER
			 (byte)0x02, (byte)0x02,
			// "1024"
			 (byte)0x04, (byte)0x00 };*/
		privateKeyDirectoryFile =
			new ElementaryFile(PRKDF, belpicDirectory, (short)0xB0);

		/*if (EmptyEidCard.cdfData == null)
			EmptyEidCard.cdfData = new byte[] {
			
			 * Authentation Certificate
			 
			// SEQUENCE
			 (byte)0x30, (byte)0x2C,
			 Common Object Attributes 
			// SEQUENCE
			 (byte)0x30, (byte)0x17,
			 Label 
			// UTF8 STRING
			 (byte)0x0C, (byte)0x0E,
			// "Authentication"
			(byte)0x41,
				(byte)0x75,
				(byte)0x74,
				(byte)0x68,
				(byte)0x65,
				(byte)0x6E,
				(byte)0x74,
				(byte)0x69,
				(byte)0x63,
				(byte)0x61,
				(byte)0x74,
				(byte)0x69,
				(byte)0x6F,
				(byte)0x6E,
			 Common Object Flags 
			// BIT STRING
			 (byte)0x03, (byte)0x02,
			// private(0), modifiable(1)
			 (byte)0x06, (byte)0xC0,
			 Authority ID 
			// OCTET STRING
			 (byte)0x04, (byte)0x01,
			// "01"
			 (byte)0x01,
			 Common Certificate Attributes 
			// SEQUENCE
			 (byte)0x30, (byte)0x06,
			 Identifier 
			// OCTET STRING
			 (byte)0x04, (byte)0x01,
			// "02"
			 (byte)0x02,
			 [3] Implicit Trust 
			// IMPLICIT BOOLEAN
			 (byte)0x83, (byte)0x01,
			// "false"
			 (byte)0x00,
			 [1] X509CertificateAttributes 
			 (byte)0xA1, (byte)0x0C,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x0A,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x08,
			 Path 
			// OCTET STRING
			 (byte)0x04, (byte)0x06,
			// MF/Belpic/Cert#2(auth)
			(byte)0x3F,
				(byte)0x00,
				(byte)0xDF,
				(byte)0x00,
				(byte)0x50,
				(byte)0x38,
			 
			 * Non-Repudiation Certificate
			 
			// SEQUENCE
			 (byte)0x30, (byte)0x27,
			 Common Object Attributes 
			// SEQUENCE
			 (byte)0x30, (byte)0x12,
			 Label 
			// UTF8 STRING
			 (byte)0x0C, (byte)0x09,
			// "Signature"
			(byte)0x53,
				(byte)0x69,
				(byte)0x67,
				(byte)0x6E,
				(byte)0x61,
				(byte)0x74,
				(byte)0x75,
				(byte)0x72,
				(byte)0x65,//64
			 Common Object Flags 
			// BIT STRING
			 (byte)0x03, (byte)0x02,
			// private(0), modifiable(1)
			 (byte)0x06, (byte)0xC0,
			 Authority ID 
			// OCTET STRING
			 (byte)0x04, (byte)0x01,
			// "01"
			 (byte)0x01,
			 Common Certificate Attributes 
			// SEQUENCE
			 (byte)0x30, (byte)0x06,
			 Identifier 
			// OCTET STRING
			 (byte)0x04, (byte)0x01,
			// "03"
			 (byte)0x03,
			 [3] Implicit Trust 
			// IMPLICIT BOOLEAN
			 (byte)0x83, (byte)0x01,
			// "false"
			 (byte)0x00,
			 [1] X509CertificateAttributes 
			 (byte)0xA1, (byte)0x0C,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x0A,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x08,
			 Path 
			// OCTET STRING
			 (byte)0x04, (byte)0x06,
			// MF/Belpic/Cert#3(non-rep)
			(byte)0x3F,
				(byte)0x00,
				(byte)0xDF,
				(byte)0x00,
				(byte)0x50,
				(byte)0x39,
			 
			 * Certification Authority Certificate
			 
			// SEQUENCE
			 (byte)0x30, (byte)0x23,
			 Common Object Attributes 
			// SEQUENCE
			 (byte)0x30, (byte)0x0B,
			 Label 
			// UTF8 STRING
			 (byte)0x0C, (byte)0x02,
			// "CA"
			 (byte)0x43, (byte)0x41,//101
			 Common Object Flags 
			// BIT STRING
			 (byte)0x03, (byte)0x02,
			// private(0), modifiable(1)
			 (byte)0x06, (byte)0xC0,
			 Authority ID 
			// OCTET STRING
			 (byte)0x04, (byte)0x01,
			// "01"
			 (byte)0x01,
			 Common Certificate Attributes 
			// SEQUENCE
			 (byte)0x30, (byte)0x09,
			 Identifier 
			// OCTET STRING
			 (byte)0x04, (byte)0x01,
			// "04"
			 (byte)0x04,
			 Authority 
			// BOOLEAN
			 (byte)0x01, (byte)0x01,
			// "true"
			 (byte)0xFF,
			 [3] Implicit Trust 
			// IMPLICIT BOOLEAN
			 (byte)0x83, (byte)0x01,
			// "false"
			 (byte)0x00,
			 [1] X509CertificateAttributes 
			 (byte)0xA1, (byte)0x0C,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x0A,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x08,
			 Path 
			// OCTET STRING
			 (byte)0x04, (byte)0x06,
			// MF/Belpic/Cert#4(CA)
			(byte)0x3F,
				(byte)0x00,
				(byte)0xDF,
				(byte)0x00,
				(byte)0x50,
				(byte)0x3A,
			 
			 * Root Certificate
			 
			// SEQUENCE
			 (byte)0x30, (byte)0x25,
			 Common Object Attributes 
			// SEQUENCE
			 (byte)0x30, (byte)0x0D,
			 Label 
			// UTF8 STRING
			 (byte)0x0C, (byte)0x04,
			// "Root"
			 (byte)0x52, (byte)0x6F, (byte)0x6F, (byte)0x74,
			 Common Object Flags 
			// BIT STRING
			 (byte)0x03, (byte)0x02,
			// private(0), modifiable(1)
			 (byte)0x06, (byte)0xC0,
			 Authority ID 
			// OCTET STRING
			 (byte)0x04, (byte)0x01,
			// "01"
			 (byte)0x01,
			 Common Certificate Attributes 
			// SEQUENCE
			 (byte)0x30, (byte)0x09,
			 Identifier 
			// OCTET STRING
			 (byte)0x04, (byte)0x01,
			// "06"
			 (byte)0x06,
			 Authority 
			// BOOLEAN
			 (byte)0x01, (byte)0x01,
			// "true"
			 (byte)0xFF,
			 [3] Implicit Trust 
			// IMPLICIT BOOLEAN
			 (byte)0x83, (byte)0x01,//160
			// "false"
			 (byte)0x00,
			 [1] X509CertificateAttributes 
			 (byte)0xA1, (byte)0x0C,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x0A,
			 Path 
			// SEQUENCE
			 (byte)0x30, (byte)0x08,
			 Path 
			// OCTET STRING
			 (byte)0x04, (byte)0x06,
			// MF/Belpic/Cert#6(Root)
			(byte)0x3F,
				(byte)0x00,
				(byte)0xDF,
				(byte)0x00,
				(byte)0x50,
				(byte)0x3B };*/
		certificateDirectoryFile =
			new ElementaryFile(CDF, belpicDirectory, (short)0xB0);

		idDirectory = new DedicatedFile(DF_ID, masterFile);

		/*
		 * initialize all citizen data stored on the eID card
		 * copied from sample eID card 000-0000861-85
		 */

		// initialize ID#RN EF
		/*byte[] idData = {
			 Card Number 
			 (byte)0x01, (byte)0x0C,
			// "000-0000861-85"
			(byte)0x30,
				(byte)0x30,
				(byte)0x30,
				(byte)0x30,
				(byte)0x30,
				(byte)0x30,
				(byte)0x30,
				(byte)0x38,
				(byte)0x36,
				(byte)0x31,
				(byte)0x38,
				(byte)0x35,
			 Chip Number 
			 (byte)0x02, (byte)0x10,
			// chip number (16 bytes)
			(byte)0x53,
				(byte)0x4C,
				(byte)0x49,
				(byte)0x4E,
				(byte)0x33,
				(byte)0x66,
				(byte)0x00,
				(byte)0x29,
				(byte)0x6C,
				(byte)0xFF,
				(byte)0x23,
				(byte)0x2C,
				(byte)0xF6,
				(byte)0x12,
				(byte)0x12,
				(byte)0x26,
			 Card validity data begin 
			 (byte)0x03, (byte)0x0A,
			// "18.06.2004"
			(byte)0x31,
				(byte)0x38,
				(byte)0x2E,
				(byte)0x30,
				(byte)0x36,
				(byte)0x2E,
				(byte)0x32,
				(byte)0x30,
				(byte)0x30,
				(byte)0x34,
			 Card validity data end 
			 (byte)0x04, (byte)0x0A,
			// "18.06.2009"
			(byte)0x31,
				(byte)0x38,
				(byte)0x2E,
				(byte)0x30,
				(byte)0x36,
				(byte)0x2E,
				(byte)0x32,
				(byte)0x30,
				(byte)0x30,
				(byte)0x39,
			 Card delivery municipality 
			 (byte)0x05, (byte)0x12,
			// "Certipost Specimen" 
			(byte)0x43,
				(byte)0x65,
				(byte)0x72,
				(byte)0x74,
				(byte)0x69,
				(byte)0x70,
				(byte)0x6F,
				(byte)0x73,
				(byte)0x74,
				(byte)0x20,
				(byte)0x53,
				(byte)0x70,
				(byte)0x65,
				(byte)0x63,
				(byte)0x69,
				(byte)0x6D,
				(byte)0x65,
				(byte)0x6E,
			 Natianol Number 
			 (byte)0x06, (byte)0x0B,
			// "71.71.51-000.70"
			(byte)0x37,
				(byte)0x31,
				(byte)0x37,
				(byte)0x31,
				(byte)0x35,
				(byte)0x31,
				(byte)0x30,
				(byte)0x30,
				(byte)0x30,
				(byte)0x37,
				(byte)0x30,
			 Name 
			 (byte)0x07, (byte)0x08,
			// "SPECIMEN"
			(byte)0x53,
				(byte)0x50,
				(byte)0x45,
				(byte)0x43,
				(byte)0x49,
				(byte)0x4D,
				(byte)0x45,
				(byte)0x4E,
			 2 first first names 
			 (byte)0x08, (byte)0x0B,
			// "Alice A0861"
			(byte)0x41,
				(byte)0x6C,
				(byte)0x69,
				(byte)0x63,
				(byte)0x65,
				(byte)0x20,
				(byte)0x41,
				(byte)0x30,
				(byte)0x38,
				(byte)0x36,
				(byte)0x31,
			 First letter of third first name 
			 (byte)0x09, (byte)0x01,
			// "A"
			 (byte)0x41,
			 Nationality 
			 (byte)0x0A, (byte)0x04,
			// "Belg"
			 (byte)0x42, (byte)0x65, (byte)0x6C, (byte)0x67,
			 Birth Location 
			 (byte)0x0B, (byte)0x0C,
			// "Hamont-Achel"
			(byte)0x48,
				(byte)0x61,
				(byte)0x6D,
				(byte)0x6F,
				(byte)0x6E,
				(byte)0x74,
				(byte)0x2D,
				(byte)0x41,
				(byte)0x63,
				(byte)0x68,
				(byte)0x65,
				(byte)0x6C,
			 Birth Date 
			 (byte)0x0C, (byte)0x0B,
			// "01 JAN 1971"
			(byte)0x30,
				(byte)0x31,
				(byte)0x20,
				(byte)0x4A,
				(byte)0x41,
				(byte)0x4E,
				(byte)0x20,
				(byte)0x31,
				(byte)0x39,
				(byte)0x37,
				(byte)0x31,
			 Sex 
			 (byte)0x0D, (byte)0x01,
			// "V"
			 (byte)0x56,
			 Noble condition 
			 (byte)0x0E, (byte)0x00,
			 Document type 
			 (byte)0x0F, (byte)0x01,
			// "1" (Belgian citizen)
			 (byte)0x31,
			 Special status 
			 (byte)0x10, (byte)0x01,
			// "0" (no status)
			 (byte)0x30,
			 Hash photo 
			 (byte)0x11, (byte)0x14,
			// SHA-1 hash (20 bytes)
			(byte)0x75,
				(byte)0x3B,
				(byte)0x10,
				(byte)0xBD,
				(byte)0x13,
				(byte)0x12,
				(byte)0x41,
				(byte)0xB6,
				(byte)0x0F,
				(byte)0x55,
				(byte)0xED,
				(byte)0xFF,
				(byte)0x1F,
				(byte)0x48,
				(byte)0x35,
				(byte)0x38,
				(byte)0x9D,
				(byte)0x80,
				(byte)0x49,
				(byte)0xD5 };*/
		identityFile = new ElementaryFile(IDENTITY, idDirectory, (short)0xD0);

		// initialize SGN#RN EF 
		/*byte[] idSignData =
			{
				(byte)0x4F,
				(byte)0xC8,
				(byte)0xB1,
				(byte)0x00,
				(byte)0x2A,
				(byte)0x8E,
				(byte)0x21,
				(byte)0xA1,
				(byte)0x72,
				(byte)0x84,
				(byte)0x2A,
				(byte)0x15,
				(byte)0xB4,
				(byte)0x58,
				(byte)0x78,
				(byte)0x6F,
				(byte)0x75,
				(byte)0x4F,
				(byte)0xF9,
				(byte)0x49,
				(byte)0x15,
				(byte)0x53,
				(byte)0x0C,
				(byte)0x60,
				(byte)0xED,
				(byte)0x50,
				(byte)0x84,
				(byte)0xEB,
				(byte)0x0D,
				(byte)0x70,
				(byte)0x0E,
				(byte)0xD1,
				(byte)0x4C,
				(byte)0xCC,
				(byte)0xDE,
				(byte)0x0A,
				(byte)0x37,
				(byte)0x0D,
				(byte)0x97,
				(byte)0x9E,
				(byte)0xF4,
				(byte)0x7C,
				(byte)0x23,
				(byte)0xA9,
				(byte)0x24,
				(byte)0x4B,
				(byte)0x37,
				(byte)0x79,
				(byte)0x38,
				(byte)0xA9,
				(byte)0x51,
				(byte)0x92,
				(byte)0x8F,
				(byte)0x53,
				(byte)0xA1,
				(byte)0x1C,
				(byte)0x63,
				(byte)0x6A,
				(byte)0x33,
				(byte)0x0C,
				(byte)0xAC,
				(byte)0x72,
				(byte)0xC1,
				(byte)0x6C,
				(byte)0x4E,
				(byte)0x75,
				(byte)0x9E,
				(byte)0x35,
				(byte)0x70,
				(byte)0x97,
				(byte)0xE1,
				(byte)0xEB,
				(byte)0x0B,
				(byte)0xB8,
				(byte)0x78,
				(byte)0xA9,
				(byte)0xA9,
				(byte)0x43,
				(byte)0x78,
				(byte)0x72,
				(byte)0xC6,
				(byte)0xFB,
				(byte)0x3C,
				(byte)0x0F,
				(byte)0xFF,
				(byte)0x1B,
				(byte)0xB6,
				(byte)0x49,
				(byte)0x1F,
				(byte)0xA6,
				(byte)0x1B,
				(byte)0xCB,
				(byte)0x46,
				(byte)0x87,
				(byte)0x91,
				(byte)0xEF,
				(byte)0x8C,
				(byte)0x68,
				(byte)0x3C,
				(byte)0x51,
				(byte)0x57,
				(byte)0x11,
				(byte)0x48,
				(byte)0x36,
				(byte)0x36,
				(byte)0x1F,
				(byte)0xE3,
				(byte)0x92,
				(byte)0xC2,
				(byte)0xFC,
				(byte)0xB3,
				(byte)0x28,
				(byte)0xE8,
				(byte)0x87,
				(byte)0x3F,
				(byte)0x0C,
				(byte)0x36,
				(byte)0xD4,
				(byte)0x6A,
				(byte)0x52,
				(byte)0x9F,
				(byte)0x26,
				(byte)0x66,
				(byte)0xC0,
				(byte)0x0F,
				(byte)0x54,
				(byte)0x0D,
				(byte)0x53 };*/
		identityFileSignature =
			new ElementaryFile(SGN_IDENTITY, idDirectory, (short)0x80);

		// initialize ID#Address EF 
		/*byte[] address = {
			 Street + number 
			 (byte)0x01, (byte)0x12,
			// "Meirplaats 1 bus 1"
			(byte)0x4D,
				(byte)0x65,
				(byte)0x69,
				(byte)0x72,
				(byte)0x70,
				(byte)0x6C,
				(byte)0x61,
				(byte)0x61,
				(byte)0x74,
				(byte)0x73,
				(byte)0x20,
				(byte)0x31,
				(byte)0x20,
				(byte)0x62,
				(byte)0x75,
				(byte)0x73,
				(byte)0x20,
				(byte)0x31,
			 ZIP code 
			 (byte)0x02, (byte)0x04,
			// "2000"
			 (byte)0x32, (byte)0x30, (byte)0x30, (byte)0x30,
			 Municipality 
			 (byte)0x03, (byte)0x09,
			// "Antwerpen"
			(byte)0x41,
				(byte)0x6E,
				(byte)0x74,
				(byte)0x77,
				(byte)0x65,
				(byte)0x72,
				(byte)0x70,
				(byte)0x65,
				(byte)0x6E };
		// address is 117 bytes, and should be padded with zeros
		*/
		
		/*byte[] addressData = new byte[113];
		Util.arrayCopy(
			address,
			(short)0,
			addressData,
			(short)0,
			(short)address.length);*/
		addressFile = new ElementaryFile(ADDRESS, idDirectory, (short)117);

		// initialize SGN#Address EF 
		/*byte[] addressSignData =
			{
				(byte)0x59,
				(byte)0xDC,
				(byte)0xD9,
				(byte)0x8F,
				(byte)0x7F,
				(byte)0x48,
				(byte)0x81,
				(byte)0x15,
				(byte)0x65,
				(byte)0xBB,
				(byte)0xDC,
				(byte)0x17,
				(byte)0x57,
				(byte)0x63,
				(byte)0xC4,
				(byte)0x3A,
				(byte)0x8C,
				(byte)0x91,
				(byte)0x31,
				(byte)0xBA,
				(byte)0x39,
				(byte)0x7A,
				(byte)0xB3,
				(byte)0x23,
				(byte)0xBC,
				(byte)0x87,
				(byte)0xE9,
				(byte)0xE4,
				(byte)0xCD,
				(byte)0xA7,
				(byte)0x5B,
				(byte)0x36,
				(byte)0x85,
				(byte)0x86,
				(byte)0xC4,
				(byte)0x14,
				(byte)0x18,
				(byte)0xE5,
				(byte)0x32,
				(byte)0xED,
				(byte)0x2C,
				(byte)0x78,
				(byte)0xF1,
				(byte)0x62,
				(byte)0xCF,
				(byte)0xC0,
				(byte)0x78,
				(byte)0xE0,
				(byte)0xCC,
				(byte)0xD5,
				(byte)0xC0,
				(byte)0xFE,
				(byte)0xFC,
				(byte)0x3D,
				(byte)0xB7,
				(byte)0x1D,
				(byte)0x71,
				(byte)0x6D,
				(byte)0x8B,
				(byte)0xB0,
				(byte)0x85,
				(byte)0x3F,
				(byte)0x0E,
				(byte)0xFD,
				(byte)0x44,
				(byte)0x30,
				(byte)0xA3,
				(byte)0x4B,
				(byte)0x5D,
				(byte)0x08,
				(byte)0x9A,
				(byte)0x40,
				(byte)0x4C,
				(byte)0x99,
				(byte)0xC4,
				(byte)0x66,
				(byte)0x37,
				(byte)0xE8,
				(byte)0x72,
				(byte)0x52,
				(byte)0x34,
				(byte)0xCB,
				(byte)0x7D,
				(byte)0xE6,
				(byte)0xFD,
				(byte)0x38,
				(byte)0x0B,
				(byte)0x4E,
				(byte)0xEE,
				(byte)0x1F,
				(byte)0x91,
				(byte)0x51,
				(byte)0xCC,
				(byte)0x9A,
				(byte)0x94,
				(byte)0x45,
				(byte)0x29,
				(byte)0x27,
				(byte)0x63,
				(byte)0x4E,
				(byte)0x20,
				(byte)0x6D,
				(byte)0x2C,
				(byte)0x4E,
				(byte)0xBD,
				(byte)0x11,
				(byte)0xFB,
				(byte)0x5C,
				(byte)0xDA,
				(byte)0xE9,
				(byte)0x8B,
				(byte)0xED,
				(byte)0x05,
				(byte)0xCE,
				(byte)0x69,
				(byte)0xAC,
				(byte)0x6F,
				(byte)0xCC,
				(byte)0x7B,
				(byte)0xE4,
				(byte)0xD0,
				(byte)0x87,
				(byte)0xEA,
				(byte)0x0B,
				(byte)0x09,
				(byte)0xAA,
				(byte)0x75,
				(byte)0xB1 };*/
		addressFileSignature =
			new ElementaryFile(SGN_ADDRESS, idDirectory, (short)128);

		// initialize PuK#7 ID (CA Role ID) EF 
		/*byte[] caRoleIdData =
			{
				(byte)0x8F,
				(byte)0x14,
				(byte)0x65,
				(byte)0x80,
				(byte)0x2B,
				(byte)0xBA,
				(byte)0x01,
				(byte)0xE4,
				(byte)0xD1,
				(byte)0x37,
				(byte)0x4B,
				(byte)0x91,
				(byte)0x0E,
				(byte)0x4A,
				(byte)0xEB,
				(byte)0x71,
				(byte)0xCB,
				(byte)0x9E,
				(byte)0x91,
				(byte)0x97 };*/
		caRoleIDFile =
			new ElementaryFile(CA_ROLE_ID, idDirectory, (short)0x20);

		// initialize Preferences EF to 100 zero bytes 
		//byte[] prefData = new byte[100];
		preferencesFile =
			new ElementaryFile(PREFERENCES, idDirectory, (short)100);

	}

	/**
	 * initialize empty files that need to be filled latter using UPDATE BINARY
	 */
	private void initializeEmptyLargeFiles() {

		/*
		 * these 3 certificates are the same for all sample eid card applets
		 * therefor they are made static and the data is allocated only once
		 */
		/*if (EmptyEidCard.citizenCaCert == null)
			EmptyEidCard.citizenCaCert = new byte[1017];*/
		caCertificate =
			new ElementaryFile(
				CA_CERTIFICATE,
				belpicDirectory,
				(short)1200);

		/*if (EmptyEidCard.rrnCert == null)
			EmptyEidCard.rrnCert = new byte[968];*/
		rrnCertificate =
			new ElementaryFile(
				RRN_CERTIFICATE,
				belpicDirectory,
				(short)1200);

		/*if (EmptyEidCard.rootCaCert == null)
			EmptyEidCard.rootCaCert = new byte[952];*/
		rootCaCertificate =
			new ElementaryFile(
				ROOT_CA_CERTIFICATE,
				belpicDirectory,
				(short)1200);

		/*
		 * to save some memory we only support 1 photo for all subclasses
		 * ideally this should be applet specific and have max size 3584 (3.5K)
		 */
		/*if (EmptyEidCard.photoData == null)
			EmptyEidCard.photoData = new byte[2887];*/
		photoFile =
			new ElementaryFile(PHOTO, idDirectory, (short)3584);

		/*
		 * certificate #2 and #3 are applet specific
		 * allocate enough memory
		 */
		authenticationCertificate =
			new ElementaryFile(AUTH_CERTIFICATE, belpicDirectory, (short)1200);
		nonRepudiationCertificate =
			new ElementaryFile(
				NONREP_CERTIFICATE,
				belpicDirectory,
				(short)1200);

	}

	/**
	 * initialize basic key pair
	 */
	private void initializeKeyPairs() {
		
		/*
		 * basicKeyPair is static (so same for all applets)
		 * so only allocate memory once
		 */
		if (EidCard.basicKeyPair != null)
			return;

		/*
		 * stuff generated by openssl
		 * 
		 * $ openssl genrsa -out key 1024
		 * $ openssl rsa -in key -text -noout
		 */
		
		
		basicKeyPair = new KeyPair(KeyPair.ALG_RSA_CRT, (short)1024);
		basicKeyPair.genKeyPair();
		
		authPrivateKey =
			(RSAPrivateCrtKey)KeyBuilder.buildKey(
				KeyBuilder.TYPE_RSA_CRT_PRIVATE,
				KeyBuilder.LENGTH_RSA_1024,
				false);
		nonRepPrivateKey =
			(RSAPrivateCrtKey)KeyBuilder.buildKey(
					KeyBuilder.TYPE_RSA_CRT_PRIVATE,
					KeyBuilder.LENGTH_RSA_1024,
					false);
		

		/*byte[] P =
			{
				(byte)0xdd,
				(byte)0x15,
				(byte)0xa9,
				(byte)0xe5,
				(byte)0x4b,
				(byte)0x6f,
				(byte)0xb7,
				(byte)0x41,
				(byte)0xe6,
				(byte)0xda,
				(byte)0x99,
				(byte)0xc2,
				(byte)0x41,
				(byte)0x98,
				(byte)0xae,
				(byte)0x14,
				(byte)0x71,
				(byte)0xa3,
				(byte)0xca,
				(byte)0xf7,
				(byte)0x8a,
				(byte)0x38,
				(byte)0xd1,
				(byte)0x7e,
				(byte)0x9b,
				(byte)0xbf,
				(byte)0x2c,
				(byte)0x1a,
				(byte)0x3b,
				(byte)0x3c,
				(byte)0xde,
				(byte)0xd2,
				(byte)0xee,
				(byte)0x21,
				(byte)0x01,
				(byte)0x22,
				(byte)0x46,
				(byte)0x97,
				(byte)0x57,
				(byte)0xf2,
				(byte)0xc7,
				(byte)0xcc,
				(byte)0xef,
				(byte)0xd0,
				(byte)0x24,
				(byte)0xa9,
				(byte)0x29,
				(byte)0xc0,
				(byte)0x11,
				(byte)0xc6,
				(byte)0x59,
				(byte)0xf6,
				(byte)0x3f,
				(byte)0x18,
				(byte)0xd3,
				(byte)0x05,
				(byte)0x84,
				(byte)0x48,
				(byte)0xb3,
				(byte)0x9d,
				(byte)0x8b,
				(byte)0x96,
				(byte)0xdd,
				(byte)0xc7 };
		privateKey.setP(P, (short)0, (short)64);

		byte[] Q =
			{
				(byte)0xc9,
				(byte)0x3f,
				(byte)0x7a,
				(byte)0xf1,
				(byte)0x9a,
				(byte)0x77,
				(byte)0x51,
				(byte)0x31,
				(byte)0x49,
				(byte)0xb6,
				(byte)0x75,
				(byte)0x2a,
				(byte)0x9f,
				(byte)0x20,
				(byte)0x43,
				(byte)0xe5,
				(byte)0x5e,
				(byte)0x78,
				(byte)0x72,
				(byte)0x8b,
				(byte)0x44,
				(byte)0x77,
				(byte)0xd7,
				(byte)0x4f,
				(byte)0x66,
				(byte)0x88,
				(byte)0x7a,
				(byte)0x0a,
				(byte)0x71,
				(byte)0x2b,
				(byte)0x28,
				(byte)0x50,
				(byte)0x7b,
				(byte)0x42,
				(byte)0x69,
				(byte)0x41,
				(byte)0xa3,
				(byte)0x11,
				(byte)0xb7,
				(byte)0xfe,
				(byte)0x8c,
				(byte)0x9e,
				(byte)0x44,
				(byte)0x09,
				(byte)0xa9,
				(byte)0x74,
				(byte)0x85,
				(byte)0xad,
				(byte)0x30,
				(byte)0x64,
				(byte)0xd3,
				(byte)0xbe,
				(byte)0x76,
				(byte)0x7a,
				(byte)0xb7,
				(byte)0x97,
				(byte)0x01,
				(byte)0xbf,
				(byte)0xf1,
				(byte)0x37,
				(byte)0x09,
				(byte)0x14,
				(byte)0xcb,
				(byte)0x7b };
		privateKey.setQ(Q, (short)0, (short)64);

		byte[] DP1 =
			{
				(byte)0x48,
				(byte)0xf9,
				(byte)0x5d,
				(byte)0x9a,
				(byte)0xd1,
				(byte)0xcb,
				(byte)0x8e,
				(byte)0x31,
				(byte)0xb2,
				(byte)0x81,
				(byte)0x75,
				(byte)0x3f,
				(byte)0x29,
				(byte)0x67,
				(byte)0xbc,
				(byte)0x0e,
				(byte)0x03,
				(byte)0x74,
				(byte)0x8d,
				(byte)0x0a,
				(byte)0x28,
				(byte)0x15,
				(byte)0x99,
				(byte)0x10,
				(byte)0xb1,
				(byte)0x57,
				(byte)0xe8,
				(byte)0xb6,
				(byte)0xbf,
				(byte)0xd6,
				(byte)0xd7,
				(byte)0xb5,
				(byte)0xc7,
				(byte)0xe4,
				(byte)0x1c,
				(byte)0xfb,
				(byte)0xb3,
				(byte)0x51,
				(byte)0x41,
				(byte)0x36,
				(byte)0x61,
				(byte)0xbc,
				(byte)0xc3,
				(byte)0x6b,
				(byte)0x70,
				(byte)0xae,
				(byte)0x65,
				(byte)0x99,
				(byte)0x80,
				(byte)0x44,
				(byte)0x78,
				(byte)0x6d,
				(byte)0x4f,
				(byte)0x66,
				(byte)0x62,
				(byte)0x40,
				(byte)0xef,
				(byte)0xe9,
				(byte)0x0f,
				(byte)0x60,
				(byte)0x71,
				(byte)0x32,
				(byte)0xdb,
				(byte)0x01 };
		privateKey.setDP1(DP1, (short)0, (short)64);

		byte[] DQ1 =
			{
				(byte)0xa3,
				(byte)0x52,
				(byte)0xfc,
				(byte)0x71,
				(byte)0x05,
				(byte)0x7e,
				(byte)0x1e,
				(byte)0x0b,
				(byte)0x95,
				(byte)0x1a,
				(byte)0x19,
				(byte)0x9e,
				(byte)0x9c,
				(byte)0x83,
				(byte)0xaf,
				(byte)0xf6,
				(byte)0x7f,
				(byte)0x23,
				(byte)0xdb,
				(byte)0x3a,
				(byte)0x01,
				(byte)0x38,
				(byte)0x0d,
				(byte)0x2a,
				(byte)0x28,
				(byte)0x39,
				(byte)0x4c,
				(byte)0x6a,
				(byte)0x1b,
				(byte)0x0b,
				(byte)0xfe,
				(byte)0x6c,
				(byte)0xca,
				(byte)0x8b,
				(byte)0xcc,
				(byte)0x26,
				(byte)0x73,
				(byte)0xb4,
				(byte)0x16,
				(byte)0x91,
				(byte)0xe4,
				(byte)0x07,
				(byte)0x31,
				(byte)0x8a,
				(byte)0x71,
				(byte)0xd6,
				(byte)0xda,
				(byte)0x02,
				(byte)0x03,
				(byte)0x0b,
				(byte)0x60,
				(byte)0xf8,
				(byte)0xea,
				(byte)0xe8,
				(byte)0x8f,
				(byte)0x04,
				(byte)0x63,
				(byte)0x6c,
				(byte)0x25,
				(byte)0xd4,
				(byte)0x17,
				(byte)0x6f,
				(byte)0xa3,
				(byte)0xef };
		privateKey.setDQ1(DQ1, (short)0, (short)64);

		byte[] PQ =
			{
				(byte)0xa0,
				(byte)0x5c,
				(byte)0xeb,
				(byte)0x21,
				(byte)0x5b,
				(byte)0x78,
				(byte)0x94,
				(byte)0xc9,
				(byte)0x54,
				(byte)0x5d,
				(byte)0x51,
				(byte)0xb7,
				(byte)0x5f,
				(byte)0x46,
				(byte)0x63,
				(byte)0x0b,
				(byte)0xad,
				(byte)0xd4,
				(byte)0x24,
				(byte)0xea,
				(byte)0x1f,
				(byte)0x0c,
				(byte)0xdc,
				(byte)0x05,
				(byte)0xa1,
				(byte)0xb2,
				(byte)0xbb,
				(byte)0x6a,
				(byte)0x71,
				(byte)0x78,
				(byte)0x37,
				(byte)0xc8,
				(byte)0x73,
				(byte)0xfc,
				(byte)0xb5,
				(byte)0x3d,
				(byte)0x4e,
				(byte)0x38,
				(byte)0xae,
				(byte)0x38,
				(byte)0x66,
				(byte)0x9c,
				(byte)0xca,
				(byte)0xf8,
				(byte)0x21,
				(byte)0xfc,
				(byte)0x6f,
				(byte)0x51,
				(byte)0xba,
				(byte)0x7c,
				(byte)0x52,
				(byte)0x4c,
				(byte)0x72,
				(byte)0x32,
				(byte)0x1b,
				(byte)0x44,
				(byte)0x39,
				(byte)0x3a,
				(byte)0xcd,
				(byte)0xf5,
				(byte)0x96,
				(byte)0xa1,
				(byte)0x19,
				(byte)0x39 };
		privateKey.setPQ(PQ, (short)0, (short)64);

		RSAPublicKey publicKey =
			(RSAPublicKey)KeyBuilder.buildKey(
				KeyBuilder.TYPE_RSA_PUBLIC,
				KeyBuilder.LENGTH_RSA_1024,
				false);

		// exponent is 65537=2^16+1
		byte[] exponent = {(byte)0x01, (byte)0x00, (byte)0x01 };
		publicKey.setExponent(exponent, (short)0, (short)3);

		byte[] modulus =
			{
				(byte)0xad,
				(byte)0xcc,
				(byte)0xd4,
				(byte)0xe6,
				(byte)0xd1,
				(byte)0x6a,
				(byte)0x91,
				(byte)0x94,
				(byte)0x6d,
				(byte)0x81,
				(byte)0x76,
				(byte)0x8a,
				(byte)0x5c,
				(byte)0x66,
				(byte)0x8f,
				(byte)0xcc,
				(byte)0x01,
				(byte)0xe8,
				(byte)0xd8,
				(byte)0x60,
				(byte)0x39,
				(byte)0xb9,
				(byte)0xc9,
				(byte)0x9a,
				(byte)0x2c,
				(byte)0x61,
				(byte)0xf7,
				(byte)0x39,
				(byte)0x7c,
				(byte)0x07,
				(byte)0x43,
				(byte)0xd1,
				(byte)0x2e,
				(byte)0xf9,
				(byte)0xbb,
				(byte)0xe4,
				(byte)0x78,
				(byte)0x58,
				(byte)0xc9,
				(byte)0xdc,
				(byte)0x57,
				(byte)0xec,
				(byte)0x21,
				(byte)0xca,
				(byte)0x70,
				(byte)0x4f,
				(byte)0x58,
				(byte)0x0e,
				(byte)0x58,
				(byte)0xae,
				(byte)0x94,
				(byte)0x9c,
				(byte)0x12,
				(byte)0x12,
				(byte)0xec,
				(byte)0x15,
				(byte)0xe7,
				(byte)0x4d,
				(byte)0xa1,
				(byte)0x17,
				(byte)0x76,
				(byte)0x63,
				(byte)0xd0,
				(byte)0x0c,
				(byte)0x33,
				(byte)0x91,
				(byte)0x54,
				(byte)0xd2,
				(byte)0x5e,
				(byte)0x5f,
				(byte)0x71,
				(byte)0x16,
				(byte)0xc2,
				(byte)0x4d,
				(byte)0x2f,
				(byte)0x8c,
				(byte)0xe3,
				(byte)0x82,
				(byte)0x19,
				(byte)0xa0,
				(byte)0x6f,
				(byte)0x2a,
				(byte)0xd1,
				(byte)0x9e,
				(byte)0x1c,
				(byte)0xf1,
				(byte)0x07,
				(byte)0x60,
				(byte)0x33,
				(byte)0xe8,
				(byte)0x47,
				(byte)0xb7,
				(byte)0xc2,
				(byte)0xe9,
				(byte)0x1e,
				(byte)0x45,
				(byte)0xa6,
				(byte)0x00,
				(byte)0x14,
				(byte)0xdf,
				(byte)0x71,
				(byte)0xec,
				(byte)0x73,
				(byte)0x95,
				(byte)0x3d,
				(byte)0x44,
				(byte)0xb6,
				(byte)0x80,
				(byte)0xd9,
				(byte)0x71,
				(byte)0x3e,
				(byte)0xeb,
				(byte)0x01,
				(byte)0xeb,
				(byte)0x0c,
				(byte)0xe5,
				(byte)0xc4,
				(byte)0x80,
				(byte)0x5c,
				(byte)0x9b,
				(byte)0x0a,
				(byte)0x3e,
				(byte)0x6c,
				(byte)0xb9,
				(byte)0x05,
				(byte)0xe5,
				(byte)0x5b,
				(byte)0x9d };
		publicKey.setModulus(modulus, (short)0, (short)128);

		basicKeyPair = new KeyPair(publicKey, privateKey);*/

	}

}