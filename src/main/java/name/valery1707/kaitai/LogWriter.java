package name.valery1707.kaitai;

import org.apache.commons.io.output.WriterOutputStream;
import org.slf4j.Logger;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class LogWriter extends Writer {
	public enum Mode {
		INFO,
		ERROR,
	}

	private final Logger log;
	private final Mode mode;

	private LogWriter(Logger log, Mode mode) {
		this.log = log;
		this.mode = mode;
	}

	public static OutputStream log(Logger log, Mode mode) {
		return new WriterOutputStream(new LogWriter(log, mode), StandardCharsets.UTF_8, 1024, true);
	}

	public static OutputStream logInfo(Logger log) {
		return log(log, Mode.INFO);
	}

	public static OutputStream logError(Logger log) {
		return log(log, Mode.ERROR);
	}

	private static final Pattern NEW_LINE = Pattern.compile("[\\n\\r]");

	@Override
	public void write(char[] cbuf, int off, int len) {
		String lines = new String(cbuf, off, len).trim();
		for (String line : NEW_LINE.split(lines)) {
			logImpl(line.trim());
		}
	}

	private void logImpl(String line) {
		if (line.isEmpty()) {
			return;
		}
		switch (mode) {
			case INFO:
				log.info(line);
				break;
			case ERROR:
			default:
				log.error(line);
		}
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() {
	}
}
