package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.BaseProjectTreeBuilder
import com.intellij.ide.projectView.impl.AbstractProjectViewPSIPane
import com.intellij.ide.util.treeView.AbstractTreeBuilder
import com.intellij.ide.util.treeView.AbstractTreeUpdater
import com.intellij.openapi.project.Project
import javax.swing.tree.DefaultTreeModel

abstract class CourseViewPaneBase(project: Project) : AbstractProjectViewPSIPane(project) {
  @Suppress("UnstableApiUsage")
  override fun createBuilder(treeModel: DefaultTreeModel): BaseProjectTreeBuilder? = null

  @Suppress("UnstableApiUsage")
  override fun createTreeUpdater(treeBuilder: AbstractTreeBuilder): AbstractTreeUpdater = error("This tree is async now")

}