/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package com.jetbrains.edu.learning.projectView

import com.intellij.ide.SelectInTarget
import com.intellij.ide.impl.ProjectViewSelectInTarget
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.impl.AbstractProjectViewPSIPane
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase
import com.intellij.ide.projectView.impl.ProjectTreeStructure
import com.intellij.ide.projectView.impl.ProjectViewTree
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.treeView.AbstractTreeBuilder
import com.intellij.ide.util.treeView.AbstractTreeUpdater
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.progress.util.ColorProgressBar
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ObjectUtils
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import icons.EducationalCoreIcons
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.TestOnly
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.border.EmptyBorder
import javax.swing.plaf.basic.BasicProgressBarUI
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class CourseViewPane(project: Project) : AbstractProjectViewPSIPane(project) {
  private lateinit var progressBar: JProgressBar

  override fun createTree(treeModel: DefaultTreeModel): ProjectViewTree {
    return object : ProjectViewTree(myProject, treeModel) {
      override fun getSelectedNode(): DefaultMutableTreeNode? {
        val path = selectedPath
        return if (path == null) null else ObjectUtils.tryCast(path.lastPathComponent, DefaultMutableTreeNode::class.java)
      }

      override fun toString(): String {
        return title + " " + super.toString()
      }
    }
  }

  override fun createComponent(): JComponent {
    val component = super.createComponent()
    if (!EduUtils.isStudentProject(myProject)) return component
    val panel = JPanel(BorderLayout())
    panel.background = UIUtil.getTreeBackground()

    panel.add(createProgressPanel(), BorderLayout.NORTH)
    panel.add(tree, BorderLayout.CENTER)

    updateCourseProgress()
    return ScrollPaneFactory.createScrollPane(panel)
  }

  private fun createProgressPanel(): JPanel {
    val panel = JPanel(BorderLayout())

    progressBar = JProgressBar()

    progressBar.ui = object : BasicProgressBarUI() {
      override fun getPreferredSize(c: JComponent?): Dimension {
        return Dimension(super.getPreferredSize(c).width, 4)
      }
    }
    progressBar.background = JBColor(Gray._237, Color(76, 77, 79))
    progressBar.foreground = ColorProgressBar.GREEN
    progressBar.isIndeterminate = false
    progressBar.putClientProperty("ProgressBar.flatEnds", java.lang.Boolean.TRUE)
    panel.background = UIUtil.getTreeBackground()
    panel.add(progressBar, BorderLayout.NORTH)
    panel.border = EmptyBorder(0, 0, 5, 0)
    return panel
  }

  override fun addToolbarActions(actionGroup: DefaultActionGroup?) {
    actionGroup?.removeAll()
    val hideSolvedLessons = object: ToggleAction("Hide Solved Lessons"), DumbAware {
      override fun isSelected(p0: AnActionEvent?): Boolean {
        return PropertiesComponent.getInstance().getBoolean(HIDE_SOLVED_LESSONS, false)
      }

      override fun setSelected(p0: AnActionEvent?, p1: Boolean) {
        val hideSolved = PropertiesComponent.getInstance().getBoolean(HIDE_SOLVED_LESSONS, false)
        PropertiesComponent.getInstance().setValue(HIDE_SOLVED_LESSONS, !hideSolved)
        ProjectView.getInstance(myProject).refresh()
      }
    }
    actionGroup?.add(hideSolvedLessons)
  }

  fun updateCourseProgress() {
    val course = StudyTaskManager.getInstance(myProject).course
    val lessons = course?.lessons

    var progress = ProgressUtil.countProgressAsOneTaskWithSubtasks(lessons!!)
    if (progress == null) {
      progress = ProgressUtil.countProgressWithoutSubtasks(lessons)
    }

    val taskSolved = progress.getFirst()
    val tasksTotal = progress.getSecond()

    progressBar.maximum = tasksTotal
    progressBar.value = taskSolved
  }

  @TestOnly
  fun getProgressBar(): JProgressBar {
    return progressBar
  }

  override fun createStructure(): ProjectAbstractTreeStructureBase {
    return object : ProjectTreeStructure(myProject, ID) {}
  }

  override fun createTreeUpdater(treeBuilder: AbstractTreeBuilder): AbstractTreeUpdater {
    return AbstractTreeUpdater(treeBuilder)
  }

  override fun getTitle(): String {
    return ID
  }

  override fun getIcon(): Icon {
    return EducationalCoreIcons.Course
  }

  override fun getId(): String {
    return ID
  }

  override fun getWeight(): Int {
    return 10
  }

  override fun createSelectInTarget(): SelectInTarget {
    return object : ProjectViewSelectInTarget(myProject) {
      override fun getMinorViewId(): String? {
        return ID
      }

      override fun toString(): String {
        return ID
      }
    }
  }

  override fun supportsFoldersAlwaysOnTop(): Boolean {
    return false
  }

  override fun supportsSortByType(): Boolean {
    return false
  }

  companion object {
    @NonNls
    val ID = "Course"
    const val HIDE_SOLVED_LESSONS = "Edu.HideSolvedLessons"
  }
}
