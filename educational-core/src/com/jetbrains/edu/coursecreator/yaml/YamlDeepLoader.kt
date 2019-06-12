package com.jetbrains.edu.coursecreator.yaml

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.yaml.YamlDeserializer.deserializeContent
import com.jetbrains.edu.coursecreator.yaml.format.getRemoteChangeApplierForItem
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task

object YamlDeepLoader {
  @JvmStatic
  fun loadCourse(project: Project): Course? {
    val projectDir = project.courseDir
    val courseConfig = projectDir.findChild(YamlFormatSettings.COURSE_CONFIG)
    if (courseConfig == null) {
      val notification = Notification("Edu.InvalidConfig", "Invalid yaml",
                                      "Cannot load course. Config file '${YamlFormatSettings.COURSE_CONFIG}' not found.",
                                      NotificationType.ERROR)
      notification.notify(project)
      return null
    }

    val deserializedCourse = YamlDeserializer.deserializeItem(project, courseConfig) as? Course ?: return null
    deserializedCourse.courseMode = if (EduUtils.isStudentProject(project)) EduNames.STUDY else CCUtils.COURSE_MODE

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


    // we init course before setting description and remote info, as we have to set parent item
    // to obtain description/remote config file to set info from
    deserializedCourse.init(null, null, true)
    deserializedCourse.loadRemoteInfoRecursively(project)
    deserializedCourse.setDescriptionInfo(project)
    return deserializedCourse
  }

  private fun Course.loadRemoteInfoRecursively(project: Project) {
    course.loadRemoteInfo(project)
    sections.forEach { section -> section.loadRemoteInfo(project) }

    // top-level and from sections
    visitLessons { lesson ->
      lesson.loadRemoteInfo(project)
      lesson.taskList.forEach { task -> task.loadRemoteInfo(project) }
    }
  }

  private fun StudyItem.loadRemoteInfo(project: Project) {
    val itemDir = getDir(project) ?: yamlIllegalStateError(noDirForItemMessage(name))
    val remoteConfigFile = itemDir.findChild(remoteConfigFileName)
    if (remoteConfigFile == null) {
      if (id > 0) {
        yamlIllegalStateError(notFoundMessage("config file $remoteConfigFileName", "item: '$name'"))
      }
      else return
    }

    val courseWithRemoteInfo = YamlDeserializer.deserializeRemoteItem(VfsUtil.loadText(remoteConfigFile), remoteConfigFile.name)
    getRemoteChangeApplierForItem(courseWithRemoteInfo).applyChanges(this, courseWithRemoteInfo)
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
    val taskDir = getTaskDir(project) ?: yamlIllegalStateError(noItemDirMessage("task", name))
    val file = taskDir.findChild(EduNames.TASK_HTML) ?: taskDir.findChild(EduNames.TASK_MD)
    return file ?: error("No task description file for $name")
  }

  private fun VirtualFile.toDescriptionFormat(): DescriptionFormat {
    return DescriptionFormat.values().firstOrNull { it.fileExtension == extension } ?: yamlIllegalStateError("Invalid description format")
  }
}