package be.cosic.eidtoolset.engine;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import javax.smartcardio.CardException;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;


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
	
	private ObjectFactory of;
	private MasterFile mf;
	

	public EidCard(int type, String appName) {
		
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
			mf.getIDDirectory().getAddressFile().setFileData(readBinaryFile(selectCitizenAddressDataCommand));
		}
		return mf.getIDDirectory().getAddressFile().getFileData();
	}

	public byte[] readCitizenPhotoBytes() throws NoReadersAvailable,
			InvalidResponse, NoSuchAlgorithmException, CardException, AIDNotFound, NoCardConnected {
		lookForSmartCard();
		
		if (mf.getIDDirectory().getPhotoFile().getFileData() == null) {
			mf.getIDDirectory().getPhotoFile().setFileData(readBinaryFile(selectCitizenPhotoCommand));
		}
		return mf.getIDDirectory().getPhotoFile().getFileData();
		
	}

	
/**
 * All the following methods are used when reading out all the data from the card to build a full eid data xml file
 * The data they contain are not really useful in the user interface/GUI
 * @throws CardException 
 * @throws NoCardConnected 
 * @throws InvalidResponse 
 */
	
	private byte[] readDirFile() throws InvalidResponse, NoCardConnected, CardException{
		
		return readBinaryFile(selectDirFileCommand);
	}
	
	private byte[] readObjectDirectoryFile() throws InvalidResponse, NoCardConnected, CardException{
		return readBinaryFile(selectObjectDirectoryFileCommand);
	}

	private byte[] readTokenInfo() throws InvalidResponse, NoCardConnected, CardException{
		return readBinaryFile(selectTokenInfoCommand);
	}
	
	private byte[] readAuthenticationObjectDirectoryFile() throws InvalidResponse, NoCardConnected, CardException{
		return readBinaryFile(selectAuthenticationObjectDirectoryFileCommand);
	}
	
	private byte[] readPrivateKeyDirectoryFile() throws InvalidResponse, NoCardConnected, CardException{
		return readBinaryFile(selectPrivateKeyDirectoryFileCommand);
	}
	
	private byte[] readCertificateDirectoryFile() throws InvalidResponse, NoCardConnected, CardException{
		return readBinaryFile(selectCertificateDirectoryFileCommand);
	}
	
	private byte[] readCaRoleIDFile() throws InvalidResponse, NoCardConnected, CardException{
		return readBinaryFile(selectCaRoleIDFileCommand);
	}
	
	private byte[] readPreferencesFile() throws InvalidResponse, NoCardConnected, CardException{
		return readBinaryFile(selectPreferencesFileCommand);
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
		
		
		lookForSmartCard();
				
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
		
		
		
		writeDocument(mf, path);
	
	}
	
	/**
	 * This method will load the xml file given in path into the master file of this class
	 * 
	 * @param path
	 * @throws JAXBException
	 * @throws IOException
	 */
	public void libraryToEid(String path) throws JAXBException, IOException{
		mf = readDocument(path);
	}
	
	/**
	 * Write a Masterfile to an xml document
	 * @param masterf
	 * @param pathname
	 * @throws JAXBException
	 * @throws IOException
	 */
	public void writeDocument( MasterFile masterf, String pathname )
    		throws JAXBException, IOException {

	    JAXBContext context =
	        JAXBContext.newInstance( masterf.getClass().getPackage().getName() );
	    Marshaller m = context.createMarshaller();
	    m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
	    m.marshal( masterf, new FileOutputStream( pathname ) );
	}
	
	/**
	 * Load a Masterfile using a .xml pathname
	 * @param pathname
	 * @return
	 * @throws JAXBException
	 * @throws IOException
	 */
	public MasterFile readDocument(String pathname )
		throws JAXBException, IOException {

		JAXBContext context =
			JAXBContext.newInstance( MasterFile.class.getPackage().getName() );
		Unmarshaller u = context.createUnmarshaller();
		return (MasterFile)u.unmarshal( new File( pathname ) );
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
 * 
 * @param rootCACertificateBytes
 */
	
	// As in the modifiable files, the changes are kept in this object, these will also be stored in the xml file
	//WARNING: the not modifiable files having dependencies with the modifiable ones should be changed here accordingly
	//TODO check for dependencies
	doen
	
	public void setRootCACertificateBytes(byte[] rootCACertificateBytes) {
		mf.getBelPicDirectory().getRootCaCertificate().setFileData(rootCACertificateBytes);
	}

	public void setAuthenticationCertificateBytes(
			byte[] authenticationCertificateBytes) {
		mf.getBelPicDirectory().getAuthenticationCertificate().setFileData(authenticationCertificateBytes);
	}

	public void setNonRepudiationCertificateBytes(
			byte[] nonRepudiationCertificateBytes) {
		mf.getBelPicDirectory().getNonRepudiationCertificate().setFileData(nonRepudiationCertificateBytes);
	}

	public void setCaCertificateBytes(byte[] caCertificateBytes) {
		mf.getBelPicDirectory().getCaCertificate().setFileData(caCertificateBytes);
	}

	public void setRrnCertificateBytes(byte[] rrnCertificateBytes) {
		mf.getBelPicDirectory().getRrnCertificate().setFileData(rrnCertificateBytes);
	}

	public void setCitizenPhoto(byte[] citizenPhoto) {
		
		//TODO: hier ook de hash in ID field veranderen
		
		
		mf.getIDDirectory().getPhotoFile().setFileData(citizenPhoto);
	}

	public void setCitizenIdentityFileBytes(byte[] citizenIdentityFileBytes) {
		mf.getIDDirectory().getIdentityFile().setFileData(citizenIdentityFileBytes);
	}

	public void setCitizenAddressBytes(byte[] citizenAddressBytes) {
		mf.getIDDirectory().getAddressFile().setFileData(citizenAddressBytes);
	}

	public void setIdentityFileSignatureBytes(byte[] identityFileSignatureBytes) {
		mf.getIDDirectory().getIdentityFileSignature().setFileData(identityFileSignatureBytes);
	}

	
	public void setAddressFileSignatureBytes(byte[] addressFileSignatureBytes) {
		mf.getIDDirectory().getAddressFileSignature().setFileData(addressFileSignatureBytes);
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
	
	private byte[] writeCACertificateBytes(byte[] caCertificate) throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		lookForSmartCard();
		return writeBinaryFile(selectCaCertificateCommand, caCertificate);
	}
	
	private byte[] writeIdentityFileSignatureBytes(byte[] identityFileSignature) throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		lookForSmartCard();
		return writeBinaryFile(selectIdentityFileSignatureCommand, identityFileSignature);
	}

	private byte[] writeAddressFileSignatureBytes(byte[] addressFileSignature) throws NoSuchFeature, InvalidResponse, NoCardConnected, CardException, GeneralSecurityException, NoSuchAlgorithmException, NoReadersAvailable, AIDNotFound {
		lookForSmartCard();
		return writeBinaryFile(selectAddressFileSignatureCommand, addressFileSignature);
	}

	private byte[] writeRRNCertificateBytes(byte[] rrnCertificate) throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		lookForSmartCard();
		return writeBinaryFile(selectRrnCertificateCommand, rrnCertificate);
	}
	
	private byte[] writeRootCACertificateBytes(byte[] rootCACertificate) throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		lookForSmartCard();
		return writeBinaryFile(selectRootCaCertificateCommand, rootCACertificate);
	}

	private byte[] writeCitizenIdentityDataBytes(byte[] citizenIdentityFile) throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		lookForSmartCard();
		return writeBinaryFile(selectCitizenIdentityDataCommand, citizenIdentityFile);
	}

	private byte[] writeCitizenAddressBytes(byte[] citizenAddress) throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		lookForSmartCard();
		return writeBinaryFile(selectCitizenAddressDataCommand, citizenAddress);
	}

	private byte[] writeCitizenPhotoBytes(byte[] citizenPhoto) throws NoSuchFeature, NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound, InvalidResponse, NoCardConnected, GeneralSecurityException {
		lookForSmartCard();
		
		return writeBinaryFile(selectCitizenPhotoCommand, citizenPhoto);
	}
	
	/**
	 * This method writes the current master file to the connected eid
	 * 
	 * @throws AIDNotFound 
	 * @throws CardException 
	 * @throws NoReadersAvailable 
	 * @throws NoSuchAlgorithmException 
	 */
	public void writeEid() throws NoSuchAlgorithmException, NoReadersAvailable, CardException, AIDNotFound{
		lookForSmartCard();
		
		//TODO write all data
		write
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