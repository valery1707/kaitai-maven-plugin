package name.valery1707.kaitai;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static name.valery1707.kaitai.KaitaiMojo.*;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.slf4j.helpers.NOPLogger.NOP_LOGGER;

public class KaitaiMojoTest {
	private static final Logger LOG = NOP_LOGGER;

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
	public void testPrepareCache_useExternal() throws IOException, MojoExecutionException {
		File folder = temporaryFolder.newFolder();
		assertThat(prepareCache(folder, null, LOG))
			.isDirectory()
			.isWritable()
			.hasFileName(folder.getName())
		;
	}

	@Test
	public void testDownloadKaitai_invalidZipContent() throws IOException, MojoExecutionException {
		exception.expect(MojoExecutionException.class);
		exception.expectMessage(containsString("Fail to find start script"));
		Path cache = temporaryFolder.newFolder().toPath();
		downloadKaitai(getClass().getResource("/demo-vertx.zip"), cache, LOG);
	}

	@Test
	public void testGenerate() throws IOException, MojoExecutionException, URISyntaxException, KaitaiException {
		Path cache = temporaryFolder.newFolder().toPath();
		Path kaitai = downloadKaitai(prepareUrl(null, "0.8"), cache, LOG);
		Path source = Paths.get(getClass().getResource("/demo-vertx.zip").toURI())
			.getParent().getParent().getParent()
			.resolve("src/it")
			.resolve("it-source-exist/src/main/resources/kaitai/ico.ksy");
		Path generated = cache.resolve("generated");
		Files.createDirectory(generated);
		KaitaiGenerator generator = KaitaiGenerator
			.generator(kaitai, generated, "name.valery1707.kaitai.test")
			.withSource(source);
		Path target = generator.generate(LOG);
		assertThat(target).isDirectory().hasFileName("src");
		Path pkg = target.resolve(generator.getPackageName().replace('.', '/'));
		assertThat(pkg).isDirectory();
		for (Path src : generator.getSources()) {
			String kaitaiName = src.getFileName().toString();
			String javaName = capitalize(removeExtension(kaitaiName) + ".java");
			assertThat(pkg.resolve(javaName)).isRegularFile();
		}
	}
}
