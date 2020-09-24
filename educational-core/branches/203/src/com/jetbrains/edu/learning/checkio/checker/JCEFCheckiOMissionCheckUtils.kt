package com.jetbrains.edu.learning.checkio.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.courseFormat.tasks.Task

fun getJCEFCheckiOMissionCheck(project: Project,
                               task: Task,
                               oAuthConnector: CheckiOOAuthConnector,
                               interpreterName: String,
                               testFormTargetUrl: String): JCEFCheckiOMissionCheck? =
  JCEFCheckiOMissionCheck(project, task, oAuthConnector, interpreterName, testFormTargetUrl)