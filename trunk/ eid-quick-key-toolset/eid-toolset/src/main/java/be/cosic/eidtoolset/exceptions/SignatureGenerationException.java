package be.cosic.eidtoolset.exceptions;

public class SignatureGenerationException extends GeneralSecurityException {
	public SignatureGenerationException() {
		super();
	}

	public SignatureGenerationException(String msg) {
		super(msg);
	}
}
