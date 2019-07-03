package com.jetbrains.edu.yaml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.EditorTestUtil
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer.configFileName
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory

abstract class YamlCodeInsightTest : EduTestCase() {

  override fun setUp() {
    super.setUp()
    val factory = JsonSchemaProviderFactory.EP_NAME.findExtension(EduYamlSchemaProviderFactory::class.java)
    factory?.getProviders(project)?.forEach { provider ->
      val schemaResourcePath = provider.getSchemaResourcePath()
      val path = VfsUtilCore.urlToPath(EduYamlSchemaProviderFactory::class.java.getResource(schemaResourcePath).path)
      VfsRootAccess.allowRootAccess(testRootDisposable, path)
    }
  }

  /**
   * Opens this config in editor, supports placing caret, selection and
   * highlighting (ex. warning descriptions) tags
   */
  protected fun openConfigFileWithText(item: StudyItem, configText: String) {
    val configFile = runWriteAction { item.getDir(project)!!.findOrCreateChildData(project, item.configFileName) }
    val document = FileDocumentManager.getInstance().getDocument(configFile)!!
    runWriteAction {
      document.setText(configText)
    }
    myFixture.openFileInEditor(configFile)
    val caretsState = EditorTestUtil.extractCaretAndSelectionMarkers(document)
    EditorTestUtil.setCaretsAndSelection(myFixture.editor, caretsState)
  }
}