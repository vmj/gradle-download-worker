package fi.linuxbox.gradle.download.worker;

import java.io.File;
import java.io.Serializable;
import java.net.URL;

class Params implements Serializable {
    private final URL from;
    private final File to;
    private final int connectTimeout;
    private final int readTimeout;

    public Params(URL from, File to, int connectTimeout, int readTimeout) {
        this.from = from;
        this.to = to;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public URL getFrom() {
        return from;
    }

    public File getTo() {
        return to;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }
}
