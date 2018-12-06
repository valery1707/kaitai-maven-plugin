package name.valery1707.kaitai;

import org.apache.maven.plugin.MojoExecutionException;
import org.buildobjects.process.ExternalProcessFailureException;
import org.buildobjects.process.ProcBuilder;
import org.buildobjects.process.StartupException;
import org.buildobjects.process.TimeoutException;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static name.valery1707.kaitai.IoUtils.*;

@SuppressWarnings("WeakerAccess")
public class KaitaiGenerator {
	private final Path kaitai;
	private final Path output;
	private final String packageName;
	private final Set<Path> sources = new LinkedHashSet<>();
	private boolean overwrite = false;

	public static KaitaiGenerator generator(Path kaitai, Path output, String packageName) throws MojoExecutionException {
		checkFileIsExecutable(kaitai);
		checkDirectoryIsWritable(output);
		return new KaitaiGenerator(kaitai, output, packageName);
	}

	private KaitaiGenerator(Path kaitai, Path output, String packageName) {
		this.kaitai = kaitai;
		this.output = output;
		this.packageName = packageName;
	}

	public Path getKaitai() {
		return kaitai;
	}

	public Path getOutput() {
		return output;
	}

	public String getPackageName() {
		return packageName;
	}

	public Set<Path> getSources() {
		return unmodifiableSet(sources);
	}

	public KaitaiGenerator withSource(Path source) throws MojoExecutionException {
		checkFileIsReadable(source);
		this.sources.add(source);
		return this;
	}

	public KaitaiGenerator withSource(Iterable<Path> sources) throws MojoExecutionException {
		for (Path source : sources) {
			withSource(source);
		}
		return this;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public KaitaiGenerator overwrite(boolean overwrite) {
		this.overwrite = overwrite;
		return this;
	}

	private ProcBuilder process(Logger log) {
		return new ProcBuilder(getKaitai().normalize().toAbsolutePath().toString())
			.withErrorStream(LogWriter.logError(log))
			.withOutputStream(LogWriter.logInfo(log))
			.withExpectedExitStatuses(0)
			;
	}

	private void execute(ProcBuilder builder) throws KaitaiException {
		try {
			builder.run();
		} catch (StartupException | TimeoutException | ExternalProcessFailureException e) {
			throw new KaitaiException(
				"Fail to execute kaitai command"
				, e
			);
		}
	}

	public Path generate(Logger log) throws KaitaiException {
		if (!isOverwrite()) {
			//todo Remove exists file from source
		}

		log.info("Kaitai: check version");
		execute(this
			.process(log)
			.withArg("--version")
		);

		ProcBuilder builder = process(log)
			.withArgs("--target", "java")
			.withArgs("--outdir", getOutput().normalize().toFile().getAbsolutePath())
			.withArgs("--java-package", getPackageName());

		for (Path source : getSources()) {
			builder.withArg(source.normalize().toFile().getAbsolutePath());
		}

		log.info("Kaitai: generate");
		execute(builder);
		return getOutput().resolve("src");
	}
}
