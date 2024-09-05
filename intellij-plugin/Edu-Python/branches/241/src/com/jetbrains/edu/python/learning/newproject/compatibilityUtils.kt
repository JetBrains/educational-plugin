package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.python.sdk.associateWithModule

@RequiresEdt
fun Sdk.setAssociationToModule(project: Project) {
    associateWithModule(null, project.basePath)
    runWriteAction {
      sdkModificator.commitChanges()
    }
}