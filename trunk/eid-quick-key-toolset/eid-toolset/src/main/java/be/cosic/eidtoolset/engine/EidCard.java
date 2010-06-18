/*
 * Quick-Key Toolset Project.
 * Copyright (C) 2010 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */
package be.cosic.eidtoolset.engine;


import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

import javax.smartcardio.CardException;
import javax.xml.bind.JAXBException;


import be.cosic.eidtoolset.eidlibrary.MasterFile;
import be.cosic.eidtoolset.eidlibrary.ObjectFactory;
import be.cosic.eidtoolset.eidlibrary.MasterFile.BelPicDirectory;
import be.cosic.eidtoolset.eidlibrary.MasterFile.IDDirectory;
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
import be.cosic.eidtoolset.exceptions.UnsupportedEncodingException;
import be.cosic.eidtoolset.interfaces.BelpicCommandsInterface;
import be.cosic.eidtoolset.interfaces.EidCardInterface;
import be.cosic.util.CryptoUtils;
import be.cosic.util.FileUtils;
import be.cosic.util.MathUtils;
import be.cosic.util.TextUtils;
import be.cosic.util.TimeUtils;


@SuppressWarnings("restriction")
public class EidCard extends SmartCard implements EidCardInterface,
		BelpicCommandsInterface {
	
	

	
	
	public final static int ApduHeaderLength = 5;

	public final static int PinBlockLength = 8;
	
	private String signatureSessionText;

	public String getSignatureSessionText() {
		return signatureSessionText;
	}

	public final static String authenticationSignatureSession = "Authentication session";

	public final static String nonRepudiationSignatureSession = "Non-Repudiation session";

	private String defaultPin = "";
	
	private final static int pinPadDelayBeforeReturning = 500;
	
	private static String preferredSmartCardReader;
	
	//TODO: check if EidCard interface is synched with methods in here
	

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
	
	private ObjectFactory of;
	private MasterFile mf;
	
	
	
	public EidCard(int type, String appName) throws JAXBException, IOException, CertificateException, NoSuchAlgorithmException {
		
		myType = type;
		
		of = new ObjectFactory();
		mf = of.createMasterFile();
		mf.setDirFile(of.createMasterFileDirFile());
		
		BelPicDirectory bpd = of.createMasterFileBelPicDirectory();
		bpd.setObjectDirectoryFile(of.createMasterFileBelPicDirectoryObjectDirectoryFile());
		bpd.setTokenInfo(of.createMasterFileBelPicDirectoryTokenInfo());
		bpd.setAuthenticationObjectDirectoryFile(of.createMasterFileBelPicDirectoryAuthenticationObjectDirectoryFile());
		bpd.setPrivateKeyDirectoryFile(of.createMasterFileBelPicDirectoryPrivateKeyDirectoryFile());
		bpd.setCertificateDirectoryFile(of.createMasterFileBelPicDirectoryCertificateDirectoryFile());
		bpd.setAuthenticationCertificate(of.createMasterFileBelPicDirectoryAuthenticationCertificate());
		bpd.setNonRepudiationCertificate(of.createMasterFileBelPicDirectoryNonRepudiationCertificate());
		bpd.setCaCertificate(of.createMasterFileBelPicDirectoryCaCertificate());
		bpd.setRootCaCertificate(of.createMasterFileBelPicDirectoryRootCaCertificate());
		bpd.setRrnCertificate(of.createMasterFileBelPicDirectoryRrnCertificate());
		
		IDDirectory idd = of.createMasterFileIDDirectory();
		idd.setIdentityFile(of.createMasterFileIDDirectoryIdentityFile());
		idd.setIdentityFileSignature(of.createMasterFileIDDirectoryIdentityFileSignature());
		idd.setAddressFile(of.createMasterFileIDDirectoryAddressFile());
		idd.setAddressFileSignature(of.createMasterFileIDDirectoryAddressFileSignature());
		idd.setPhotoFile(of.createMasterFileIDDirectoryPhotoFile());
		idd.setCaRoleIDFile(of.createMasterFileIDDirectoryCaRoleIDFile());
		idd.setPreferencesFile(of.createMasterFileIDDirectoryPreferencesFile());
		
		
		mf.setBelPicDirectory(bpd);
		mf.setIDDirectory(idd);
		
		preferredSmartCardReader = defaultPreferredSmartCardReader;
		
		
		//Next: not necessary as key & certificate management goes by fedict
		/*
		//Initialise private and public specimen key: from fedict?
		KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
		gen.initialize(1024);
		KeyPair kp = gen.generateKeyPair();
		specimen_priv = (RSAPrivateCrtKey) kp.getPrivate();
		
		
		//Create three CA certificates with this specimen key: from fedict (current) or copy of existing with new siganture
			// HAve a certificate factory /x509utils to make them? 
		libraryToEid("C:\\test.xml");
		
		specimenCACert = X509Utils.deriveCertificateFrom(mf.getBelPicDirectory().getCaCertificate().getFileData());
		specimenRootCACert = X509Utils.deriveCertificateFrom(mf.getBelPicDirectory().getRootCaCertificate().getFileData());
		specimenRRNCert = X509Utils.deriveCertificateFrom(mf.getBelPicDirectory().getRrnCertificate().getFileData());
		*/
		
	}

	public int returnMyType() {
		return myType;
	}
	
	public MasterFile returnMasterFile() {
		return mf;
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


	
	
	/**
	 * Return card data
	 * 
	 * @return
	 * @throws NoSuchFeature 
	 */
	public byte[] readCardData() throws NoSuchFeature{
		if (mf.getBelPicDirectory().getTokenInfo().getFileData() == null)
			try {
				lookForSmartCard();
				mf.getBelPicDirectory().getTokenInfo().setFileData(readBinaryFile(selectTokenInfoCommand));

			} catch (Exception e) {
				e.printStackTrace();
				throw new NoSuchFeature();
			}
		return mf.getBelPicDirectory().getTokenInfo().getFileData();
		
		//check if other info needed appart from tokenINfo. if not: just return tokenINfo by calling right method
	}
	
	/**
	 * For creating new certificate on public key if necessary
	 */
	public byte[] createNewAuthenticationKey() throws NoSuchFeature {
		try {
				lookForSmartCard();
				
				myReader.sendCommand(createAuthenticationKeyPairCommand);

				//The returned data is in the form: '0x02, 0x08, 8 byte exponent, 0x03, 0x8180, 128 byte modulus
				return myReader.sendCommand(getAuthenticationKeyCommand);
				
		} catch (Exception e) {
				e.printStackTrace();
				throw new NoSuchFeature();
		}
		
	}
	
	/**
	 * For creating new certificate on public key if necessary
	 */
	public byte[] createPublicNonRepudiationKey() throws NoSuchFeature {
		try {
				lookForSmartCard();
				myReader.sendCommand(createNonRepudiationKeyPairCommand);

				//The returned data is in the form: '0x02, 0x08, 8 byte exponent, 0x03, 0x8180, 128 byte modulus
				return myReader.sendCommand(getNonRepudiationKeyCommand);


		} catch (Exception e) {
				e.printStackTrace();
				throw new NoSuchFeature();
		}
	}
	
	
	

	public byte[] readAuthCertificateBytes() throws NoSuchFeature {
		if (mf.getBelPicDirectory().getAuthenticationCertificate().getFileData() == null)
			try {
				lookForSmartCard();
				mf.getBelPicDirectory().getAuthenticationCertificate().setFileData(readBinaryFile(selectAuthenticationCertificateCommand));

			} catch (Exception e) {
				e.printStackTrace();
				throw new NoSuchFeature();
			}
		return mf.getBelPicDirectory().getAuthenticationCertificate().getFileData();
	}

	public byte[] readNonRepCertificateBytes() throws NoSuchFeature {
		if (mf.getBelPicDirectory().getNonRepudiationCertificate().getFileData() == null)
			try {
				lookForSmartCard();
				mf.getBelPicDirectory().getNonRepudiationCertificate().setFileData(readBinaryFile(selectNonRepudiationCertificateCommand));

			} catch (Exception e) {
				e.printStackTrace();
				throw new NoSuchFeature();
			}
		return mf.getBelPicDirectory().getNonRepudiationCertificate().getFileData();
	}

	public byte[] readCACertificateBytes() throws NoSuchFeature {
		try {
			if (mf.getBelPicDirectory().getCaCertificate().getFileData() == null) {
				lookForSmartCard();
				mf.getBelPicDirectory().getCaCertificate().setFileData(readBinaryFile(selectCaCertificateCommand));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuchFeature();
		}
		return mf.getBelPicDirectory().getCaCertificate().getFileData();
	}

	public byte[] readIdentityFileSignatureBytes() throws NoSuchFeature {
		try {
			if (mf.getIDDirectory().getIdentityFileSignature().getFileData() == null) {
				lookForSmartCard();
				mf.getIDDirectory().getIdentityFileSignature().setFileData(readBinaryFile(selectIdentityFileSignatureCommand));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuchFeature();
		}
		return mf.getIDDirectory().getIdentityFileSignature().getFileData();
	}

	public byte[] readAddressFileSignatureBytes() throws NoSuchFeature {
		try {
			if (mf.getIDDirectory().getAddressFileSignature().getFileData() == null) {
				lookForSmartCard();
				mf.getIDDirectory().getAddressFileSignature().setFileData(readBinaryFile(selectAddressFileSignatureCommand));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuchFeature();
		}
		return mf.getIDDirectory().getAddressFileSignature().getFileData();
	}

	public byte[] readRRNCertificateBytes() throws NoSuchFeature {
		try {
			if (mf.getBelPicDirectory().getRrnCertificate().getFileData() == null) {
				lookForSmartCard();
				mf.getBelPicDirectory().getRrnCertificate().setFileData(readBinaryFile(selectRrnCertificateCommand));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuchFeature();
		}
		return mf.getBelPicDirectory().getRrnCertificate().getFileData();
	}

	public byte[] readRootCACertificateBytes() throws UnknownCardException,
			NoReadersAvailable, NoSuchFeature, SmartCardReaderException,
			InvalidResponse, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		if (mf.getBelPicDirectory().getRootCaCertificate().getFileData() == null) {
			lookForSmartCard();
			mf.getBelPicDirectory().getRootCaCertificate().setFileData(readBinaryFile(selectRootCaCertificateCommand));
		}
		return mf.getBelPicDirectory().getRootCaCertificate().getFileData();
	}

	public byte[] readCitizenIdentityDataBytes() throws UnknownCardException,
			NoReadersAvailable, NoSuchFeature, SmartCardReaderException,
			InvalidResponse, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		if (mf.getIDDirectory().getIdentityFile().getFileData() == null) {
			lookForSmartCard();
			mf.getIDDirectory().getIdentityFile().setFileData(readBinaryFile(selectCitizenIdentityDataCommand));
		}
		return mf.getIDDirectory().getIdentityFile().getFileData();
	}

	public byte[] readCitizenAddressBytes() throws UnknownCardException,
			NoReadersAvailable, NoSuchFeature, SmartCardReaderException,
			InvalidResponse, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		
		if (mf.getIDDirectory().getAddressFile().getFileData() == null) {
			lookForSmartCard();
			mf.getIDDirectory().getAddressFile().setFileData(readBinaryFile(selectCitizenAddressDataCommand));
		}
		return mf.getIDDirectory().getAddressFile().getFileData();
	}

	public byte[] readCitizenPhotoBytes() throws NoReadersAvailable,
			InvalidResponse, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		
		if (mf.getIDDirectory().getPhotoFile().getFileData() == null) {
			lookForSmartCard();
			mf.getIDDirectory().getPhotoFile().setFileData(readBinaryFile(selectCitizenPhotoCommand));
		}
		return mf.getIDDirectory().getPhotoFile().getFileData();
		
	}

	
/**
 * All the following methods are used when reading out all the data from the card to build a full eid data master file
 * The data they contain are not really useful in the user interface/GUI but are necessary when writing new card
 * @throws CardException 
 * @throws NoCardConnected 
 * @throws InvalidResponse 
 * @throws AIDNotFound 
 * @throws NoReadersAvailable 
 * @throws NoSuchAlgorithmException 
 */
	
	
	private byte[] readDirFile() throws InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, NoReadersAvailable, AIDNotFound{
		
		if (mf.getDirFile().getFileData() == null) {
			lookForSmartCard();
			mf.getDirFile().setFileData(readBinaryFile(selectDirFileCommand));
		}
		return mf.getDirFile().getFileData();
	}
	
	private byte[] readObjectDirectoryFile() throws InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, NoReadersAvailable, AIDNotFound{
		if (mf.getBelPicDirectory().getObjectDirectoryFile().getFileData() == null) {
			lookForSmartCard();
			mf.getBelPicDirectory().getObjectDirectoryFile().setFileData(readBinaryFile(selectObjectDirectoryFileCommand));
		}
		return mf.getBelPicDirectory().getObjectDirectoryFile().getFileData();
	}

	private byte[] readTokenInfo() throws InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, NoReadersAvailable, AIDNotFound{
		if (mf.getBelPicDirectory().getTokenInfo().getFileData() == null) {
			lookForSmartCard();
			mf.getBelPicDirectory().getTokenInfo().setFileData(readBinaryFile(selectTokenInfoCommand));
		}
		return mf.getBelPicDirectory().getTokenInfo().getFileData();
	}
	
	private byte[] readAuthenticationObjectDirectoryFile() throws InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, NoReadersAvailable, AIDNotFound{
		if (mf.getBelPicDirectory().getAuthenticationObjectDirectoryFile().getFileData() == null) {
			lookForSmartCard();
			mf.getBelPicDirectory().getAuthenticationObjectDirectoryFile().setFileData(readBinaryFile(selectAuthenticationObjectDirectoryFileCommand));
		}
		return mf.getBelPicDirectory().getAuthenticationObjectDirectoryFile().getFileData();
	}
	
	private byte[] readPrivateKeyDirectoryFile() throws InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, NoReadersAvailable, AIDNotFound{
		if (mf.getBelPicDirectory().getPrivateKeyDirectoryFile().getFileData() == null) {
			lookForSmartCard();
			mf.getBelPicDirectory().getPrivateKeyDirectoryFile().setFileData(readBinaryFile(selectPrivateKeyDirectoryFileCommand));
		}
		return mf.getBelPicDirectory().getPrivateKeyDirectoryFile().getFileData();
	}
	
	private byte[] readCertificateDirectoryFile() throws InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, NoReadersAvailable, AIDNotFound{
		if (mf.getBelPicDirectory().getCertificateDirectoryFile().getFileData() == null) {
			lookForSmartCard();
			mf.getBelPicDirectory().getCertificateDirectoryFile().setFileData(readBinaryFile(selectCertificateDirectoryFileCommand));
		}
		return mf.getBelPicDirectory().getCertificateDirectoryFile().getFileData();
	}
	
	private byte[] readCaRoleIDFile() throws InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, NoReadersAvailable, AIDNotFound{
		if (mf.getIDDirectory().getCaRoleIDFile().getFileData() == null) {
			lookForSmartCard();
			mf.getIDDirectory().getCaRoleIDFile().setFileData(readBinaryFile(selectCaRoleIDFileCommand));
		}
		return mf.getIDDirectory().getCaRoleIDFile().getFileData();
	}
	
	private byte[] readPreferencesFile() throws InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, NoReadersAvailable, AIDNotFound{
		if (mf.getIDDirectory().getPreferencesFile().getFileData() == null) {
			lookForSmartCard();
			mf.getIDDirectory().getPreferencesFile().setFileData(readBinaryFile(selectPreferencesFileCommand));
		}
		return mf.getIDDirectory().getPreferencesFile().getFileData();
	}
	
	public void readFullEid() throws NoSuchAlgorithmException, InvalidResponse, NoCardConnected, CardException, NoReadersAvailable, AIDNotFound, NoSuchFeature, UnknownCardException, SmartCardReaderException{
		
		//Call all read methods and put all results in xml masterfile
		//As either all data will come from an existing card or will be set using the set methods, 
		//and in these set methods dependencies over different files are checked upon, we do not need to do this anymore here.
		
		mf.getDirFile().setFileData(readDirFile());
		
		mf.getBelPicDirectory().getObjectDirectoryFile().setFileData(this.readObjectDirectoryFile());
		mf.getBelPicDirectory().getTokenInfo().setFileData(this.readTokenInfo());
		mf.getBelPicDirectory().getAuthenticationObjectDirectoryFile().setFileData(this.readAuthenticationObjectDirectoryFile());
		mf.getBelPicDirectory().getPrivateKeyDirectoryFile().setFileData(this.readPrivateKeyDirectoryFile());
		mf.getBelPicDirectory().getCertificateDirectoryFile().setFileData(this.readCertificateDirectoryFile());
		mf.getBelPicDirectory().getAuthenticationCertificate().setFileData(this.readAuthCertificateBytes());
		mf.getBelPicDirectory().getNonRepudiationCertificate().setFileData(this.readNonRepCertificateBytes());
		mf.getBelPicDirectory().getCaCertificate().setFileData(this.readCACertificateBytes());
		mf.getBelPicDirectory().getRootCaCertificate().setFileData(this.readRootCACertificateBytes());
		mf.getBelPicDirectory().getRrnCertificate().setFileData(this.readRRNCertificateBytes());
		
		mf.getIDDirectory().getIdentityFile().setFileData(this.readCitizenIdentityDataBytes());
		mf.getIDDirectory().getIdentityFileSignature().setFileData(this.readIdentityFileSignatureBytes());
		mf.getIDDirectory().getAddressFile().setFileData(this.readCitizenAddressBytes());
		mf.getIDDirectory().getAddressFileSignature().setFileData(this.readAddressFileSignatureBytes());
		mf.getIDDirectory().getPhotoFile().setFileData(this.readCitizenPhotoBytes());
		mf.getIDDirectory().getCaRoleIDFile().setFileData(this.readCaRoleIDFile());
		mf.getIDDirectory().getPreferencesFile().setFileData(this.readPreferencesFile());
		
	}
	
	
	/**
	 * This method will read data out of the current connected card and put it in a new xml file
	 * The name of the file and the path where to store it should be given as parameter
	 * @throws AIDNotFound 
	 * @throws CardException 
	 * @throws NoReadersAvailable 
	 * @throws NoSuchAlgorithmException 
	 * @throws NoSuchFeature 
	 * @throws NoCardConnected 
	 * @throws InvalidResponse 
	 * @throws SmartCardReaderException 
	 * @throws UnknownCardException 
	 * @throws IOException 
	 * @throws JAXBException 
	 */
	public void eIDToLibrary(String path) throws NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, NoSuchFeature, UnknownCardException, SmartCardReaderException, InvalidResponse, NoCardConnected, JAXBException, IOException{
			
		//First read all data in, in this masterfile (if not already done)
		readFullEid();
		
		FileUtils.writeDocument(mf, path);
	
	}
	
	/**
	 * This method will load the xml file given in path into the master file of this class
	 * 
	 * @param path
	 * @throws JAXBException
	 * @throws IOException
	 */
	public void libraryToEid(String path) throws JAXBException, IOException{
		mf = FileUtils.readDocument(path);
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

	

/**
 * The following methods are used to enable the user to change fields in this masterfile
 * Dependencies in between fields should be checked upon here (also non modifiable fields)
 * Only the truly usable files can be set by the user. the others follow form these changes (e.g. certificates)
 * @throws CertificateEncodingException 
 * 
 */
	
	//All signature dependent files can not be set by the user but should be set when 
	//changing ID values. Changing ID values means changing signature on them 
	//(using specimen National Register private key) and thus means changing certificates
	
	

	

	public void setCitizenPhoto(byte[] citizenPhoto) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, CertificateEncodingException {
		
	
		//1. set photo data
		mf.getIDDirectory().getPhotoFile().setFileData(citizenPhoto);
		
		//2. change hash in ID field 
		byte[] photoHash = CryptoUtils.computeSha1(citizenPhoto);
		
		byte[] id = mf.getIDDirectory().getIdentityFile().getFileData();
		System.arraycopy(photoHash, 0, id, (id.length-photoHash.length), photoHash.length);
		mf.getIDDirectory().getIdentityFile().setFileData(id);
		
		//3. change signature on id field: should be done externally
		//byte[] sigBuffer = CryptoUtils.signSha1Rsa1024(id, specimen_priv);
		//mf.getIDDirectory().getIdentityFileSignature().setFileData(sigBuffer);
		
		//4. change all certificates to specimen ones if not already done (use boolean): should be done externally
		/*if(!specimen_certificate_set){
			//the tree fixed specimen CA certificates can be hardcoded: just encode them and set them: see constructor
			mf.getBelPicDirectory().getCaCertificate().setFileData(specimenCACert.getEncoded());
			mf.getBelPicDirectory().getRootCaCertificate().setFileData(specimenRootCACert.getEncoded());
			mf.getBelPicDirectory().getRrnCertificate().setFileData(specimenRRNCert.getEncoded());
			
			
			//Change user certificates so they contain new data: should be done externally
			//byte[] newAuthCert = X509Utils.changeCertSignature(X509Utils.changeCertSubjectData(mf.getBelPicDirectory().getAuthenticationCertificate().getFileData(), citizenIdentityFileBytes), specimen_priv);
			//mf.getBelPicDirectory().getAuthenticationCertificate().setFileData(newAuthCert);
			//byte[] newNonRepCert = X509Utils.changeCertSignature(X509Utils.changeCertSubjectData(mf.getBelPicDirectory().getNonRepudiationCertificate().getFileData(), citizenIdentityFileBytes), specimen_priv);
			//mf.getBelPicDirectory().getNonRepudiationCertificate().setFileData(newNonRepCert);
		}*/
		
		
	}

	public void setCitizenIdentityFileBytes(byte[] citizenIdentityFileBytes) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, CertificateException, IOException {
		
		//1. set the id data
		//do not change hash on photo data: first copy old hash in new array
		byte[] id = mf.getIDDirectory().getIdentityFile().getFileData();
		//sha-1 hash is 20 bytes long
		System.arraycopy(id, (id.length-20), citizenIdentityFileBytes, citizenIdentityFileBytes.length - 20, 20);
		mf.getIDDirectory().getIdentityFile().setFileData(citizenIdentityFileBytes);
		
		//2. change signature on id field and chip number in tokeninfo if changed: should be done externally
		//byte[] sigBuffer = CryptoUtils.signSha1Rsa1024(citizenIdentityFileBytes, specimen_priv);
		//mf.getIDDirectory().getIdentityFileSignature().setFileData(sigBuffer);
		
		//3. change all certificates to specimen ones if not already done (use boolean): should be done externally
		/*if(!specimen_certificate_set){
			//the tree fixed specimen CA certificates can be hardcoded: just encode them and set them: see constructor
			mf.getBelPicDirectory().getCaCertificate().setFileData(specimenCACert.getEncoded());
			mf.getBelPicDirectory().getRootCaCertificate().setFileData(specimenRootCACert.getEncoded());
			mf.getBelPicDirectory().getRrnCertificate().setFileData(specimenRRNCert.getEncoded());
		}*/
		
		//Change user certificates so they contain new data: should be done externally
		//byte[] newAuthCert = X509Utils.changeCertSignature(X509Utils.changeCertSubjectData(mf.getBelPicDirectory().getAuthenticationCertificate().getFileData(), citizenIdentityFileBytes), specimen_priv);
		//mf.getBelPicDirectory().getAuthenticationCertificate().setFileData(newAuthCert);
		//byte[] newNonRepCert = X509Utils.changeCertSignature(X509Utils.changeCertSubjectData(mf.getBelPicDirectory().getNonRepudiationCertificate().getFileData(), citizenIdentityFileBytes), specimen_priv);
		//mf.getBelPicDirectory().getNonRepudiationCertificate().setFileData(newNonRepCert);
	
	}

	public void setCitizenAddressBytes(byte[] citizenAddressBytes) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException, CertificateEncodingException {
		
		//1. set the address data
		mf.getIDDirectory().getAddressFile().setFileData(citizenAddressBytes);
		
		//2. change the signature on the address file: should be done externally
		//byte[] sigBuffer = CryptoUtils.signSha1Rsa1024(citizenAddressBytes, specimen_priv);
		//mf.getIDDirectory().getAddressFileSignature().setFileData(sigBuffer);
		
		//3. change all certificates to specimen ones if not already done (use boolean): should be done externally
		/*if(!specimen_certificate_set){
			//the tree fixed specimen CA certificates can be hardcoded: just encode them and set them: see constructor
			mf.getBelPicDirectory().getCaCertificate().setFileData(specimenCACert.getEncoded());
			mf.getBelPicDirectory().getRootCaCertificate().setFileData(specimenRootCACert.getEncoded());
			mf.getBelPicDirectory().getRrnCertificate().setFileData(specimenRRNCert.getEncoded());
			
			//Change user certificates so they contain new data: should be done externally
			//byte[] newAuthCert = X509Utils.changeCertSignature(X509Utils.changeCertSubjectData(mf.getBelPicDirectory().getAuthenticationCertificate().getFileData(), citizenIdentityFileBytes), specimen_priv);
			//mf.getBelPicDirectory().getAuthenticationCertificate().setFileData(newAuthCert);
			//byte[] newNonRepCert = X509Utils.changeCertSignature(X509Utils.changeCertSubjectData(mf.getBelPicDirectory().getNonRepudiationCertificate().getFileData(), citizenIdentityFileBytes), specimen_priv);
			//mf.getBelPicDirectory().getNonRepudiationCertificate().setFileData(newNonRepCert);
		}*/
	}

	
	public void setCACertificate(byte[] caCertificate) {
		mf.getBelPicDirectory().getCaCertificate().setFileData(caCertificate);
	}
	
	public void setRootCACertificate(byte[] rootCACertificate) {
		mf.getBelPicDirectory().getRootCaCertificate().setFileData(rootCACertificate);
	}
	
	public void setRnnCertificate(byte[] rnnCertificate) {
		mf.getBelPicDirectory().getRrnCertificate().setFileData(rnnCertificate);
	}
	
	public void setNonRepudiationCertificate(byte[] nonRepudiationCertificate) {
		mf.getBelPicDirectory().getNonRepudiationCertificate().setFileData(nonRepudiationCertificate);
	}
	
	public void setAuthenticationCertificate(byte[] authenticationCertificate) {
		mf.getBelPicDirectory().getAuthenticationCertificate().setFileData(authenticationCertificate);
	}
	
	
	public void clearCache() {
		mf.getIDDirectory().getPhotoFile().setFileData(null);
		mf.getIDDirectory().getAddressFile().setFileData(null);
		mf.getBelPicDirectory().getRootCaCertificate().setFileData(null);
		mf.getIDDirectory().getIdentityFile().setFileData(null);
		mf.getBelPicDirectory().getAuthenticationCertificate().setFileData(null);
		mf.getBelPicDirectory().getNonRepudiationCertificate().setFileData(null);
		mf.getBelPicDirectory().getCaCertificate().setFileData(null);
		mf.getBelPicDirectory().getRrnCertificate().setFileData(null);
		mf.getIDDirectory().getIdentityFileSignature().setFileData(null);
		mf.getIDDirectory().getAddressFileSignature().setFileData(null);
		
		mf.getDirFile().setFileData(null);
		mf.getBelPicDirectory().getObjectDirectoryFile().setFileData(null);
		mf.getBelPicDirectory().getTokenInfo().setFileData(null);
		mf.getBelPicDirectory().getAuthenticationObjectDirectoryFile().setFileData(null);
		mf.getBelPicDirectory().getPrivateKeyDirectoryFile().setFileData(null);
		mf.getBelPicDirectory().getCertificateDirectoryFile().setFileData(null);
		mf.getIDDirectory().getCaRoleIDFile().setFileData(null);		
		mf.getIDDirectory().getPreferencesFile().setFileData(null);
		
		//For when certificates are adapted internally. Not the case now. specimen_certificate_set = false;
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
	
	/**
	 * This method sets the preferred reader to the given reader
	 */
	public void setPreferredReader(String reader){
		closeReader();
		
		preferredSmartCardReader = reader;
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

	private boolean checkThisPin(String pinvalue,
			int nofMilliSecondsBeforeReturning) throws InvalidResponse, NoCardConnected, CardException, InvalidPinException {
		boolean keepTrying = true;
		byte[] result = verifyThisPin(pinvalue);
		if (TextUtils.hexDump(result).equals("9000")) {
			//pinPad.setStatusText("OK...");
			TimeUtils.sleep(nofMilliSecondsBeforeReturning);
			keepTrying = false;
		} else {
			if (TextUtils.hexDump(result).equals("63C2")) {
				//pinPad.setStatusText("Invalid PIN, 2 tries left...");
				TimeUtils.sleep(nofMilliSecondsBeforeReturning);
			} else if (TextUtils.hexDump(result).equals("63C1")) {
				//pinPad.setStatusText("Invalid PIN, 1 try left...");
				TimeUtils.sleep(nofMilliSecondsBeforeReturning);
			} else {
				if (TextUtils.hexDump(result).equals("63C0")) {
					//pinPad.setStatusText("Invalid PIN, Card blocked...");
					TimeUtils.sleep(nofMilliSecondsBeforeReturning);
					keepTrying = false;
				}
			}
			throw new InvalidPinException(Byte.toString(result[3]));
		}
		return keepTrying;
	}

	

	private void pinValidationEngine(String pinvalue, String pintext)
			throws InvalidPinException, PinException, InvalidResponse,
			NoSuchFeature, NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		lookForSmartCard();
		
		
		if (myReader.getMyName().toUpperCase().indexOf(
				VascoSmartCardReader.referenceName.toUpperCase()) >= 0) {
			myReader.verifyPIN();
			return;
		} else if (pinvalue != "")
			checkThisPin(pinvalue, pinPadDelayBeforeReturning);
		else {
			throw new PinException();
		}
	}

	public void changeThisPin(String currentpinvalue, String newpinvalue,
			 int nofMilliSecondsBeforeReturning)
			throws NoSuchFeature, NoReadersAvailable, InvalidPinException, PinException, InvalidResponse, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		
		
		byte[] result = myReader.sendCommand(insertTwoPinsIntoApdu(
				changePinApdu, currentpinvalue, newpinvalue));
		if (TextUtils.hexDump(result).equals("9000")) {
			//pinPad.setStatusText("OK...");
			//TimeUtils.sleep(nofMilliSecondsBeforeReturning);
		} else {
			if (TextUtils.hexDump(result).equals("63C2")) {
				//pinPad.setStatusText("Invalid PIN, 2 tries left...");
				//TimeUtils.sleep(nofMilliSecondsBeforeReturning);
			} else if (TextUtils.hexDump(result).equals("63C1")) {
				//pinPad.setStatusText("Invalid PIN, 1 try left...");
				//TimeUtils.sleep(nofMilliSecondsBeforeReturning);
			} else {
				if (TextUtils.hexDump(result).equals("63C0")) {
					//pinPad.setStatusText("Invalid PIN, Card blocked...");
					//TimeUtils.sleep(nofMilliSecondsBeforeReturning);
				}
			}
			throw new InvalidPinException();
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

	
/**
 * The write methods are used in the writeEid method to write the local master file to the card
 * 
 * 
 * @param caCertificate
 * @return
 * @throws NoSuchFeature
 * @throws NoSuchAlgorithmException
 * @throws NoReadersAvailable
 * @throws CardException
 * @throws AIDNotFound
 * @throws InvalidResponse
 * @throws NoCardConnected
 * @throws GeneralSecurityException
 */
	
	/*
	 * The following writes are only allowed under certain authentication to the card
	 * see PKCS15 document of eid p 13
	 */
	private byte[] writeDirFile() throws InvalidResponse, NoCardConnected, CardException, GeneralSecurityException, NoSuchAlgorithmException, NoReadersAvailable, AIDNotFound{
		
		return writeBinaryFile(selectDirFileCommand, readDirFile());
	}
	
	private byte[] writeCitizenAddressBytes() throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException, UnknownCardException, SmartCardReaderException {
		
		return writeBinaryFile(selectCitizenAddressDataCommand, readCitizenAddressBytes());
	}
	
	private byte[] writeAddressFileSignatureBytes() throws NoSuchFeature, InvalidResponse, NoCardConnected, CardException, GeneralSecurityException, NoSuchAlgorithmException, NoReadersAvailable, AIDNotFound {
		
		return writeBinaryFile(selectAddressFileSignatureCommand, readAddressFileSignatureBytes());
	}
	
	private byte[] writeAuthCertificateBytes() throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		
		//New certificate on public key of new card should be build: should be done externally
		return writeBinaryFile(selectAuthenticationCertificateCommand, readAuthCertificateBytes());
	}
	
	private byte[] writeNonRepCertificateBytes() throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		
		//New certificate on public key of new card should be build: should be done externally
		return writeBinaryFile(selectNonRepudiationCertificateCommand, readNonRepCertificateBytes());
	}
	
	private byte[] writeCACertificateBytes() throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		
		return writeBinaryFile(selectCaCertificateCommand, readCACertificateBytes());
	}

	private byte[] writeRootCACertificateBytes() throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException, UnknownCardException, SmartCardReaderException {
		
		
		return writeBinaryFile(selectRootCaCertificateCommand, readRootCACertificateBytes());
	}
	
	
	/*
	 * The following writes are only allowed during personalisation 
	 */
	private byte[] writeCitizenPhotoBytes() throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		
		return writeBinaryFile(selectCitizenPhotoCommand, readCitizenPhotoBytes());
	}
	
	private byte[] writeRRNCertificateBytes() throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		
		
		return writeBinaryFile(selectRrnCertificateCommand, readRRNCertificateBytes());
	}
	
	private byte[] writeObjectDirectoryFile() throws InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, GeneralSecurityException, NoReadersAvailable, AIDNotFound{
		return writeBinaryFile(selectObjectDirectoryFileCommand, readObjectDirectoryFile());
	}

	private byte[] writeTokenInfo() throws InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, GeneralSecurityException, NoReadersAvailable, AIDNotFound{
		return writeBinaryFile(selectTokenInfoCommand, readTokenInfo());
	}
	
	private byte[] writeAuthenticationObjectDirectoryFile() throws InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, GeneralSecurityException, NoReadersAvailable, AIDNotFound{
		return writeBinaryFile(selectAuthenticationObjectDirectoryFileCommand, readAuthenticationObjectDirectoryFile());
	}
	
	private byte[] writePrivateKeyDirectoryFile() throws InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, GeneralSecurityException, NoReadersAvailable, AIDNotFound{
		return writeBinaryFile(selectPrivateKeyDirectoryFileCommand, readPrivateKeyDirectoryFile());
	}
	
	private byte[] writeCertificateDirectoryFile() throws InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, GeneralSecurityException, NoReadersAvailable, AIDNotFound{
		return writeBinaryFile(selectCertificateDirectoryFileCommand, readCertificateDirectoryFile());
	}
	
	private byte[] writeCaRoleIDFile() throws InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, GeneralSecurityException, NoReadersAvailable, AIDNotFound{
		return writeBinaryFile(selectCaRoleIDFileCommand, readCaRoleIDFile());
	}
	
	private byte[] writePreferencesFile() throws InvalidResponse, NoCardConnected, CardException, NoSuchAlgorithmException, GeneralSecurityException, NoReadersAvailable, AIDNotFound{
		return writeBinaryFile(selectPreferencesFileCommand, readPreferencesFile());
	}
	
	private byte[] writeCitizenIdentityDataBytes() throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException, UnknownCardException, SmartCardReaderException {
		
		return writeBinaryFile(selectCitizenIdentityDataCommand, readCitizenIdentityDataBytes());
	}

	private byte[] writeIdentityFileSignatureBytes() throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		
		return writeBinaryFile(selectIdentityFileSignatureCommand, readIdentityFileSignatureBytes());
	}

	

	
	
	/**
	 * This method writes the current master file to the connected eid
	 * 
	 * @throws AIDNotFound 
	 * @throws CardException 
	 * @throws NoReadersAvailable 
	 * @throws NoSuchAlgorithmException 
	 * @throws GeneralSecurityException 
	 * @throws NoCardConnected 
	 * @throws InvalidResponse 
	 * @throws NoSuchFeature 
	 * @throws SmartCardReaderException 
	 * @throws UnknownCardException 
	 */
	public void writeEid() throws NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException, NoSuchFeature, UnknownCardException, SmartCardReaderException{
		
		lookForSmartCard();
		
		//first check if eid is writable: is done during first write: correct exception will be thrown
		writeDirFile();
		
		writeObjectDirectoryFile();
		writeTokenInfo();
		writeAuthenticationObjectDirectoryFile();
		writePrivateKeyDirectoryFile();
		writeCertificateDirectoryFile();
		writeAuthCertificateBytes();
		writeNonRepCertificateBytes();
		writeCACertificateBytes();
		writeRRNCertificateBytes();
		writeRootCACertificateBytes();
		
		writeCitizenIdentityDataBytes();
		writeCitizenAddressBytes();
		writeIdentityFileSignatureBytes();
		writeAddressFileSignatureBytes();
		writeCitizenPhotoBytes();
		writeCaRoleIDFile();
		writePreferencesFile();
	}
	
	/**
	 * Activate the card to go from personalisation phase to operational phase
	 * @return response
	 * @throws AIDNotFound 
	 * @throws CardException 
	 * @throws NoReadersAvailable 
	 * @throws NoSuchAlgorithmException 
	 * @throws NoCardConnected 
	 * @throws InvalidResponse 
	 */
	public byte[] activateCard() throws NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected{
		lookForSmartCard();
		return myReader.sendCommand(activateCardCommand);
	}
	
	
	public String nameOfActiveReader() throws NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		lookForSmartCard();
		return myReader.getMyName();
	}

	public byte[] fetchATR() throws NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		lookForSmartCard();
		return myReader.getATR();
	}
	
	public byte[] changeATR() throws NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected, InvalidResponse {
		lookForSmartCard();
		return myReader.sendCommand(changeATRCommand);
	}
	
	public byte[] getChallenge() throws NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound, InvalidResponse, NoCardConnected {
		lookForSmartCard();
		return myReader.sendCommand(getChallengeCommand);
	}

	public SmartCardReader getSmartCardReader() throws NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		lookForSmartCard();
		return myReader;
	}
	
	public String[] getSmartCardReaders() throws NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound {
		if (smartCardReaderShouldBeInitialized) {//If already initialized (and thus selected): not necessary to do so again
			myReader = new SmartCardReader();
		}
		return myReader.getReaders();
	}

	private void lookForSmartCard() throws NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		if (smartCardReaderShouldBeInitialized)
			lookForSmartCard(preferredSmartCardReader,
					defaultTimeoutInMilliSecondsBeforeTryingAnotherReader);
	}

	private boolean smartCardReaderShouldBeInitialized = true;

	
	private void lookForSmartCard(String preferredReader,
			int milliSecondsBeforeTryingAnotherReader)
			throws NoReadersAvailable, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		if (smartCardReaderShouldBeInitialized) {//If already initialized (and thus selected): not necessary to do so again
			myReader = new SmartCardReader();
			myReader.lookForSmartCard(preferredReader,
					milliSecondsBeforeTryingAnotherReader, selectAID_APDU);
			smartCardReaderShouldBeInitialized = false;
		}
	}

	
	
	
	
	
	
	
}