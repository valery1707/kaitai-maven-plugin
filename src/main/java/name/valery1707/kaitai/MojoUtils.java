package name.valery1707.kaitai;

import name.valery1707.download.ProgressListener;
import name.valery1707.download.manager.StandardDownloadManager;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public final class MojoUtils {
	private MojoUtils() {
	}

	public static void checkDirectoryIsReadable(File target) throws MojoExecutionException {
		if (!target.isDirectory() || !target.canRead()) {
			throw new MojoExecutionException(format(
				"Fail to read from directory: %s"
				, target.getAbsolutePath()
			));
		}
	}

	public static List<File> scanFiles(File root, String[] includes, String[] excludes) throws MojoExecutionException {
		checkDirectoryIsReadable(root);
		FileFilter filter = FileFilterUtils.and(
			new WildcardFileFilter(includes)
			, FileFilterUtils.notFileFilter(new WildcardFileFilter(excludes))
		);
		File[] files = root.listFiles(filter);
		if (files != null) {
			return asList(files);
		} else {
			return emptyList();
		}
	}

	public static void checkDirectoryIsWritable(File target) throws MojoExecutionException {
		if (!target.isDirectory() || !target.canWrite()) {
			throw new MojoExecutionException(format(
				"Fail to write into directory: %s"
				, target.getAbsolutePath()
			));
		}
	}

	public static void mkdirs(File target) throws MojoExecutionException {
		if (!target.exists()) {
			if (!target.mkdirs()) {
				throw new MojoExecutionException(format(
					"Fail to create directory: %s"
					, target.getAbsolutePath()
				));
			}
		}
		checkDirectoryIsWritable(target);
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
				, path.toAbsolutePath().toFile().getAbsolutePath()
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

	public static void download(URL source, Path target, ProgressListener progressListener) throws MojoExecutionException {
		if (Files.exists(target)) {
			return;
		}
		Path temp = target.resolveSibling(target.getFileName().toString() + ".tmp");
		delete(temp);
		try {
			StandardDownloadManager.builder()
				.build()
				.download(source, temp, progressListener);
		} catch (IOException e) {
			throw new MojoExecutionException(format(
				"Fail to download '%s' into '%s'"
				, source, temp.normalize().toFile().getAbsolutePath()
			)
				, e
			);
		}
		move(temp, target);
	}
}
