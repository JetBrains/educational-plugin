package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.codeInsight.documentation.DocumentationManagerProtocol
import com.intellij.ide.actions.QualifiedNameProvider
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.NavigatablePsiElement
import com.intellij.util.io.URLUtil
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class ToolWindowLinkHandler(val project: Project) {
  open fun process(url: String): Boolean {
    val matcher = IN_COURSE_LINK.matcher(url)
    return when {
      url.startsWith(PSI_ELEMENT_PROTOCOL) -> processPsiElementLink(url)
      matcher.matches() -> processInCourseLink(matcher)
      else -> processExternalLink(url)
    }
  }

  private fun processInCourseLink(matcher: Matcher): Boolean {
    try {
      EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.IN_COURSE)
      var sectionName: String? = null
      val lessonName: String
      val taskName: String
      if (matcher.group(3) != null) {
        sectionName = matcher.group(1)
        lessonName = matcher.group(2)
        taskName = matcher.group(4)
      }
      else {
        lessonName = matcher.group(1)
        taskName = matcher.group(2)
      }
      NavigationUtils.navigateToTask(project, sectionName, lessonName, taskName)
      return true
    } catch (e: Exception) {
      LOG.error(e)
      return false
    }
  }

  private fun processPsiElementLink(url: String): Boolean {
    return try {
      navigateToPsiElement(project, url)
      true
    } catch (e: Exception) {
      LOG.error(e)
      false
    }
  }

  abstract fun processExternalLink(url: String): Boolean

  companion object {
    const val PSI_ELEMENT_PROTOCOL: String = DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL
    private val IN_COURSE_LINK: Pattern = Pattern.compile("#(\\w+)#(\\w+)#((\\w+)#)?")
    private val LOG = Logger.getInstance(this::class.java)

    @JvmStatic
    fun isRelativeLink(href: String): Boolean {
      return !href.startsWith("http")
    }

    @JvmStatic
    fun navigateToPsiElement(project: Project, url: String) {
      val urlEncodedName = url.replace(PSI_ELEMENT_PROTOCOL, "")
      // Sometimes a user has to encode element reference because it contains invalid symbols like ` `.
      // For example, Java support produces `Foo#foo(int, int)` as reference for `foo` method in the following `Foo` class
      // ```
      // class Foo {
      //     public void foo(int bar, int baz) {}
      // }
      // ```
      //
      val qualifiedName = URLUtil.decode(urlEncodedName)

      val application = ApplicationManager.getApplication()
      application.invokeLater {
        application.runReadAction {
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
  }
}