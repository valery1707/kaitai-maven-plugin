package name.valery1707.kaitai;

@SuppressWarnings("WeakerAccess")
public class KaitaiException extends Exception {
	@SuppressWarnings("unused")
	public KaitaiException(String message) {
		super(message);
	}

	public KaitaiException(String message, Throwable cause) {
		super(message, cause);
	}
}
