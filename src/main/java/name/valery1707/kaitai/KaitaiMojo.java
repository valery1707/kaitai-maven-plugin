package name.valery1707.kaitai;

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
	 * @since 0.1.0
	 */
	@Parameter(property = "kaitai.version", defaultValue = "0.8")
	private String version;

	/**
	 * @since 0.1.0
	 */
	@Parameter(property = "kaitai.url")
	private URL url;

	/**
	 * @since 0.1.0
	 */
	@Parameter(property = "kaitai.cache")
	private File cacheDir;

	/**
	 * @since 0.1.0
	 */
	@Parameter(property = "kaitai.source", defaultValue = "${project.build.sourceDirectory}//kaitai")
	private File sourceDirectory;

	/**
	 * @since 0.1.0
	 */
	private String[] includes;

	/**
	 * @since 0.1.0
	 */
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

		//todo Scan source files

		//todo Add generated directory into Maven's build scope

		if (url == null) {
			try {
				url = new URL(String.format(URL_FORMAT, version));
			} catch (MalformedURLException e) {
				throw new MojoExecutionException("Invalid version: " + version, e);
			}
		}

		if (cacheDir == null) {
			Path repository = new File(session.getLocalRepository().getBasedir()).toPath();
			cacheDir = repository.resolve(".cache").resolve("kaitai").normalize().toFile();
			if (!cacheDir.exists()) {
				if (!cacheDir.mkdir()) {
					throw new MojoExecutionException("Fail to create cache directory: " + cacheDir.getAbsolutePath());
				}
			}
		}
		if (!cacheDir.isDirectory() || !cacheDir.canWrite()) {
			throw new MojoExecutionException("Fail to use cache directory: " + cacheDir.getAbsolutePath());
		}

		//todo Download Kaitai distribution into cache and unzip it

		//todo Check for exists files

		//todo Generate Java sources
	}
}
