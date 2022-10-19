package com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.NoOperationTransformer

fun createViewerDependingOnCurrentUILibrary(project: Project, htmlTransformer: HtmlTransformer = NoOperationTransformer) =
  if (EduSettings.getInstance().javaUiLibraryWithCheck == JavaUILibrary.JCEF) {
    JcefUIHtmlViewer(project, htmlTransformer)
  }
  else {
    SwingUIHtmlViewer(project, htmlTransformer)
  }