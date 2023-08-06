package com.jetbrains.edu.github

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.github.PostToGithubActionProvider
import org.jetbrains.plugins.github.GithubShareAction
import org.jetbrains.plugins.github.util.GithubGitHelper

class PostToGithubProviderImpl : PostToGithubActionProvider {

  override fun postToGitHub(project: Project, file: VirtualFile) {
    // BACKCOMPAT 2022.3: replace with GHShareProjectUtil.shareProjectOnGithub(project, file)
    @Suppress("DEPRECATION")
    GithubShareAction.shareProjectOnGithub(project, file)
  }

  override fun getWebUrl(project: Project, file: VirtualFile): String? {
    val gitRepository = GithubGitHelper.findGitRepository(project, file) ?: return null
    return gitRepository.remotes.firstOrNull()?.urls?.firstOrNull()
  }
}
