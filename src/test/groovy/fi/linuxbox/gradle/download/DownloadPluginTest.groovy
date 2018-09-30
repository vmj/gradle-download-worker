package fi.linuxbox.gradle.download

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class DownloadPluginTest {
    @Test
    void testApply() {
        final Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply("fi.linuxbox.download")
    }
}
