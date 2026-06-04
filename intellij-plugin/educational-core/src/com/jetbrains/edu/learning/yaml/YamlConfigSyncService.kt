package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.progress.checkCanceled
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.actions.EduActionUtils
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
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.SAVE_TO_CONFIG
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class YamlConfigSyncService(private val project: Project, private val scope: CoroutineScope) : EduTestAware {

  private val item2SaveJob: ConcurrentMap<String, Job> = ConcurrentHashMap()

  fun save(studyItem: StudyItem, configName: String, mapper: ObjectMapper) {
    val saveTask = createSaveTask(studyItem, configName, mapper)

    val currentModality = ModalityState.defaultModalityState()

    item2SaveJob.compute(saveTask.jobKey) { _, oldJob ->
      oldJob?.cancel()
      scope.launch {
        doSave(saveTask, currentModality)
      }.apply {
        invokeOnCompletion {
          item2SaveJob.remove(saveTask.jobKey, this@apply)
        }
      }
    }
  }

  suspend fun saveSync(studyItem: StudyItem, configName: String, mapper: ObjectMapper) {
    val saveTask = createSaveTask(studyItem, configName, mapper)

    item2SaveJob.remove(saveTask.jobKey)?.cancelAndJoin()
    doSave(saveTask, ModalityState.defaultModalityState())
  }

  private fun createSaveTask(studyItem: StudyItem, configName: String, mapper: ObjectMapper): SaveTask {
    val itemDir = studyItem.getConfigDir(project)
    val jobKey = "${itemDir.pathInCourse(project)}/$configName"
    return SaveTask(studyItem, itemDir, configName, mapper, jobKey)
  }

  private suspend fun doSave(saveTask: SaveTask, currentModality: ModalityState) {
    val configFile = readAction {
      if (!saveTask.itemDir.isValid) return@readAction null
      saveTask.itemDir.findChild(saveTask.configName)
    }
    if (configFile?.getUserData(SAVE_TO_CONFIG) == false) return

    val formattedYamlText = withContext(Dispatchers.IO) {
      val studyItem = saveTask.studyItem
      if (studyItem is Task) {
        studyItem.disambiguateTaskFilesContents(project)
        studyItem.persistEduFiles(project)
        checkCanceled()
      }

      if (studyItem is Course) {
        studyItem.disambiguateAdditionalFilesContents(project)
        studyItem.persistAdditionalFiles(project)
        checkCanceled()
      }

      val yamlText = saveTask.mapper.writeValueAsString(studyItem)

      reformatYaml(project, yamlText)
    }

    withContext(Dispatchers.EDT + currentModality.asContextElement()) {
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

  private suspend fun reformatYaml(project: Project, text: String): String {
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

  @TestOnly
  fun waitForAllJobs() {
    if (ApplicationManager.getApplication().isDispatchThread) {

      item2SaveJob.values.forEach {
        EduActionUtils.waitAndDispatchInvocationEvents(it.asCompletableFuture())
      }

      // This waits for load jobs before the loading of the YAML configs is moved to this service
      UIUtil.dispatchAllInvocationEvents()
    }
    else {
      item2SaveJob.values.forEach {
        it?.asCompletableFuture()?.get(10, TimeUnit.SECONDS)
      }
    }
  }

  @TestOnly
  override fun cleanUpState() {
    waitForAllJobs()
  }

  companion object {
    fun getInstance(project: Project): YamlConfigSyncService = project.service()
  }

  private data class SaveTask(
    val studyItem: StudyItem,
    val itemDir: VirtualFile,
    val configName: String,
    val mapper: ObjectMapper,
    val jobKey: String
  )
}