package com.jetbrains.edu.coursecreator.yaml

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.edu.learning.EduDocumentListenerBase
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.isLocalConfigFile

class YamlSynchronizationListener(project: Project) : EduDocumentListenerBase(project) {
  override fun documentChanged(event: DocumentEvent) {
    if (!event.isInProjectContent()) return
    val eventDocument = event.document
    val configFile = fileDocumentManager.getFile(eventDocument) ?: return
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