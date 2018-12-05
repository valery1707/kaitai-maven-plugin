package name.valery1707.kaitai;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.removeStart;

@SuppressWarnings("WeakerAccess")
public final class MojoUtils {
	private MojoUtils() {
	}

	public static void checkFileIsReadable(Path target) throws MojoExecutionException {
		if (!Files.isRegularFile(target) || !Files.isReadable(target)) {
			throw new MojoExecutionException(format(
				"Fail to read file: %s"
				, target.normalize().toFile().getAbsolutePath()
			));
		}
	}

	public static void checkFileIsExecutable(Path target) throws MojoExecutionException {
		checkFileIsReadable(target);
		if (!Files.isExecutable(target)) {
			try {
				Set<PosixFilePermission> perms = new HashSet<>(
					Files.getPosixFilePermissions(
						target,
						LinkOption.NOFOLLOW_LINKS
					)
				);
				perms.add(PosixFilePermission.OWNER_EXECUTE);
				Files.setPosixFilePermissions(target, perms);
			} catch (IOException e) {
				throw new MojoExecutionException(format(
					"Fail to set executable flag to file: %s"
					, target.normalize().toFile().getAbsolutePath()
				));
			}
		}
		if (!Files.isExecutable(target)) {
			throw new MojoExecutionException(format(
				"Fail to execute file: %s"
				, target.normalize().toFile().getAbsolutePath()
			));
		}
	}

	public static void checkDirectoryIsReadable(Path target) throws MojoExecutionException {
		if (!Files.isDirectory(target) || !Files.isReadable(target)) {
			throw new MojoExecutionException(format(
				"Fail to read from directory: %s"
				, target.normalize().toFile().getAbsolutePath()
			));
		}
	}

	public static List<Path> scanFiles(Path root, String[] includes, String[] excludes) throws MojoExecutionException {
		checkDirectoryIsReadable(root);
		FileFilter filter = FileFilterUtils.and(
			new WildcardFileFilter(includes)
			, FileFilterUtils.notFileFilter(new WildcardFileFilter(excludes))
		);
		try {
			ArrayList<Path> list = new ArrayList<>();
			Files.walkFileTree(root.normalize(), new FilterFileVisitor(filter, list));
			return list;
		} catch (IOException e) {
			throw new MojoExecutionException(format(
				"Fail to scan directory: %s"
				, root.normalize().toFile().getAbsolutePath()
			)
				, e
			);
		}
	}

	public static void checkDirectoryIsWritable(Path target) throws MojoExecutionException {
		if (!Files.isDirectory(target) || !Files.isWritable(target)) {
			throw new MojoExecutionException(format(
				"Fail to write into directory: %s"
				, target.normalize().toFile().getAbsolutePath()
			));
		}
	}

	public static Path mkdirs(Path target) throws MojoExecutionException {
		target = target.normalize();
		if (!Files.exists(target)) {
			try {
				Files.createDirectories(target);
			} catch (IOException e) {
				throw new MojoExecutionException(format(
					"Fail to create directory: %s"
					, target.normalize().toFile().getAbsolutePath()
				)
					, e
				);
			}
		}
		checkDirectoryIsWritable(target);
		return target;
	}

	private static void delete(Path path) throws MojoExecutionException {
		if (!Files.exists(path)) {
			return;
		}
		try {
			Files.delete(path);
		} catch (IOException e) {
			throw new MojoExecutionException(format(
				"Fail to delete: %s"
				, path.normalize().toFile().getAbsolutePath()
			)
				, e
			);
		}
	}

	private static void move(Path source, Path target) throws MojoExecutionException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			throw new MojoExecutionException(format(
				"Fail to move '%s' into '%s'"
				, source.normalize().toFile().getAbsolutePath()
				, target.normalize().toFile().getAbsolutePath()
			)
				, e
			);
		}
	}

	@SuppressWarnings("UnnecessarySemicolon")
	public static void download(URL source, Path target, Log log) throws MojoExecutionException {
		if (Files.exists(target)) {
			return;
		}
		Path temp = target.resolveSibling(target.getFileName().toString() + ".tmp");
		delete(temp);
		log.info(format(
			"KaiTai distribution: Downloading: %s"
			, source
		));
		try (
			InputStream is = source.openStream();
			OutputStream os = Files.newOutputStream(temp, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		) {
			IOUtils.copy(is, os);
		} catch (IOException e) {
			throw new MojoExecutionException(format(
				"Fail to download '%s' into '%s'"
				, source
				, temp.normalize().toFile().getAbsolutePath()
			)
				, e
			);
		}
		move(temp, target);
	}

	@SuppressWarnings("UnnecessarySemicolon")
	public static Path unpack(Path zip, Log log) throws MojoExecutionException {
		String filename = zip.getFileName().toString();
		String extension = FilenameUtils.getExtension(filename);
		Path dir = zip.resolveSibling(filename.replace("." + extension, ""));
		if (Files.isDirectory(dir)) {
			return dir;
		}
		log.info(format(
			"KaiTai distribution: Extracting: %s"
			, zip.normalize().toFile().getAbsolutePath()
		));
		Path temp = dir.resolveSibling(dir.getFileName().toString() + "-tmp");
		delete(temp);
		try (
			InputStream is = Files.newInputStream(zip);
			ZipInputStream zis = new ZipInputStream(is);
		) {
			Files.createDirectory(temp);
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}
				Path current = temp.resolve(removeStart(entry.getName(), "/"));
				Files.createDirectories(current.getParent());
				Files.copy(zis, current, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			throw new MojoExecutionException(format(
				"Fail to extract content of '%s'"
				, zip.normalize().toFile().getAbsolutePath()
			)
				, e
			);
		}
		move(temp, dir);
		return dir;
	}

	private static class FilterFileVisitor extends SimpleFileVisitor<Path> {
		private final FileFilter filter;
		private final List<Path> target;

		private FilterFileVisitor(FileFilter filter, List<Path> target) {
			this.filter = filter;
			this.target = target;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
			if (filter.accept(file.toFile())) {
				target.add(file.normalize().toAbsolutePath());
			}
			return FileVisitResult.CONTINUE;
		}
	}
}
