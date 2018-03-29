package name.valery1707.kaitai.it;

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.*;

public class KaitaiIcoTest {
	private KaitaiStream getResourceAsStream(String resource) {
		try (
			InputStream in = getClass().getResourceAsStream(resource);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
		) {
			byte[] buffer = new byte[1024 * 8];
			int len;
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}
			out.flush();
			return new ByteBufferKaitaiStream(out.toByteArray());
		} catch (IOException e) {
			throw new IllegalStateException(String.format(
				"Fail to load data from resource: %s"
				, resource
			)
				, e
			);
		}
	}

	@Test
	public void test() {
		Ico ico = new Ico(getResourceAsStream("/document.ico"));
		assertNotNull(ico);
		assertEquals(23, ico.numImages());
	}
}
