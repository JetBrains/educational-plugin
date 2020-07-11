package com.jetbrains.edu.learning.checkio.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import javax.swing.JComponent

class JCEFCheckiOMissionCheck(project: Project,
                              task: Task,
                              oAuthConnector: CheckiOOAuthConnector,
                              interpreterName: String,
                              testFormTargetUrl: String
) : CheckiOMissionCheck(project, task, oAuthConnector, interpreterName, testFormTargetUrl) {

  override fun doCheck() {
    TODO("Not yet implemented")
  }

  override fun getPanel(): JComponent {
    TODO("Not yet implemented")
  }
}