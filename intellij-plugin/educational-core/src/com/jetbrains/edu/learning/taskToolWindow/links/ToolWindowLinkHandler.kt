package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.ide.actions.QualifiedNameProvider
import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.NavigatablePsiElement
import com.intellij.util.io.URLUtil
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector


open class ToolWindowLinkHandler(val project: Project) {

  protected fun processExternalLink(url: String) = EduBrowser.getInstance().browse(url)

  /**
   * @return false to continue (for example open external link at task description), otherwise true
   */
  open fun process(url: String, referUrl: String? = null): Boolean {
    for ((protocol, processor) in INTERNAL_LINK_PROCESSORS) {
      if (url.startsWith(protocol.protocol)) {
        val urlPath = url.substringAfter(protocol.protocol)
        // If url path contains invalid symbols like ` ` from the URI point of view,
        // you have to encode the path to make it work.
        // So, the path should be decoded to support such cases.
        val decodedUrlPath = urlPath.decode()
        processor(project, decodedUrlPath)
        return true
      }
    }
    processExternalLink(url)
    return true
  }

  companion object {
    private val LOG = Logger.getInstance(ToolWindowLinkHandler::class.java)

    private val INTERNAL_LINK_PROCESSORS = mapOf(
      TaskDescriptionLinkProtocol.PSI_ELEMENT to ::processPsiElementLink,
      TaskDescriptionLinkProtocol.COURSE to ::processInCourseLink,
      TaskDescriptionLinkProtocol.FILE to ::processFileLink,
      TaskDescriptionLinkProtocol.SETTINGS to ::processSettingsLink,
      TaskDescriptionLinkProtocol.TOOL_WINDOW to ::processToolWindowLink
    )

    private fun processPsiElementLink(project: Project, qualifiedName: String) {
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

    private fun processInCourseLink(project: Project, urlPath: String) {
      EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.IN_COURSE)
      val course = project.course ?: return

      val parsedLink = parseInCourseLink(project, course, urlPath)
      if (parsedLink == null) {
        LOG.warn("Failed to find course item for `${TaskDescriptionLinkProtocol.COURSE.protocol}$urlPath`")
        return
      }

      runInEdt {
        runReadAction {
          parsedLink.navigate(project)
        }
      }
    }

    private fun processFileLink(project: Project, filePath: String) {
      val file = project.courseDir.findFileByRelativePath(filePath)
      if (file == null) {
        LOG.warn("Can't find file for url $filePath")
      }
      else {
        navigateToFile(project, file)
      }
    }

    private fun navigateToFile(project: Project, fileDir: VirtualFile) {
      runInEdt {
        runReadAction {
          fileDir.let { FileEditorManager.getInstance(project).openFile(it, false) }
        }
      }
      EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.FILE)
    }

    private fun parseInCourseLink(project: Project, course: Course, path: String): ParsedInCourseLink? {

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
            val taskFile = container.getTaskFile(remainingPath) ?: return null
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

      return parseNextItem(course, path)
    }

    private fun processToolWindowLink(project: Project, toolWindowId: String) {
      runInEdt {
        ToolWindowManager.getInstance(project).getToolWindow(toolWindowId)?.show()
      }
    }

    private fun processSettingsLink(project: Project, configurableId: String) {
      runInEdt {
        ShowSettingsUtilImpl.showSettingsDialog(project, configurableId, null)
      }
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

private fun String.decode(): String = URLUtil.decode(this)
