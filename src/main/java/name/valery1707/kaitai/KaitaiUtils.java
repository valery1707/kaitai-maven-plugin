package name.valery1707.kaitai;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.removeStart;

@SuppressWarnings("WeakerAccess")
public final class KaitaiUtils {
	private KaitaiUtils() {
	}

	/**
	 * Check path for be regular readable file.
	 *
	 * @param target Path for check
	 * @throws KaitaiException If path's target is not exists or is not regular file or not readable
	 */
	public static void checkFileIsReadable(Path target) throws KaitaiException {
		if (!Files.isRegularFile(target) || !Files.isReadable(target)) {
			throw new KaitaiException(format(
				"Fail to read file: %s"
				, target.normalize().toFile().getAbsolutePath()
			));
		}
	}

	/**
	 * Check path for be regular executable file.
	 *
	 * @param target Path for check
	 * @throws KaitaiException If path's target is not exists or is not regular file or not executable
	 */
	public static void checkFileIsExecutable(Path target) throws KaitaiException {
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
				throw new KaitaiException(format(
					"Fail to set executable flag to file: %s"
					, target.normalize().toFile().getAbsolutePath()
				));
			}
		}
		if (!Files.isExecutable(target)) {
			throw new KaitaiException(format(
				"Fail to execute file: %s"
				, target.normalize().toFile().getAbsolutePath()
			));
		}
	}

	/**
	 * Check path for be regular readable directory.
	 *
	 * @param target Path for check
	 * @throws KaitaiException If path's target is not exists or is not directory or not readable
	 */
	public static void checkDirectoryIsReadable(Path target) throws KaitaiException {
		if (!Files.isDirectory(target) || !Files.isReadable(target)) {
			throw new KaitaiException(format(
				"Fail to read from directory: %s"
				, target.normalize().toFile().getAbsolutePath()
			));
		}
	}

	/**
	 * Recursively scan for files from {@code root} directory into deep and return list of founded files
	 * which matches with any {@code includes} wildcard mask and don't matches with any {@code excludes} wildcard mask.
	 *
	 * @param root     Root directory for scan
	 * @param includes Includes wildcard mask
	 * @param excludes Excludes wildcard mask
	 * @return List of matched files
	 * @throws KaitaiException If any io-exception was occurs
	 */
	public static List<Path> scanFiles(Path root, String[] includes, String[] excludes) throws KaitaiException {
		checkDirectoryIsReadable(root);
		FilenameFilter filter = FileFilterUtils.and(
			new WildcardFileFilter(includes)
			, FileFilterUtils.notFileFilter(new WildcardFileFilter(excludes))
		);
		try {
			ArrayList<Path> list = new ArrayList<>();
			Files.walkFileTree(root.normalize(), new FilterFileVisitor(filter, list));
			return list;
		} catch (IOException e) {
			throw new KaitaiException(format(
				"Fail to scan directory: %s"
				, root.normalize().toFile().getAbsolutePath()
			)
				, e
			);
		}
	}

	/**
	 * Check path for be regular writable directory.
	 *
	 * @param target Path for check
	 * @throws KaitaiException If path's target is not exists or is not directory or not writable
	 */
	public static void checkDirectoryIsWritable(Path target) throws KaitaiException {
		if (!Files.isDirectory(target) || !Files.isWritable(target)) {
			throw new KaitaiException(format(
				"Fail to write into directory: %s"
				, target.normalize().toFile().getAbsolutePath()
			));
		}
	}

	/**
	 * Create directory if it don't exists already.
	 *
	 * @param target Path for create
	 * @return Path to created directory
	 * @throws KaitaiException If directory can not be created
	 */
	public static Path mkdirs(Path target) throws KaitaiException {
		target = target.normalize();
		if (!Files.exists(target)) {
			try {
				Files.createDirectories(target);
			} catch (IOException e) {
				throw new KaitaiException(format(
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

	/**
	 * Create unique directory inside path {@code java.io.tmpdir} on default FileSystem.
	 *
	 * @param prefix     Prefix for name
	 * @param attributes Attributes for created directory
	 * @return Path to created directory
	 * @throws KaitaiException If directory can not be created
	 */
	public static Path createTempDirectory(String prefix, FileAttribute<?>... attributes) throws KaitaiException {
		try {
			return Files.createTempDirectory(prefix, attributes);
		} catch (IOException e) {
			throw new KaitaiException(
				"Fail to create temp directory"
				, e
			);
		}
	}

	/**
	 * Remove path: file or entry directory.
	 *
	 * @param path Path for delete
	 * @throws KaitaiException If directory/files can not be deleted
	 */
	public static void delete(Path path) throws KaitaiException {
		if (!Files.exists(path)) {
			return;
		}
		try {
			//todo Symbolic
			if (Files.isRegularFile(path)) {
				Files.delete(path);
			} else if (Files.isDirectory(path)) {
				Files.walkFileTree(
					path,
					new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							Files.delete(file);
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							if (exc != null) throw exc;
							Files.delete(dir);
							return FileVisitResult.CONTINUE;
						}
					}
				);
			}
		} catch (IOException e) {
			throw new KaitaiException(format(
				"Fail to delete: %s"
				, path.normalize().toAbsolutePath()
			)
				, e
			);
		}
	}

	/**
	 * Move {@code source} into {@code target} with replace exists.
	 *
	 * @param source Source path
	 * @param target Target path
	 * @throws KaitaiException If path can not be moved
	 */
	public static void move(Path source, Path target) throws KaitaiException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new KaitaiException(format(
				"Fail to move '%s' into '%s'"
				, source.normalize().toAbsolutePath()
				, target.normalize().toAbsolutePath()
			)
				, e
			);
		}
	}

	/**
	 * Move all paths from {@code items} which are inside {@code sourceRoot} into {@code targetRoot}.
	 *
	 * @param sourceRoot Source root directory
	 * @param items      Paths for moving
	 * @param targetRoot Target root directory
	 * @throws KaitaiException If some path can not be moved
	 */
	public static void move(Path sourceRoot, Collection<Path> items, Path targetRoot) throws KaitaiException {
		sourceRoot = sourceRoot.toAbsolutePath().normalize();
		for (Path source : items) {
			Path target = targetRoot.resolve(sourceRoot.relativize(source));
			mkdirs(target.getParent());
			if (Files.isRegularFile(source)) {
				move(source, target);
			} else {
				mkdirs(target);
			}
		}
	}

	/**
	 * Download content from target {@code URL} and store in {@code target} file.
	 *
	 * <p>
	 * Skipped if {@code target} file already exists.
	 *
	 * <p>
	 * Download into temporary file and atomically move into target file.
	 *
	 * @param source URL for download
	 * @param target Path to store
	 * @param log    Logger for messages
	 * @throws KaitaiException If any io-exception was occurs
	 */
	@SuppressWarnings("UnnecessarySemicolon")
	public static void download(URL source, Path target, Logger log) throws KaitaiException {
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
			throw new KaitaiException(format(
				"Fail to download '%s' into '%s'"
				, source
				, temp.normalize().toFile().getAbsolutePath()
			)
				, e
			);
		}
		move(temp, target);
	}

	/**
	 * Safe unpack archive into directory with same name as archive (without extension) with logging.
	 *
	 * <p>
	 * Skipped if {@code target} directory already exists.
	 *
	 * <p>
	 * Unpack into temporary directory and atomically move into target directory.
	 *
	 * @param zip Path to archive
	 * @param log Logger for messages
	 * @return Path to directory with unpacked content
	 * @throws KaitaiException If any io-exception was occurs
	 */
	@SuppressWarnings("UnnecessarySemicolon")
	public static Path unpack(Path zip, Logger log) throws KaitaiException {
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
			throw new KaitaiException(format(
				"Fail to extract content of '%s'"
				, zip.normalize().toFile().getAbsolutePath()
			)
				, e
			);
		}
		move(temp, dir);
		return dir;
	}

	private static final String URL_FORMAT = "https://dl.bintray.com/kaitai-io/universal/%s/kaitai-struct-compiler-%s.zip";

	/**
	 * Use external {@code url} if it non null or create new url from template.
	 *
	 * @param url     External configured URL
	 * @param version Suggested version
	 * @return Nonnull URL for kaitai compiler distribution
	 * @throws KaitaiException If version broke URL format
	 */
	public static URL prepareUrl(URL url, String version) throws KaitaiException {
		if (url == null) {
			try {
				return new URL(format(URL_FORMAT, version, version));
			} catch (MalformedURLException e) {
				throw new KaitaiException("Invalid version: " + version, e);
			}
		} else {
			return url;
		}
	}

	/**
	 * Prepare directory for cache.
	 *
	 * @param cache Path for cache directory
	 * @param log   Logger for messages
	 * @return Path to created directory
	 * @throws KaitaiException If any io-exception was occurs
	 */
	public static Path prepareCache(Path cache, Logger log) throws KaitaiException {
		log.debug(format(
			"KaiTai distribution: Prepare cache directory: %s"
			, cache.normalize().toFile().getAbsolutePath()
		));
		return mkdirs(cache);
	}

	private static final String KAITAI_START_SCRIPT = "kaitai-struct-compiler.bat";
	private static final Map<Boolean, String> SCRIPT_SUFFIX_REMOVER = Collections.unmodifiableMap(new HashMap<Boolean, String>() {
		{
			put(true, "");
			put(false, ".bat");
		}
	});

	/**
	 * Download, cache and unpack distribution of kaitai compiler.
	 *
	 * <p>
	 * Download step will be skipped if file was already downloaded before.
	 *
	 * <p>
	 * Unpack step will be skipped if file was already unpacked before.
	 *
	 * @param url      URL of distribution
	 * @param cacheDir Directory for caching
	 * @param log      Logger for messages
	 * @return Path into kaitai compiler executable
	 * @throws KaitaiException If any io-exception was occurs
	 */
	public static Path downloadKaitai(URL url, Path cacheDir, Logger log) throws KaitaiException {
		Path distZip = cacheDir.resolve(FilenameUtils.getName(url.getFile()));
		download(url, distZip, log);
		Path dist = unpack(distZip, log);
		List<Path> bats = scanFiles(dist, new String[]{KAITAI_START_SCRIPT}, new String[0]);
		if (bats.size() != 1) {
			throw new KaitaiException(format(
				"Fail to find start script '%s' in Kaitai distribution: %s"
				, KAITAI_START_SCRIPT
				, dist.normalize().toFile().getAbsolutePath()
			));
		}
		Path bat = bats.get(0);
		String suffixToRemove = SCRIPT_SUFFIX_REMOVER.get(SystemUtils.IS_OS_WINDOWS);
		return bat.resolveSibling(bat.getFileName().toString().replace(suffixToRemove, ""));
	}

	private static class FilterFileVisitor extends SimpleFileVisitor<Path> {
		private final FilenameFilter filter;
		private final List<Path> target;

		private FilterFileVisitor(FilenameFilter filter, List<Path> target) {
			this.filter = filter;
			this.target = target;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
			if (filter.accept(null, file.getFileName().toString())) {
				target.add(file.normalize().toAbsolutePath());
			}
			return FileVisitResult.CONTINUE;
		}
	}
}
