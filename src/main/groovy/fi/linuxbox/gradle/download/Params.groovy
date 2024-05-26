package fi.linuxbox.gradle.download

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.TupleConstructor
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters

@PackageScope
@CompileStatic
interface Params extends WorkParameters, Serializable {
    Property<String> getFrom()
    Property<String> getTo()
    Property<Integer> getConnectTimeout()
    Property<Integer> getReadTimeout()
}
