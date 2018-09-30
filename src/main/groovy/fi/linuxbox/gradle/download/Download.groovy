package fi.linuxbox.gradle.download

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

@CompileStatic
class Download extends DefaultTask {
    private final static Integer DEFAULT_CONNECT_TIMEOUT = 30_000
    private final static Integer DEFAULT_READ_TIMEOUT = 30_000
    private final WorkerExecutor workerExecutor

    @Input
    final Property<String> from = project.objects.property(String)

    @OutputFile
    final RegularFileProperty to = newOutputFile()

    @Internal
    final Property<Integer> connectTimeout = project.objects.property(Integer)

    @Internal
    final Property<Integer> readTimeout = project.objects.property(Integer)

    @Inject
    Download(final WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
        this.connectTimeout.set(DEFAULT_CONNECT_TIMEOUT)
        this.readTimeout.set(DEFAULT_READ_TIMEOUT)
        this.getOutputs().upToDateWhen { task -> false }
    }

    @TaskAction
    void fetch() {
        final Params params = new Params(
                from.get().toURL(),
                to.get().asFile,
                connectTimeout.getOrElse(DEFAULT_CONNECT_TIMEOUT),
                readTimeout.getOrElse(DEFAULT_READ_TIMEOUT))

        workerExecutor.submit(DownloadRunnable.class) { final config ->
            config.setIsolationMode(IsolationMode.NONE)
            config.setParams(params)
        }
    }

    void from(final String url) {
        this.from.set(url)
    }

    void to(final Object to) {
        if (to instanceof Provider)
            this.to.set(to as Provider)
        else
            this.to.set(project.file(to))
    }

    void connectTimeout(final Integer connectTimeout) {
        this.connectTimeout.set(connectTimeout)
    }

    void readTimeout(final Integer readTimeout) {
        this.readTimeout.set(readTimeout)
    }
}
