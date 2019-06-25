package com.jetbrains.edu.rust

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseDir
import org.rust.cargo.CargoConstants

private val INVALID_SYMBOLS = """[\s-]""".toRegex()

fun String.toPackageName(): String = replace(INVALID_SYMBOLS, "_").toLowerCase()

val Project.isSingleWorkspaceProject: Boolean get() = courseDir.findChild(CargoConstants.MANIFEST_FILE) != null
