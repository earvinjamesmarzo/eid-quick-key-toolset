package be.cosic.eid;

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

		return data;

	}

	public short getCurrentSize() {

		return size;

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

		//Update the data array to the right length
		data = new byte[(short)(dataOffset + length)];
		// update size 
		size = (short) (dataOffset + length);
		
		// copy new data
		Util.arrayCopy(newData, newDataOffset, data, dataOffset, length);
		
	}

}