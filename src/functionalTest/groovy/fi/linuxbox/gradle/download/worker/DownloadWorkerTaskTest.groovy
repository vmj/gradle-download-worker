package fi.linuxbox.gradle.download.worker

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.IgnoreIf
import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class DownloadWorkerTaskTest extends Specification {
    private static final Set<String> gradleVersions = ['4.3', '4.0']

    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
    }

    @Unroll
    def "task type is ok (Gradle #gradleVersion)"() {
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
    @IgnoreIf({System.getProperty('IS_OFFLINE') == '1'})
    def "can execute download (Gradle #gradleVersion)"() {
        given:
        buildFile << """
        plugins {
          id 'fi.linuxbox.download.worker' version "0.3"
        }
        import fi.linuxbox.gradle.download.worker.DownloadWorkerTask
      
        task("my-download", type: DownloadWorkerTask) {
          from 'https://www.google.com/robots.txt'
          to "\$buildDir/robots.txt"
        }
        """

        when:
        def result = GradleRunner
                .create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.root)
                .withArguments('my-download')
                .withPluginClasspath()
                .build()

        then:
        result.task(':my-download').outcome == SUCCESS
        projectFile('build/robots.txt').exists()

        where:
        gradleVersion << gradleVersions
    }

    @Unroll
    @IgnoreIf({System.getProperty('IS_OFFLINE') == '1'})
    def "can use GString as from (Gradle #gradleVersion)"() {
        given:
        buildFile << """
        plugins {
          id 'fi.linuxbox.download.worker'
        }
        import fi.linuxbox.gradle.download.worker.DownloadWorkerTask
        task('fetch-Slackware64-13.0', type: DownloadWorkerTask) {
          def v = '13.0'
          from "http://ftp.osuosl.org/pub/slackware/slackware-\$v/ChangeLog.txt"
          to new File(buildDir, "changelogs/slackware/\$v/ChangeLog.txt")
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
        projectFile('build/changelogs/slackware/13.0/ChangeLog.txt').exists()

        where:
        gradleVersion << gradleVersions
    }

    def "Unknown host error"() {
        given:
        buildFile << """
        plugins {
          id 'fi.linuxbox.download.worker'
        }
        import fi.linuxbox.gradle.download.worker.DownloadWorkerTask
        task('fetch', type: DownloadWorkerTask) {
          from 'http://no.such.site.com/foo.txt'
          to "\$buildDir/foo.txt"
        }
        """
        and:
        def writer = new StringWriter()

        when:
        def result = GradleRunner
                .create()
                .withGradleVersion('4.0')
                .withProjectDir(testProjectDir.root)
                .withArguments("fetch")
                .withPluginClasspath()
                .forwardStdError(writer)
                .buildAndFail()

        then:
        result.task(":fetch").outcome == FAILED
        writer.toString().contains('unknown host: ')

    }

    def "Connect timeout error"() {
        given:
        buildFile << """
        plugins {
          id 'fi.linuxbox.download.worker'
        }
        import fi.linuxbox.gradle.download.worker.DownloadWorkerTask
        task('fetch', type: DownloadWorkerTask) {
          from 'http://www.linuxbox.fi/robots.txt'
          to "\$buildDir/foo.txt"
          connectTimeout 1 // milliseconds
        }
        """
        and:
        def writer = new StringWriter()

        when:
        def result = GradleRunner
                .create()
                .withGradleVersion('4.0')
                .withProjectDir(testProjectDir.root)
                .withArguments("fetch")
                .withPluginClasspath()
                .forwardStdError(writer)
                //.withDebug(true)
                .buildAndFail()

        then:
        result.task(":fetch").outcome == FAILED
        writer.toString().contains('connect timed out')
    }

    def "Read timeout error"() {
        given:
        buildFile << """
        plugins {
          id 'fi.linuxbox.download.worker'
        }
        import fi.linuxbox.gradle.download.worker.DownloadWorkerTask
        task('fetch', type: DownloadWorkerTask) {
          from 'https://www.google.com/robots.txt'
          to "\$buildDir/foo.txt"
          readTimeout 1 // milliseconds
        }
        """
        and:
        def writer = new StringWriter()

        when:
        def result = GradleRunner
                .create()
                .withGradleVersion('4.0')
                .withProjectDir(testProjectDir.root)
                .withArguments('--stacktrace', 'fetch')
                .withPluginClasspath()
                .forwardStdError(writer)
                //.withDebug(true)
                .buildAndFail()

        then:
        result.task(":fetch").outcome == FAILED
        writer.toString().contains('Read timed out')
    }

    def "HTTP error"() {
        given:
        buildFile << """
        plugins {
          id 'fi.linuxbox.download.worker'
        }
        import fi.linuxbox.gradle.download.worker.DownloadWorkerTask
        task('fetch', type: DownloadWorkerTask) {
          from 'http://www.linuxbox.fi/no-such-file.txt'
          to "\$buildDir/foo.txt"
        }
        """
        and:
        def writer = new StringWriter()

        when:
        def result = GradleRunner
                .create()
                .withGradleVersion('4.0')
                .withProjectDir(testProjectDir.root)
                .withArguments("fetch")
                .withPluginClasspath()
                .forwardStdError(writer)
                .buildAndFail()

        then:
        result.task(":fetch").outcome == FAILED
        writer.toString().contains('HTTP error: ')
    }

    def projectFile(String path) {
        def file = testProjectDir.root
        if (path != null) {
            for (String pathSegment : path.split(/\//)) {
                file = new File(file, pathSegment)
            }
        }
        return file
    }
}
