package com.jetbrains.edu.learning.github

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ui.CommitHelper
import com.intellij.util.FunctionUtil
import git4idea.GitVcs
import git4idea.actions.BasicAction
import git4idea.commands.Git
import org.jetbrains.plugins.github.GithubShareAction
import org.jetbrains.plugins.github.util.GithubGitHelper

private const val title = "Share Project on Github"

class ShareProjectAction(val project: Project) : AnAction(title, title, AllIcons.Vcs.Vendors.Github) {

  override fun actionPerformed(e: AnActionEvent?) {
    BasicAction.saveAll()
    val gitRepository = GithubGitHelper.findGitRepository(project, project.baseDir)
    if (gitRepository != null) {
      val git = Git.getInstance()
      val result = git.checkoutNewBranch(gitRepository, "Stage1", null)
      gitRepository.update()
      if (result.success()) {
        val commitExecutors = GitVcs.getInstance(project).commitExecutors
        assert(commitExecutors.size == 1)
        val changeList = ChangeListManager.getInstance(project).defaultChangeList
        val helper = CommitHelper(project, changeList, changeList.changes.toList(), changeList.name,
                                  "Added stage 1", emptyList(), true,
                                  false, FunctionUtil.nullConstant(), null, false, null)
        if (helper.doCommit()) {
          val branch = gitRepository.currentBranch
          if (branch != null) {
            val urls = gitRepository.remotes.flatMap { remote -> remote.urls }
            assert(urls.size == 1)
//            val result = git.push(gitRepository, "origin", urls[0], branch.name, true)
//            println(result)
          }
        }
      }
    }
    else {
      GithubShareAction.shareProjectOnGithub(project, project.baseDir)
    }
  }
}
