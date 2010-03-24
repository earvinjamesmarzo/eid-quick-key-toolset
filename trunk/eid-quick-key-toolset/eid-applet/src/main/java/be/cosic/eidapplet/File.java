package be.cosic.eidapplet;

public abstract class File {

	// file identifier
	private short fileID;
	
	public File(short fid) {
	
		fileID = fid;
	
	}
	
	public short getFileID() {
		
		return fileID;
		
	}
	
	public abstract short[] getPath();

}