package com.jetbrains.edu.learning.stepik.builtInServer

import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.PathUtil
import com.intellij.util.xmlb.XmlSerializationException
import com.jetbrains.edu.learning.EduNames.STUDY_PROJECT_XML_PATH
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.yaml.YamlDeepLoader.loadRemoteInfo
import com.jetbrains.edu.learning.yaml.YamlDeserializerFactory
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_COURSE_CONFIG
import org.jdom.Element
import org.jdom.JDOMException
import org.jdom.input.SAXBuilder
import java.io.File
import java.io.IOException

object EduBuiltInServerUtils {

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
      project = ProjectUtil.openProject(projectPath, null, true)
      project?.requestFocus()
    }
    return project
  }

  private fun Project.requestFocus() = ProjectUtil.focusProjectWindow(this, true)

  @JvmStatic
  fun openRecentProject(coursePredicate: (Course) -> Boolean): Pair<Project, Course>? {
    val recentPaths = RecentProjectsManagerBase.instanceEx.getRecentPaths()
    val parser = SAXBuilder()

    for (projectPath in recentPaths) {
      val component = readComponent(parser, projectPath)
      val course = if (component != null) getCourse(component) else getCourseFromYaml(projectPath)
      if (course == null) {
        continue
      }
      if (coursePredicate(course)) {
        val project = openProject(projectPath) ?: continue
        val realProjectCourse = project.course ?: continue
        return project to realProjectCourse
      }
    }
    return null
  }

  private fun getCourseFromYaml(projectPath: String): Course? {
    val projectFile = File(PathUtil.toSystemDependentName(projectPath))
    val projectDir = VfsUtil.findFile(projectFile.toPath(), true) ?: return null
    val remoteInfoConfig = projectDir.findChild(REMOTE_COURSE_CONFIG) ?: return null
    val localCourseConfig = projectDir.findChild(COURSE_CONFIG) ?: return null
    return runReadAction {
      val localCourse = YamlDeserializerFactory.getDefaultDeserializer().deserializeItem(localCourseConfig, null) as? Course
                        ?: return@runReadAction null
      localCourse.loadRemoteInfo(remoteInfoConfig, YamlDeserializerFactory.getDeserializer(localCourse))
      localCourse
    }
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

}
