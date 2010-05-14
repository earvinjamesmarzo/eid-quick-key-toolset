package be.cosic.eidtoolset.interfaces;

import be.cosic.eidtoolset.exceptions.*;

public interface SmartCardInterface {
	public int type = 0;

	public byte[] getRandom(int length) throws Exception;

	public void verifyPin(String pinvalue) throws UnknownCardException, SmartCardReaderException, InvalidPinException;

	public void changePin(String currentPin, String newPin,
			String newPinConfirmation) throws UnknownCardException, SmartCardReaderException, InvalidPinException;

	public byte[] readBinaryFile(int fileSelectionCommand)
			throws UnknownCardException, SmartCardReaderException,
			InvalidResponse;

	public byte[] sendCommand(byte[] command);

	public void open() throws UnknownCardException, SmartCardReaderException;

	public String getSmartCardReaderName();

	public void close();

	public byte[] fetchATR();
	
	

}