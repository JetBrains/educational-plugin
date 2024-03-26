package com.jetbrains.edu.rust

import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_RUST
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessage
import com.jetbrains.edu.rust.messages.EduRustBundle
import org.rust.cargo.project.RsToolchainPathChoosingComboBox
import org.rust.cargo.toolchain.RsToolchainBase
import org.rust.cargo.toolchain.RsToolchainProvider
import org.rust.cargo.toolchain.flavors.RsToolchainFlavor
import java.awt.BorderLayout
import java.nio.file.Path
import javax.swing.JComponent

class RsLanguageSettings : LanguageSettings<RsProjectSettings>() {

  private val toolchainComboBox: RsToolchainPathChoosingComboBox by lazy {
    RsToolchainPathChoosingComboBox { updateToolchain() }
  }

  private var loadingFinished: Boolean = false

  private var rustToolchain: RsToolchainBase? = null

  override fun getSettings(): RsProjectSettings = RsProjectSettings(rustToolchain)

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    Disposer.register(disposable, toolchainComboBox)
    toolchainComboBox.addToolchainsAsync(::findAllToolchainsPath) {
      loadingFinished = true
      if (disposable.isDisposed) return@addToolchainsAsync
      // `RsToolchainPathChoosingComboBox` sets initial empty text after addition of all items
      // But we want to show text of selected item
      val combobox = toolchainComboBox.childComponent
      val selectedItem = combobox.selectedItem
      if (selectedItem is Path) {
        toolchainComboBox.selectedPath = selectedItem
      }
      updateToolchain()
    }

    return listOf<LabeledComponent<JComponent>>(LabeledComponent.create(toolchainComboBox, EduRustBundle.message("toolchain.label.text"), BorderLayout.WEST))
  }

  private fun findAllToolchainsPath(): List<Path> {
    checkIsBackgroundThread()
    return RsToolchainFlavor.getApplicableFlavors().flatMap { it.suggestHomePaths() }.distinct()
  }

  private fun updateToolchain() {
    // Unfortunately, `RsToolchainPathChoosingComboBox` changes its text before final callback is called
    // To avoid unexpected updates of toolchain, just skip all changes before call of final callback
    if (!loadingFinished) return
    val toolchainPath = toolchainComboBox.selectedPath
    if (toolchainPath != null) {
      // We already have toolchain for this path
      if (rustToolchain?.location == toolchainPath) return

      rustToolchain = RsToolchainProvider.getToolchain(toolchainPath)
    }
    notifyListeners()
  }

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    if (!loadingFinished) return SettingsValidationResult.Pending
    val toolchain = rustToolchain
    val validationMessage = when {
      toolchain == null -> {
        ValidationMessage(EduRustBundle.message("error.no.toolchain.location", ""), ENVIRONMENT_CONFIGURATION_LINK_RUST)
      }
      !toolchain.looksLikeValidToolchain() -> {
        ValidationMessage(EduRustBundle.message("error.incorrect.toolchain.location"), ENVIRONMENT_CONFIGURATION_LINK_RUST)
      }
      else -> null
    }
    return SettingsValidationResult.Ready(validationMessage)
  }
}
