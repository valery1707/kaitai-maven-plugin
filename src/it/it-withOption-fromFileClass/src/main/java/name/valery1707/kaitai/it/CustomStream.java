package name.valery1707.kaitai.it;

import io.kaitai.struct.ByteBufferKaitaiStream;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CustomStream extends ByteBufferKaitaiStream {
	public CustomStream(String fileName) throws IOException {
		super(fileName);
		System.out.println("CustomStream.CustomStream(String)");
	}

	public CustomStream(byte[] arr) {
		super(arr);
		System.out.println("CustomStream.CustomStream(byte[])");
	}

	public CustomStream(ByteBuffer buffer) {
		super(buffer);
		System.out.println("CustomStream.CustomStream(ByteBuffer)");
	}
}
