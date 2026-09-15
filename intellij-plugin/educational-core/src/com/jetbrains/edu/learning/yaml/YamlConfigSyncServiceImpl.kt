package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.*
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.disambiguateContents
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.pathInCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.storage.persistAdditionalFiles
import com.jetbrains.edu.learning.storage.persistEduFiles
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.LOAD_FROM_CONFIG
import kotlinx.coroutines.*
import org.jetbrains.annotations.NonNls
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

open class YamlConfigSyncServiceImpl(protected val project: Project, protected val scope: CoroutineScope) : YamlConfigSyncService {

  protected val item2SaveJob: ConcurrentMap<String, Job> = ConcurrentHashMap()
  protected val suppressedSaveKeys: ConcurrentMap<String, Int> = ConcurrentHashMap()

  override fun requestSave(studyItem: StudyItem, configName: String, mapper: ObjectMapper) {
    val saveTask = createSaveTask(studyItem, configName, mapper) ?: return

    val currentModality = ModalityState.defaultModalityState()

    scope.launch(
      context = currentModality.asContextElement(),
      start = CoroutineStart.UNDISPATCHED // the job will start immediately, so it will survive if the scope is already canceled
    ) {
      execute(saveTask)
    }
  }

  override suspend fun save(studyItem: StudyItem, configName: String, mapper: ObjectMapper) {
    val saveTask = createSaveTask(studyItem, configName, mapper) ?: return
    execute(saveTask)
  }

  private suspend fun execute(saveTask: SaveTask) {
    withContext(NonCancellable) {

      val jobKey = saveTask.jobKey

      val currentJob = currentCoroutineContext().job
      val previousJob = item2SaveJob.put(jobKey, currentJob)

      try {
        previousJob?.join()
        doSave(saveTask)
      }
      finally {
        // An older save must not remove a newer save's marker
        item2SaveJob.remove(saveTask.jobKey, currentJob)
      }
    }
  }

  private fun createSaveTask(studyItem: StudyItem, configName: String, mapper: ObjectMapper): SaveTask? {
    val itemDir = studyItem.getConfigDir(project)
    val jobKey = getJobKey(itemDir, configName)

    if (isSaveSuppressed(jobKey)) return null

    return SaveTask(studyItem, itemDir, configName, mapper, jobKey)
  }

  override fun <T> withSaveSuppressed(configFile: VirtualFile?, action: () -> T): T {
    val itemDir = configFile?.parent ?: return action()
    val jobKey = getJobKey(itemDir, configFile.name)
    suppressSave(jobKey)
    return try {
      action()
    }
    finally {
      resumeSave(jobKey)
    }
  }

  private fun getJobKey(itemDir: VirtualFile, configName: String): String = "${itemDir.pathInCourse(project)}/$configName"

  private fun suppressSave(jobKey: String) {
    suppressedSaveKeys.compute(jobKey) { _, counter -> (counter ?: 0) + 1 }
  }

  private fun resumeSave(jobKey: String) {
    suppressedSaveKeys.computeIfPresent(jobKey) { _, counter ->
      (counter - 1).takeIf { it > 0 }
    }
  }

  private fun isSaveSuppressed(jobKey: String): Boolean {
    return suppressedSaveKeys.containsKey(jobKey)
  }

  private suspend fun doSave(saveTask: SaveTask) {
    val formattedYamlText = withContext(Dispatchers.IO) {
      val studyItem = saveTask.studyItem
      if (studyItem is Task) {
        studyItem.disambiguateTaskFilesContents(project)
        studyItem.persistEduFiles(project)
      }

      if (studyItem is Course) {
        studyItem.disambiguateAdditionalFilesContents(project)
        studyItem.persistAdditionalFiles(project)
      }

      val yamlText = saveTask.mapper.writeValueAsString(studyItem)

      reformatYaml(yamlText)
    }

    withContext(Dispatchers.EDT) {
      val file = writeAction {
        if (!saveTask.itemDir.isValid) return@writeAction null
        saveTask.itemDir.findOrCreateChildData(saveTask.studyItem.javaClass, saveTask.configName)
      }

      if (file == null) return@withContext

      try {
        file.putUserData(LOAD_FROM_CONFIG, false)
        ensureYamlHasAssociation(file)

        writeAction { VfsUtil.saveText(file, formattedYamlText) }

        // make sure that there is no conflict between disk contents and ide in-memory document contents
        FileDocumentManager.getInstance().reloadFiles(file)
      }
      finally {
        file.putUserData(LOAD_FROM_CONFIG, true)
      }
    }
  }

  private fun ensureYamlHasAssociation(file: VirtualFile) {
    if (FileTypeManager.getInstance().getFileTypeByFile(file) == UnknownFileType.INSTANCE) {
      @NonNls
      val errorMessageToLog = "Failed to get extension for file ${file.name}"
      FileTypeManager.getInstance().associateExtension(
        PlainTextFileType.INSTANCE,
        file.extension ?: error(errorMessageToLog)
      )
    }
  }

  private suspend fun reformatYaml(text: String): String {
    // We are able to reformat YAML only if the IDE supports the YAML language
    val yamlFileType = FileTypeManager.getInstance().findFileTypeByName("YAML") ?: return text

    val psiFile = readAction {
      PsiFileFactory.getInstance(project).createFileFromText("temporary-config.yaml", yamlFileType, text)
    }

    writeAction {
      CodeStyleManager.getInstance(project).reformat(psiFile)
    }

    return psiFile.text ?: text
  }

  private fun Task.disambiguateTaskFilesContents(project: Project) {
    for ((path, taskFile) in taskFiles) {
      val file = taskFile.getVirtualFile(project)
      val disambiguatedContents = if (file != null) {
        taskFile.contents.disambiguateContents(file)
      }
      else {
        taskFile.contents.disambiguateContents(path)
      }

      taskFile.contents = disambiguatedContents
    }
  }

  private fun Course.disambiguateAdditionalFilesContents(project: Project) {
    val courseDir = project.courseDir

    additionalFiles.forEach { additionalFile ->
      val filePath = additionalFile.name
      val file = courseDir.findFile(filePath)

      val disambiguatedContents = if (file != null) {
        additionalFile.contents.disambiguateContents(file)
      }
      else {
        additionalFile.contents.disambiguateContents(filePath)
      }

      additionalFile.contents = disambiguatedContents
    }
  }

  private class SaveTask(
    val studyItem: StudyItem,
    val itemDir: VirtualFile,
    val configName: String,
    val mapper: ObjectMapper,
    val jobKey: String,
  )
}
