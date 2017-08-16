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
        final URL url = new URL(this.from);
        final File dest = getProject().file(this.to);

        workerExecutor.submit(Download.class, config -> {
            config.setIsolationMode(IsolationMode.NONE);
            config.setParams(url, dest);
        });
    }

    @Input
    public String getFrom() {
        return from;
    }

    public void setFrom(final String from) {
        this.from = from;
    }

    @OutputFile
    public Object getTo() {
        return to;
    }

    public void setTo(final Object to) {
        this.to = to;
    }
}
