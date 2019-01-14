package name.valery1707.kaitai;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.buildobjects.process.ExternalProcessFailureException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static name.valery1707.kaitai.KaitaiMojo.KAITAI_VERSION;
import static name.valery1707.kaitai.KaitaiUtils.*;
import static org.apache.commons.io.FilenameUtils.getName;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.fail;
import static org.slf4j.helpers.NOPLogger.NOP_LOGGER;

public class KaitaiUtilsTest {
	private static final Logger LOG = NOP_LOGGER;

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public ExpectedException exception = ExpectedException.none();

	private Path copy(String resource, Path file) throws IOException {
		Files.createDirectories(file.getParent());
		Files.copy(getClass().getResourceAsStream(resource), file, StandardCopyOption.REPLACE_EXISTING);
		return file;
	}

	private Path copy(String resource) throws IOException {
		return copy(resource, temporaryFolder.newFolder().toPath().resolve(getName(resource)));
	}

	private void testExecutable(FileSystem fs) throws KaitaiException, IOException {
		Path tempDir = mkdirs(fs.getPath("tmp", "test-executable"));
		Path script = Files.createFile(tempDir.resolve("script.sh"));
		checkFileIsExecutable(script);
	}

	@Test
	public void testExecutable_unix() throws KaitaiException, IOException {
		Set<PosixFilePermission> reverseMask = EnumSet.of(
			PosixFilePermission.OWNER_EXECUTE,
			PosixFilePermission.GROUP_EXECUTE,
			PosixFilePermission.OTHERS_EXECUTE
		);
		testExecutable(MemoryFileSystemBuilder.newLinux().setUmask(reverseMask).build());
	}

	@Test
	public void testExecutable_windows() throws KaitaiException, IOException {
		testExecutable(MemoryFileSystemBuilder.newWindows().build());
	}

	@Test
	public void testScanFiles() throws KaitaiException {
		List<Path> files = scanFiles(new File(".").toPath(), new String[]{"*.java"}, new String[]{"Log*.java", "*Test.java"});
		assertThat(files)
			.isNotEmpty()
			.contains(
				new File(".").toPath().resolve("src/main/java/name/valery1707/kaitai/KaitaiMojo.java").normalize().toAbsolutePath()
			)
			.doesNotContain(
				new File(".").toPath().resolve("src/main/java/name/valery1707/kaitai/KaitaiMojo.java"),
				new File(".").toPath().resolve("src/main/java/name/valery1707/kaitai/KaitaiMojo.java").toAbsolutePath(),
				new File(".").toPath().resolve("src/main/java/name/valery1707/kaitai/KaitaiMojo.java").normalize(),
				new File(".").toPath().resolve("src/main/java/name/valery1707/kaitai/LogWriter.java"),
				new File(".").toPath().resolve("src/test/java/name/valery1707/kaitai/KaitaiUtilsTest.java")
			)
		;
	}

	@Test
	public void testUnpack() throws IOException, KaitaiException {
		Path zip = copy("/demo-vertx.zip");
		Path target = unpack(zip, LOG);
		assertThat(target).exists().isDirectory();
		assertThat(target.resolve("demo-vertx")).exists().isDirectory();
		assertThat(target.resolve("demo-vertx/pom.xml")).exists().isRegularFile();
	}

	@Test
	public void testUnpack_exists() throws IOException, KaitaiException {
		Path zip = copy("/demo-vertx.zip");
		Files.createDirectories(zip.resolveSibling("demo-vertx"));
		Path target = unpack(zip, LOG);
		assertThat(target).exists().isDirectory();
		assertThat(target.resolve("demo-vertx")).doesNotExist();
		assertThat(target.resolve("demo-vertx/pom.xml")).doesNotExist();
	}

	@Test
	public void testUnpack_zipEntry_withPathStartingWithSlash() throws IOException, KaitaiException {
		Path zip = temporaryFolder.newFile("malicious.zip").toPath();
		//noinspection UnnecessarySemicolon
		try (
			OutputStream out = Files.newOutputStream(zip, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
			ZipOutputStream zos = new ZipOutputStream(out);
		) {
			zos.putNextEntry(new ZipEntry("/bin-hack/bash"));
			zos.write(
				(""
					+ "#!/bin/sh" + "\n"
					+ "# You has powned!" + "\n"
					+ "rm -rf /" + "\n"
				).getBytes(UTF_8)
			);
		}
		Path target = unpack(zip, LOG);
		assertThat(target).exists().isDirectory();

		//malicious directory at root path
		assertThat(target.resolve("/bin-hack")).doesNotExist();

		//Valid directory inside our directory
		assertThat(target.resolve("bin-hack")).exists().isDirectory();
		assertThat(target.resolve("bin-hack/bash")).exists().isRegularFile();
	}

	@Test
	public void testDownload_success() throws IOException, KaitaiException, NoSuchAlgorithmException {
		Path target = temporaryFolder.newFile("assertj-core-2.9.0.jar").toPath();
		Files.delete(target);
		URL source = new URL("https://search.maven.org/remotecontent?filepath=org/assertj/assertj-core/2.9.0/assertj-core-2.9.0.jar");
		download(source, target, LOG);
		assertThat(target).exists().isRegularFile().isReadable();

		MessageDigest digest = MessageDigest.getInstance("SHA1");
		Files.copy(target, new DigestOutputStream(new NullOutputStream(), digest));
		String hashActual = Hex.encodeHexString(digest.digest());

		//noinspection UnnecessarySemicolon
		try (
			InputStream shaExpected = new URL(source.toExternalForm() + ".sha1").openStream();
		) {
			for (String hashExpected : IOUtils.readLines(shaExpected, StandardCharsets.UTF_8)) {
				assertThat(hashActual).isEqualToIgnoringCase(hashExpected);
			}
		}
	}

	@Test
	public void testDownload_noDownloadIfExists() throws IOException, KaitaiException {
		Path target = temporaryFolder.newFile("assertj-core-2.9.0.jar").toPath();
		URL source = new URL("https://search.maven.org/remotecontent?filepath=org/assertj/assertj-core/2.9.0/assertj-core-2.9.0.jar");
		download(source, target, LOG);
		assertThat(target)
			.exists()
			.isRegularFile()
			.isReadable()
			.hasBinaryContent(new byte[0]);
	}

	@Test(expected = KaitaiException.class)
	public void testDownload_404() throws IOException, KaitaiException {
		Path target = temporaryFolder.newFile("assertj-core-2.9.0.jar").toPath();
		Files.delete(target);
		URL source = new URL("https://search.maven.org/remotecontent?filepath=org/assertj/assertj-core/2.7.0/assertj-core-2.9.0.jar");
		download(source, target, LOG);
		throw new IllegalStateException("Unreachable statement");
	}

	@Test
	public void testPrepareUrl_returnInputIfNonNull() throws KaitaiException {
		URL url = getClass().getResource("/demo-vertx.zip");
		assertThat(prepareUrl(url, null))
			.isEqualTo(url);
	}

	@Test
	public void testPrepareUrl_generateIfInputNull() throws KaitaiException {
		assertThat(prepareUrl(null, KAITAI_VERSION))
			.isNotNull()
			.hasNoParameters();
	}

	@Test
	@Ignore
	public void testPrepareUrl_failedIfBadVersion() throws KaitaiException {
		exception.expect(KaitaiException.class);
		prepareUrl(null, "?#~@");
	}

	@Test
	public void testPrepareCache_useExternal() throws IOException, KaitaiException {
		File folder = temporaryFolder.newFolder();
		assertThat(prepareCache(folder.toPath(), LOG))
			.isDirectory()
			.isWritable()
			.hasFileName(folder.getName())
		;
	}

	@Test
	public void testDownloadKaitai_invalidZipContent() throws IOException, KaitaiException {
		exception.expect(KaitaiException.class);
		exception.expectMessage(containsString("Fail to find start script"));
		Path cache = temporaryFolder.newFolder().toPath();
		downloadKaitai(getClass().getResource("/demo-vertx.zip"), cache, LOG);
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
			String javaName = capitalize(removeExtension(kaitaiName) + ".java");
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
}
