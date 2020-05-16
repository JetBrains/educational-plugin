package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.codeInsight.documentation.DocumentationManagerProtocol
import com.intellij.ide.BrowserUtil
import com.intellij.ide.actions.QualifiedNameProvider
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.NavigatablePsiElement
import com.intellij.util.io.URLUtil
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.StepikNames
import java.util.regex.Matcher
import java.util.regex.Pattern

open class LinkInToolWindowHandler(val project: Project) {
  fun process(url: String) {
    if (url.startsWith(PSI_ELEMENT_PROTOCOL)) {
      psiElementLinkHandler(url)
      return
    }

    val matcher = IN_COURSE_LINK.matcher(url)
    if (matcher.matches()) {
      inCourseLinkHandler(matcher)
      return
    }

    externalLinkHandler(url)
  }

  open fun inCourseLinkHandler(matcher: Matcher) {
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
  }

  open fun psiElementLinkHandler(url: String) {
    navigateToPsiElement(project, url)
  }

  open fun externalLinkHandler(url: String) {
    EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.EXTERNAL)
    var urlToOpen = url
    if (isRelativeLink(urlToOpen)) {
      urlToOpen = StepikNames.STEPIK_URL + urlToOpen
    }
    BrowserUtil.browse(urlToOpen)
    if (urlToOpen.contains(StepikNames.STEPIK_URL)) {
      EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.STEPIK)
    }
  }

  companion object {
    const val PSI_ELEMENT_PROTOCOL: String = DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL
    private val IN_COURSE_LINK: Pattern = Pattern.compile("#(\\w+)#(\\w+)#((\\w+)#)?")

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