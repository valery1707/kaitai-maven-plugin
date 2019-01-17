package name.valery1707.kaitai.it;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class KaitaiDocContainerTest {
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
		DocContainer doc = DocContainer.fromFile(
			extract("/encrypted.txt").getAbsoluteFile().getCanonicalFile().toString()
		);
		assertThat(doc).isNotNull();

		assertThat(doc.doc()).isNotNull().isInstanceOf(CustomEncryptedObject.class);
		assertThat(doc.doc().getText()).isNotNull().isEqualToIgnoringNewLines("CAESAR'S CIPHER");
	}
}
