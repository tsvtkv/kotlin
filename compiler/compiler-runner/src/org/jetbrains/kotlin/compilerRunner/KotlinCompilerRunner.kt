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

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.daemon.client.CompilationServices
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.isDaemonEnabled
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.StringReader

abstract class KotlinCompilerRunner<in Env : CompilerEnvironment> {
    protected val K2JVM_COMPILER = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
    protected val K2JS_COMPILER = "org.jetbrains.kotlin.cli.js.K2JSCompiler"
    protected val INTERNAL_ERROR = ExitCode.INTERNAL_ERROR.toString()

    class DaemonConnection(val daemon: CompileService?, val sessionId: Int = CompileService.NO_SESSION)
    protected abstract fun getDaemonConnection(environment: Env, messageCollector: MessageCollector): DaemonConnection

    protected abstract fun logInfo(msg: String)
    protected abstract fun logDebug(msg: String)

    protected fun processCompilerOutput(
            messageCollector: MessageCollector,
            collector: OutputItemsCollector,
            stream: ByteArrayOutputStream,
            exitCode: String
    ) {
        val reader = BufferedReader(StringReader(stream.toString()))
        CompilerOutputParser.parseCompilerMessagesFromReader(messageCollector, reader, collector)

        if (INTERNAL_ERROR == exitCode) {
            reportInternalCompilerError(messageCollector)
        }
    }

    protected fun reportInternalCompilerError(messageCollector: MessageCollector) {
        messageCollector.report(ERROR, "Compiler terminated with internal error", CompilerMessageLocation.NO_LOCATION)
    }

    protected open fun runCompiler(
            compilerClassName: String,
            arguments: CommonCompilerArguments,
            additionalArguments: String,
            messageCollector: MessageCollector,
            collector: OutputItemsCollector,
            environment: Env) {
        try {
            val argumentsList = ArgumentUtils.convertArgumentsToStringList(arguments)
            argumentsList.addAll(additionalArguments.split(" "))

            val argsArray = argumentsList.toTypedArray()

            if (!tryCompileWithDaemon(compilerClassName, argsArray, environment, messageCollector, collector)) {
                fallbackCompileStrategy(argsArray, collector, compilerClassName, environment, messageCollector)
            }
        }
        catch (e: Throwable) {
            MessageCollectorUtil.reportException(messageCollector, e)
            reportInternalCompilerError(messageCollector)
        }

    }

    protected abstract fun fallbackCompileStrategy(
            argsArray: Array<String>,
            collector: OutputItemsCollector,
            compilerClassName: String,
            environment: Env,
            messageCollector: MessageCollector)

    private fun tryCompileWithDaemon(compilerClassName: String,
                                     argsArray: Array<String>,
                                     environment: Env,
                                     messageCollector: MessageCollector,
                                     collector: OutputItemsCollector,
                                     retryOnConnectionError: Boolean = true): Boolean {

        if (isDaemonEnabled()) {

            logDebug("Try to connect to daemon")
            val connection = getDaemonConnection(environment, messageCollector)

            if (connection.daemon != null) {
                logInfo("Connected to daemon")

                val compilerOut = ByteArrayOutputStream()
                val daemonOut = ByteArrayOutputStream()

                val services = CompilationServices(
                        incrementalCompilationComponents = environment.services.get(IncrementalCompilationComponents::class.java),
                        compilationCanceledStatus = environment.services.get(CompilationCanceledStatus::class.java))

                val targetPlatform = when (compilerClassName) {
                    K2JVM_COMPILER -> CompileService.TargetPlatform.JVM
                    K2JS_COMPILER -> CompileService.TargetPlatform.JS
                    else -> throw IllegalArgumentException("Unknown compiler type $compilerClassName")
                }

                fun retryOrFalse(e: Exception): Boolean {
                    if (retryOnConnectionError) {
                        logDebug("retrying once on daemon connection error: ${e.message}")
                        return tryCompileWithDaemon(compilerClassName, argsArray, environment, messageCollector, collector, retryOnConnectionError = false)
                    }
                    logInfo("daemon connection error: ${e.message}")
                    return false
                }

                val res: Int = try {
                    KotlinCompilerClient.incrementalCompile(connection.daemon, connection.sessionId, targetPlatform, argsArray, services, compilerOut, daemonOut)
                }
                catch (e: java.rmi.ConnectException) {
                    return retryOrFalse(e)
                }
                catch (e: java.rmi.UnmarshalException) {
                    return retryOrFalse(e)
                }

                processCompilerOutput(messageCollector, collector, compilerOut, res.toString())
                BufferedReader(StringReader(daemonOut.toString())).forEachLine {
                    messageCollector.report(CompilerMessageSeverity.INFO, it, CompilerMessageLocation.NO_LOCATION)
                }
                return true
            }

            logInfo("Daemon not found")
        }
        return false
    }
}

