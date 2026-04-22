package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
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
import com.jetbrains.rd.util.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.ConcurrentMap

@Service(Service.Level.PROJECT)
class YamlConfigSyncService(private val project: Project, private val scope: CoroutineScope): EduTestAware {

  private val map: ConcurrentMap<String, Job?> = ConcurrentHashMap()

  fun save(studyItem: StudyItem, configName: String, mapper: ObjectMapper) {
    val itemDir = studyItem.getConfigDir(project)

    val jobKey = itemDir.pathInCourse(project) + "/" + configName

    val currentModality = ModalityState.defaultModalityState()

    map.compute(jobKey) { _, oldJob ->
      oldJob?.cancel()
      scope.launch(Dispatchers.IO + currentModality.asContextElement()) {
        studyItem.doSave(itemDir, configName, mapper)
      }.apply {
        invokeOnCompletion {
          map.remove(jobKey, this@apply)
        }
      }
    }
  }

  private suspend fun StudyItem.doSave(itemDir: VirtualFile, configName: String, mapper: ObjectMapper) {
    val configFile = readAction { itemDir.findChild(configName) }
    if (configFile?.getUserData(SAVE_TO_CONFIG) == false) return

    if (this is Task) {
      disambiguateTaskFilesContents(project)
      persistEduFiles(project)
      checkCanceled()
    }

    if (this is Course) {
      disambiguateAdditionalFilesContents(project)
      persistAdditionalFiles(project)
      checkCanceled()
    }

    val yamlText = mapper.writeValueAsString(this)

    val formattedYamlText = reformatYaml(project, yamlText)

    withContext(Dispatchers.EDT) {
      val file = writeAction { itemDir.findOrCreateChildData(javaClass, configName) }

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

  @VisibleForTesting
  fun waitForAllJobs() {
    map.values.forEach {
      if (it == null) return@forEach
      EduActionUtils.waitAndDispatchInvocationEvents(it.asCompletableFuture())
    }
  }

  override fun cleanUpState() {
    waitForAllJobs()
  }

  companion object {
    fun getInstance(project: Project): YamlConfigSyncService = project.service()
  }
}