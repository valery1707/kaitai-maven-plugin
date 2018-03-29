package name.valery1707.kaitai;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.maven.plugin.logging.Log;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogWriter extends Writer {
	public enum Mode {
		INFO,
		ERROR,
	}

	private final Log log;
	private final Mode mode;

	private LogWriter(Log log, Mode mode) {
		this.log = log;
		this.mode = mode;
	}

	public static OutputStream log(Log log, Mode mode) {
		return new WriterOutputStream(new LogWriter(log, mode), StandardCharsets.UTF_8, 1024, true);
	}

	public static OutputStream logInfo(Log log) {
		return log(log, Mode.INFO);
	}

	public static OutputStream logError(Log log) {
		return log(log, Mode.ERROR);
	}

	private static final Pattern NEW_LINE = Pattern.compile("[\\n\\r]");

	@Override
	public void write(char[] cbuf, int off, int len) {
		String lines = new String(cbuf, off, len).trim();
		for (String line : NEW_LINE.split(lines)) {
			log(line.trim());
		}
	}

	private void log(String line) {
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
