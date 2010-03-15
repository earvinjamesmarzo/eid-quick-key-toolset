package be.cosic.eid;

public class MasterFile extends DedicatedFile {
	
	private static final short MF_FID = 0x3F00;

	public MasterFile() {

		// file identifier of MasterFile is hardcode to 3F00
		super(MF_FID);

	}
	
}