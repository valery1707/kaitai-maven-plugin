package name.valery1707.kaitai;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.URL;

import static name.valery1707.kaitai.KaitaiMojo.prepareUrl;
import static org.assertj.core.api.Assertions.assertThat;

public class KaitaiMojoTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();

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
}
