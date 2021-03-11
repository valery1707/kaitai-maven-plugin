package name.valery1707.kaitai;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang3.SystemUtils;
import org.buildobjects.process.ExternalProcessFailureException;
import org.buildobjects.process.ProcBuilder;
import org.buildobjects.process.StartupException;
import org.buildobjects.process.TimeoutException;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableSet;
import static name.valery1707.kaitai.KaitaiUtils.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;

@SuppressWarnings("WeakerAccess")
public class KaitaiGenerator {
	private final Path kaitai;
	private final Path output;
	private final String packageName;
	private final Set<Path> sources = new LinkedHashSet<>();
	private boolean overwrite = false;
	private boolean exactOutput = false;
	private final ByteArrayOutputStream streamError = new ByteArrayOutputStream(256);
	private final ByteArrayOutputStream streamOutput = new ByteArrayOutputStream(256);
	private long executionTimeout = 5_000;
	private String fromFileClass;
	private Boolean opaqueTypes;
	private boolean noVersionCheck;
	private boolean noAutoRead;

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
		setOverwrite(overwrite);
		return this;
	}

	public boolean isExactOutput() {
		return exactOutput;
	}

	public void setExactOutput(boolean exactOutput) {
		this.exactOutput = exactOutput;
	}

	public KaitaiGenerator exactOutput(boolean exactOutput) {
		setExactOutput(exactOutput);
		return this;
	}

	/**
	 * Get configured execution timeout value.
	 *
	 * @return Current execution timeout value
	 */
	public long getExecutionTimeout() {
		return executionTimeout;
	}

	/**
	 * Set execution timeout value.
	 *
	 * @param executionTimeout New value
	 */
	public void setExecutionTimeout(long executionTimeout) {
		this.executionTimeout = executionTimeout;
	}

	/**
	 * Set execution timeout value.
	 *
	 * @param executionTimeout New value
	 * @return self
	 */
	public KaitaiGenerator executionTimeout(long executionTimeout) {
		setExecutionTimeout(executionTimeout);
		return this;
	}

	/**
	 * Get configured classname with custom KaitaiStream implementations.
	 *
	 * @return Configured classname with custom KaitaiStream implementations
	 */
	public String getFromFileClass() {
		return fromFileClass;
	}

	/**
	 * Set new classname with custom KaitaiStream implementations.
	 *
	 * @param fromFileClass Classname with custom KaitaiStream implementations
	 */
	public void setFromFileClass(String fromFileClass) {
		this.fromFileClass = trimToNull(fromFileClass);
	}

	/**
	 * Set new classname with custom KaitaiStream implementations.
	 *
	 * @param fromFileClass Classname with custom KaitaiStream implementations
	 * @return self
	 */
	public KaitaiGenerator fromFileClass(String fromFileClass) {
		setFromFileClass(fromFileClass);
		return this;
	}

	/**
	 * Get opaque types mode.
	 *
	 * @return Opaque types mode
	 */
	public Boolean getOpaqueTypes() {
		return opaqueTypes;
	}

	/**
	 * Set opaque types mode.
	 *
	 * @param opaqueTypes Opaque types mode
	 */
	public void setOpaqueTypes(Boolean opaqueTypes) {
		this.opaqueTypes = opaqueTypes;
	}

	/**
	 * Set opaque types mode.
	 *
	 * @param opaqueTypes Opaque types mode
	 * @return self
	 */
	public KaitaiGenerator opaqueTypes(Boolean opaqueTypes) {
		setOpaqueTypes(opaqueTypes);
		return this;
	}

	/**
	 * Get version check mode.
	 *
	 * @return Version check mode
	 */
	public boolean isNoVersionCheck() {
		return noVersionCheck;
	}

	/**
	 * Set version check mode.
	 *
	 * @param noVersionCheck Version check mode
	 */
	public void setNoVersionCheck(boolean noVersionCheck) {
		this.noVersionCheck = noVersionCheck;
	}

	/**
	 * Set version check mode.
	 *
	 * @param noVersionCheck Version check mode
	 * @return self
	 */
	public KaitaiGenerator noVersionCheck(boolean noVersionCheck) {
		setNoVersionCheck(noVersionCheck);
		return this;
	}
	/**
	 * Get no auto read mode.
	 *
	 * @return Version no auto read mode
	 */
	public boolean isNoAutoRead() {
		return noAutoRead;
	}
	/**
	 * Set no auto read mode.
	 *
	 * @param noAutoRead no auto read mode
	 */
	public void setNoAutoRead(boolean noAutoRead) {
		this.noAutoRead = noAutoRead;
	}

	/**
	 * Set no auto read mode.
	 *
	 * @param noAutoRead no auto read mode
	 * @return self
	 */
	public KaitaiGenerator noAutoRead(boolean noAutoRead) {
		setNoAutoRead(noAutoRead);
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

		if (isNoVersionCheck()) {
			if (SystemUtils.IS_OS_WINDOWS) {
				log.info("Option `noVersionCheck` is ignored on Windows");
			} else {
				builder.withArgs("-no-version-check");
			}
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
					+ new String(streamError.toByteArray(), UTF_8)
					+ new String(streamOutput.toByteArray(), UTF_8)
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

		Path output = getOutput().normalize();
		if (isExactOutput()) {
			output = createTempDirectory("kaitai-" + getPackageName());
		}

		ProcBuilder builder = process(log)
			.withArgs("--target", "java")
			.withArgs("--outdir", output.toFile().getAbsolutePath())
			.withArgs("--java-package", getPackageName());
		if (isNoAutoRead()) {
			builder.withArgs("--no-auto-read");
		}
		if (isNotBlank(getFromFileClass())) {
			builder.withArgs("--java-from-file-class", getFromFileClass());
		}
		if (getOpaqueTypes() != null) {
			builder.withArgs("--opaque-types", getOpaqueTypes().toString());
		}

		for (Path source : getSources()) {
			builder.withArg(source.normalize().toFile().getAbsolutePath());
		}

		log.info("Kaitai: generate");
		execute(builder);
		output = output.resolve("src");
		if (isExactOutput()) {
			Path root = getOutput();
			List<Path> generated = scanFiles(output, new String[]{"*"}, new String[0]);
			move(output, generated, root);
			delete(output);
			return root;
		} else {
			return output;
		}
	}
}
