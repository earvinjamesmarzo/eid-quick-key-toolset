package be.cosic.eidtoolset.interfaces;

public interface SmartCardCommandsInterface  {
	public int type=0;
	
	public String[] supportedCardTypes = new String[]{"Gemalto GemXpresso", "NXP SmartMX"};
	
	public String[] getSupportedCardTypes();
}