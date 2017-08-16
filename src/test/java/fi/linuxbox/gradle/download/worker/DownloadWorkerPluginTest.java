package fi.linuxbox.gradle.download.worker;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

public class DownloadWorkerPluginTest {
    @Test
    public void testApply() {
        final Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("fi.linuxbox.download.worker");
    }
}
