package com.jetbrains.edu.java.courseView

import com.intellij.ide.projectView.impl.ProjectViewState
import com.jetbrains.edu.learning.courseView.CourseViewTestBase

abstract class JNodesTestsBase : CourseViewTestBase() {
  override fun setUp() {
    super.setUp()
    // Since 2020.1 this setting is disabled by default in tests
    ProjectViewState.getInstance(project).hideEmptyMiddlePackages = true
  }
}
