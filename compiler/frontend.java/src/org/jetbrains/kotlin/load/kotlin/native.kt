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

package org.jetbrains.kotlin.load.kotlin.nativeDeclarations

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetDeclarationWithBody
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DeclarationChecker
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.diagnostics.FUNCTION_NO_BODY_ERRORS
import org.jetbrains.kotlin.resolve.diagnostics.SuppressDiagnosticsByAnnotations
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

private val NATIVE_ANNOTATION_CLASS_NAME = FqName("kotlin.jvm.native")

public fun DeclarationDescriptor.hasNativeAnnotation(): Boolean {
    return this is FunctionDescriptor && this.isExternal
           || this is PropertyAccessorDescriptor && this.isExternal
           || annotations.findAnnotation(NATIVE_ANNOTATION_CLASS_NAME) != null
}

public class SuppressNoBodyErrorsForNativeDeclarations : SuppressDiagnosticsByAnnotations(FUNCTION_NO_BODY_ERRORS, NATIVE_ANNOTATION_CLASS_NAME)

public class NativeFunChecker : DeclarationChecker {
    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (!descriptor.hasNativeAnnotation()) return

        if (DescriptorUtils.isInterface(descriptor.getContainingDeclaration())) {
            diagnosticHolder.report(ErrorsJvm.EXTERNAL_DECLARATION_IN_INTERFACE.on(declaration))
        }
        else if (descriptor is CallableMemberDescriptor &&
            descriptor.getModality() == Modality.ABSTRACT) {
            diagnosticHolder.report(ErrorsJvm.EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT.on(declaration))
        }

        if (descriptor !is ConstructorDescriptor && declaration is JetDeclarationWithBody && declaration.hasBody()) {
            diagnosticHolder.report(ErrorsJvm.EXTERNAL_DECLARATION_CANNOT_HAVE_BODY.on(declaration))
        }

        if (InlineUtil.isInline(descriptor)) {
            diagnosticHolder.report(ErrorsJvm.EXTERNAL_DECLARATION_CANNOT_BE_INLINED.on(declaration))
        }

    }
}
