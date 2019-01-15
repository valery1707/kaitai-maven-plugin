package name.valery1707.kaitai;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.buildobjects.process.ExternalProcessFailureException;
import org.buildobjects.process.ProcBuilder;
import org.buildobjects.process.StartupException;
import org.buildobjects.process.TimeoutException;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableSet;
import static name.valery1707.kaitai.KaitaiUtils.*;

@SuppressWarnings("WeakerAccess")
public class KaitaiGenerator {
	private final Path kaitai;
	private final Path output;
	private final String packageName;
	private final Set<Path> sources = new LinkedHashSet<>();
	private boolean overwrite = false;
	private final ByteArrayOutputStream streamError = new ByteArrayOutputStream(256);
	private final ByteArrayOutputStream streamOutput = new ByteArrayOutputStream(256);
	private long executionTimeout = 5_000;

	/**
	 * Build {@code KaitaiGenerator} with preconfigured state.
	 *
	 * @param kaitai      Path to kaitai compiler runner
	 * @param output      Path to output directory
	 * @param packageName Package name for Java-classes
	 * @return New {@code KaitaiGenerator}
	 * @throws KaitaiException If compiler is not executable or output is not writable
	 */
	public static KaitaiGenerator generator(Path kaitai, Path output, String packageName) throws KaitaiException {
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

	/**
	 * Add path to kaitai specification from {@code Iterable}.
	 *
	 * @param source Path to kaitai specification
	 * @return self
	 * @throws KaitaiException If path is not readable
	 */
	public KaitaiGenerator withSource(Path source) throws KaitaiException {
		checkFileIsReadable(source);
		this.sources.add(source);
		return this;
	}

	/**
	 * Add paths to kaitai specification from {@code Iterable}.
	 *
	 * @param sources Iterable with paths to kaitai specification
	 * @return self
	 * @throws KaitaiException If any path is not readable
	 */
	public KaitaiGenerator withSource(Iterable<Path> sources) throws KaitaiException {
		for (Path source : sources) {
			withSource(source);
		}
		return this;
	}

	public KaitaiGenerator withSource(Path... sources) throws KaitaiException {
		return withSource(Arrays.asList(sources));
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

	public long getExecutionTimeout() {
		return executionTimeout;
	}

	public void setExecutionTimeout(long executionTimeout) {
		this.executionTimeout = executionTimeout;
	}

	public KaitaiGenerator executionTimeout(long executionTimeout) {
		setExecutionTimeout(executionTimeout);
		return this;
	}

	private ProcBuilder process(Logger log) {
		ProcBuilder builder = new ProcBuilder(getKaitai().normalize().toAbsolutePath().toString())
			.withErrorStream(new TeeOutputStream(LogWriter.logError(log), streamError))
			.withOutputStream(new TeeOutputStream(LogWriter.logInfo(log), streamOutput))
			.withExpectedExitStatuses(0);
		if (getExecutionTimeout() < 0) {
			builder.withNoTimeout();
		} else {
			builder.withTimeoutMillis(getExecutionTimeout());
		}
		return builder;
	}

	private void execute(ProcBuilder builder) throws KaitaiException {
		try {
			streamError.reset();
			streamOutput.reset();
			builder.run();
		} catch (StartupException | TimeoutException | ExternalProcessFailureException e) {
			throw new KaitaiException(
				"Fail to execute kaitai command: "
					+ streamError.toString(UTF_8)
					+ streamOutput.toString(UTF_8)
				, e
			);
		}
	}

	/**
	 * Start generation process.
	 *
	 * @param log Logger for messages
	 * @return Root directory with generated files
	 * @throws KaitaiException If any exception occurs on compile step
	 */
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
