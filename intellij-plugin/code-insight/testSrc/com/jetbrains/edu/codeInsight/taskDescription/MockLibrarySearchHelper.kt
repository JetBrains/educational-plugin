package com.jetbrains.edu.codeInsight.taskDescription

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ext.LibrarySearchHelper

class MockLibrarySearchHelper : LibrarySearchHelper {
  override fun isLibraryExists(project: Project): Boolean = true
}
