package be.cosic.util;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;

public class CryptoUtils {
	public static byte[] computeSha1(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA1");
		md.update(data);
		return md.digest();
	}
	
	public static byte[] signSha1Rsa1024(byte[] data, RSAPrivateCrtKey key) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature RSASign = Signature.getInstance("SHA1withRSA");
		RSASign.initSign(key);
		RSASign.update(data);
		return RSASign.sign();
	}	
	
}
