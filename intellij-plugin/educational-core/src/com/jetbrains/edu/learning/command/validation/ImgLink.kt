package com.jetbrains.edu.learning.command.validation

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskToolWindow.SRC_ATTRIBUTE
import com.jetbrains.edu.learning.taskToolWindow.getDarkImageSrc
import com.jetbrains.edu.learning.taskToolWindow.links.FileLink
import com.jetbrains.edu.learning.taskToolWindow.links.HttpLink
import com.jetbrains.edu.learning.taskToolWindow.links.TaskDescriptionLink
import org.jsoup.nodes.Element

class ImgLink private constructor(link: String, private val task: Task) : TaskDescriptionLink<ImgLink.ImageSrc, ImgLink.ImageSrc>(link) {
  override fun resolve(project: Project): ImageSrc {
    @Suppress("HttpUrlsUsage")
    return when {
      link.startsWith("http://") || link.startsWith("https://") -> HttpSrc(link)
      // links with explicit schemes
      BrowserUtil.isAbsoluteURL(link) -> AbsoluteSrc(link)
      else -> LocalSrc(link, task)
    }
  }

  override suspend fun validate(project: Project, resolved: ImageSrc): String? {
    return when (resolved) {
      is HttpSrc -> resolved.delegate.validate(project)
      // TODO: process such cases as well. See https://youtrack.jetbrains.com/issue/EDU-6801
      is AbsoluteSrc -> null
      is LocalSrc -> resolved.delegate.validate(project)
    }
  }

  override fun open(project: Project, resolved: ImageSrc) {}

  companion object {
    fun collectImageLinks(project: Project, task: Task, imgElement: Element): List<ImgLink> {
      return buildList {
        val src = imgElement.attr(SRC_ATTRIBUTE)
        if (src.isNotEmpty()) {
          add(ImgLink(src, task))
        }
        val darkSrc = imgElement.getDarkImageSrc(project, task)
        if (darkSrc != null) {
          add(ImgLink(darkSrc, task))
        }
      }
    }
  }

  sealed class ImageSrc
  private data class HttpSrc(val url: String) : ImageSrc() {
    val delegate: HttpLink = HttpLink(url)
  }
  private data class LocalSrc(val path: String, val task: Task) : ImageSrc() {
    val delegate: FileLink = object : FileLink(path) {
      override fun rootDir(project: Project): VirtualFile? = task.getDir(project.courseDir)
    }
  }
  private data class AbsoluteSrc(val url: String) : ImageSrc()
}
