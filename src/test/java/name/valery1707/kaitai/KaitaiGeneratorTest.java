package name.valery1707.kaitai;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.buildobjects.process.ExternalProcessFailureException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static name.valery1707.kaitai.KaitaiMojo.KAITAI_VERSION;
import static name.valery1707.kaitai.KaitaiUtils.downloadKaitai;
import static name.valery1707.kaitai.KaitaiUtils.prepareUrl;
import static name.valery1707.kaitai.KaitaiUtilsTest.copy;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.slf4j.helpers.NOPLogger.NOP_LOGGER;

public class KaitaiGeneratorTest {
	private static final Logger LOG = NOP_LOGGER;

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private String readToString(Path source) throws IOException {
		try (Reader reader = Files.newBufferedReader(source, UTF_8)) {
			return IOUtils.toString(reader);
		}
	}

	private String capitalize(String source) {
		String[] parts = StringUtils.split(source, '_');
		for (int i = 0; i < parts.length; i++) {
			parts[i] = StringUtils.capitalize(parts[i]);
		}
		return StringUtils.join(parts);
	}

	private Path findIt() throws URISyntaxException {
		return Paths.get(getClass().getResource("/demo-vertx.zip").toURI())
			.getParent().getParent().getParent()
			.resolve("src/it");
	}

	private KaitaiGenerator generator(Path... sources) throws IOException, KaitaiException {
		Path cache = temporaryFolder.newFolder().toPath();
		Path kaitai = downloadKaitai(prepareUrl(null, KAITAI_VERSION), cache, LOG);
		Path generated = cache.resolve("generated");
		Files.createDirectory(generated);
		return KaitaiGenerator
			.generator(kaitai, generated, "name.valery1707.kaitai.test")
			.withSource(sources);
	}

	@Test
	public void testGenerate_success() throws IOException, URISyntaxException, KaitaiException {
		Path source = findIt()
			.resolve("it-source-exist/src/main/resources/kaitai/ico.ksy");
		KaitaiGenerator generator = generator(source);
		Path target = generator.generate(LOG);
		assertThat(target).isDirectory().hasFileName("src");
		Path pkg = target.resolve(generator.getPackageName().replace('.', '/'));
		assertThat(pkg).isDirectory();
		for (Path src : generator.getSources()) {
			String kaitaiName = src.getFileName().toString();
			String javaName = capitalize(removeExtension(kaitaiName)) + ".java";
			assertThat(pkg.resolve(javaName)).isRegularFile();
		}
	}

	@Test
	public void testGenerate_failed() throws IOException, URISyntaxException, KaitaiException {
		Path source = findIt()
			.resolve("it-source-failed/src/main/resources/kaitai/demo.ksy");
		KaitaiGenerator generator = generator(source);
		try {
			generator.generate(LOG);
			fail("Must generate exception because of problems in specification");
		} catch (KaitaiException e) {
			assertThat(e)
				.hasMessageContaining("/types/header/seq/0/id: invalid attribute ID: 'Magic', expected /^[a-z][a-z0-9_]*$/")
			;
			assertThat(e.getMessage()).doesNotContain(KAITAI_VERSION);

			assertThat(e.getCause())
				.isInstanceOf(ExternalProcessFailureException.class)
				.hasMessageContaining("Stderr unavailable as it has been consumed by user provided stream.")
				.hasMessageContaining("returned 2 after")
			;

			assertThat(generator.getOutput().resolve("scr")).doesNotExist();
		}
	}

	/**
	 * @return Generator targeted into executable with timeout for 1 second
	 * @throws IOException     If resource not found
	 * @throws KaitaiException If resource is invalid
	 */
	private KaitaiGenerator testExecutionTimeout() throws IOException, KaitaiException {
		Path executable = copy(
			SystemUtils.IS_OS_WINDOWS
				? "/executable/_timeout.bat"
				: "/executable/_timeout.sh"
			, temporaryFolder
		);
		return KaitaiGenerator.generator(executable, executable.getParent(), getClass().getPackage().getName());
	}

	@Test
	public void testExecutionTimeout_positiveSuccess() throws IOException, KaitaiException {
		KaitaiGenerator generator = testExecutionTimeout();
		generator.executionTimeout(2_000);
		assertThat(generator.generate(LOG)).isNotNull();
	}

	@Test
	public void testExecutionTimeout_positiveFailure() throws IOException, KaitaiException {
		KaitaiGenerator generator = testExecutionTimeout();
		generator.executionTimeout(500);
		try {
			assertThat(generator.generate(LOG)).isNotNull();
			fail("Must throw exception");
		} catch (KaitaiException e) {
			assertThat(e.getCause())
				.isInstanceOf(org.buildobjects.process.TimeoutException.class)
				.hasMessageContaining("timed out after 500ms")
			;
		}
	}

	@Test
	public void testExecutionTimeout_negativeSuccess() throws IOException, KaitaiException {
		KaitaiGenerator generator = testExecutionTimeout();
		generator.executionTimeout(-1);
		assertThat(generator.generate(LOG)).isNotNull();
	}

	@Test
	public void testOption_fromFileClass() throws URISyntaxException, IOException, KaitaiException {
		Path source = findIt()
			.resolve("it-source-exist/src/main/resources/kaitai/ico.ksy");
		String fromFileClassName = "TestStream";
		String fromFileClass = "name.valery1707.kaitai.test" + "." + fromFileClassName;
		KaitaiGenerator generator = generator(source)
			.fromFileClass(fromFileClass);
		Path target = generator.generate(LOG);
		assertThat(target).isDirectory().hasFileName("src");
		Path pkg = target.resolve(generator.getPackageName().replace('.', '/'));
		assertThat(pkg).isDirectory();
		for (Path src : generator.getSources()) {
			String kaitaiName = src.getFileName().toString();
			String javaName = capitalize(removeExtension(kaitaiName)) + ".java";
			Path javaFile = pkg.resolve(javaName);
			assertThat(javaFile).isRegularFile().isReadable();
			assertThat(readToString(javaFile))
				.contains(fromFileClass)
				.contains("new " + fromFileClassName + "(")
			;
		}
	}

	@Test
	public void testOption_opaqueTypes_enabled() throws URISyntaxException, IOException, KaitaiException {
		Path source = findIt()
			.resolve("it-withOption-opaqueTypes/src/main/resources/kaitai/doc_container.ksy");
		KaitaiGenerator generator = generator(source)
			.opaqueTypes(true);
		Path target = generator.generate(LOG);
		assertThat(target).isDirectory().hasFileName("src");
		Path pkg = target.resolve(generator.getPackageName().replace('.', '/'));
		assertThat(pkg).isDirectory();
		for (Path src : generator.getSources()) {
			String kaitaiName = src.getFileName().toString();
			String javaName = capitalize(removeExtension(kaitaiName)) + ".java";
			Path javaFile = pkg.resolve(javaName);
			assertThat(javaFile).isRegularFile().isReadable();
			assertThat(readToString(javaFile))
				.contains("new CustomEncryptedObject(this._io)")
			;
		}
	}

	@Test
	public void testOption_opaqueTypes_disabled() throws URISyntaxException, IOException, KaitaiException {
		Path source = findIt()
			.resolve("it-withOption-opaqueTypes/src/main/resources/kaitai/doc_container.ksy");
		KaitaiGenerator generator = generator(source)
			.opaqueTypes(false);
		try {
			generator.generate(LOG);
			fail("Must generate exception because of problems in specification");
		} catch (KaitaiException e) {
			assertThat(e)
				.hasMessageContaining("/seq/0: unable to find type 'custom_encrypted_object', searching from doc_container")
			;
			assertThat(e.getMessage()).doesNotContain(KAITAI_VERSION);

			assertThat(e.getCause())
				.isInstanceOf(ExternalProcessFailureException.class)
				.hasMessageContaining("Stderr unavailable as it has been consumed by user provided stream.")
				.hasMessageContaining("returned 2 after")
			;

			assertThat(generator.getOutput().resolve("scr")).doesNotExist();
		}
	}
}
