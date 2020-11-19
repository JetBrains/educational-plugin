package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.codeInsight.documentation.DocumentationManagerProtocol
import com.intellij.ide.actions.QualifiedNameProvider
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.NavigatablePsiElement
import com.intellij.util.io.URLUtil
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector

abstract class ToolWindowLinkHandler(val project: Project) {
  open fun process(url: String): Boolean {
    return when {
      url.startsWith(PSI_ELEMENT_PROTOCOL) -> {
        processPsiElementLink(project, url)
        true
      }
      url.startsWith(IN_COURSE_PROTOCOL) -> {
        processInCourseLink(project, url)
        true
      }
      else -> processExternalLink(url)
    }
  }

  abstract fun processExternalLink(url: String): Boolean

  companion object {
    const val PSI_ELEMENT_PROTOCOL: String = DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL
    const val IN_COURSE_PROTOCOL: String = "course://"
    private val LOG = Logger.getInstance(ToolWindowLinkHandler::class.java)

    @JvmStatic
    fun isRelativeLink(href: String): Boolean {
      return !href.startsWith("http")
    }

    @JvmStatic
    fun processPsiElementLink(project: Project, url: String) {
      val urlEncodedName = url.substringAfter(PSI_ELEMENT_PROTOCOL)
      // Sometimes a user has to encode element reference because it contains invalid symbols like ` `.
      // For example, Java support produces `Foo#foo(int, int)` as reference for `foo` method in the following `Foo` class
      // ```
      // class Foo {
      //     public void foo(int bar, int baz) {}
      // }
      // ```
      //
      val qualifiedName = URLUtil.decode(urlEncodedName)

      runInEdt {
        runReadAction {
          val dumbService = DumbService.getInstance(project)
          if (dumbService.isDumb) {
            val message = ActionUtil.getUnavailableMessage(EduCoreBundle.message("label.navigation"), false)
            dumbService.showDumbModeNotification(message)
          }
          else {
            for (provider in QualifiedNameProvider.EP_NAME.extensionList) {
              val element = provider.qualifiedNameToElement(qualifiedName, project)
              if (element is NavigatablePsiElement) {
                if (element.canNavigate()) {
                  element.navigate(true)
                }
                break
              }
            }
          }
        }
      }
      EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.PSI)
    }

    @JvmStatic
    fun processInCourseLink(project: Project, url: String) {
      EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.IN_COURSE)
      val course = project.course ?: return

      val parsedLink = parseInCourseLink(project, course, url)
      if (parsedLink == null) {
        LOG.warn("Failed to find course item for `$url`")
        return
      }

      runInEdt {
        runReadAction {
          parsedLink.navigate(project)
        }
      }
    }

    private fun parseInCourseLink(project: Project, course: Course, url: String): ParsedInCourseLink? {

      fun parseNextItem(container: StudyItem, remainingPath: String?): ParsedInCourseLink? {
        if (remainingPath == null) {
          val courseDir = project.courseDir
          val dir = container.getDir(courseDir) ?: return null

          return when (container) {
            is ItemContainer -> ParsedInCourseLink.ItemContainerDirectory(dir)
            is Task -> ParsedInCourseLink.TaskDirectory(container, dir)
            else -> error("Unexpected item type: ${container.itemType}")
          }
        }

        return when (container) {
          is Task -> {
            val taskFile = container.getFile(remainingPath) ?: return null
            val file = taskFile.getVirtualFile(project) ?: return null
            ParsedInCourseLink.FileInTask(container, file)
          }
          is ItemContainer -> {
            val segments = remainingPath.split("/", limit = 2)
            val childItemName = segments[0]
            val childItem = container.getItem(childItemName) ?: return null
            parseNextItem(childItem, segments.getOrNull(1))
          }
          else -> null
        }
      }

      val urlEncodedPath = url.substringAfter(IN_COURSE_PROTOCOL)
      val path = URLUtil.decode(urlEncodedPath)
      return parseNextItem(course, path)
    }
  }

  private sealed class ParsedInCourseLink(val file: VirtualFile) {

    abstract fun navigate(project: Project)

    class ItemContainerDirectory(dir: VirtualFile) : ParsedInCourseLink(dir) {
      override fun navigate(project: Project) {
        OpenFileDescriptor(project, file).navigate(true)
      }

    }
    class TaskDirectory(val task: Task, file: VirtualFile) : ParsedInCourseLink(file) {
      override fun navigate(project: Project) {
        NavigationUtils.navigateToTask(project, task, closeOpenedFiles = false)
      }
    }

    class FileInTask(val task: Task, file: VirtualFile) : ParsedInCourseLink(file) {
      override fun navigate(project: Project) {
        NavigationUtils.navigateToTask(project, task, closeOpenedFiles = false, fileToActivate = file)
      }
    }
  }
}
