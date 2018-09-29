package fi.linuxbox.gradle.download.worker

import groovy.transform.PackageScope

@PackageScope
class Params implements Serializable {
    private final URL from
    private final File to
    private final int connectTimeout
    private final int readTimeout

    Params(URL from, File to, int connectTimeout, int readTimeout) {
        this.from = from
        this.to = to
        this.connectTimeout = connectTimeout
        this.readTimeout = readTimeout
    }

    URL getFrom() {
        return from
    }

    File getTo() {
        return to
    }

    int getConnectTimeout() {
        return connectTimeout
    }

    int getReadTimeout() {
        return readTimeout
    }
}
