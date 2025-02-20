package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.AdditionalFilesUtils.collectAdditionalFiles
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager.findConfigurator
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_PROJECTS_URL
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.shouldBeEmpty
import com.jetbrains.edu.learning.courseFormat.ext.updateDescriptionTextAndFormat
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.configFileName
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.mapper
import com.jetbrains.edu.learning.yaml.YamlLoader.deserializeContent
import com.jetbrains.edu.learning.yaml.errorHandling.RemoteYamlLoadingException
import com.jetbrains.edu.learning.yaml.errorHandling.loadingError
import com.jetbrains.edu.learning.yaml.format.getRemoteChangeApplierForItem
import com.jetbrains.edu.learning.yaml.migrate.ADDITIONAL_FILES_COLLECTOR_MAPPER_KEY
import com.jetbrains.edu.learning.yaml.migrate.YAML_VERSION_MAPPER_KEY
import com.jetbrains.edu.learning.yaml.migrate.YamlMigrator
import org.jetbrains.annotations.NonNls

object YamlDeepLoader {
  private val HYPERSKILL_PROJECT_REGEX = "$HYPERSKILL_PROJECTS_URL/(\\d+)/.*".toRegex()
  private val LOG = Logger.getInstance(YamlDeepLoader::class.java)

  fun loadCourse(project: Project): Course? {
    val projectDir = project.courseDir

    @NonNls
    val errorMessageToLog = "Course yaml config cannot be null"
    val courseConfig = projectDir.findChild(YamlConfigSettings.COURSE_CONFIG) ?: error(errorMessageToLog)

    // the initial mapper has no idea whether the course is in the CC or in the Student mode
    val initialMapper = YamlMapper.basicMapper()
    initialMapper.setupForMigration(project)
    val deserializedCourse = deserializeItemProcessingErrors(courseConfig, project, mapper=initialMapper) as? Course ?: return null
    val needMigration = YamlMigrator(initialMapper).needMigration()

    // this mapper already respects course mode, it will be used to deserialize all other course items
    val mapper = deserializedCourse.mapper()
    mapper.setupForMigration(project)
    mapper.setEduValue(YAML_VERSION_MAPPER_KEY, initialMapper.getEduValue(YAML_VERSION_MAPPER_KEY))

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

    if (needMigration) {
      project.invokeLater {
        // After migration, we save all YAMLs back to disk.
        // In theory, com.jetbrains.edu.learning.yaml.YamlLoader.loadItem() could be fired before the migrated YAMLs are saved,
        // and that could lead to incorrectly read YAML.
        // One of the dangerous places: the FileEditorManagerListener calls loadItem() to refresh editor notifications for
        // YAML files, and this happens right after the project is loaded.
        YamlFormatSynchronizer.saveAll(project)
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
    try {
      val remoteConfigFile = remoteConfigFile(project)
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
    catch (th: Throwable) {
      throw RemoteYamlLoadingException(this, th)
    }
  }

  fun StudyItem.loadRemoteInfo(remoteConfigFile: VirtualFile) {
    val itemRemoteInfo = YamlDeserializer.deserializeRemoteItem(remoteConfigFile.name, VfsUtil.loadText(remoteConfigFile))
    if (itemRemoteInfo.id > 0 || itemRemoteInfo is HyperskillCourse) {
      getRemoteChangeApplierForItem(itemRemoteInfo).applyChanges(this, itemRemoteInfo)
    }
  }

  /**
   * Reloads the content of a remote config if it exists.
   */
  fun StudyItem.reloadRemoteInfo(project: Project) {
    val remoteConfigFile = remoteConfigFile(project) ?: return
    loadRemoteInfo(remoteConfigFile)
  }

  private fun Course.setDescriptionInfo(project: Project) {
    visitLessons { lesson ->
      lesson.visitTasks {
        it.updateDescriptionTextAndFormat(project)
      }
    }
  }

  /**
   * Adds all edu values to ObjectMapper needed for YAML migration.
   * If a new migration step is implemented, add here all the edu values necessary for that migration step to work.
   */
  private fun ObjectMapper.setupForMigration(project: Project) {
    setEduValue(ADDITIONAL_FILES_COLLECTOR_MAPPER_KEY) { courseType, environment, languageId ->
      val language = Language.findLanguageByID(languageId)
      if (language == null) {
        LOG.warn("Failed to find language with ID $languageId during course YAML migration: collect additional files")
        return@setEduValue emptyList()
      }
      val configurator = findConfigurator(courseType, environment, language)

      if (configurator == null) {
        LOG.warn("Failed to find EduConfigurator during course YAML migration: courseType=$courseType environment=$environment languageId=$languageId")
        return@setEduValue emptyList()
      }

      return@setEduValue collectAdditionalFiles(configurator, project, saveDocuments = false, detectTaskFoldersByContents = true)
    }
  }
}