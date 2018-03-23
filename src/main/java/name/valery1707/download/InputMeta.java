package name.valery1707.download;

import java.io.InputStream;

public interface InputMeta {
	long skip();

	InputStream stream();

	long total();
}
