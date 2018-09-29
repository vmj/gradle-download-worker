package fi.linuxbox.gradle.download.worker

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

class DownloadWorkerTask extends DefaultTask {
    private final WorkerExecutor workerExecutor

    private String from
    private Object to
    private Integer connectTimeout
    private Integer readTimeout

    @Inject
    DownloadWorkerTask(final WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
        this.getOutputs().upToDateWhen { task -> false }
    }

    @TaskAction
    void fetch() throws MalformedURLException {
        final Params params = new Params(
                getFrom(),
                getTo(),
                getConnectTimeout(),
                getReadTimeout())

        workerExecutor.submit(Download.class, { config ->
            config.setIsolationMode(IsolationMode.NONE)
            config.setParams(params)
        })
    }

    @Input
    URI getFrom() {
        final i = this.from?.indexOf(':')
        if (i in [4, 5] && this.from[0..i-1] in ['http', 'https']) {
            try {
                return new URI(this.from)
            } catch (final Exception e) {
                throw new RuntimeException("invalid 'from' URL", e)
            }
        }
        throw new RuntimeException("invalid 'from' URL")
    }

    void setFrom(final String from) {
        this.from = from
    }

    @OutputFile
    File getTo() {
        return getProject().file(this.to)
    }

    void setTo(final Object to) {
        this.to = to
    }

    @Internal
    Integer getConnectTimeout() {
        return connectTimeout != null ? connectTimeout : 30000
    }

    void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout
    }

    @Internal
    Integer getReadTimeout() {
        return readTimeout != null ? readTimeout : 30000
    }

    void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout
    }
}
