package be.cosic.eidtoolset.exceptions;

public class SignatureException extends GeneralSecurityException {
	public SignatureException() {
		super();
	}

	public SignatureException(String msg) {
		super(msg);
	}
}
