package fi.linuxbox.gradle.download.worker

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class DownloadWorkerTaskTest extends Specification {
    private static final Set<String> gradleVersions = ['4.1', '4.0']

    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
    }

    @Unroll
    def "task type is ok with Gradle version #gradleVersion"() {
        given:
        buildFile << """
        plugins {
          id 'fi.linuxbox.download.worker'
        }
        import fi.linuxbox.gradle.download.worker.DownloadWorkerTask
        task('fetch-Slackware64-13.0', type: DownloadWorkerTask) {
          from 'http://ftp.osuosl.org/pub/slackware/slackware-13.0/ChangeLog.txt'
          to new File(buildDir, 'changelogs/slackware/13.0/ChangeLog.txt')
        }
        """

        when:
        def result = GradleRunner
                .create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.root)
                .withArguments("help")
                .withPluginClasspath()
                .build()

        then:
        result.task(":help").outcome == SUCCESS

        where:
        gradleVersion << gradleVersions
    }

    @Unroll
    def "can execute download with Gradle version #gradleVersion"() {
        given:
        buildFile << """
        plugins {
          id 'fi.linuxbox.download.worker'
        }
        import fi.linuxbox.gradle.download.worker.DownloadWorkerTask
        task('fetch-Slackware64-13.0', type: DownloadWorkerTask) {
          from 'http://ftp.osuosl.org/pub/slackware/slackware-13.0/ChangeLog.txt'
          to new File(buildDir, 'changelogs/slackware/13.0/ChangeLog.txt')
        }
        """

        when:
        def result = GradleRunner
                .create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.root)
                .withArguments("fetch-Slackware64-13.0")
                .withPluginClasspath()
                .build()

        then:
        result.task(":fetch-Slackware64-13.0").outcome == SUCCESS

        where:
        gradleVersion << gradleVersions
    }
}
