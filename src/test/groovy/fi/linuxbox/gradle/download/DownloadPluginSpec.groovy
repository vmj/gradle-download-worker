package fi.linuxbox.gradle.download

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class DownloadPluginSpec extends Specification {
    private static final String minimumGradleVersion = '5.6'
    private static final Set<String> gradleVersions = [
            '8.7', // Mar 22, 2024
            '8.6', // Feb 02, 2024
            '8.5', // Nov 29, 2023
            '8.4', // Oct 04, 2023
            '8.3', // Aug 17, 2023
            '8.2.1', // Jul 10, 2023
            '8.1.1', // Apr 21, 2023
            '8.0.2', // Mar 03, 2023
            '7.6.4', // Feb 05, 2024
            '7.5.1', // Aug 05, 2022
            '7.4.2', // Mar 31, 2022
            '7.3.3', // Dec 22, 2021
            '7.2', // Aug 17, 2021
            '7.1.1', // Jul 02, 2021
            '7.0.2', // May 14, 2021
            '6.9.4', // Feb 22, 2023
            '6.8.3', // Feb 22, 2021
            '6.7.1', // Nov 16, 2020
            '6.6.1', // Aug 25, 2020
            '6.5.1', // Jun 30, 2020
            '6.4.1', // May 15, 2020
            '6.3',  // Mar 24, 2020
            '6.2.2', // Mar 04, 2020
            '6.1.1', // Jan 24, 2020
            '6.0.1', // Nov 18, 2019
            '5.6.4', // Nov 01, 2019
            minimumGradleVersion // Aug 14, 2019
    ]

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
          id 'fi.linuxbox.download'
        }
        import fi.linuxbox.gradle.download.Download
        task('fetch-Slackware64-13.0', type: Download) {
          from 'http://ftp.osuosl.org/pub/slackware/slackware-13.0/ChangeLog.txt'
          to new File(buildDir, 'changelogs/slackware/13.0/ChangeLog.txt')
        }
        """

        when:
        def result = gradle(gradleVersion, "help").build()

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
            id 'fi.linuxbox.download'
        }
        import fi.linuxbox.gradle.download.Download

        task('foo', type: Download) {
            connectTimeout 1
            readTimeout 2
        }
        task('bar', type: Download) {
        }
        task('baz', type: Download) {
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
        def result = gradle(gradleVersion, '-q', 'test').build()

        then:
        result.task(':test').outcome == SUCCESS
        result.output == ['1','2','30000','30000','4','5',''].join(System.getProperty('line.separator'))

        where:
        gradleVersion << gradleVersions
    }

    @Unroll
    @IgnoreIf({System.getProperty('IS_OFFLINE') == '1'})
    def "can execute download (Gradle #gradleVersion)"() {
        given:
        buildFile << """
        plugins {
          id 'fi.linuxbox.download'
        }
        import fi.linuxbox.gradle.download.Download
      
        task("my-download", type: Download) {
          from 'https://www.google.com/robots.txt'
          to "\$buildDir/robots.txt"
        }
        """

        when:
        def result = gradle(gradleVersion, 'my-download').build()

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
          id 'fi.linuxbox.download'
        }
        import fi.linuxbox.gradle.download.Download
        task('fetch-Slackware64-13.0', type: Download) {
          def v = '13.0'
          from "http://ftp.osuosl.org/pub/slackware/slackware-\$v/ChangeLog.txt"
          to new File(buildDir, "changelogs/slackware/\$v/ChangeLog.txt")
        }
        """

        when:
        def result = gradle(gradleVersion, "fetch-Slackware64-13.0").build()

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
          id 'fi.linuxbox.download'
        }
        import fi.linuxbox.gradle.download.Download
        task('fetch', type: Download) {
          from 'http://no.such.site.com/foo.txt'
          to "\$buildDir/foo.txt"
        }
        """
        and:
        def writer = new StringWriter()

        when:
        def result = gradle(minimumGradleVersion, "fetch")
                .forwardStdError(writer)
                .buildAndFail()

        then:
        result.task(":fetch").outcome == FAILED
        writer.toString().contains('unknown host: ')

    }

    @Ignore("Fails more often than works")
    @IgnoreIf({System.getProperty('IS_OFFLINE') == '1'})
    def "Connect timeout error"() {
        given:
        buildFile << """
        plugins {
          id 'fi.linuxbox.download'
        }
        import fi.linuxbox.gradle.download.Download
        task('fetch', type: Download) {
          from 'http://www.linuxbox.fi/robots.txt'
          to "\$buildDir/foo.txt"
          connectTimeout 1 // milliseconds
        }
        """
        and:
        def writer = new StringWriter()

        when:
        def result = gradle(minimumGradleVersion, "fetch")
                .forwardStdError(writer)
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
          id 'fi.linuxbox.download'
        }
        import fi.linuxbox.gradle.download.Download
        task('fetch', type: Download) {
          from 'https://www.google.com/robots.txt'
          to "\$buildDir/foo.txt"
          readTimeout 1 // milliseconds
        }
        """
        and:
        def writer = new StringWriter()

        when:
        def result = gradle(minimumGradleVersion, 'fetch')
                .forwardStdError(writer)
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
          id 'fi.linuxbox.download'
        }
        import fi.linuxbox.gradle.download.Download
        task('fetch', type: Download) {
          from 'http://yle.fi/no-such-file.txt'
          to "\$buildDir/foo.txt"
        }
        """
        and:
        def writer = new StringWriter()

        when:
        def result = gradle(minimumGradleVersion, "fetch")
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
            id 'fi.linuxbox.download'
        }
        
        import fi.linuxbox.gradle.download.Download
        
        final downloadAll = tasks.register('downloadAll') {
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
                final download = tasks.register("download-$distro-changelog", Download) {
                    from "$mirror/$distro/ChangeLog.txt"
                    to "$buildDir/changelogs/$path/ChangeLog.txt"
                }
        
                // Just to demo the UP-TO-DATE functionality:
                // even though the download task does some work (conditional GET)
                // it doesn't necessarily touch the artifact.
                // That allows Gradle to skip the copy task.
                final copy = tasks.register("copy-$distro-changelog", Copy) {
                    from download
                    into "$buildDir/copies/$path/"
                }
        
                downloadAll.configure {
                    dependsOn copy
                }
            }
        }
        '''

        when:
        def result = gradle(gradleVersion, "downloadAll").build()

        then:
        result.task(":downloadAll").outcome == SUCCESS
        result.task(":download-slackware64-14.2-changelog").outcome == SUCCESS
        result.task(":copy-slackware64-14.2-changelog").outcome == SUCCESS

        when:
        result = gradle(gradleVersion, "downloadAll").build()

        then:
        result.task(":downloadAll").outcome == UP_TO_DATE
        result.task(":download-slackware64-14.2-changelog").outcome == SUCCESS
        result.task(":copy-slackware64-14.2-changelog").outcome == UP_TO_DATE

        where:
        gradleVersion << gradleVersions
    }

    def gradle(String gradleVersion, String... args) {
        GradleRunner
                .create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.root)
                .withPluginClasspath()
                .withArguments(args)
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
