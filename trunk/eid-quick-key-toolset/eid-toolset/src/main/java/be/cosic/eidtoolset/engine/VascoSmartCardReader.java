package be.cosic.eidtoolset.engine;

public class VascoSmartCardReader extends SmartCardReader  {

	public boolean hasSecurePinPad = true;

	public boolean hasSecurePinPad() {
		return hasSecurePinPad;
	}

	public final static String referenceName = "VASCO";

}