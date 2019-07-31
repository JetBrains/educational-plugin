package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.yaml.YamlDeserializer.deserializeContent
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer.MAPPER
import com.jetbrains.edu.coursecreator.yaml.format.getRemoteChangeApplierForItem
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.yaml.EduYamlUtil

object YamlDeepLoader {
  @JvmStatic
  fun loadCourse(project: Project): Course? {
    val projectDir = project.courseDir
    val courseConfig = projectDir.findChild(YamlFormatSettings.COURSE_CONFIG) ?: error("Course yaml config cannot be null")

    val deserializedCourse = YamlDeserializer.deserializeItem(project, courseConfig) as? Course ?: return null
    val mapper = if (deserializedCourse.isStudy) EduYamlUtil.EDU_MAPPER else MAPPER

    deserializedCourse.items = deserializedCourse.deserializeContent(project, deserializedCourse.items, mapper)
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

          deserializedItem.removeNonExistingTaskFiles(project)
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

  /**
   * If project was opened with a config file containing task file that doesn't have the corresponding dir,
   * we remove it from task object but keep in the config file.
   */
  private fun Lesson.removeNonExistingTaskFiles(project: Project) {
    taskList.forEach { task ->
      // set parent to get dir
      task.lesson = this
      val taskDir = task.getDir(project)
      val invalidTaskFilesNames = task.taskFiles
        .filter { (name, _) -> taskDir?.findFileByRelativePath(name) == null }.map { it.key }
      invalidTaskFilesNames.forEach { task.taskFiles.remove(it) }
    }
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
    val itemDir = getDir(project) ?: error(noDirForItemMessage(name))
    val remoteConfigFile = itemDir.findChild(remoteConfigFileName)
    if (remoteConfigFile == null) {
      if (id > 0) {
        loadingError(notFoundMessage("config file $remoteConfigFileName", "item '$name'"))
      }
      else return
    }

    val courseWithRemoteInfo = YamlDeserializer.deserializeRemoteItem(remoteConfigFile)
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
    val taskDir = getTaskDir(project) ?: error(noDirForItemMessage(name, EduNames.TASK))
    val file = taskDir.findChild(EduNames.TASK_HTML) ?: taskDir.findChild(EduNames.TASK_MD)
    return file ?: error("No task description file for $name")
  }

  private fun VirtualFile.toDescriptionFormat(): DescriptionFormat {
    return DescriptionFormat.values().firstOrNull { it.fileExtension == extension } ?: loadingError("Invalid description format")
  }
}