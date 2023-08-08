package com.jetbrains.edu.learning.yaml

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.findTaskDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.shouldBeEmpty
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_PROJECTS_URL
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeContent
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.mapper
import com.jetbrains.edu.learning.yaml.errorHandling.loadingError
import com.jetbrains.edu.learning.yaml.format.getRemoteChangeApplierForItem
import org.jetbrains.annotations.NonNls

object YamlDeepLoader {
  private val HYPERSKILL_PROJECT_REGEX = "$HYPERSKILL_PROJECTS_URL/(\\d+)/.*".toRegex()
  private val LOG = Logger.getInstance(YamlDeepLoader::class.java)

  fun loadCourse(project: Project): Course? {
    val projectDir = project.courseDir

    @NonNls
    val errorMessageToLog = "Course yaml config cannot be null"
    val courseConfig = projectDir.findChild(YamlFormatSettings.COURSE_CONFIG) ?: error(errorMessageToLog)

    val deserializedCourse = YamlDeserializer.deserializeItem(courseConfig, project) as? Course ?: return null
    val mapper = deserializedCourse.mapper

    deserializedCourse.items = deserializedCourse.deserializeContent(project, deserializedCourse.items, mapper)
    deserializedCourse.items.forEach { deserializedItem ->
      when (deserializedItem) {
        is Section -> {
          // set parent to correctly obtain dirs in deserializeContent method
          deserializedItem.parent = deserializedCourse
          deserializedItem.items = deserializedItem.deserializeContent(project, deserializedItem.items, mapper)
          deserializedItem.lessons.forEach {
            it.parent = deserializedItem
            it.items = it.deserializeContent(project, it.taskList, mapper)
          }
        }
        is Lesson -> {
          // set parent to correctly obtain dirs in deserializeContent method
          deserializedItem.parent = deserializedCourse
          deserializedItem.items = deserializedItem.deserializeContent(project, deserializedItem.taskList, mapper)
          addNonEditableFilesToCourse(deserializedItem, deserializedCourse, project)
          deserializedItem.removeNonExistingTaskFiles(project)
        }
      }
    }

    // we initialize course before setting description and remote info, as we have to set parent item
    // to obtain description/remote config file to set info from
    deserializedCourse.init(true)
    deserializedCourse.loadRemoteInfoRecursively(project)
    if (!deserializedCourse.isStudy) {
      deserializedCourse.setDescriptionInfo(project)
    }
    return deserializedCourse
  }

  private fun addNonEditableFilesToCourse(taskContainer: Lesson, course: Course, project: Project) {
    val nonEditableFile = taskContainer.taskList.flatMap { task ->
      task.taskFiles.values.mapNotNull { taskFile ->
        if (taskFile.isEditable) return@mapNotNull null
        project.courseDir
          .findChild(taskContainer.name)
          ?.findChild(task.name)
          ?.findFileByRelativePath(taskFile.name)
      }
    }
    project.invokeLater {
      runWriteAction {
        for (virtualFile in nonEditableFile) {
          GeneratorUtils.addNonEditableFileToCourse(course, virtualFile)
        }
      }
    }
  }

  /**
   * If project was opened with a config file containing task file that doesn't have the corresponding dir,
   * we remove it from task object but keep in the config file.
   */
  private fun Lesson.removeNonExistingTaskFiles(project: Project) {
    for (task in taskList) {
      if (this is FrameworkLesson && task.index != currentTaskIndex + 1) {
        continue
      }
      // set parent to get dir
      task.parent = this
      val taskDir = task.getDir(project.courseDir)
      val invalidTaskFilesNames = task.taskFiles
        .filter { (name, _) -> taskDir?.findFileByRelativePath(name) == null && !task.shouldBeEmpty(name)}.map { it.key }
      invalidTaskFilesNames.forEach { task.removeTaskFile(it) }
    }
  }

  private fun Course.loadRemoteInfoRecursively(project: Project) {
    loadRemoteInfo(project)
    sections.forEach { section -> section.loadRemoteInfo(project) }

    // top-level and from sections
    visitLessons { lesson ->
      lesson.loadRemoteInfo(project)
      lesson.taskList.forEach { task -> task.loadRemoteInfo(project) }
    }

    if (this is HyperskillCourse && hyperskillProject == null) {
      reconnectHyperskillProject()
    }
  }

  private fun HyperskillCourse.reconnectHyperskillProject() {
    LOG.info("Current project is disconnected from Hyperskill")
    val firstTask = getProjectLesson()?.taskList?.firstOrNull() ?: return
    val link = firstTask.feedbackLink ?: return
    val matchResult = HYPERSKILL_PROJECT_REGEX.matchEntire(link) ?: return
    val projectId = matchResult.groupValues[1].toInt()

    ApplicationManager.getApplication().executeOnPooledThread {
      HyperskillConnector.getInstance().getProject(projectId).let {
        when (it) {
          is Err -> return@executeOnPooledThread
          is Ok -> {
            hyperskillProject = it.value
            LOG.info("Current project successfully reconnected to Hyperskill")
          }
        }
      }

      HyperskillConnector.getInstance().getStages(projectId)?.let {
        stages = it
        LOG.info("Stages for disconnected Hyperskill project retrieved")
      }
    }
  }

  private fun StudyItem.loadRemoteInfo(project: Project) {
    val itemDir = getConfigDir(project)
    val remoteConfigFile = itemDir.findChild(remoteConfigFileName)
    if (remoteConfigFile == null) {
      if (id > 0) {
        loadingError(
          EduCoreBundle.message("yaml.editor.invalid.format.config.file.not.found", configFileName, name)
        )
      }
      else return
    }

    loadRemoteInfo(remoteConfigFile)
  }

  fun StudyItem.loadRemoteInfo(remoteConfigFile: VirtualFile) {
    val itemRemoteInfo = YamlDeserializer.deserializeRemoteItem(remoteConfigFile)
    if (itemRemoteInfo.id > 0 || itemRemoteInfo is HyperskillCourse) {
      getRemoteChangeApplierForItem(itemRemoteInfo).applyChanges(this, itemRemoteInfo)
    }
  }

  private fun Course.setDescriptionInfo(project: Project) {
    visitLessons { lesson ->
      lesson.visitTasks {
        val taskDescriptionFile = it.findTaskDescriptionFile(project)
        if (taskDescriptionFile != null) {
          it.descriptionFormat = taskDescriptionFile.toDescriptionFormat()
          it.descriptionText = VfsUtil.loadText(taskDescriptionFile)
        } else {
          it.descriptionFormat = DescriptionFormat.HTML
          it.descriptionText = EduCoreBundle.message("task.description.not.found")
        }
      }
    }
  }

  private fun VirtualFile.toDescriptionFormat(): DescriptionFormat {
    return DescriptionFormat.values().firstOrNull { it.fileExtension == extension } ?: loadingError(
      EduCoreBundle.message("yaml.editor.invalid.description"))
  }
}