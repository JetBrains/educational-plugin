package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.ide.BrowserUtil
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_URL
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindow.Companion.HINT_HEADER
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindow.Companion.HINT_HEADER_EXPANDED
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager
import com.sun.javafx.application.PlatformImpl
import com.sun.webkit.dom.DocumentImpl
import com.sun.webkit.dom.ElementImpl
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.text.FontSmoothingType
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import org.jetbrains.annotations.TestOnly
import org.w3c.dom.Element
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.EventTarget
import org.w3c.dom.html.HTMLDivElement
import java.util.*

class BrowserWindow(private val myProject: Project, private val myLinkInNewBrowser: Boolean) : Disposable {
  var panel: JFXPanel = JFXPanel()
  private lateinit var myWebComponent: WebView
  private lateinit var myPane: StackPane
  private lateinit var myEngine: WebEngine

  val engine: WebEngine
    get() = myWebComponent.engine

  init {
    ApplicationManager.getApplication().messageBus.connect(this)
      .subscribe(LafManagerListener.TOPIC, StudyLafManagerListener())
    PlatformImpl.startup {
      Platform.setImplicitExit(false)
      myPane = StackPane()
      myWebComponent = WebView()
      myWebComponent.fontSmoothingTypeProperty().value = FontSmoothingType.GRAY
      myEngine = myWebComponent.engine
      Disposer.register(myProject, this)

      myPane.children.add(myWebComponent)
      if (myLinkInNewBrowser) {
        initHyperlinkListener()
      }
      val scene = Scene(myPane)
      panel.scene = scene
      updateLaf()
    }
    panel.isVisible = true
  }

  private fun updateLaf() {
    Platform.runLater {
      val baseStylesheet = StyleResourcesManager.resourceUrl(StyleResourcesManager.BROWSER_CSS)
      myEngine.userStyleSheetLocation = baseStylesheet
      panel.scene.stylesheets.clear()
      panel.scene.stylesheets.add(baseStylesheet)
      panel.scene.stylesheets.addAll(StyleResourcesManager.scrollBarStylesheetResources)
      myPane.style = "-fx-background-color: ${StyleManager().bodyBackground};"
      myEngine.reload()
    }
  }

  fun loadContent(content: String) {
    StudyTaskManager.getInstance(myProject).course ?: return
    Platform.runLater {
      myEngine.loadContent(htmlWithResources(myProject, content))
    }
  }

  private fun initHyperlinkListener() {
    myEngine.loadWorker.stateProperty().addListener { _, _, newState ->
      if (newState === Worker.State.SUCCEEDED) {
        val listener = makeHyperLinkListener()

        addListenerToAllHyperlinkItems(listener)
      }
    }
  }

  private fun addListenerToAllHyperlinkItems(listener: EventListener) {
    val doc = myEngine.document
    if (doc != null) {
      val nodeList = doc.getElementsByTagName("a")
      for (i in 0 until nodeList.length) {
        (nodeList.item(i) as EventTarget).addEventListener(EVENT_TYPE_CLICK, listener, false)
      }
      // listener is added only for collapsed hints so as not to have stats for educator
      // in educator projects expanded is default state for hints
      val hints = (doc as DocumentImpl).getElementsByClassName(HINT_HEADER)
      for (i in 0 until hints.length) {
        (hints.item(i) as EventTarget).addEventListener(EVENT_TYPE_CLICK, listener, false)
      }
    }
  }

  private fun makeHyperLinkListener(): EventListener {
    return object : EventListener {
      override fun handleEvent(ev: Event) {
        val domEventType = ev.type
        if (domEventType == EVENT_TYPE_CLICK) {
          ev.preventDefault()
          val target = ev.target as Element
          if (target is HTMLDivElement) {
            val className = target.className
            if (className == HINT_HEADER) {
              EduCounterUsageCollector.hintExpanded()
            }
            else if (className == HINT_HEADER_EXPANDED) {
              EduCounterUsageCollector.hintCollapsed()
            }
            return
          }
          val hrefAttribute = getElementWithATag(target).getAttribute("href")

          if (hrefAttribute != null) {
            val url = getLink(target) ?: return
            JavaFXToolWindowLinkHandler().process(url)
          }
        }
      }

      private fun getElementWithATag(element: Element): Element {
        var currentElement = element
        while (currentElement.tagName.toLowerCase(Locale.ENGLISH) != "a") {
          currentElement = (currentElement as ElementImpl).parentElement
        }
        return currentElement
      }

      private fun getLink(element: Element): String? {
        val href = element.getAttribute("href")
        return href ?: getLinkFromNodeWithCodeTag(element)
      }

      private fun getLinkFromNodeWithCodeTag(element: Element): String? {
        var parentNode = element.parentNode
        var attributes = parentNode.attributes
        while (attributes.length > 0 && attributes.getNamedItem("class") != null) {
          parentNode = parentNode.parentNode
          attributes = parentNode.attributes
        }
        return attributes.getNamedItem("href").nodeValue
      }
    }
  }

  override fun dispose() {
    Platform.runLater {
      myEngine.load(null)
    }
  }

  inner class JavaFXToolWindowLinkHandler : ToolWindowLinkHandler(myProject) {
    override fun processExternalLink(url: String): Boolean {
      EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.EXTERNAL)
      myEngine.isJavaScriptEnabled = true
      myEngine.loadWorker.cancel()
      val urlToOpen = if (isRelativeLink(url)) STEPIK_URL + url else url
      BrowserUtil.browse(urlToOpen)
      if (urlToOpen.startsWith(STEPIK_URL)) {
        EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.STEPIK)
      }
      return true
    }
  }

  private inner class StudyLafManagerListener : LafManagerListener {
    override fun lookAndFeelChanged(manager: LafManager) {
      updateLaf()
      TaskDescriptionView.updateAllTabs(TaskDescriptionView.getInstance(myProject))
    }
  }

  companion object {
    private const val EVENT_TYPE_CLICK = "click"

    @TestOnly
    fun processContent(content: String, project: Project): String {
      return htmlWithResources(project, content)
    }
  }
}
