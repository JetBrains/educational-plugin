@file:JvmName("OpenApiExtCompatibility")

package com.jetbrains.edu.learning

import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.project.Project

// BACKCOMPAT: 2019.3. Inline it
fun Project.isLight(): Boolean = LightEdit.owns(this)
