package be.cosic.eidapplet;

import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

public class ElementaryFile extends File {

	// link to parent DF
	private DedicatedFile parentFile;

	// data stored in file
	private byte[] data;
	// current size of data stored in file
	short size;

	public ElementaryFile(short fid, DedicatedFile parent, byte[] d) {

		super(fid);
		parentFile = parent;
		parent.addSibling(this);
		data = d;
		size = (short)d.length;
		
	}

	public ElementaryFile(short fid, DedicatedFile parent, short maxSize) {

		super(fid);
		parentFile = parent;
		parent.addSibling(this);
		data = new byte[maxSize];
		size = (short)0;

	}

	public DedicatedFile getParent() {

		return parentFile;

	}

	public byte[] getData() {

		if(active == true)
			return data;
		else {
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
			return null;
		}

	}

	public short getCurrentSize() {

		if(active == true)
			return size;
		else {
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
			return 0;
		}

	}
	
	public short getMaxSize() {
		
		return (short)data.length;
		
	}
 
	public short[] getPath() {

		short[] path = parentFile.getPath();
		path[(short)(path.length + 1)] = getFileID();
		return path;

	}

	public void eraseData(short offset) {

		Util.arrayFillNonAtomic(data, offset, size, (byte)0);

	}

	public void updateData(
		short dataOffset,
		byte[] newData,
		short newDataOffset,
		short length) {

		
		// update size 
		size = (short) (dataOffset + length);
		
		// copy new data
		Util.arrayCopy(newData, newDataOffset, data, dataOffset, length);
		
	}

}