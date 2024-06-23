package com.jetbrains.edu.learning

import com.intellij.openapi.project.Project
import com.intellij.testFramework.IndexingTestUtil

// BACKCOMPAT: 2024.1. Inline it
fun waitUntilIndexesAreReady(project: Project) {
  IndexingTestUtil.waitUntilIndexesAreReady(project)
}
