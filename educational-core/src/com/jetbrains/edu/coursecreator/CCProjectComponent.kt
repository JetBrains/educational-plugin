package com.jetbrains.edu.coursecreator

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener
import com.jetbrains.edu.coursecreator.yaml.YamlDeserializer
import com.jetbrains.edu.coursecreator.yaml.YamlDeserializer.deserializeContent
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.COURSE_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.isEduYamlProject
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.coursecreator.yaml.YamlLoader
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.statistics.EduUsagesCollector

@Suppress("ComponentNotRegistered") // educational-core.xml
class CCProjectComponent(private val myProject: Project) : ProjectComponent {
  private var myTaskFileLifeListener: CCVirtualFileListener? = null

  private fun startTaskDescriptionFilesSynchronization() {
    StudyTaskManager.getInstance(myProject).course ?: return
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(SynchronizeTaskDescription(myProject), myProject)
  }

  override fun getComponentName(): String {
    return "CCProjectComponent"
  }

  override fun projectOpened() {
    // it is also false for newly created courses as config files isn't created yet.
    // it's ok as we don't need to load course from config
    if (myProject.isEduYamlProject()) {
      loadCourse()
    }

    if (StudyTaskManager.getInstance(myProject).course != null) {
      initCCProject()
    }
    else {
      val connection = myProject.messageBus.connect()
      connection.subscribe(StudyTaskManager.COURSE_SET, object : CourseSetListener {
        override fun courseSet(course: Course) {
          connection.disconnect()
          initCCProject()
        }
      })
    }
  }

  private fun initCCProject() {
    if (CCUtils.isCourseCreator(myProject)) {
      if (!ApplicationManager.getApplication().isUnitTestMode) {
        registerListener()
      }

      EduUsagesCollector.projectTypeOpened(CCUtils.COURSE_MODE)
      startTaskDescriptionFilesSynchronization()

      YamlFormatSynchronizer.startSynchronization(myProject)

      if (!ApplicationManager.getApplication().isUnitTestMode) {
        // check key value in post-startup activity as key is put after project is opened
        StartupManager.getInstance(myProject).registerPostStartupActivity {
          // for existing project, create missing files if feature was enabled after project was created
          if (myProject.getUserData(CourseProjectGenerator.EDU_PROJECT_CREATED) == null) {
            YamlFormatSynchronizer.saveAll(myProject)
          }
        }
      }
    }
  }

  private fun loadCourse() {
    val course = loadCourseRecursively(myProject)
    StudyTaskManager.getInstance(myProject).course = course
  }

  private fun registerListener() {
    if (myTaskFileLifeListener == null) {
      myTaskFileLifeListener = CCVirtualFileListener(myProject)
      VirtualFileManager.getInstance().addVirtualFileListener(myTaskFileLifeListener!!)
    }
  }

  override fun projectClosed() {
    if (myTaskFileLifeListener != null) {
      VirtualFileManager.getInstance().removeVirtualFileListener(myTaskFileLifeListener!!)
    }
  }
}

private fun loadCourseRecursively(project: Project): Course {
  val projectDir = project.courseDir
  val courseConfig = projectDir.findChild(YamlFormatSettings.COURSE_CONFIG) ?: error(
    "Cannot load course. Config file '${YamlFormatSettings.COURSE_CONFIG}' not found.")

  val deserializedCourse = YamlDeserializer.deserialize(VfsUtil.loadText(courseConfig), Course::class.java)
  deserializedCourse.courseMode = if (EduUtils.isStudentProject(project)) EduNames.STUDY else CCUtils.COURSE_MODE
  deserializedCourse.setRemoteInfoFromConfig(project.courseDir.findChild(REMOTE_COURSE_CONFIG))

  deserializedCourse.items = deserializedCourse.deserializeContent(project, deserializedCourse.items)
  deserializedCourse.items.forEach { deserializedItem ->
    when (deserializedItem) {
      is Section -> {
        // set parent to correctly obtain dirs in deserializeContent method
        deserializedItem.course = deserializedCourse
        deserializedItem.items = deserializedItem.deserializeContent(project, deserializedItem.items)
        deserializedItem.lessons.forEach {
          it.section = deserializedItem
          it.items = it.deserializeContent(project, it.taskList)
        }
      }
      is Lesson -> {
        // set parent to correctly obtain dirs in deserializeContent method
        deserializedItem.course = deserializedCourse
        deserializedItem.items = deserializedItem.deserializeContent(project, deserializedItem.taskList)
      }
    }
  }

  // we init course before setting description info, as we have to set lessons for tasks first
  // to obtain task description file to set description info from
  deserializedCourse.init(null, null, true)
  deserializedCourse.setDescriptionInfo(project)
  return deserializedCourse
}

private fun Course.setRemoteInfoFromConfig(remoteCourseConfig: VirtualFile?) {
  if (this !is EduCourse || remoteCourseConfig == null) {
    return
  }

  val courseWithRemoteInfo = YamlDeserializer.deserializeCourseWithRemoteInfo(VfsUtil.loadText(remoteCourseConfig))
  id = courseWithRemoteInfo.id
  updateDate = courseWithRemoteInfo.updateDate
}

private fun Course.setDescriptionInfo(project: Project) {
  visitLessons { lesson ->
    lesson.visitTasks {
      val taskDescriptionFile = it.findTaskDescriptionFile(project)
      it.descriptionFormat = taskDescriptionFile.toDescriptionFormat()
      it.descriptionText = VfsUtil.loadText(taskDescriptionFile)
    }
  }
}

private fun Task.findTaskDescriptionFile(project: Project): VirtualFile {
  val taskDir = getTaskDir(project) ?: YamlLoader.taskDirNotFoundError(name)
  val file = taskDir.findChild(EduNames.TASK_HTML) ?: taskDir.findChild(EduNames.TASK_MD)
  return file ?: error("No task description file for $name")
}

private fun VirtualFile.toDescriptionFormat(): DescriptionFormat {
  return DescriptionFormat.values().firstOrNull { it.fileExtension == extension } ?: error("Invalid description format")
}
