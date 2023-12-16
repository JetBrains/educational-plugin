package com.jetbrains.edu.rust

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseDir
import org.rust.cargo.CargoConstants

private val INVALID_SYMBOLS = """[^a-zA-Z0-9_]""".toRegex()

fun String.toPackageName(): String = replace(INVALID_SYMBOLS, "_").lowercase()

val Project.isSingleWorkspaceProject: Boolean get() = courseDir.findChild(CargoConstants.MANIFEST_FILE) != null
