package com.jetbrains.edu.coursecreator.yaml

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer.isLocalConfigFile

class YamlSynchronizationListener(val project: Project) : DocumentListener {
  override fun documentChanged(event: DocumentEvent) {
    val eventDocument = event.document
    val configFile = FileDocumentManager.getInstance().getFile(eventDocument) ?: return
    if ((configFile is LightVirtualFile) || !isLocalConfigFile(configFile)) {
      return
    }
    val loadFromConfig = configFile.getUserData(YamlFormatSynchronizer.LOAD_FROM_CONFIG) ?: true
    if (loadFromConfig) {
      runInEdt {
        YamlLoader.loadItem(project, configFile)
        ProjectView.getInstance(project).refresh()
      }
    }
  }
}