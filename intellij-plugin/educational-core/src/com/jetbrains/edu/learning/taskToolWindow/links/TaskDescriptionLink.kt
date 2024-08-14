package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.io.URLUtil
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector

abstract class TaskDescriptionLink<T, R : T?>(
  val link: String,
  private val linkType: EduCounterUsageCollector.LinkType? = null
) {

  /**
   * Returns link part after schema prefix (i.e. after `://` substring).
   * Returning value is already [URL-decoded](https://en.wikipedia.org/wiki/Percent-encoding)
   */
  protected val linkPath: String get() {
    val path = link.substringAfter(URLUtil.SCHEME_SEPARATOR)
    // If url path contains invalid symbols like ` ` from the URI point of view,
    // you have to encode the path to make it work.
    // So, the path should be decoded to support such cases.
    return URLUtil.decode(path)
  }

  /**
   * Provides an object which the link references to.
   *
   * It might be the same link in the case of [HttpLink] or some other object depending on link nature.
   */
  @RequiresReadLock
  protected abstract fun resolve(project: Project): R
  protected abstract fun open(project: Project, resolved: T)
  protected abstract suspend fun validate(project: Project, resolved: R): String?

  /**
   * Opens the corresponding link depending on link nature if possible.
   *
   * It might be an opening a browser in case of [HttpLink],
   * editor tab in IDE with the corresponding file in the case of [FileLink], etc.
   */
  fun open(project: Project) {
    if (linkType != null) {
      EduCounterUsageCollector.linkClicked(linkType)
    }

    val resolved = runReadAction { resolve(project) } ?: return
    open(project, resolved)
  }

  /**
   * Validates the links and returns an error message if the corresponding resource doesn't exist.
   *
   * @return an error message if the link is invalid, `null` otherwise
   */
  suspend fun validate(project: Project): String? {
    val resolved = readAction { resolve(project) }
    return validate(project, resolved)
  }

  companion object {
    fun fromUrl(link: String): TaskDescriptionLink<*, *>? {
      for ((schema, linkConstructor) in LINK_MAP) {
        if (link.startsWith(schema)) {
          return linkConstructor(link)
        }
      }
      return null
    }

    private val LINK_MAP = mapOf(
      "http://" to ::HttpLink,
      "https://" to ::HttpLink,
      TaskDescriptionLinkProtocol.FILE.protocol to ::FileLink,
      TaskDescriptionLinkProtocol.COURSE.protocol to ::CourseLink,
      TaskDescriptionLinkProtocol.PSI_ELEMENT.protocol to ::PsiElementLink,
      TaskDescriptionLinkProtocol.SETTINGS.protocol to ::SettingsLink,
      TaskDescriptionLinkProtocol.TOOL_WINDOW.protocol to ::ToolWindowLink,
      TaskDescriptionLinkProtocol.FEATURE_TRAINER.protocol to ::ToolWindowLink,
    )
  }
}
