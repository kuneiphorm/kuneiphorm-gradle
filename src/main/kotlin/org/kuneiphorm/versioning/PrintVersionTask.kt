package org.kuneiphorm.versioning

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

/**
 * Gradle task that prints a version string to standard output.
 *
 * @author Florent Guille
 * @since 0.0.0
 */
@UntrackedTask(because = "Version output depends on git state")
abstract class PrintVersionTask : DefaultTask() {

    @get:Input
    abstract val version: Property<String>

    @TaskAction
    fun print() {
        println(version.get())
    }
}
