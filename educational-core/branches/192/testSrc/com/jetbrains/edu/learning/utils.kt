package com.jetbrains.edu.learning

import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.project.Project

fun createFileEditorManager(project: Project): FileEditorManagerImpl = FileEditorManagerImpl(project)
