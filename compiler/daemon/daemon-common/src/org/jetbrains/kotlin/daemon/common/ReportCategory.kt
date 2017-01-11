/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.daemon.common

import java.io.Serializable

enum class ReportCategory(val code: Int) : Serializable {
    COMPILER_MESSAGE(0),
    DAEMON_MESSAGE(1),
    IC_MESSAGE(2);

    companion object {
        const val serialVersionUID: Long = 0
    }
}

enum class ReportSeverity(val code: Int) {
    ERROR(0),
    WARNING(1),
    INFO(2),
    TRACE(3)
}