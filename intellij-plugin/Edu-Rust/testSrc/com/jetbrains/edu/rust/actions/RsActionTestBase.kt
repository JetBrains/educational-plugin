package com.jetbrains.edu.rust.actions

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.EduActionTestCase
import org.intellij.lang.annotations.Language
import org.rust.cargo.CargoConstants

abstract class RsActionTestBase : EduActionTestCase() {

  protected fun checkCargoToml(@Language("TOML") expectedText: String) {
    val manifest = LightPlatformTestCase.getSourceRoot().findChild(CargoConstants.MANIFEST_FILE)
                   ?: error("Failed to find root ${CargoConstants.MANIFEST_FILE}")
    assertFalse("${CargoConstants.MANIFEST_FILE} has unsaved changes", FileDocumentManager.getInstance().isFileModified(manifest))
    myFixture.openFileInEditor(manifest)
    myFixture.checkResult(expectedText.trimIndent())
  }
}
