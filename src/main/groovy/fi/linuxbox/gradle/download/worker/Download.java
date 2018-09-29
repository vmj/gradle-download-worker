package fi.linuxbox.gradle.download.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

public class Download implements Runnable {
    private final Logger log = LoggerFactory.getLogger(Download.class);

    private final Params params;

    @Inject
    public Download(final Params params) {
        this.params = params;
    }

    @Override
    public void run() {
        try {
            fetch();
        } catch (UnknownHostException e) {
            throw new RuntimeException("unknown host: " + e.getMessage(), e);
        } catch (SocketTimeoutException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("download failed", e);
        }
    }

    private void fetch() throws IOException {
        final HttpURLConnection cnx = urlConnection(params.getFrom());

        if (params.getTo().exists())
            cnx.setIfModifiedSince(params.getTo().lastModified());

        cnx.connect(); // both timeouts thrown from here

        final int responseCode = cnx.getResponseCode();

        logHeaders(cnx.getHeaderFields());

        if (responseCode >= 400) {
            throw new RuntimeException("HTTP error: " + responseCode);
        }
        if (responseCode >= 300) {
            // as long as we don't touch the output file,
            // gradle seems to figure out the next task is up-to-date
            log.info("up-to-date");
            // Task is not serializable; can't add reference to it.
            // This task is always out of date.  Would be nice to have
            // UP-TO-DATE next to the task.
            //task.setDidWork(false);
            return;
        }

        write(cnx.getInputStream(), new FileOutputStream(params.getTo()), cnx.getContentLengthLong());

        if (!params.getTo().setLastModified(cnx.getLastModified()))
            log.warn("unable to set modification time; up-to-date checks may not work");
    }

    /**
     * Returns a configured URL connection.
     *
     * @param url URL to open
     * @return A URL connection
     * @throws IOException in case opening the connection fails.
     */
    private HttpURLConnection urlConnection(final URL url) throws IOException {
        final HttpURLConnection cnx = (HttpURLConnection) url.openConnection();

        cnx.setAllowUserInteraction(false);
        cnx.setDoInput(true);
        cnx.setDoOutput(false);
        cnx.setUseCaches(true);

        cnx.setConnectTimeout(params.getConnectTimeout());
        cnx.setReadTimeout(params.getReadTimeout());

        return cnx;
    }

    private void write(final InputStream inputStream,
                       final OutputStream outStream,
                       final long contentLength) throws IOException {
        byte[] buffer = new byte[contentLength > 0 ? (int)(contentLength % Integer.MAX_VALUE) : 256 * 1024];
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
            if (contentLength > 0 && contentLength != totalWritten) {
                log.warn("closing streams before full write: content " + contentLength + ", r:" + totalRead + "/w:" + totalWritten + " (bytes)");
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

    private void logHeaders(final Map<String, List<String>> headerFields) {
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
