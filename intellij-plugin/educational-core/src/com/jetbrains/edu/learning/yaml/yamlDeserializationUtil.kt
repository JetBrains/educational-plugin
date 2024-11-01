package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.codeStyle.NameUtil
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.getEditor
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.errorHandling.InvalidYamlFormatException
import com.jetbrains.edu.learning.yaml.errorHandling.showInvalidConfigNotification

/**
 * [parentItem] is an already deserialized study item, if known
 */
fun deserializeItemProcessingErrors(
  configFile: VirtualFile,
  project: Project,
  loadFromVFile: Boolean = true,
  mapper: ObjectMapper = YamlMapper.basicMapper(),
  parentItem: StudyItem? = null
): StudyItem? {
  val configFileText = if (loadFromVFile) VfsUtil.loadText(configFile) else configFile.document.text
  val configName = configFile.name
  return ProgressManager.getInstance().computeInNonCancelableSection<StudyItem, Exception> {
    try {
      YamlDeserializer.deserializeItem(configName, mapper, configFileText, parentItem, configFile.parent?.name)
    }
    catch (e: Exception) {
      processErrors(project, configFile, e)
      null
    }
  }
}

fun showError(
  project: Project,
  originalException: Exception?,
  configFile: VirtualFile,
  cause: String = EduCoreBundle.message("yaml.editor.notification.invalid.config"),
) {
  // to make test failures more comprehensible
  if (isUnitTestMode && project.getUserData(YamlFormatSettings.YAML_TEST_THROW_EXCEPTION) == true) {
    if (originalException != null) {
      throw YamlLoader.ProcessedException(cause, originalException)
    }
  }
  runInEdt {
    val editor = configFile.getEditor(project)
    project.messageBus.syncPublisher(YamlLoader.YAML_LOAD_TOPIC).yamlFailedToLoad(configFile, cause)
    if (editor == null) {
      showInvalidConfigNotification(project, configFile, cause)
    }
  }
}

private fun processErrors(project: Project, configFile: VirtualFile, e: Exception) {
  @Suppress("DEPRECATION")
  // suppress deprecation for MarkedYAMLException as it is actually thrown from com.fasterxml.jackson.dataformat.yaml.YAMLParser.nextToken
  when (e) {
    is MissingKotlinParameterException -> {
      val parameterName = e.parameter.name
      if (parameterName == null) {
        showError(project, e, configFile)
      }
      else {
        val cause = EduCoreBundle.message(
          "yaml.editor.notification.parameter.is.empty",
          NameUtil.nameToWordsLowerCase(parameterName).joinToString("_")
        )
        showError(project, e, configFile, cause)
      }
    }

    is InvalidYamlFormatException -> showError(project, e, configFile, e.message)
    is MismatchedInputException -> {
      showError(project, e, configFile)
    }

    is com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException -> {
      val message = yamlParsingErrorNotificationMessage(e.problem, e.contextMark?.line)
      if (message != null) {
        showError(project, e, configFile, message)
      }
      else {
        showError(project, e, configFile)
      }
    }

    is JsonMappingException -> {
      val causeException = e.cause
      if (causeException?.message == null || causeException !is InvalidYamlFormatException) {
        showError(project, e, configFile)
      }
      else {
        showError(project, causeException, configFile, causeException.message)
      }
    }

    else -> throw e
  }
}

// it doesn't require localization as `problems` is snakeyaml error message on which we have no influence
@Suppress("UnstableApiUsage")
@NlsSafe
private fun yamlParsingErrorNotificationMessage(problem: String?, line: Int?) =
  if (problem != null && line != null) "$problem at line ${line + 1}" else null
