package com.jetbrains.edu.php

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_PHP
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessage
import com.jetbrains.edu.php.messages.EduPhpBundle
import com.jetbrains.php.composer.execution.phar.ComposerPhpInterpretersCombo
import com.jetbrains.php.config.PhpProjectWorkspaceConfiguration
import com.jetbrains.php.config.interpreters.PhpInterpreter
import com.jetbrains.php.config.interpreters.PhpInterpretersConfigurable
import com.jetbrains.php.config.interpreters.PhpInterpretersManagerImpl
import com.jetbrains.php.config.interpreters.PhpSdkType
import java.awt.BorderLayout
import javax.swing.JComponent

class PhpLanguageSettings : LanguageSettings<PhpProjectSettings>() {
  private var phpProjectSettings = PhpProjectSettings()
  private val composerPhpInterpretersCombo: ComposerPhpInterpretersCombo

  init {
    val defaultProject = ProjectManager.getInstance().defaultProject
    composerPhpInterpretersCombo = ComposerPhpInterpretersCombo(defaultProject)
    composerPhpInterpretersCombo.isNoItemAllowed = false
    addDefaultInterpreterIntoComboBox(defaultProject)

    composerPhpInterpretersCombo.addChangeListener {
      val interpreterId = (it.source as? ComposerPhpInterpretersCombo)?.interpreterId
      val interpreter = PhpInterpretersManagerImpl.getInstance(defaultProject).findInterpreterById(interpreterId)
      selectInterpreter(defaultProject, interpreter)
    }
  }

  private fun addDefaultInterpreterIntoComboBox(project: Project) {
    val notBlankInterpreters = PhpInterpretersManagerImpl.getInstance(project).interpreters.filter { it.name.isNotBlank() }
    val interpreter = if (notBlankInterpreters.isEmpty()) {
      createDefaultInterpreter(project)
    }
    else {
      notBlankInterpreters.firstOrNull()
    }

    if (interpreter != null) {
      composerPhpInterpretersCombo.reset(interpreter.name)
      selectInterpreter(project, interpreter)
    }
  }

  private fun createDefaultInterpreter(project: Project): PhpInterpreter? {
    val phpSdk = PhpSdkType.getInstance().suggestHomePaths().firstOrNull()
    return if (phpSdk != null) {
      createDefaultInterpreter(project, phpSdk)?.also {
        PhpInterpretersManagerImpl.getInstance(project).addInterpreter(it)
      }
    }
    else {
      null
    }
  }

  private fun createDefaultInterpreter(project: Project, phpSdk: String): PhpInterpreter? {
    val phpSdkFile = LocalFileSystem.getInstance().findFileByPath(phpSdk)
    if (phpSdkFile != null) {
      val interpreter = PhpInterpreter().apply { homePath = FileUtil.toSystemDependentName(phpSdkFile.path) }
      val configurable = PhpInterpretersConfigurable(project, interpreter.name)
      val suggestedName = configurable.suggestUniqueName(interpreter)
      interpreter.name = suggestedName
      return interpreter
    }
    return null
  }

  private fun selectInterpreter(project: Project, interpreter: PhpInterpreter?) {
    phpProjectSettings.phpInterpreter = interpreter
    project.getService(PhpProjectWorkspaceConfiguration::class.java).state?.interpreterName = interpreter?.name
    notifyListeners()
  }

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> = listOf(
    LabeledComponent.create(composerPhpInterpretersCombo, EduCoreBundle.message("select.interpreter"), BorderLayout.WEST))


  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    val phpInterpreter = phpProjectSettings.phpInterpreter

    if (phpInterpreter == null) {
      return SettingsValidationResult.Ready(
        ValidationMessage(EduPhpBundle.message("error.no.php.interpreter", ""), ENVIRONMENT_CONFIGURATION_LINK_PHP)
      )
    }
    return SettingsValidationResult.OK
  }

  override fun getSettings(): PhpProjectSettings = phpProjectSettings
}
