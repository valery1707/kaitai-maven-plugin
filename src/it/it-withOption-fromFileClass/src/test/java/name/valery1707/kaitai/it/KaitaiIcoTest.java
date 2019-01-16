package name.valery1707.kaitai.it;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class KaitaiIcoTest {
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File extract(String resource) throws IOException {
		File target = temporaryFolder.newFile();
		try (
			InputStream in = getClass().getResourceAsStream(resource);
			FileOutputStream out = new FileOutputStream(target);
		) {
			byte[] buffer = new byte[1024 * 8];
			int len;
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}
			out.flush();
		}
		return target;
	}

	@Test
	public void test() throws IOException {
		Ico ico = Ico.fromFile(
			extract("/document.ico").getAbsoluteFile().getCanonicalFile().toString()
		);
		assertThat(ico).isNotNull();

		assertThat(ico.magic()).isNotNull().hasSize(4).containsExactly(0, 0, 1, 0);

		assertThat(ico.images()).isNotNull().hasSize(23).hasSize(ico.numImages());

		assertThat(ico.images().get(0)).isNotNull();
		assertThat(ico.images().get(0).img()).isNotNull().hasSize(816).startsWith(40, 0, 0, 0, 48);
	}
}
