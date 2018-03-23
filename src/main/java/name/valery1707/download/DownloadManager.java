package name.valery1707.download;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public interface DownloadManager {
	void download(InputSupplier input, OutputConsumer output, ProgressListener listener);

	void download(URL input, Path file, ProgressListener listener) throws IOException;
}
