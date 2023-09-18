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
import com.intellij.ide.projectView.ProjectViewSettings
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.AbstractProjectViewPaneWithAsyncSupport
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase
import com.intellij.ide.projectView.impl.ProjectTreeStructure
import com.intellij.ide.projectView.impl.ProjectViewTree
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ArrayUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.coursecreator.CCStudyItemDeleteProvider
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.projectView.CCLessonNode
import com.jetbrains.edu.coursecreator.projectView.CCSectionNode
import com.jetbrains.edu.coursecreator.projectView.CCTaskNode
import com.jetbrains.edu.learning.CourseSetListener
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.marketplace.actions.ShareMySolutionsAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.projectView.ProgressUtil.createProgressBar
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.TestOnly
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.border.EmptyBorder
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class CourseViewPane(project: Project) : AbstractProjectViewPaneWithAsyncSupport(project) {

  private val myStudyItemDeleteProvider = CCStudyItemDeleteProvider()

  private lateinit var progressBar: JProgressBar

  override fun createTree(treeModel: DefaultTreeModel): ProjectViewTree {
    return object : ProjectViewTree(treeModel) {
      override fun toString(): String = "$title ${super.toString()}"
    }
  }

  override fun createComponent(): JComponent {
    val component = super.createComponent()

    if (!myProject.isStudentProject()) return component
    val panel = JPanel(BorderLayout())
    panel.background = UIUtil.getTreeBackground()

    panel.add(createProgressPanel(), BorderLayout.NORTH)
    panel.add(tree, BorderLayout.CENTER)

    if (StudyTaskManager.getInstance(myProject).course != null) {
      updateCourseProgress()
    }
    else {
      val connection = myProject.messageBus.connect()
      connection.subscribe(StudyTaskManager.COURSE_SET, object : CourseSetListener {
        override fun courseSet(course: Course) {
          connection.disconnect()
          updateCourseProgress()
        }
      })
    }
    return ScrollPaneFactory.createScrollPane(panel)
  }

  override fun createComparator(): Comparator<NodeDescriptor<*>> = EduNodeComparator

  private fun createProgressPanel(): JPanel {
    val panel = JPanel(BorderLayout())

    progressBar = createProgressBar()
    panel.background = UIUtil.getTreeBackground()
    panel.add(progressBar, BorderLayout.NORTH)
    panel.border = EmptyBorder(0, 0, 5, 0)
    return panel
  }

  override fun addToolbarActions(actionGroup: DefaultActionGroup) {
    actionGroup.removeAll()
    val hideSolvedLessons = object : ToggleAction(EduCoreBundle.message("action.hide.solved.lessons.text")), DumbAware {
      override fun isSelected(e: AnActionEvent): Boolean {
        return PropertiesComponent.getInstance().getBoolean(HIDE_SOLVED_LESSONS, false)
      }

      override fun setSelected(e: AnActionEvent, state: Boolean) {
        val hideSolved = PropertiesComponent.getInstance().getBoolean(HIDE_SOLVED_LESSONS, false)
        PropertiesComponent.getInstance().setValue(HIDE_SOLVED_LESSONS, !hideSolved)
        ProjectView.getInstance(myProject).refresh()
      }
    }
    actionGroup.addAll(hideSolvedLessons, ShareMySolutionsAction())
  }

  private fun updateCourseProgress() {
    val course = StudyTaskManager.getInstance(myProject).course
    if (course == null) {
      Logger.getInstance(CourseViewPane::class.java).error("course is null")
      return
    }
    val (taskSolved, tasksTotal) = ProgressUtil.countProgress(course)

    updateCourseProgress(tasksTotal, taskSolved)
  }

  fun updateCourseProgress(tasksTotal: Int, taskSolved: Int) {
    progressBar.maximum = tasksTotal
    progressBar.value = taskSolved
  }

  @TestOnly
  fun getProgressBar(): JProgressBar = progressBar

  override fun createStructure(): ProjectAbstractTreeStructureBase = object : ProjectTreeStructure(myProject, ID), ProjectViewSettings {
    override fun createRoot(project: Project, settings: ViewSettings): AbstractTreeNode<*> {
      return RootNode(myProject, settings)
    }

    override fun getChildElements(element: Any): Array<Any> {
      if (element !is AbstractTreeNode<*>) {
        return ArrayUtil.EMPTY_OBJECT_ARRAY
      }
      val elements = element.children
      elements.forEach { node -> node.setParent(element) }
      return ArrayUtil.toObjectArray(elements)
    }

    override fun isShowExcludedFiles() = false
  }

  override fun getTitle(): String = EduCoreBundle.message("project.view.course.pane.title")

  override fun getIcon(): Icon = EducationalCoreIcons.CourseTree
  override fun getId(): String = ID
  override fun getWeight(): Int = 10

  override fun createSelectInTarget(): SelectInTarget {
    return object : ProjectViewSelectInTarget(myProject) {
      override fun getMinorViewId(): String = ID
      override fun toString(): String = ID
    }
  }

  @Suppress("UnstableApiUsage")
  override fun supportsFoldersAlwaysOnTop(): Boolean = false
  @Suppress("UnstableApiUsage")
  override fun supportsSortByType(): Boolean = false

  override fun getData(dataId: String): Any? {
    if (myProject.isDisposed) return null

    if (CCUtils.isCourseCreator(myProject)) {
      val studyItem = when (val userObject = (selectedPath?.lastPathComponent as? DefaultMutableTreeNode)?.userObject) {
        is CCTaskNode -> userObject.item
        is CCLessonNode -> userObject.item
        is CCSectionNode -> userObject.item
        else -> null
      }
      if (studyItem != null) {
        when {
          PlatformDataKeys.DELETE_ELEMENT_PROVIDER.`is`(dataId) -> return myStudyItemDeleteProvider
          STUDY_ITEM.`is`(dataId) -> return studyItem
        }
      }
    }
    return super.getData(dataId)
  }

  override fun isDefaultPane(project: Project): Boolean = project.isEduProject()

  companion object {
    @NonNls
    const val ID = "Course"
    const val HIDE_SOLVED_LESSONS = "Edu.HideSolvedLessons"

    val STUDY_ITEM: DataKey<StudyItem> = DataKey.create("Edu.studyItem")
  }
}
