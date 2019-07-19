package name.valery1707.kaitai;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static name.valery1707.kaitai.KaitaiMojo.KAITAI_VERSION;
import static name.valery1707.kaitai.KaitaiUtils.*;
import static org.apache.commons.io.FilenameUtils.getName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.CoreMatchers.containsString;
import static org.slf4j.helpers.NOPLogger.NOP_LOGGER;

public class KaitaiUtilsTest {
	private static final Logger LOG = NOP_LOGGER;

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public ExpectedException exception = ExpectedException.none();

	static Path copy(String resource, Path file) throws IOException {
		Files.createDirectories(file.getParent());
		Files.copy(KaitaiUtilsTest.class.getResourceAsStream(resource), file, StandardCopyOption.REPLACE_EXISTING);
		return file;
	}

	static Path copy(String resource, TemporaryFolder temporaryFolder) throws IOException {
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
	public void testCreateTempDirectory() throws KaitaiException {
		Path temp = createTempDirectory("temp-");
		assertThat(temp)
			.exists()
			.isDirectory()
			.isReadable()
		;
		assertThat(temp.toFile().listFiles()).isNotNull().isEmpty();
	}

	private void testDelete(Path path) throws KaitaiException {
		assertThat(path).exists();
		delete(path);
		assertThat(path).doesNotExist();
	}

	private void testDelete_root(FileSystem fs) {
		try {
			testDelete(fs.getRootDirectories().iterator().next());
			fail("Must generate exception because root can't be deleted");
		} catch (KaitaiException e) {
			assertThat(e).hasMessageStartingWith("Fail to delete: ");
			assertThat(e.getCause()).isNotNull().hasMessageContaining("can not delete root");
		}
	}

	@Test
	public void testDelete_root_Linux() throws IOException {
		FileSystem fs = MemoryFileSystemBuilder.newLinux().build();
		testDelete_root(fs);
	}

	@Test
	public void testDelete_root_Windows() throws IOException {
		FileSystem fs = MemoryFileSystemBuilder.newWindows().build();
		testDelete_root(fs);
	}

	@Test
	public void testDelete_file_Linux() throws IOException, KaitaiException {
		FileSystem fs = MemoryFileSystemBuilder.newLinux().build();
		testDelete(Files.createFile(fs.getPath(".", "temp.file")));
	}

	@Test
	public void testDelete_file_Windows() throws IOException, KaitaiException {
		FileSystem fs = MemoryFileSystemBuilder.newWindows().build();
		testDelete(Files.createFile(fs.getPath(".", "temp.file")));
	}

	private void testDelete_fileLocked(FileSystem fs) throws IOException {
		Path locked = Files.createFile(fs.getPath(".", "temp.file"));
		try (InputStream ignored = Files.newInputStream(locked)) {
			testDelete(locked);
			fail("Must generate exception because file is locked");
		} catch (KaitaiException e) {
			assertThat(e).hasMessageStartingWith("Fail to delete:").hasMessageEndingWith(locked.getFileName().toString());
			assertThat(e.getCause()).hasMessageContaining("file still open");
		}
	}

	@Test
	public void testDelete_fileLocked_Linux() throws IOException {
		FileSystem fs = MemoryFileSystemBuilder.newLinux().build();
		testDelete_fileLocked(fs);
	}

	@Test
	public void testDelete_fileLocked_Windows() throws IOException {
		FileSystem fs = MemoryFileSystemBuilder.newWindows().build();
		testDelete_fileLocked(fs);
	}

	private Path createPathTree(Path root, Map<String[], Character> content) throws IOException {
		for (Map.Entry<String[], Character> entry : content.entrySet()) {
			String[] paths = entry.getKey();
			char type = entry.getValue();
			Path path = root;
			for (String part : paths) {
				path = path.resolve(part);
			}
			switch (type) {
				case 'f':
					Files.createFile(path);
					break;
				case 'd':
					Files.createDirectory(path);
					break;
				default:
					throw new IllegalArgumentException("Invalid path type '" + type + "' for:" + path);
			}
		}
		return root;
	}

	private static final Map<String[], Character> directorySimple = new LinkedHashMap<String[], Character>() {{
		put(new String[]{"nested1.file"}, 'f');
		put(new String[]{"nested1-path"}, 'd');
		put(new String[]{"nested1-path", "nested4.file"}, 'f');
		put(new String[]{"nested1-path", "nested5.file"}, 'f');
		put(new String[]{"nested1-path", "nested6.file"}, 'f');
		put(new String[]{"nested2.file"}, 'f');
		put(new String[]{"nested2-path"}, 'd');
		put(new String[]{"nested2-path", "nested7.file"}, 'f');
		put(new String[]{"nested2-path", "nested8.file"}, 'f');
		put(new String[]{"nested2-path", "nested9.file"}, 'f');
		put(new String[]{"nested3.file"}, 'f');
	}};

	private void testDelete_dirFilled(Path root, Map<String[], Character> content) throws KaitaiException, IOException {
		testDelete(createPathTree(root, content));
	}

	@Test
	public void testDelete_dirEmpty_Linux() throws IOException, KaitaiException {
		FileSystem fs = MemoryFileSystemBuilder.newLinux().build();
		testDelete_dirFilled(
			Files.createDirectory(fs.getPath(".", "temp-dir")),
			Collections.<String[], Character>emptyMap()
		);
	}

	@Test
	public void testDelete_dirEmpty_Windows() throws IOException, KaitaiException {
		FileSystem fs = MemoryFileSystemBuilder.newWindows().build();
		testDelete_dirFilled(
			Files.createDirectory(fs.getPath(".", "temp-dir")),
			Collections.<String[], Character>emptyMap()
		);
	}

	@Test
	public void testDelete_dirFilled_Linux() throws IOException, KaitaiException {
		FileSystem fs = MemoryFileSystemBuilder.newLinux().build();
		testDelete_dirFilled(
			Files.createDirectory(fs.getPath(".", "temp-dir")),
			directorySimple
		);
	}

	@Test
	public void testDelete_dirFilled_Windows() throws IOException, KaitaiException {
		FileSystem fs = MemoryFileSystemBuilder.newWindows().build();
		testDelete_dirFilled(
			Files.createDirectory(fs.getPath(".", "temp-dir")),
			directorySimple
		);
	}

	private void testDelete_dirLocked(FileSystem fs) throws IOException {
		Path root = Files.createDirectory(fs.getPath(".", "temp-dir"));
		try (InputStream ignored = Files.newInputStream(Files.createFile(root.resolve("test.lock")))) {
			testDelete_dirFilled(
				root,
				directorySimple
			);
			fail("Must generate exception because directory contains locked file");
		} catch (KaitaiException e) {
			assertThat(e).hasMessageStartingWith("Fail to delete:").hasMessageEndingWith(root.getFileName().toString());
			assertThat(e.getCause()).hasMessageContaining("file still open");
		}
	}

	@Test
	public void testDelete_dirLocked_Linux() throws IOException {
		FileSystem fs = MemoryFileSystemBuilder.newLinux().build();
		testDelete_dirLocked(fs);
	}

	@Test
	public void testDelete_dirLocked_Windows() throws IOException {
		FileSystem fs = MemoryFileSystemBuilder.newWindows().build();
		testDelete_dirLocked(fs);
	}

	private void testMoveSingle_absent(FileSystem fs) {
		try {
			move(fs.getPath(".", "absent-1.file"), fs.getPath(".", "absent-2.file"));
			fail("Must generate exception because source path is absent");
		} catch (KaitaiException e) {
			assertThat(e).hasMessageStartingWith("Fail to move").hasMessageContaining("absent-1.file").hasMessageContaining("absent-2.file");
			assertThat(e.getCause()).hasMessageContaining("absent-1.file");
		}
	}

	@Test
	public void testMoveSingle_absent_Linux() throws IOException {
		FileSystem fs = MemoryFileSystemBuilder.newLinux().build();
		testMoveSingle_absent(fs);
	}

	@Test
	public void testMoveSingle_absent_Windows() throws IOException {
		FileSystem fs = MemoryFileSystemBuilder.newWindows().build();
		testMoveSingle_absent(fs);
	}

	private void testMoveSingle_exists2absent(FileSystem fs) throws KaitaiException, IOException {
		byte[] content = IOUtils.toByteArray(getClass().getResourceAsStream("/executable/_timeout.bat"));
		Path source = Files.write(fs.getPath(".", "source.file"), content);
		Path target = fs.getPath(".", "target.file");
		assertThat(source).exists().hasBinaryContent(content);
		assertThat(target).doesNotExist();
		move(source, target);
		assertThat(source).doesNotExist();
		assertThat(target).exists().hasBinaryContent(content);
	}

	@Test
	public void testMoveSingle_exists2absent_Linux() throws IOException, KaitaiException {
		FileSystem fs = MemoryFileSystemBuilder.newLinux().build();
		testMoveSingle_exists2absent(fs);
	}

	@Test
	public void testMoveSingle_exists2absent_Windows() throws IOException, KaitaiException {
		FileSystem fs = MemoryFileSystemBuilder.newWindows().build();
		testMoveSingle_exists2absent(fs);
	}

	private void testMoveSingle_exists2exists(FileSystem fs) throws KaitaiException, IOException {
		byte[] contentS = IOUtils.toByteArray(getClass().getResourceAsStream("/executable/_timeout.bat"));
		byte[] contentT = IOUtils.toByteArray(getClass().getResourceAsStream("/executable/_timeout.sh"));
		Path source = Files.write(fs.getPath(".", "source.file"), contentS);
		Path target = Files.write(fs.getPath(".", "target.file"), contentT);
		assertThat(source).exists().hasBinaryContent(contentS);
		assertThat(target).exists().hasBinaryContent(contentT);
		move(source, target);
		assertThat(source).doesNotExist();
		assertThat(target).exists().hasBinaryContent(contentS);
	}

	@Test
	public void testMoveSingle_exists2exists_Linux() throws IOException, KaitaiException {
		FileSystem fs = MemoryFileSystemBuilder.newLinux().build();
		testMoveSingle_exists2exists(fs);
	}

	@Test
	public void testMoveSingle_exists2exists_Windows() throws IOException, KaitaiException {
		FileSystem fs = MemoryFileSystemBuilder.newWindows().build();
		testMoveSingle_exists2exists(fs);
	}

	private void testMoveCollection_exists2absent(FileSystem fs) throws IOException, KaitaiException {
		Path source = createPathTree(Files.createDirectory(fs.getPath(".", "source")), directorySimple);
		Path target = fs.getPath(".", "target");
		assertThat(source).exists();
		assertThat(target).doesNotExist();
		move(source, scanFiles(source, new String[]{"*.file"}, new String[0]), target);
		//Source still has some empty directories inside itself
		assertThat(source).exists();
		assertThat(scanFiles(source, new String[]{"*"}, new String[0]))
			.isEmpty()
		;
		assertThat(target).exists();
		assertThat(scanFiles(target, new String[]{"*"}, new String[0]))
			.hasSize(9)
		;
	}

	@Test
	public void testMoveCollection_exists2absent_Linux() throws IOException, KaitaiException {
		FileSystem fs = MemoryFileSystemBuilder.newLinux().build();
		testMoveCollection_exists2absent(fs);
	}

	@Test
	public void testMoveCollection_exists2absent_Windows() throws IOException, KaitaiException {
		FileSystem fs = MemoryFileSystemBuilder.newWindows().build();
		testMoveCollection_exists2absent(fs);
	}

	@Test
	public void testUnpack() throws IOException, KaitaiException {
		Path zip = copy("/demo-vertx.zip", temporaryFolder);
		Path target = unpack(zip, LOG);
		assertThat(target).exists().isDirectory();
		assertThat(target.resolve("demo-vertx")).exists().isDirectory();
		assertThat(target.resolve("demo-vertx/pom.xml")).exists().isRegularFile();
	}

	@Test
	public void testUnpack_exists() throws IOException, KaitaiException {
		Path zip = copy("/demo-vertx.zip", temporaryFolder);
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
}
