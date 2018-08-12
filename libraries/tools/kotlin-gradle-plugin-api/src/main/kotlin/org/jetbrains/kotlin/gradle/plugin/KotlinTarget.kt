/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

// think about packages (org.jetbrains.kotlin.model.mpp ?)
package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.internal.component.UsageContext

interface KotlinTarget: Named, HasAttributes {
    // is target name needed if name is already present?
    val targetName: String
    // should be clarified in javadoc
    val disambiguationClassifier: String? get() = targetName

    val platformType: KotlinPlatformType

    val compilations: NamedDomainObjectContainer<out KotlinCompilation>

    val project: Project

    val artifactsTaskName: String

    val defaultConfigurationName: String
    val apiElementsConfigurationName: String
    val runtimeElementsConfigurationName: String

    // I don't think that using internal types in public API is a good idea
    fun createUsageContexts(): Set<UsageContext>

    override fun getName(): String = targetName
}