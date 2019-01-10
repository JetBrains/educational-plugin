package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.coursecreator.CCProjectComponent
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSettings
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSynchronizer
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSynchronizer.deserializeLesson
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSynchronizer.deserializeTask
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduProjectComponent
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindowFactory

class LoadCourseFromConfigs : DumbAwareAction("Load course from configs") {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val projectDir = project.guessProjectDir() ?: return
    val courseConfig = projectDir.findChild(YamlFormatSettings.COURSE_CONFIG) ?: return
    val course = loadStudyItem(courseConfig, Course::class.java)
    val courseDir = course.getDir(project)
    val items = course.items.map {
      val itemDir = courseDir.findChild(it.name) ?: throwNoMatchingDirError(it)
      val sectionConfig = itemDir.findChild(YamlFormatSettings.SECTION_CONFIG)
      if (sectionConfig != null) {
        //load section
        val section = loadStudyItem(sectionConfig, Section::class.java)
        return@map section
      }
      else {
        val lessonConfig = itemDir.findChild(YamlFormatSettings.LESSON_CONFIG) ?: throwNoConfigFileError(it)
        val lesson = loadLessonFromConfig(lessonConfig, project, course, it.name)
        return@map lesson
      }
    }

    for ((i, item) in items.withIndex()) {
      item.index = i + 1
    }
    course.items = items
    StudyTaskManager.getInstance(project).course = course
    course.courseMode = CCUtils.COURSE_MODE
    course.init(null, null, false)
    if (getTaskDescriptionToolWindow(project) == null) {
      ToolWindowManager.getInstance(project).registerToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW, false,
                                                                ToolWindowAnchor.RIGHT)
      val toolWindow = getTaskDescriptionToolWindow(project) ?: error("Task Description tool window not found")
      TaskDescriptionToolWindowFactory().createToolWindowContent(project, toolWindow)
    }
    project.getComponent(EduProjectComponent::class.java).projectOpened()
    project.getComponent(CCProjectComponent::class.java).projectOpened()
  }

  private fun throwNoMatchingDirError(it: StudyItem): Nothing {
    error("Failed to find matching dir for ${it.name}")
  }

  private fun throwNoConfigFileError(it: StudyItem): Nothing {
    error("No config file for ${it.name}")
  }

  private fun loadLessonFromConfig(configFile: VirtualFile,
                                   project: Project,
                                   course: Course,
                                   name: String): Lesson {
    val lesson = deserializeLesson(VfsUtil.loadText(configFile))
    lesson.name = name
    lesson.course = course
    val tasks = lesson.taskList.map {
      val taskDir = configFile.parent.findChild(it.name) ?: throwNoMatchingDirError(it)
      val taskConfig = taskDir.findChild(YamlFormatSettings.TASK_CONFIG) ?: throwNoConfigFileError(it)
      val task = deserializeTask(VfsUtil.loadText(taskConfig))
      task.name = it.name
      task.lesson = lesson
      val taskDescriptionFile = findTaskDescriptionFile(task, project)
      task.descriptionFormat = taskDescriptionFile.toDescriptionFormat()
      task.descriptionText = VfsUtil.loadText(taskDescriptionFile)
      task
    }
    for ((i, item) in tasks.withIndex()) {
      item.index = i + 1
    }
    lesson.taskList = tasks
    return lesson
  }

  private fun findTaskDescriptionFile(task: Task, project: Project) : VirtualFile {
    val taskDir = task.getTaskDir(project) ?: error("No task dir for task ${task.name}")
    val htmlFile = taskDir.findChild(EduNames.TASK_HTML)
    if (htmlFile != null) {
      return htmlFile
    }
    return taskDir.findChild(EduNames.TASK_MD) ?: error("No task description file for ${task.name}")
  }


  private fun VirtualFile.toDescriptionFormat(): DescriptionFormat {
    return DescriptionFormat.values().firstOrNull { it.fileExtension == extension } ?: error("Invalid description format")
  }

  private fun getTaskDescriptionToolWindow(project: Project) =
    ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW)

  private fun <T : StudyItem> loadStudyItem(courseConfig: VirtualFile, clazz: Class<T>): T =
    YamlFormatSynchronizer.MAPPER.readValue(VfsUtil.loadText(courseConfig), clazz) ?: error(
      "Failed to load ${clazz.simpleName} from ${courseConfig.path}")
}