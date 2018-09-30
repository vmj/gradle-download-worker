package fi.linuxbox.gradle.download

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import javax.inject.Inject

@Slf4j
@CompileStatic
class Download implements Runnable {
    private final Params params

    @Inject
    Download(final Params params) {
        this.params = params
    }

    @Override
    void run() {
        try {
            fetch()
        } catch (final UnknownHostException e) {
            throw new RuntimeException("unknown host: ${e.message}", e)
        } catch (final SocketTimeoutException e) {
            throw new RuntimeException(e.message, e)
        } catch (final IOException e) {
            throw new RuntimeException("download failed", e)
        }
    }

    private void fetch() throws IOException {
        final HttpURLConnection cnx = urlConnection(params.from)

        if (params.to.exists())
            cnx.ifModifiedSince = params.to.lastModified()

        cnx.connect() // both timeouts thrown from here

        final int responseCode = cnx.responseCode

        logHeaders(cnx.headerFields)

        if (responseCode >= 400) {
            throw new RuntimeException("HTTP error: $responseCode")
        }
        if (responseCode >= 300) {
            // as long as we don't touch the output file,
            // gradle seems to figure out the next task is up-to-date
            log.info("up-to-date")
            // Task is not serializable; can't add reference to it.
            // This task is always out of date.  Would be nice to have
            // UP-TO-DATE next to the task.
            //task.setDidWork(false);
            return
        }

        cnx.inputStream.with { final inputStream ->
            params.to.newOutputStream().with { final outputStream ->
                outputStream << inputStream
            }
        }

        if (!params.to.setLastModified(cnx.lastModified))
            log.warn("unable to set modification time; up-to-date checks may not work")
    }

    /**
     * Returns a configured URL connection.
     *
     * @param url URL to open
     * @return A URL connection
     * @throws IOException in case opening the connection fails.
     */
    private HttpURLConnection urlConnection(final URL url) throws IOException {
        final HttpURLConnection cnx = (HttpURLConnection) url.openConnection()

        cnx.allowUserInteraction = false
        cnx.doInput = true
        cnx.doOutput = false
        cnx.useCaches = true

        cnx.connectTimeout = params.connectTimeout
        cnx.readTimeout = params.readTimeout

        return cnx
    }

    private static void logHeaders(final Map<String, List<String>> headerFields) {
        // Non-standard map implementation: allows null key
        headerFields.get(null).each {
            log.debug("> $it")
        }
        headerFields.keySet().each { final headerField ->
            headerFields.get(headerField).each { final headerValue ->
                log.debug("> $headerField: $headerValue")
            }
        }
    }
}
