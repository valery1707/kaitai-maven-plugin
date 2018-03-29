package name.valery1707.kaitai;

public class KaitaiException extends Exception {
	public KaitaiException(String message) {
		super(message);
	}

	public KaitaiException(String message, Throwable cause) {
		super(message, cause);
	}
}
