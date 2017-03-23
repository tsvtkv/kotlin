// DO NOT EDIT MANUALLY!
// Generated by org/jetbrains/kotlin/generators/arguments/GenerateGradleOptions.kt
package org.jetbrains.kotlin.gradle.dsl

interface KotlinJvmOptions  : org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions {

    /**
     * Include Kotlin runtime in to resulting .jar
     * Default value: false
     */
    @get:org.gradle.api.tasks.Input
     var includeRuntime: kotlin.Boolean

    /**
     * Generate metadata for Java 1.8 reflection on method parameters
     * Default value: false
     */
    @get:org.gradle.api.tasks.Input
     var javaParameters: kotlin.Boolean

    /**
     * Path to JDK home directory to include into classpath, if differs from default JAVA_HOME
     * Default value: null
     */
    @get:org.gradle.api.tasks.Optional
    @get:org.gradle.api.tasks.Input
     var jdkHome: kotlin.String?

    /**
     * Target version of the generated JVM bytecode (1.6 or 1.8), default is 1.6
     * Possible values: "1.6", "1.8"
     * Default value: "1.6"
     */
    @get:org.gradle.api.tasks.Input
     var jvmTarget: kotlin.String

    /**
     * Don't include Java runtime into classpath
     * Default value: false
     */
    @get:org.gradle.api.tasks.Input
     var noJdk: kotlin.Boolean

    /**
     * Don't include Kotlin reflection implementation into classpath
     * Default value: true
     */
    @get:org.gradle.api.tasks.Input
     var noReflect: kotlin.Boolean

    /**
     * Don't include Kotlin runtime into classpath
     * Default value: true
     */
    @get:org.gradle.api.tasks.Input
     var noStdlib: kotlin.Boolean
}
