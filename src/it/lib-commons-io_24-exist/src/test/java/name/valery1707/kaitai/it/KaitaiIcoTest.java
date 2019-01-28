package name.valery1707.kaitai.it;

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(ico).isNotNull();

		assertThat(ico.magic()).isNotNull().hasSize(4).containsExactly(0, 0, 1, 0);

		assertThat(ico.images()).isNotNull().hasSize(23).hasSize(ico.numImages());

		assertThat(ico.images().get(0)).isNotNull();
		assertThat(ico.images().get(0).img()).isNotNull().hasSize(816).startsWith(40, 0, 0, 0, 48);
	}
}
