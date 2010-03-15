package be.cosic.eid;

public class DedicatedFile extends File {

	// link to parent DF
	private DedicatedFile parentFile;

	// list of sibling files (either EF or DF)
	private static final byte MAX_SIBLINGS = 10;
	private File[] siblings = new File[MAX_SIBLINGS];
	// number of siblings
	private byte number = 0;

	// constructor only used by MasterFile 
	protected DedicatedFile(short fid) {

		super(fid);
		// MasterFile does not have a parent, as it is the root of all files
		parentFile = null;

	}

	public DedicatedFile(short fid, DedicatedFile parent) {

		super(fid);
		parentFile = parent;
		parent.addSibling(this);

	}

	public short[] getPath() {

		short[] path;
		if (parentFile != null) {
			path = parentFile.getPath();
			path[(short)(path.length + 1)] = getFileID();
		} else
			path = new short[] { getFileID()};

		return path;

	}

	public DedicatedFile getParent() {

		return parentFile;

	}
	
	public byte getNumberOfSiblings() {
		
		return number;
		
	}

	public File getSibling(short fid) {

		for (byte i = 0; i < number; i++) {
			if (siblings[i].getFileID() == fid)
				return siblings[i];
		}

		return null;

	}
	
	protected void addSibling(File s) {

		if (number < MAX_SIBLINGS)
			siblings[number++] = s;

	}

}