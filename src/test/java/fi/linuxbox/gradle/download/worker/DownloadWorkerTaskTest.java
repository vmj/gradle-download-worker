package fi.linuxbox.gradle.download.worker;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;
import static org.junit.Assert.assertEquals;

public class DownloadWorkerTaskTest {
    private static final String BUILD_FILE = "build.gradle";

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    private File buildFile;

    @Before
    public void setup() throws IOException {
        buildFile = testProjectDir.newFile(BUILD_FILE);
    }

    @Test
    public void testTaskType() throws IOException {
        // given: build file contents
        final String buildFileContents =
          "plugins {\n"
        + "  id 'fi.linuxbox.download.worker'\n"
        + "}\n\n"
        + "import fi.linuxbox.gradle.download.worker.DownloadWorkerTask\n"
        + "task('fetch-Slackware64-13.0', type: DownloadWorkerTask) {\n"
        + "  from 'http://ftp.osuosl.org/pub/slackware/slackware-13.0/ChangeLog.txt'\n"
        + "  to new File(buildDir, 'changelogs/slackware/13.0/ChangeLog.txt')\n"
        + "}\n\n";

        // when:
        final BuildResult result = build("help", buildFileContents);

        // then:
        assertEquals(result.task(":help").getOutcome(), SUCCESS);
    }

    @Test
    public void testFetch() throws IOException {
        // given: build file contents
        final String buildFileContents =
          "plugins {\n"
        + "  id 'fi.linuxbox.download.worker'\n"
        + "}\n\n"
        + "import fi.linuxbox.gradle.download.worker.DownloadWorkerTask\n"
        + "task('fetch-Slackware64-13.0', type: DownloadWorkerTask) {\n"
        + "  from 'http://ftp.osuosl.org/pub/slackware/slackware-13.0/ChangeLog.txt'\n"
        + "  to new File(buildDir, 'changelogs/slackware/13.0/ChangeLog.txt')\n"
        + "}\n\n";

        // when:
        final BuildResult result = build("fetch-Slackware64-13.0", buildFileContents);

        // then:
        assertEquals(SUCCESS, result.task(":fetch-Slackware64-13.0").getOutcome());

        // This will not work until worker API allows task.setDidWork(false)
        //final BuildResult upToDate = gradle("fetch-Slackware64-13.0");
        //assertEquals(UP_TO_DATE, upToDate.task(":fetch-Slackware64-13.0").getOutcome());
    }

    private BuildResult build(final String arguments, final String buildFileContent) throws IOException {
        writeFile(buildFile, buildFileContent);
        return gradle(arguments);
    }

    private BuildResult gradle(final String arguments) {
        return GradleRunner
                .create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(arguments)
                .forwardOutput()
                .withPluginClasspath()
                .withDebug(true)
                .build();
    }

    private void writeFile(File destination, String content) throws IOException {
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(destination));
            output.write(content);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
}
