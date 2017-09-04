package fi.linuxbox.gradle.download.worker;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
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

    @Inject
    public DownloadWorkerTask(final WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor;
        this.getOutputs().upToDateWhen(task -> false);
    }

    @TaskAction
    public void fetch() throws MalformedURLException {
        final URL url = getFrom();
        final File dest = getTo();

        workerExecutor.submit(Download.class, config -> {
            config.setIsolationMode(IsolationMode.NONE);
            config.setParams(url, dest);
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
}
