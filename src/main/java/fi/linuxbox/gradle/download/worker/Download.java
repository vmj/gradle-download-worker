package fi.linuxbox.gradle.download.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public class Download implements Runnable {
    private final Logger log = LoggerFactory.getLogger(Download.class);

    private final URL from;
    private final File to;

    @Inject
    public Download(final URL from, final File to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public void run() {
        try {
            fetch();
        } catch (IOException e) {
            throw new RuntimeException("download failed", e);
        }
    }

    private void fetch() throws IOException {
        final URLConnection cnx = urlConnection(from);

        if (to.exists())
            cnx.setIfModifiedSince(to.lastModified());

        cnx.connect();

        log(cnx.getHeaderFields());

        final long contentLength = cnx.getContentLengthLong();

        if (contentLength <= 0) {
            // as long as we don't touch the output file,
            // gradle seems to figure out the next task is up-to-date
            log.info("up-to-date");
            // Task is not serializable; can't add reference to it.
            // This task is always out of date.  Would be nice to have
            // UP-TO-DATE next to the task.
            //task.setDidWork(false);
            return;
        }

        write(cnx.getInputStream(), new FileOutputStream(to), contentLength);

        if (!to.setLastModified(cnx.getLastModified()))
            log.warn("unable to set modification time; up-to-date checks may not work");
    }

    /**
     * Returns a configured URL connection.
     *
     * @param url URL to open
     * @return A URL connection
     * @throws IOException in case opening the connection fails.
     */
    private URLConnection urlConnection(final URL url) throws IOException {
        final URLConnection cnx = url.openConnection();

        cnx.setAllowUserInteraction(false);
        cnx.setDoInput(true);
        cnx.setDoOutput(false);
        cnx.setUseCaches(true);

        return cnx;
    }

    private void write(final InputStream inputStream,
                       final OutputStream outStream,
                       final long contentLength) throws IOException {
        byte[] buffer = new byte[(int)(contentLength % Integer.MAX_VALUE)];
        int bytesRead;
        int totalRead = 0;
        int totalWritten = 0;
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalRead += bytesRead;
                outStream.write(buffer, 0, bytesRead);
                totalWritten += bytesRead;
            }
        } finally {
            if (contentLength != totalWritten) {
                log.warn("closing streams before full write, r:" + totalRead + "/w:" + totalWritten + " (bytes)");
            }
            try {
                inputStream.close();
            } catch (final IOException e) {
                log.warn("unable to close response stream");
            }
            try {
                outStream.close();
            } catch (final IOException e) {
                log.warn("unable to close file stream");
            }
        }
    }

    private void log(final Map<String, List<String>> headerFields) {
        for (final String headerValue : headerFields.get(null)) {
            log.debug("> " + headerValue);
        }
        for (final String headerField : headerFields.keySet()) {
            if (headerField != null) {
                for (final String headerValue : headerFields.get(headerField)) {
                    log.debug("> " + headerField + ": " + headerValue);
                }
            }
        }
    }
}
