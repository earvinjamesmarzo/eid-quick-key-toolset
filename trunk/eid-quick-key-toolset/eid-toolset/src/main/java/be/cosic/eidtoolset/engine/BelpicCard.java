package be.cosic.eidtoolset.engine;

import java.security.NoSuchAlgorithmException;

import javax.smartcardio.CardException;

import be.cosic.eidtoolset.exceptions.*;
import be.cosic.eidtoolset.interfaces.*;
import be.cosic.eidtoolset.exceptions.UnknownCardException;
import be.cosic.eidtoolset.interfaces.BelpicCommandsEngine;

@SuppressWarnings("restriction")
public final class BelpicCard extends EidCard implements BelpicCommandsEngine {
	
	
	public BelpicCard(String appName) throws UnknownCardException, SmartCardReaderException {
		super(EidCardInterface.BELPIC_CARD, appName);
	}

	public void reActivateCard(String puk1, String puk2) throws InvalidPinException,InvalidResponse,NoReadersAvailable,NoSuchFeature,SmartCardReaderException,UnknownCardException, NoSuchAlgorithmException, NoCardConnected, CardException, AIDNotFound{
		reActivate(puk1 ,puk2);
	}
}