/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.java.components

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaAnnotationDescriptor
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.ConstantValueFactory
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.types.ErrorUtils
import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.Target
import java.util.*

object JavaAnnotationMapper {

    private val JAVA_TARGET_FQ_NAME = FqName(Target::class.java.canonicalName)
    private val JAVA_RETENTION_FQ_NAME = FqName(Retention::class.java.canonicalName)
    private val JAVA_DEPRECATED_FQ_NAME = FqName(java.lang.Deprecated::class.java.canonicalName)
    private val JAVA_DOCUMENTED_FQ_NAME = FqName(Documented::class.java.canonicalName)
    // Java8-specific thing
    private val JAVA_REPEATABLE_FQ_NAME = FqName("java.lang.annotation.Repeatable")

    internal val DEPRECATED_ANNOTATION_MESSAGE = Name.identifier("message")
    internal val TARGET_ANNOTATION_ALLOWED_TARGETS = Name.identifier("allowedTargets")

    fun mapOrResolveJavaAnnotation(annotation: JavaAnnotation, c: LazyJavaResolverContext): AnnotationDescriptor? =
            when (annotation.classId) {
                ClassId.topLevel(JAVA_TARGET_FQ_NAME) -> JavaTargetAnnotationDescriptor(annotation, c)
                ClassId.topLevel(JAVA_RETENTION_FQ_NAME) -> JavaRetentionAnnotationDescriptor(annotation, c)
                ClassId.topLevel(JAVA_REPEATABLE_FQ_NAME) -> JavaAnnotationDescriptor(c, annotation, c.module.builtIns.repeatableAnnotation)
                ClassId.topLevel(JAVA_DOCUMENTED_FQ_NAME) -> JavaAnnotationDescriptor(c, annotation, c.module.builtIns.mustBeDocumentedAnnotation)
                ClassId.topLevel(JAVA_DEPRECATED_FQ_NAME) -> null
                else -> LazyJavaAnnotationDescriptor(c, annotation)
            }

    fun findMappedJavaAnnotation(kotlinName: FqName,
                                        annotationOwner: JavaAnnotationOwner,
                                        c: LazyJavaResolverContext
    ): AnnotationDescriptor? {
        if (kotlinName == KotlinBuiltIns.FQ_NAMES.deprecated) {
            val javaAnnotation = annotationOwner.findAnnotation(JAVA_DEPRECATED_FQ_NAME)
            if (javaAnnotation != null || annotationOwner.isDeprecatedInJavaDoc) {
                return JavaDeprecatedAnnotationDescriptor(javaAnnotation, c)
            }
        }
        return kotlinToJavaNameMap[kotlinName]?.let {
            annotationOwner.findAnnotation(it)?.let {
                mapOrResolveJavaAnnotation(it, c)
            }
        }
    }

    // kotlin.annotation.annotation is treated separately
    private val kotlinToJavaNameMap: Map<FqName, FqName> =
            mapOf(KotlinBuiltIns.FQ_NAMES.target to JAVA_TARGET_FQ_NAME,
                  KotlinBuiltIns.FQ_NAMES.retention to JAVA_RETENTION_FQ_NAME,
                  KotlinBuiltIns.FQ_NAMES.repeatable to JAVA_REPEATABLE_FQ_NAME,
                  KotlinBuiltIns.FQ_NAMES.mustBeDocumented to JAVA_DOCUMENTED_FQ_NAME)

    val javaToKotlinNameMap: Map<FqName, FqName> =
            mapOf(JAVA_TARGET_FQ_NAME     to KotlinBuiltIns.FQ_NAMES.target,
                  JAVA_RETENTION_FQ_NAME  to KotlinBuiltIns.FQ_NAMES.retention,
                  JAVA_DEPRECATED_FQ_NAME to KotlinBuiltIns.FQ_NAMES.deprecated,
                  JAVA_REPEATABLE_FQ_NAME to KotlinBuiltIns.FQ_NAMES.repeatable,
                  JAVA_DOCUMENTED_FQ_NAME to KotlinBuiltIns.FQ_NAMES.mustBeDocumented)
}

open class JavaAnnotationDescriptor(
        c: LazyJavaResolverContext,
        annotation: JavaAnnotation?,
        private val kotlinAnnotationClassDescriptor: ClassDescriptor
): AnnotationDescriptor {
    private val source = annotation?.let { c.components.sourceElementFactory.source(it) } ?: SourceElement.NO_SOURCE

    override fun getType() = kotlinAnnotationClassDescriptor.defaultType

    override fun getSource() = source

    protected val valueParameters: List<ValueParameterDescriptor>
            get() = kotlinAnnotationClassDescriptor.constructors.single().valueParameters

    protected val firstArgument: JavaAnnotationArgument? = annotation?.arguments?.firstOrNull()

    override fun getAllValueArguments() = emptyMap<ValueParameterDescriptor, ConstantValue<*>?>()
}

class JavaDeprecatedAnnotationDescriptor(
        annotation: JavaAnnotation?,
        c: LazyJavaResolverContext
): JavaAnnotationDescriptor(c, annotation, c.module.builtIns.deprecatedAnnotation) {

    private val valueArguments = c.storageManager.createLazyValue {
        val parameterDescriptor = valueParameters.firstOrNull {
            it.name == JavaAnnotationMapper.DEPRECATED_ANNOTATION_MESSAGE
        }
        parameterDescriptor?.let { mapOf(it to ConstantValueFactory(c.module.builtIns).createConstantValue("Deprecated in Java")) } ?: emptyMap()
    }

    override fun getAllValueArguments() = valueArguments()
}

class JavaTargetAnnotationDescriptor(
        annotation: JavaAnnotation,
        c: LazyJavaResolverContext
): JavaAnnotationDescriptor(c, annotation, c.module.builtIns.targetAnnotation) {

    private val valueArguments = c.storageManager.createLazyValue {
        val targetArgument = when (firstArgument) {
            is JavaArrayAnnotationArgument -> JavaAnnotationTargetMapper.mapJavaTargetArguments(firstArgument.getElements(), c.module.builtIns)
            is JavaEnumValueAnnotationArgument -> JavaAnnotationTargetMapper.mapJavaTargetArguments(listOf(firstArgument), c.module.builtIns)
            else -> return@createLazyValue emptyMap<ValueParameterDescriptor, ConstantValue<*>>()
        }
        mapOf(valueParameters.single() to targetArgument)
    }

    override fun getAllValueArguments() = valueArguments()
}

class JavaRetentionAnnotationDescriptor(
        annotation: JavaAnnotation,
        c: LazyJavaResolverContext
): JavaAnnotationDescriptor(c, annotation, c.module.builtIns.retentionAnnotation) {

    private val valueArguments = c.storageManager.createLazyValue {
        val retentionArgument = when (firstArgument) {
            is JavaEnumValueAnnotationArgument -> JavaAnnotationTargetMapper.mapJavaRetentionArgument(firstArgument, c.module.builtIns)
            else -> return@createLazyValue emptyMap<ValueParameterDescriptor, ConstantValue<*>>()
        }
        mapOf(valueParameters.single() to retentionArgument)
    }

    override fun getAllValueArguments() = valueArguments()
}

object JavaAnnotationTargetMapper {
    private val targetNameLists = mapOf("PACKAGE"         to EnumSet.noneOf(KotlinTarget::class.java),
                                        "TYPE"            to EnumSet.of(KotlinTarget.CLASS, KotlinTarget.FILE),
                                        "ANNOTATION_TYPE" to EnumSet.of(KotlinTarget.ANNOTATION_CLASS),
                                        "TYPE_PARAMETER"  to EnumSet.of(KotlinTarget.TYPE_PARAMETER),
                                        "FIELD"           to EnumSet.of(KotlinTarget.FIELD),
                                        "LOCAL_VARIABLE"  to EnumSet.of(KotlinTarget.LOCAL_VARIABLE),
                                        "PARAMETER"       to EnumSet.of(KotlinTarget.VALUE_PARAMETER),
                                        "CONSTRUCTOR"     to EnumSet.of(KotlinTarget.CONSTRUCTOR),
                                        "METHOD"          to EnumSet.of(KotlinTarget.FUNCTION,
                                                                        KotlinTarget.PROPERTY_GETTER,
                                                                        KotlinTarget.PROPERTY_SETTER),
                                        "TYPE_USE"        to EnumSet.of(KotlinTarget.TYPE)
    )

    fun mapJavaTargetArgumentByName(argumentName: String?): Set<KotlinTarget> = targetNameLists[argumentName] ?: emptySet()

    fun mapJavaTargetArguments(arguments: List<JavaAnnotationArgument>, builtIns: KotlinBuiltIns): ConstantValue<*>? {
        // Map arguments: java.lang.annotation.Target -> kotlin.annotation.Target
        val kotlinTargets = arguments.filterIsInstance<JavaEnumValueAnnotationArgument>()
                .flatMap { mapJavaTargetArgumentByName(it.resolve()?.name?.asString()) }
                .mapNotNull { builtIns.getAnnotationTargetEnumEntry(it) }
                .map { EnumValue(it) }
        val parameterDescriptor = DescriptorResolverUtils.getAnnotationParameterByName(
                JavaAnnotationMapper.TARGET_ANNOTATION_ALLOWED_TARGETS, builtIns.targetAnnotation
        )
        return ArrayValue(kotlinTargets, parameterDescriptor?.type ?: ErrorUtils.createErrorType("Error: AnnotationTarget[]"), builtIns)
    }

    private val retentionNameList = mapOf(
            "RUNTIME" to KotlinRetention.RUNTIME,
            "CLASS"   to KotlinRetention.BINARY,
            "SOURCE"  to KotlinRetention.SOURCE
    )

    fun mapJavaRetentionArgument(element: JavaAnnotationArgument, builtIns: KotlinBuiltIns): ConstantValue<*>? {
        // Map argument: java.lang.annotation.Retention -> kotlin.annotation.annotation
        return (element as? JavaEnumValueAnnotationArgument)?.let {
            retentionNameList[it.resolve()?.name?.asString()]?.let {
                (builtIns.getAnnotationRetentionEnumEntry(it) as? ClassDescriptor)?.let { EnumValue(it) }
            }
        }
    }
}
