/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.jetbrains.edu.learning.stepik.builtInServer

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.util.xmlb.XmlSerializationException
import com.jetbrains.edu.learning.EduNames.STUDY_PROJECT_XML_PATH
import com.jetbrains.edu.learning.EduUtils.execCancelable
import com.jetbrains.edu.learning.EduUtils.navigateToStep
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.stepik.StepikProjectComponent.STEP_ID
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import org.jdom.Element
import org.jdom.JDOMException
import org.jdom.input.SAXBuilder
import java.io.File
import java.io.IOException

object EduBuiltInServerUtils {

  @JvmStatic
  fun focusOpenEduProject(courseId: Int, stepId: Int): Boolean {
    val (project, course) = focusOpenProject { it is EduCourse && it.isRemote && it.getId() == courseId } ?: return false
    ApplicationManager.getApplication().invokeLater { navigateToStep(project, course, stepId) }
    return true
  }

  @JvmStatic
  fun focusOpenProject(coursePredicate: (Course) -> Boolean): Pair<Project, Course>? {
    val openProjects = ProjectManager.getInstance().openProjects
    for (project in openProjects) {
      if (project.isDefault) continue
      val course = project.course ?: continue
      if (!coursePredicate(course)) continue
      ApplicationManager.getApplication().invokeLater { project.requestFocus() }
      return project to course
    }
    return null
  }

  private fun openProject(projectPath: String): Project? {
    var project: Project? = null
    ApplicationManager.getApplication().invokeAndWait {
      TransactionGuard.getInstance().submitTransactionAndWait { project = ProjectUtil.openProject(projectPath, null, true) }
      project?.requestFocus()
    }
    return project
  }

  private fun Project.requestFocus() = ProjectUtil.focusProjectWindow(this, true)

  @JvmStatic
  fun openRecentProject(coursePredicate: (Course) -> Boolean): Pair<Project, Course>? {
    val state = recentProjectsManagerInstance.state ?: return null

    val recentPaths = state.recentPaths
    val parser = SAXBuilder()

    for (projectPath in recentPaths) {
      val component = readComponent(parser, projectPath) ?: continue
      val course = getCourse(component) ?: continue
      if (coursePredicate(course)) {
        val project = openProject(projectPath) ?: continue
        val realProjectCourse = project.course ?: continue
        return project to realProjectCourse
      }
    }
    return null
  }

  @JvmStatic
  fun openRecentEduCourse(courseId: Int, stepId: Int): Boolean {
    val course = openRecentProject { it is EduCourse && it.isRemote && it.getId() == courseId }?.second ?: return false
    course.putUserData(STEP_ID, stepId)
    return true
  }

  private fun readComponent(parser: SAXBuilder, projectPath: String): Element? {
    var component: Element? = null
    try {
      val studyProjectXML = projectPath + STUDY_PROJECT_XML_PATH
      val xmlDoc = parser.build(File(studyProjectXML))
      val root = xmlDoc.rootElement
      component = root.getChild("component")
    }
    catch (ignored: JDOMException) {
    }
    catch (ignored: IOException) {
    }

    return component
  }

  private fun getCourse(component: Element): Course? {
    try {
      val studyTaskManager = StudyTaskManager()
      studyTaskManager.loadState(component)
      return studyTaskManager.course
    }
    catch (ignored: IllegalStateException) {
    }
    catch (ignored: XmlSerializationException) {
    }

    return null
  }

  @JvmStatic
  fun createEduCourse(courseId: Int, stepId: Int): Boolean {
    ApplicationManager.getApplication().invokeLater {
      ProgressManager.getInstance().runProcessWithProgressSynchronously({
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        execCancelable<Any> {
          val course = StepikConnector.getInstance().getCourseInfo(courseId, true)
          showDialog(course, stepId)
          null
        }
      }, "Getting Course", true, null)
    }

    return true
  }

  private fun showDialog(course: Course?, stepId: Int) {
    ApplicationManager.getApplication().invokeLater {
      if (course != null) {
        course.putUserData(STEP_ID, stepId)
        JoinCourseDialog(course).show()
      }
      else {
        Messages.showErrorDialog("Can not get course info from Stepik", "Failed to Create Course")
      }
    }
  }
}
