package com.jetbrains.edu.learning.courseView

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.projectView.CourseViewPane

abstract class CourseViewTestBase : EduActionTestCase() {
  protected fun assertCourseView(structure: String) {
    val tree = createPane().tree
    PlatformTestUtil.waitWhileBusy(tree)
    TreeUtil.expandAll(tree)
    PlatformTestUtil.waitWhileBusy(tree)
    PlatformTestUtil.assertTreeEqual(tree, structure + "\n")
  }

  protected fun createPane(): CourseViewPane {
    val pane = CourseViewPane(project)
    pane.createComponent()
    Disposer.register(testRootDisposable, pane)
    return pane
  }
}
