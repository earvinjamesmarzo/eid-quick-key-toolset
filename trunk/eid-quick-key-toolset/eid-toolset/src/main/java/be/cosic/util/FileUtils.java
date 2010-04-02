package be.cosic.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import be.cosic.eidtoolset.eidlibrary.MasterFile;

public class FileUtils {

	public static InputStream bytesToStream(byte[] ba) throws IOException {
		ByteArrayInputStream fis = new ByteArrayInputStream(ba);
		DataInputStream dis = new DataInputStream(fis);
		byte[] bytes = new byte[dis.available()];
		dis.readFully(bytes);
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		return bais;
	}

	public static InputStream fileToStream(String filename)
		throws IOException {
		FileInputStream fis = new FileInputStream(filename);
		DataInputStream dis = new DataInputStream(fis);
		byte[] bytes = new byte[dis.available()];
		dis.readFully(bytes);
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		return bais;
	}

	public static byte[] readFile(String filename)
		throws FileNotFoundException, IOException {
		FileInputStream file = new FileInputStream(filename);
		byte[] data = new byte[(int) file.available()];
		file.read(data);
		file.close();
		return data;
	}

	public static void writeFile(String filename, byte[] data)
		throws FileNotFoundException, IOException {
		FileOutputStream file = new FileOutputStream(filename);
		file.write(data);
		file.close();
	}

	public static void writeBytesToFile(String filename, byte[] data)
		throws FileNotFoundException, IOException {
		FileOutputStream file = new FileOutputStream(filename);
		file.write(data);
		file.close();
	}


	/**
	 * Write a Masterfile to an xml document
	 * @param masterf
	 * @param pathname
	 * @throws JAXBException
	 * @throws IOException
	 */
	public static void writeDocument( MasterFile masterf, String pathname )
    		throws JAXBException, IOException {

	    JAXBContext context =
	        JAXBContext.newInstance( masterf.getClass().getPackage().getName() );
	    Marshaller m = context.createMarshaller();
	    m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
	    m.marshal( masterf, new FileOutputStream( pathname ) );
	}
	
	
	/**
	 * Load a Masterfile using a .xml pathname
	 * @param pathname
	 * @return
	 * @throws JAXBException
	 * @throws IOException
	 */
	public static MasterFile readDocument(String pathname )
		throws JAXBException, IOException {

		JAXBContext context =
			JAXBContext.newInstance( MasterFile.class.getPackage().getName() );
		Unmarshaller u = context.createUnmarshaller();
		return (MasterFile)u.unmarshal( new File( pathname ) );
	}
	
}