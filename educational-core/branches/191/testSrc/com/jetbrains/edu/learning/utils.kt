package com.jetbrains.edu.learning

import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.ui.docking.DockManager

fun createFileEditorManager(project: Project): FileEditorManagerImpl = FileEditorManagerImpl(project, DockManager.getInstance(project))
