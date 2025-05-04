package com.jetbrains.edu.yaml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.EditorTestUtil
import com.intellij.util.io.URLUtil
import com.intellij.util.io.URLUtil.FILE_PROTOCOL
import com.intellij.util.io.URLUtil.JAR_PROTOCOL
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.configFileName
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import java.nio.file.Paths

abstract class YamlCodeInsightTest : EduTestCase() {

  override fun setUp() {
    super.setUp()
    val factory = JsonSchemaProviderFactory.EP_NAME.findExtension(EduYamlSchemaProviderFactory::class.java)
    factory?.getProviders(project)?.forEach { provider ->
      val schemaResourcePath = provider.getSchemaResourcePath()
      val url = EduYamlSchemaProviderFactory::class.java.getResource(schemaResourcePath)?.toString() ?: return@forEach
      val file = when {
        url.startsWith(FILE_PROTOCOL) -> Paths.get(url)
        url.startsWith(JAR_PROTOCOL) -> {
          // If we are touching a file inside jar, it's ok to allow touching the whole jar file in tests
          val jarPath = URLUtil.splitJarUrl(url)?.first ?: return@forEach
          Paths.get(jarPath)
        }
        else -> return@forEach
      }
      val path = file.toAbsolutePath().toString()
      VfsRootAccess.allowRootAccess(testRootDisposable, path)
    }
  }

  /**
   * Opens this config in editor, supports placing caret, selection and
   * highlighting (ex. warning descriptions) tags
   */
  protected fun openConfigFileWithText(item: StudyItem, configText: String) {
    val configFile = runWriteAction { item.getDir(project.courseDir)!!.findOrCreateChildData(project, item.configFileName) }
    val document = FileDocumentManager.getInstance().getDocument(configFile)!!
    runWriteAction {
      document.setText(configText)
    }
    myFixture.openFileInEditor(configFile)
    val caretsState = EditorTestUtil.extractCaretAndSelectionMarkers(document)
    EditorTestUtil.setCaretsAndSelection(myFixture.editor, caretsState)
  }
}