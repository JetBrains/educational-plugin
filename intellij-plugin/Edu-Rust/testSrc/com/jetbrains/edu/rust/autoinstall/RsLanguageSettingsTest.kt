package com.jetbrains.edu.rust.autoinstall

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.rust.RsLanguageSettings
import org.junit.Test
import org.rust.cargo.project.RsToolchainPathChoosingComboBox

class RsLanguageSettingsTest : EduTestCase() {

  @Test
  fun `the editorComponent function does not fail because of the class cast exception`() {
    val rustLanguageSettings = RsLanguageSettings()

    with(rustLanguageSettings) {
      // Make sure the editorComponent function does not fail because of the class cast exception
      RsToolchainPathChoosingComboBox().editorComponent()
    }
  }
}