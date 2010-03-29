package be.cosic.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import javax.security.auth.x500.X500Principal;

import be.cosic.eidtoolset.gui.IdentityDataParser;

public class X509Utils {
	public static X509Certificate deriveCertificateFrom(InputStream inStream)
			throws IOException, CertificateException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert = (X509Certificate) cf
				.generateCertificate(inStream);
		inStream.close();
		return cert;
	}

	public static X509Certificate deriveCertificateFrom(byte[] bytes)
			throws IOException, CertificateException {
		return deriveCertificateFrom(new ByteArrayInputStream(bytes));
	}

	public static void dumpCertificateToFile(String filename,
			X509Certificate cert) throws CertificateEncodingException,
			FileNotFoundException, IOException {
		FileOutputStream file = new FileOutputStream(filename);
		file.write(cert.getEncoded());
		file.close();
	}

	public static X509Certificate importCertificateFrom(File file)
			throws CertificateException, IOException {
		return deriveCertificateFrom(new FileInputStream(file));
	}

	public static String convertCertificateToPEM(
			java.security.cert.Certificate cert)
			throws CertificateEncodingException {
		return "-----BEGIN CERTIFICATE-----\n"
				+ new sun.misc.BASE64Encoder()
						.encode(convertCertificateToDER(cert))
				+ "\n-----END CERTIFICATE-----\n";
	}

	public static byte[] convertCertificateToDER(
			java.security.cert.Certificate cert)
			throws CertificateEncodingException {
		return cert.getEncoded();
	}

	public static byte[] changeCertSignature(byte[] cert,
			RSAPrivateCrtKey specimenPriv) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
		
		//TODO: this should be replaced by getDataAtTag when this method is working for the tag corresponding to the signature
		//Get the signature data and the signature offset
		int signDataStart = 0;
		int signDataLength = 0;
		int signatureOffset = 0;
		int tagnumber = 1;
		int i = 0;
		int offset = 0;
		int length = 0;
		while (i < cert.length) {
			
			offset = i;
			
			if((cert[i]&0xff) > (byte)0x30){
				//Tag id is larger then one byte
				i += 1;
				while((short)(cert[i]&0xff) > 127){
					i += 1;
				}
				i += 1;
			}else {
				i += 1 ;
			}
			
			
			//Check if encoding length fits in one byte: MSB is 0
			if ((cert[i]&0xff) <= 127){
				i = i+1;
				length = cert[i]&0xff + (i - offset);
			}
			else {//If not the length fits in more bytes
				i = i + (cert[i]&0xff) - 127;//should be i + 3
				byte[] lengthArray = new byte[2];//We assume no length bigger then short
				System.arraycopy(cert, (i - 2), lengthArray, 0, 2);
				length = TextUtils.byteArrayToShort(lengthArray) + (i - offset);
			}
			
			//the second tag defines the signature data
			if(tagnumber == 2){
				signDataStart = i;//TODO check if signature on data or also on tag id and data length: now only on data
				signDataLength = length - (i-offset);
				signatureOffset = offset + length;
				
				break;//Exit parsing if no other fiels are required
			}
			i = offset;
			tagnumber =+1;
		}

		//Sign the signature data with the specimen priv key and put it into the certificate
		byte[] signBuffer = new byte[signDataLength];
		System.arraycopy(cert, signDataStart, signBuffer, 0, signDataLength);
		byte[] signature = CryptoUtils.signSha1Rsa1024(signBuffer, specimenPriv);
		//As signature has fixed length copy back in cert is allowed
		System.arraycopy(signature, 0, cert, signatureOffset, signature.length);
		
		//Return the new certificate
		return cert;
	}
	
	
	

	public static byte[] changeCertSubjectData(byte[] cert,
			byte[] id) throws CertificateException, IOException {
		
		//The subject data is located at tag 8 in the certificate
		//byte[] subject = returnDataAtTag(cert, 8);
		
		Hashtable table = new Hashtable();
        IdentityDataParser.ParseIdentityData(id,table);
		
        String first = table.get("First names").toString().substring(0, (table.get("First names").toString().length() - 3));
        String surname = table.get("Name").toString();
        String[] names = first.split(" ");
        String name = names[0] + " " + surname;
        
        String subj = "SERIALNUMBER = " + table.get("National Number").toString() + ", GIVENNAME = " 
      		+ first	+ ", SURNAME = " + surname +	", CN = " + name + " (Signature)" + ", C = BE";
		
        return setDataAtTag(cert, new X500Principal(subj).getEncoded(), 8);
	}
	
	
	
	
	
	private static byte[] returnDataAtTag(byte[] cert, int tag) {
		
		
		int tagnumber = 1;
		int i = 0;
		int offset = 0;
		int length = 0;
		while (i < cert.length) {
			
			offset = i;
			
			if((cert[i]&0xff) > (byte)0x30){
				//Tag id is larger then one byte
				i += 1;
				while((short)(cert[i]&0xff) > 127){
					i += 1;
				}
				i += 1;
			}else {
				i += 1 ;
			}
			
			//Check if encoding length fits in one byte: MSB is 0
			if ((cert[i]&0xff) <= 127){
				i = i+1;
				length = cert[i]&0xff + (i - offset);
			}
			else {//If not the length fits in more bytes
				i = i + (cert[i]&0xff) - 127;//should be i + 3
				byte[] lengthArray = new byte[2];//We assume no length bigger then short
				System.arraycopy(cert, (i - 2), lengthArray, 0, 2);
				length = TextUtils.byteArrayToShort(lengthArray) + (i - offset);
			}
			
			if(tagnumber == tag){
				byte[] data = new byte[length];
				System.arraycopy(cert, offset, data, 0, length);
				
				return data;
			}
			
			//When nested data: do not jump over it
			if(tagnumber != 1 && tagnumber != 2 && tagnumber != 9 && tagnumber != 11){
				i = offset + length;
			}else i += 1;
			
			tagnumber += 1;
		}
		return null;
	}
	
	private static byte[] setDataAtTag(byte[] cert, byte[] data, int tag) {
		
		int tagnumber = 1;
		int i = 0;
		int offset = 0;
		int length = 0;
		while (i < cert.length) {
			
			offset = i;
			
			if((cert[i]&0xff) > (byte)0x30){
				//Tag id is larger then one byte
				i += 1;
				while((short)(cert[i]&0xff) > 127){
					i += 1;
				}
				i += 1;
			}else {
				i += 1 ;
			}
			
			//Check if encoding length fits in one byte: MSB is 0
			if ((cert[i]&0xff) <= 127){
				i = i+1;
				length = cert[i]&0xff + (i - offset);
			}
			else {//If not the length fits in more bytes
				i = i + (cert[i]&0xff) - 127;//should be i + 3
				byte[] lengthArray = new byte[2];//We assume no length bigger then short
				System.arraycopy(cert, (i - 2), lengthArray, 0, 2);
				length = TextUtils.byteArrayToShort(lengthArray) + (i - offset);
			}
			
			if(tagnumber == tag){
				//Set the data
				//Be carefull: length may be different: create new array
				return replaceTagData(cert, data, offset, length);
				
			}
			
			//When nested data: do not jump over it
			if(tagnumber != 1 && tagnumber != 2 && tagnumber != 9 && tagnumber != 11){
				i = offset + length;
			}else i += 1;
			
			tagnumber += 1;
		}
		return null;
	}

	private static byte[] replaceTagData(byte[] cert, byte[] data, int offset,
			int length) {
		
		
		byte[] lengthLength;
		if (data.length <= 127){
			lengthLength = new byte[]{(byte)data.length};
		}else {//if (data.length <= Short.MAX_VALUE)
			lengthLength = TextUtils.shortToByteArray((short)data.length);
		}
		
		byte[] newCert = new byte[cert.length - length + data.length + lengthLength.length];
		
		System.arraycopy(cert, 0, newCert, 0, offset);
		System.arraycopy(lengthLength, 0, newCert, offset, lengthLength.length);
		System.arraycopy(data, 0, newCert, (offset + lengthLength.length), offset);
		System.arraycopy(cert, (offset+length), newCert, (offset + lengthLength.length + data.length), cert.length - (offset+length));
		
		return newCert;
	}


	
}