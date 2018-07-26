package com.jetbrains.edu.learning.courseView

import com.intellij.ide.projectView.ProjectView
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.ProjectViewTestUtil
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.projectView.CourseViewPane
import javax.swing.JTree

abstract class CourseViewTestBase : EduActionTestCase() {

  override fun setUp() {
    super.setUp()
    ProjectViewTestUtil.setupImpl(project, true)
  }

  protected fun assertCourseView(structure: String, ignoreOrder: Boolean = false) {
    val projectView = ProjectView.getInstance(project)
    projectView.refresh()
    projectView.changeView(CourseViewPane.ID)
    val pane = projectView.currentProjectViewPane
    val tree = pane.tree
    waitWhileBusy(tree)
    TreeUtil.expandAll(tree)
    waitWhileBusy(tree)
    if (ignoreOrder) {
      PlatformTestUtil.assertTreeEqualIgnoringNodesOrder(tree, structure + "\n")
    } else {
      PlatformTestUtil.assertTreeEqual(tree, structure + "\n")
    }
  }

  protected fun waitWhileBusy(tree: JTree) {
    PlatformTestUtil.waitWhileBusy(tree)
  }
}
