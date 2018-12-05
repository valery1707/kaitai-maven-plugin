package name.valery1707.kaitai;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import static name.valery1707.kaitai.MojoUtils.checkFileIsExecutable;
import static name.valery1707.kaitai.MojoUtils.mkdirs;
import static org.apache.commons.io.FilenameUtils.getName;
import static org.assertj.core.api.Assertions.assertThat;

public class MojoUtilsTest {
	private static final SystemStreamLog LOG = new SystemStreamLog();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private Path copy(String resource, Path file) throws IOException {
		Files.createDirectories(file.getParent());
		Files.copy(getClass().getResourceAsStream(resource), file, StandardCopyOption.REPLACE_EXISTING);
		return file;
	}

	private Path copy(String resource) throws IOException {
		return copy(resource, temporaryFolder.newFolder().toPath().resolve(getName(resource)));
	}

	private void testExecutable(FileSystem fs) throws MojoExecutionException, IOException {
		Path tempDir = mkdirs(fs.getPath("tmp", "test-executable"));
		Path script = Files.createFile(tempDir.resolve("script.sh"));
		checkFileIsExecutable(script);
	}

	@Test
	public void testExecutable_unix() throws MojoExecutionException, IOException {
		Set<PosixFilePermission> reverseMask = EnumSet.of(
			PosixFilePermission.OWNER_EXECUTE,
			PosixFilePermission.GROUP_EXECUTE,
			PosixFilePermission.OTHERS_EXECUTE
		);
		testExecutable(MemoryFileSystemBuilder.newLinux().setUmask(reverseMask).build());
	}

	@Test
	public void testExecutable_windows() throws MojoExecutionException, IOException {
		testExecutable(MemoryFileSystemBuilder.newWindows().build());
	}

	@Test
	public void testScanFiles() throws MojoExecutionException {
		List<Path> files = MojoUtils.scanFiles(new File(".").toPath(), new String[]{"*.java"}, new String[]{"Log*.java", "*Test.java"});
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
				new File(".").toPath().resolve("src/test/java/name/valery1707/kaitai/MojoUtilsTest.java")
			)
		;
	}

	@Test
	public void testUnpack() throws IOException, MojoExecutionException {
		Path zip = copy("/demo-vertx.zip");
		Path target = MojoUtils.unpack(zip, LOG);
		assertThat(target).exists().isDirectory();
		assertThat(target.resolve("demo-vertx")).exists().isDirectory();
		assertThat(target.resolve("demo-vertx/pom.xml")).exists().isRegularFile();
	}

	@Test
	public void testUnpack_exists() throws IOException, MojoExecutionException {
		Path zip = copy("/demo-vertx.zip");
		Files.createDirectories(zip.resolveSibling("demo-vertx"));
		Path target = MojoUtils.unpack(zip, LOG);
		assertThat(target).exists().isDirectory();
		assertThat(target.resolve("demo-vertx")).doesNotExist();
		assertThat(target.resolve("demo-vertx/pom.xml")).doesNotExist();
	}

	@Test
	public void testUnpack_zipEntry_withPathStartingWithSlash() throws IOException, MojoExecutionException {
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
		Path target = MojoUtils.unpack(zip, LOG);
		assertThat(target).exists().isDirectory();

		//malicious directory at root path
		assertThat(target.resolve("/bin-hack")).doesNotExist();

		//Valid directory inside our directory
		assertThat(target.resolve("bin-hack")).exists().isDirectory();
		assertThat(target.resolve("bin-hack/bash")).exists().isRegularFile();
	}

	@Test
	public void testDownload_success() throws IOException, MojoExecutionException, NoSuchAlgorithmException {
		Path target = temporaryFolder.newFile("assertj-core-2.9.0.jar").toPath();
		Files.delete(target);
		URL source = new URL("https://search.maven.org/remotecontent?filepath=org/assertj/assertj-core/2.9.0/assertj-core-2.9.0.jar");
		MojoUtils.download(source, target, LOG);
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
	public void testDownload_noDownloadIfExists() throws IOException, MojoExecutionException {
		Path target = temporaryFolder.newFile("assertj-core-2.9.0.jar").toPath();
		URL source = new URL("https://search.maven.org/remotecontent?filepath=org/assertj/assertj-core/2.9.0/assertj-core-2.9.0.jar");
		MojoUtils.download(source, target, LOG);
		assertThat(target)
			.exists()
			.isRegularFile()
			.isReadable()
			.hasBinaryContent(new byte[0]);
	}

	@Test(expected = MojoExecutionException.class)
	public void testDownload_404() throws IOException, MojoExecutionException {
		Path target = temporaryFolder.newFile("assertj-core-2.9.0.jar").toPath();
		Files.delete(target);
		URL source = new URL("https://search.maven.org/remotecontent?filepath=org/assertj/assertj-core/2.7.0/assertj-core-2.9.0.jar");
		MojoUtils.download(source, target, LOG);
		throw new IllegalStateException("Unreachable statement");
	}
}
