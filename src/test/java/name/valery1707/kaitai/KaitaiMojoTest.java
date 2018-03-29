package name.valery1707.kaitai;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import static name.valery1707.kaitai.KaitaiMojo.downloadKaitai;
import static name.valery1707.kaitai.KaitaiMojo.prepareUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

public class KaitaiMojoTest {
	private static final SystemStreamLog LOG = new SystemStreamLog();

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testPrepareUrl_returnInputIfNonNull() throws MojoExecutionException {
		URL url = getClass().getResource("/demo-vertx.zip");
		assertThat(prepareUrl(url, null))
			.isEqualTo(url);
	}

	@Test
	public void testPrepareUrl_generateIfInputNull() throws MojoExecutionException {
		assertThat(prepareUrl(null, "0.8"))
			.isNotNull()
			.hasNoParameters();
	}

	@Test
	@Ignore
	public void testPrepareUrl_failedIfBadVersion() throws MojoExecutionException {
		exception.expect(MojoExecutionException.class);
		prepareUrl(null, "?#~@");
	}

	@Test
	public void testDownloadKaitai_invalidZipContent() throws IOException, MojoExecutionException {
		exception.expect(MojoExecutionException.class);
		exception.expectMessage(containsString("Fail to find start script"));
		Path cache = temporaryFolder.newFolder().toPath();
		downloadKaitai(getClass().getResource("/demo-vertx.zip"), cache, LOG);
	}
}
