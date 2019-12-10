package fi.linuxbox.gradle.download

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.TupleConstructor

@PackageScope
@CompileStatic
@TupleConstructor
class Params implements Serializable {
    final URL from
    final File to
    final int connectTimeout
    final int readTimeout
}
