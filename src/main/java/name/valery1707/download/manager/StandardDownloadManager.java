package name.valery1707.download.manager;

import name.valery1707.download.*;
import name.valery1707.download.retry.SingleTry;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.*;

public class StandardDownloadManager implements DownloadManager {
	private final Executor executor;
	private final RetryRule retryRule;

	public StandardDownloadManager(Executor executor, RetryRule retryRule) {
		this.executor = executor;
		this.retryRule = retryRule;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public void download(InputSupplier input, OutputConsumer output, ProgressListener listener) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void download(URL input, Path file, ProgressListener listener) throws IOException {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	public static class Builder {
		public enum ExecutorMode {
			SINGLE,
			CACHED,
		}

		public enum Retry {
			SINGLE,
		}

		private ThreadFactory threadFactory;
		private ExecutorMode executorMode;
		private Retry retry = Retry.SINGLE;

		public Builder threadFactory(ThreadFactory threadFactory) {
			this.threadFactory = threadFactory;
			return this;
		}

		public Builder executor(ExecutorMode executorMode) {
			this.executorMode = requireNonNull(executorMode, "executorMode is null");
			return this;
		}

		public Builder retry(Retry retry) {
			this.retry = requireNonNull(retry, "retry is null");
			return this;
		}

		public StandardDownloadManager build() {
			if (threadFactory == null) {
				threadFactory = defaultThreadFactory();
			}
			Executor executor;
			switch (executorMode) {
				case CACHED:
					executor = newCachedThreadPool(threadFactory);
					break;
				case SINGLE:
					executor = newSingleThreadExecutor(threadFactory);
					break;
				default:
					throw new IllegalStateException("executorMode is unknown: " + executorMode);
			}
			RetryRule retryRule;
			switch (retry) {
				case SINGLE:
					retryRule = new SingleTry();
					break;
				default:
					throw new IllegalStateException("retry is unknown: " + retry);
			}
			return new StandardDownloadManager(executor, retryRule);
		}
	}
}
