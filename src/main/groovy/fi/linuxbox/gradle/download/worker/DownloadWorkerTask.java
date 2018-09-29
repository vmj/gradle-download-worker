package fi.linuxbox.gradle.download.worker;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadWorkerTask extends DefaultTask {
    final WorkerExecutor workerExecutor;

    private String from;
    private Object to;
    private Integer connectTimeout;
    private Integer readTimeout;

    @Inject
    public DownloadWorkerTask(final WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor;
        this.getOutputs().upToDateWhen(task -> false);
    }

    @TaskAction
    public void fetch() throws MalformedURLException {
        final Params params = new Params(
                getFrom(),
                getTo(),
                getConnectTimeout(),
                getReadTimeout());

        workerExecutor.submit(Download.class, config -> {
            config.setIsolationMode(IsolationMode.NONE);
            config.setParams(params);
        });
    }

    @Input
    public URL getFrom() {
        try {
            return new URL(this.from);
        } catch (final MalformedURLException e) {
            throw new RuntimeException("invalid 'from' URL", e);
        }
    }

    public void setFrom(final String from) {
        this.from = from;
    }

    @OutputFile
    public File getTo() {
        return getProject().file(this.to);
    }

    public void setTo(final Object to) {
        this.to = to;
    }

    @Internal
    public Integer getConnectTimeout() {
        return connectTimeout != null ? connectTimeout : 30000;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    @Internal
    public Integer getReadTimeout() {
        return readTimeout != null ? readTimeout : 30000;
    }

    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }
}
