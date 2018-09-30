package fi.linuxbox.gradle.download.worker

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.IgnoreIf
import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class DownloadWorkerTaskTest extends Specification {
    private static final String minimumGradleVersion = '4.3'
    private static final Set<String> gradleVersions = ['4.10.2', minimumGradleVersion]

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
    def "timeout defaults (Gradle #gradleVersion)"() {
        given:
        buildFile << '''
        plugins {
            id 'fi.linuxbox.download.worker'
        }
        import fi.linuxbox.gradle.download.worker.DownloadWorkerTask

        task('foo', type: DownloadWorkerTask) {
            connectTimeout 1
            readTimeout 2
        }
        task('bar', type: DownloadWorkerTask) {
        }
        task('baz', type: DownloadWorkerTask) {
            connectTimeout null
            readTimeout null
        }
        task('test') {
            doLast {
                println foo.connectTimeout.get()
                println foo.readTimeout.get()
                println bar.connectTimeout.get()
                println bar.readTimeout.get()
                println baz.connectTimeout.getOrElse(4)
                println baz.readTimeout.getOrElse(5)
            }
        }
        '''

        when:
        def result = GradleRunner
                .create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.root)
                .withArguments('-q', 'test')
                .withPluginClasspath()
                .build()

        then:
        result.task(':test').outcome == SUCCESS
        result.output == '1\n2\n30000\n30000\n4\n5\n'

        where:
        gradleVersion << gradleVersions
    }

    @Unroll
    @IgnoreIf({System.getProperty('IS_OFFLINE') == '1'})
    def "can execute download (Gradle #gradleVersion)"() {
        given:
        buildFile << """
        plugins {
          id 'fi.linuxbox.download.worker'
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

    @IgnoreIf({System.getProperty('IS_OFFLINE') == '1'})
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
                .withGradleVersion(minimumGradleVersion)
                .withProjectDir(testProjectDir.root)
                .withArguments("fetch")
                .withPluginClasspath()
                .forwardStdError(writer)
                .buildAndFail()

        then:
        result.task(":fetch").outcome == FAILED
        writer.toString().contains('unknown host: ')

    }

    @IgnoreIf({System.getProperty('IS_OFFLINE') == '1'})
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
                .withGradleVersion(minimumGradleVersion)
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

    @IgnoreIf({System.getProperty('IS_OFFLINE') == '1'})
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
                .withGradleVersion(minimumGradleVersion)
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

    @IgnoreIf({System.getProperty('IS_OFFLINE') == '1'})
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
                .withGradleVersion(minimumGradleVersion)
                .withProjectDir(testProjectDir.root)
                .withArguments("fetch")
                .withPluginClasspath()
                .forwardStdError(writer)
                .buildAndFail()

        then:
        result.task(":fetch").outcome == FAILED
        writer.toString().contains('HTTP error: ')
    }

    @Unroll
    @IgnoreIf({System.getProperty('IS_OFFLINE') == '1'})
    def "Example from README (Gradle #gradleVersion)"() {
        given:
        buildFile << '''
        plugins {
            id 'fi.linuxbox.download.worker'
        }
        
        import fi.linuxbox.gradle.download.worker.DownloadWorkerTask
      
        task downloadAll {
            group 'My tasks'
            description 'Download all ChangeLogs'
        }
      
        ext {
            mirror = 'http://ftp.osuosl.org/pub/slackware'
            distroNames = ['slackware64']
            distroVersions = ['14.2']
        }
      
        distroNames.each { final distroName ->
            distroVersions.each { final distroVersion ->
                final distro = "$distroName-$distroVersion"
                final path = "$distroName/$distroVersion"
                
                // Define a parallel download task for this distro version
                final download = task("download-$distro-changelog", type: DownloadWorkerTask) {
                    from "$mirror/$distro/ChangeLog.txt"
                    to project.layout.buildDirectory.file("changelogs/$path/ChangeLog.txt")
                }
                
                // Just to demo the UP-TO-DATE functionality:
                // even though the download task does some work (conditional GET)
                // it doesn't necessarily touch the artifact.
                // That allows Gradle to skip the copy task.
                final copy = task("copy-$distro-changelog", type: Copy) {
                    from download
                    into project.layout.buildDirectory.dir("copies/$path/")
                }
                
                downloadAll.dependsOn copy
            }
        }
        '''

        when:
        def result = GradleRunner
                .create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.root)
                .withArguments("downloadAll")
                .withPluginClasspath()
                .build()

        then:
        result.task(":downloadAll").outcome == SUCCESS
        result.task(":download-slackware64-14.2-changelog").outcome == SUCCESS
        result.task(":copy-slackware64-14.2-changelog").outcome == SUCCESS

        when:
        result = GradleRunner
                .create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.root)
                .withArguments("downloadAll")
                .withPluginClasspath()
                .build()

        then:
        result.task(":downloadAll").outcome == UP_TO_DATE
        result.task(":download-slackware64-14.2-changelog").outcome == SUCCESS
        result.task(":copy-slackware64-14.2-changelog").outcome == UP_TO_DATE

        where:
        gradleVersion << gradleVersions
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
