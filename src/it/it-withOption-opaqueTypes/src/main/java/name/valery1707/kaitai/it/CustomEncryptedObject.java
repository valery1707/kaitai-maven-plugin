package name.valery1707.kaitai.it;

import io.kaitai.struct.KaitaiStream;

public class CustomEncryptedObject {
	byte[] buf;

	public CustomEncryptedObject(KaitaiStream io) {
		// read all remaining bytes into our buffer
		buf = io.readBytesFull();

		// implement our custom super Caesar's cipher
		System.out.println("Decrypting " + buf.length + " bytes");
		for (int i = 0; i < buf.length; i++) {
			byte b = buf[i];
			if (b >= 'A' && b <= 'Z') {
				int letter = b - 'A';
				letter = (letter + 19) % 26;
				buf[i] = (byte) (letter + 'A');
			}
			System.out.println(i + ": converted '" + ((char) b) + "' into '" + ((char) buf[i]) + "'");
		}
	}

	public String getText() {
		return new String(buf, java.nio.charset.StandardCharsets.UTF_8);
	}
}
