package com.jetbrains.edu.learning.stepik.hyperskill.github

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.jetbrains.edu.learning.stepik.hyperskill.PostHyperskillProjectToGithubBase

class PostHyperskillProjectToGithub : PostHyperskillProjectToGithubBase() {

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}