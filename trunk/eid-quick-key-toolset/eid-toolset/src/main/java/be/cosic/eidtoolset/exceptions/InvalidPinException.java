package be.cosic.eidtoolset.exceptions;

public class InvalidPinException extends PinException {
	public InvalidPinException() {
		super();
	}

	public InvalidPinException(String msg) {
		super(msg);
	}
}
