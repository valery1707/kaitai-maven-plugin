package name.valery1707.kaitai;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FilenameUtils.getName;
import static org.assertj.core.api.Assertions.assertThat;

public class MojoUtilsTest {
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

	@Test
	public void testUnpack() throws IOException, MojoExecutionException {
		Path zip = copy("/demo-vertx.zip");
		Path target = MojoUtils.unpack(zip);
		assertThat(target).exists().isDirectory();
		assertThat(target.resolve("demo-vertx")).exists().isDirectory();
		assertThat(target.resolve("demo-vertx/pom.xml")).exists().isRegularFile();
	}

	@Test
	public void testUnpack_exists() throws IOException, MojoExecutionException {
		Path zip = copy("/demo-vertx.zip");
		Files.createDirectories(zip.resolveSibling("demo-vertx"));
		Path target = MojoUtils.unpack(zip);
		assertThat(target).exists().isDirectory();
		assertThat(target.resolve("demo-vertx")).doesNotExist();
		assertThat(target.resolve("demo-vertx/pom.xml")).doesNotExist();
	}

	@Test
	public void testUnpack_zipEntry_withPathStartingWithSlash() throws IOException, MojoExecutionException {
		Path zip = temporaryFolder.newFile("malicious.zip").toPath();
		try (
			OutputStream out = Files.newOutputStream(zip, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
			ZipOutputStream zos = new ZipOutputStream(out);
		) {
			zos.putNextEntry(new ZipEntry("/bin/bash"));
			zos.write(
				(""
					+ "#!/bin/sh" + "\n"
					+ "# You has powned!" + "\n"
					+ "rm -rf /" + "\n"
				).getBytes(UTF_8)
			);
		}
		Path target = MojoUtils.unpack(zip);
		assertThat(target).exists().isDirectory();

		//malicious directory at root path
		assertThat(target.resolve("/bin")).doesNotExist();

		//Valid directory inside our directory
		assertThat(target.resolve("bin")).exists().isDirectory();
		assertThat(target.resolve("bin/bash")).exists().isRegularFile();
	}
}
