package be.cosic.eidtoolset.engine;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.smartcardio.CardException;
import javax.xml.bind.JAXBException;

import be.cosic.eidtoolset.exceptions.*;
import be.cosic.eidtoolset.interfaces.*;
import be.cosic.eidtoolset.exceptions.UnknownCardException;
import be.cosic.eidtoolset.interfaces.BelpicCommandsInterface;

@SuppressWarnings("restriction")
public final class BelpicCard extends EidCard implements BelpicCommandsInterface {
	
	
	public BelpicCard(String appName) throws UnknownCardException, SmartCardReaderException, CertificateException, NoSuchAlgorithmException, JAXBException, IOException {
		super(EidCardInterface.BELPIC_CARD, appName);
	}

	public void reActivateCard(String puk1, String puk2) throws InvalidPinException,InvalidResponse,NoReadersAvailable,NoSuchFeature,SmartCardReaderException,UnknownCardException, NoSuchAlgorithmException, NoCardConnected, CardException, AIDNotFound{
		reActivate(puk1 ,puk2);
	}

	
}