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
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.StudyTaskManager
import icons.EducationalCoreIcons
import org.jetbrains.annotations.NonNls
import javax.swing.Icon
import javax.swing.tree.DefaultTreeModel

class CourseViewPane(project: Project) : AbstractProjectViewPSIPane(project) {
  override fun createTree(treeModel: DefaultTreeModel): ProjectViewTree {
    val tree = object : ProjectViewTree(myProject, treeModel) {
      override fun toString(): String {
        return title + " " + super.toString()
      }
    }

    val course = StudyTaskManager.getInstance(myProject).course
    if (course != null) {
      tree.cellRenderer = CourseTreeRenderer(course)
    }
    tree.rowHeight = -1

    return tree
  }

  override fun addToolbarActions(actionGroup: DefaultActionGroup?) {
    actionGroup?.removeAll()
    val hideSolvedLessons = object: DumbAwareToggleAction("Hide Solved Lessons"){
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

  fun updateCourseProgress(project: Project) {
    val course = StudyTaskManager.getInstance(project).course
    if (tree.cellRenderer is CourseTreeRenderer && course != null) {
      (tree.cellRenderer as CourseTreeRenderer).updateCourseProgress(course)
    }
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
