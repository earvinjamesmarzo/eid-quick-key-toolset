package be.cosic.eidtoolset.interfaces;

import javax.smartcardio.CardException;

import be.cosic.eidtoolset.exceptions.InvalidResponse;
import be.cosic.eidtoolset.exceptions.NoCardConnected;

@SuppressWarnings("restriction")
public interface SmartCardReaderInterface {
	public final static int UNKNOWN = 0;

	public int type = UNKNOWN;

	abstract byte[] sendCommand(byte[] apdu) throws InvalidResponse, NoCardConnected, CardException;
}