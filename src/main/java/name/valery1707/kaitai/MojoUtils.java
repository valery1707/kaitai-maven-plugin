package name.valery1707.kaitai;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public final class MojoUtils {
	private MojoUtils() {
	}

	public static void checkDirectoryIsReadable(File target) throws MojoExecutionException {
		if (!target.isDirectory() || !target.canRead()) {
			throw new MojoExecutionException("Fail to read from directory: " + target.getAbsolutePath());
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
			throw new MojoExecutionException("Fail to write into directory: " + target.getAbsolutePath());
		}
	}

	public static void mkdirs(File target) throws MojoExecutionException {
		if (!target.exists()) {
			if (!target.mkdirs()) {
				throw new MojoExecutionException("Fail to create directory: " + target.getAbsolutePath());
			}
		}
		checkDirectoryIsWritable(target);
	}
}
