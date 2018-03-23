package name.valery1707.kaitai;

import name.valery1707.download.ProgressListener;
import name.valery1707.download.ProgressMeta;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static name.valery1707.kaitai.MojoUtils.*;

/**
 * @see <a href="http://maven.apache.org/developers/mojo-api-specification.html">Mojo API Specification</a>
 */
@Mojo(
	name = "generate"
	, defaultPhase = LifecyclePhase.GENERATE_SOURCES
)
public class KaitaiMojo extends AbstractMojo {
	private static final String URL_FORMAT = "https://dl.bintray.com/kaitai-io/universal/%s/kaitai-struct-compiler-%s.zip";

	/**
	 * Version of <a href="http://kaitai.io/#download">KaiTai</a> library.
	 *
	 * @since 0.1.0
	 */
	@Parameter(property = "kaitai.version", defaultValue = "0.8")
	private String version;

	/**
	 * Direct link onto <a href="http://kaitai.io/#download">KaiTai universal zip archive</a>.
	 *
	 * @since 0.1.0
	 */
	@Parameter(property = "kaitai.url")
	private URL url;

	/**
	 * Cache directory for download KaiTai library.
	 *
	 * @see KaitaiMojo#version
	 * @see KaitaiMojo#url
	 * @since 0.1.0
	 */
	@Parameter(property = "kaitai.cache")
	private File cacheDir;

	/**
	 * Source directory with <a href="http://formats.kaitai.io/">Kaitai Struct language</a> files.
	 *
	 * @since 0.1.0
	 */
	@Parameter(property = "kaitai.source", defaultValue = "${project.build.sourceDirectory}/resources/kaitai")
	private File sourceDirectory;

	/**
	 * Include wildcard pattern list.
	 *
	 * @since 0.1.0
	 */
	@Parameter(property = "kaitai.includes", defaultValue = "*.ksy")
	private String[] includes;

	/**
	 * Exclude wildcard pattern list.
	 *
	 * @since 0.1.0
	 */
	@Parameter(property = "kaitai.excludes")
	private String[] excludes;

	/**
	 * Target directory for generated Java source files.
	 *
	 * @since 0.1.0
	 */
	@Parameter(property = "kaitai.output", defaultValue = "${project.build.directory}/generated-sources/kaitai")
	private File output;

	/**
	 * Skip plugin execution (don't read/validate any files, don't generate any java types).
	 *
	 * @since 0.1.0
	 */
	@Parameter(property = "kaitai.skip", defaultValue = "false")
	private boolean skip = false;

	/**
	 * Overwrite exists files in target directory.
	 *
	 * @since 0.1.0
	 */
	@Parameter(property = "kaitai.overwrite", defaultValue = "false")
	private boolean overwrite = false;

	@Component
	protected Settings settings;

	@Component
	private MavenSession session;

	@Component
	private MavenProject project;

	/**
	 * Executes the plugin, to read the given source and behavioural properties and generate POJOs.
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			return;
		}

		//Scan source files
		List<File> source = scanFiles(sourceDirectory, includes, excludes);
		if (source.isEmpty()) {
			getLog().warn("Not found any input files: skip generation step");
			return;
		}

		if (url == null) {
			try {
				url = new URL(String.format(URL_FORMAT, version, version));
			} catch (MalformedURLException e) {
				throw new MojoExecutionException("Invalid version: " + version, e);
			}
		}

		if (cacheDir == null) {
			Path repository = new File(session.getLocalRepository().getBasedir()).toPath();
			cacheDir = repository.resolve(".cache").resolve("kaitai").normalize().toFile();
		}
		mkdirs(cacheDir);

		//todo Download Kaitai distribution into cache and unzip it
		Path kaitaiDistZip = cacheDir.toPath().resolve(url.getFile());
		Path kaitaiDist = downloadAndExtract(url, kaitaiDistZip);
		Path kaitaiJar;

		mkdirs(output);
		//todo Generate Java sources
		//todo Add generated directory into Maven's build scope
		project.addCompileSourceRoot(output.getAbsolutePath());
	}

	private Path downloadAndExtract(URL url, Path zip) throws MojoExecutionException {
		download(url, zip, new ProgressListener() {
			@Override
			public void progress(ProgressMeta meta) {
				getLog().info("");
			}
		});
		return unpack(zip);
	}
}
