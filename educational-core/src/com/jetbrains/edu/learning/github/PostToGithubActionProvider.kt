package com.jetbrains.edu.learning.github

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.jetbrains.edu.learning.courseFormat.tasks.Task

interface PostToGithubActionProvider {
  fun isAvailable(task: Task): Boolean

  fun getAction(): AnAction

  companion object {
    @JvmStatic
    val EP_NAME: ExtensionPointName<PostToGithubActionProvider> = ExtensionPointName.create("Educational.postToGithub")

    fun firstAvailable(task: Task): PostToGithubActionProvider? = EP_NAME.extensionList.firstOrNull {
      it.isAvailable(task)
    }
  }
}