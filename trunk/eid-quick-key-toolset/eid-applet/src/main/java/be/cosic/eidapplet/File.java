package be.cosic.eidapplet;

public abstract class File {

	// file identifier
	private short fileID;
	protected boolean active = true;
	
	public File(short fid) {
	
		fileID = fid;
	
	}
	
	public short getFileID() {
		
		return fileID;
		
	}
	
	public abstract short[] getPath();

	public void setActive(boolean b) {
		active = b;
	}

}