package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile

/**
 * Tasks provider to be used wrapper
 * Created by Nikita.Skvortsov
 * date: 17.12.2014.
 */

public open class KotlinTasksProvider(val tasksLoader: ClassLoader) {
    val kotlinJVMCompileTaskClass: Class<AbstractCompile> =
            tasksLoader.loadClass("org.jetbrains.kotlin.gradle.tasks.KotlinCompile") as Class<AbstractCompile>

    val kotlinJSCompileTaskClass: Class<AbstractCompile> =
            tasksLoader.loadClass("org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile") as Class<AbstractCompile>

    val kotlinJVMOptionsClass: Class<Any> =
            tasksLoader.loadClass("org.jetbrains.kotlin.gradle.tasks.K2JVMGradleCompilerArguments") as Class<Any>


    public fun createKotlinJVMTask(project: Project, name: String): AbstractCompile {
        return project.tasks.create(name, kotlinJVMCompileTaskClass)
    }

    public fun createKotlinJSTask(project: Project, name: String): AbstractCompile {
        return project.tasks.create(name, kotlinJSCompileTaskClass)
    }
}
