package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.ide.BrowserUtil
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduLanguageDecorator
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.EduUsagesCollector
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_URL
import com.sun.webkit.dom.ElementImpl
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import org.jetbrains.annotations.TestOnly
import org.jsoup.Jsoup
import org.w3c.dom.Element
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.EventTarget
import java.io.File
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

class BrowserWindow(private val myProject: Project, private val myLinkInNewBrowser: Boolean) {
  var panel: JFXPanel = JFXPanel()
  private lateinit var myWebComponent: WebView
  private lateinit var myPane: StackPane

  private var myEngine: WebEngine? = null

  val engine: WebEngine
    get() = myWebComponent.engine

  init {
    Platform.runLater {
      Platform.setImplicitExit(false)
      myPane = StackPane()
      myWebComponent = WebView()
      myWebComponent.setOnDragDetected { _ -> }
      myEngine = myWebComponent.engine

      myPane.children.add(myWebComponent)
      if (myLinkInNewBrowser) {
        initHyperlinkListener()
      }
      val scene = Scene(myPane)
      panel.scene = scene
      panel.isVisible = true
      updateLaf(LafManager.getInstance().currentLookAndFeel is DarculaLookAndFeelInfo)
    }
  }

  fun updateLaf(isDarcula: Boolean) {
    if (isDarcula) {
      updateLafDarcula()
    }
    else {
      updateIntellijAndGTKLaf()
    }
  }

  private fun updateIntellijAndGTKLaf() {
    Platform.runLater {
      val scrollBarStyleUrl = javaClass.getResource(
        if (SystemInfo.isWindows) "/style/javaFXBrowserScrollBar_win.css" else "/style/javaFXBrowserScrollBar.css")
      val engineStyleUrl = javaClass.getResource(getBrowserStylesheet(false))
      myEngine!!.userStyleSheetLocation = engineStyleUrl.toExternalForm()
      panel.scene.stylesheets.clear()
      panel.scene.stylesheets.addAll(engineStyleUrl.toExternalForm(), scrollBarStyleUrl.toExternalForm())
      myEngine!!.reload()
    }
  }

  private fun updateLafDarcula() {
    Platform.runLater {
      val engineStyleUrl = javaClass.getResource(getBrowserStylesheet(true))
      val scrollBarStyleUrl = javaClass.getResource(
        if (SystemInfo.isWindows) "/style/javaFXBrowserDarculaScrollBar_win.css" else "/style/javaFXBrowserDarculaScrollBar.css")
      myEngine!!.userStyleSheetLocation = engineStyleUrl.toExternalForm()
      panel.scene.stylesheets.clear()
      panel.scene.stylesheets.addAll(engineStyleUrl.toExternalForm(), scrollBarStyleUrl.toExternalForm())
      myPane.style = "-fx-background-color: #3c3f41"
      myEngine!!.reload()
    }
  }

  fun loadContent(content: String) {
    val course = StudyTaskManager.getInstance(myProject).course ?: return

    val task = EduUtils.getCurrentTask(myProject)
    if (task == null) {
      Platform.runLater { myEngine!!.loadContent(createHtmlWithCodeHighlighting(content, course)) }
      return
    }

    val taskDir = task.getTaskDir(myProject)
    if (taskDir == null) {
      Platform.runLater { myEngine!!.loadContent(createHtmlWithCodeHighlighting(content, course)) }
      return
    }

    Platform.runLater { myEngine!!.loadContent(doProcessContent(content, taskDir, myProject)) }
  }

  private fun initHyperlinkListener() {
    myEngine!!.loadWorker.stateProperty().addListener { _, _, newState ->
      if (newState === Worker.State.SUCCEEDED) {
        val listener = makeHyperLinkListener()

        addListenerToAllHyperlinkItems(listener)
      }
    }
  }

  private fun addListenerToAllHyperlinkItems(listener: EventListener) {
    val doc = myEngine!!.document
    if (doc != null) {
      val nodeList = doc.getElementsByTagName("a")
      for (i in 0 until nodeList.length) {
        (nodeList.item(i) as EventTarget).addEventListener(EVENT_TYPE_CLICK, listener, false)
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
          val hrefAttribute = getElementWithATag(target).getAttribute("href")

          if (hrefAttribute != null) {
            val matcher = IN_COURSE_LINK.matcher(hrefAttribute)
            if (matcher.matches()) {
              EduUsagesCollector.inCourseLinkClicked()
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
              NavigationUtils.navigateToTask(myProject, sectionName, lessonName, taskName)
            }
            else {
              if (hrefAttribute.startsWith(TaskDescriptionToolWindow.PSI_ELEMENT_PROTOCOL)) {
                TaskDescriptionToolWindow.navigateToPsiElement(myProject, hrefAttribute)
              }
              else {
                EduUsagesCollector.externalLinkClicked()
                myEngine!!.isJavaScriptEnabled = true
                myEngine!!.loadWorker.cancel()
                var href: String? = getLink(target) ?: return
                if (isRelativeLink(href!!)) {
                  href = STEPIK_URL + href
                }
                BrowserUtil.browse(href)
                if (href.startsWith(STEPIK_URL)) {
                  EduUsagesCollector.stepikLinkClicked()
                }
              }
            }
          }
        }
      }

      private fun isRelativeLink(href: String): Boolean {
        return !href.startsWith("http")
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

  companion object {
    private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(TaskDescriptionToolWindow::class.java)
    private const val EVENT_TYPE_CLICK = "click"
    private val IN_COURSE_LINK = Pattern.compile("#(\\w+)#(\\w+)#((\\w+)#)?")

    private const val SRC_ATTRIBUTE = "src"

    fun getBrowserStylesheet(isDarcula: Boolean): String {
      if (SystemInfo.isMac) {
        return if (isDarcula) "/style/javaFXBrowserDarcula_mac.css" else "/style/javaFXBrowser_mac.css"
      }

      if (SystemInfo.isWindows) {
        return if (isDarcula) "/style/javaFXBrowserDarcula_win.css" else "/style/javaFXBrowser_win.css"
      }

      return if (isDarcula) "/style/javaFXBrowserDarcula_linux.css" else "/style/browser.css"
    }

    @TestOnly
    fun processContent(content: String, taskDir: VirtualFile, project: Project): String {
      return doProcessContent(content, taskDir, project)
    }

    private fun doProcessContent(content: String, taskDir: VirtualFile, project: Project): String {
      val course = StudyTaskManager.getInstance(project).course ?: return content

      val text = createHtmlWithCodeHighlighting(content, course)

      return absolutizeImgPaths(text, taskDir)
    }

    private fun absolutizeImgPaths(withCodeHighlighting: String, taskDir: VirtualFile): String {
      val document = Jsoup.parse(withCodeHighlighting)
      val imageElements = document.getElementsByTag("img")
      for (imageElement in imageElements) {
        val imagePath = imageElement.attr(SRC_ATTRIBUTE)
        if (!BrowserUtil.isAbsoluteURL(imagePath)) {
          val file = File(imagePath)
          val absolutePath = File(taskDir.path, file.path).toURI().toString()
          imageElement.attr("src", absolutePath)
        }
      }
      return document.outerHtml()
    }

    private fun createHtmlWithCodeHighlighting(content: String, course: Course): String {
      val decorator = EduLanguageDecorator.INSTANCE.forLanguage(course.languageById!!) ?: return content

      var template: String? = null
      val classLoader = BrowserWindow::class.java.classLoader
      val stream = classLoader.getResourceAsStream("/style/template.html")
      try {
        template = StreamUtil.readText(stream, "utf-8")
      }
      catch (e: IOException) {
        LOG.warn(e.message)
      }
      finally {
        try {
          stream.close()
        }
        catch (e: IOException) {
          LOG.warn(e.message)
        }
      }

      if (template == null) {
        LOG.warn("Code mirror template is null")
        return content
      }

      val bodyFontSize = bodyFontSize()
      val codeFontSize = codeFontSize()

      val bodyLineHeight = bodyLineHeight()
      val codeLineHeight = codeLineHeight()

      template = template.replace("\${body_font_size}", bodyFontSize.toString())
      template = template.replace("\${code_font_size}", codeFontSize.toString())
      template = template.replace("\${body_line_height}", bodyLineHeight.toString())
      template = template.replace("\${code_line_height}", codeLineHeight.toString())
      template = setResourcePath(template, "\${codemirror}", "/code-mirror/codemirror.js")
      template = setResourcePath(template, "\${jquery}", "/style/hint/jquery-1.9.1.js")
      template = template.replace("\${language_script}", decorator.languageScriptUrl)
      template = template.replace("\${default_mode}", decorator.defaultHighlightingMode)
      template = setResourcePath(template, "\${runmode}", "/code-mirror/runmode.js")
      template = setResourcePath(template, "\${colorize}", "/code-mirror/colorize.js")
      template = setResourcePath(template, "\${javascript}", "/code-mirror/javascript.js")
      if (LafManager.getInstance().currentLookAndFeel is DarculaLookAndFeelInfo) {
        template = setResourcePath(template, "\${css_oldcodemirror}", "/code-mirror/codemirror-old-darcula.css")
        template = setResourcePath(template, "\${css_codemirror}", "/code-mirror/codemirror-darcula.css")
        template = setResourcePath(template, "\${hint_base}", "/style/hint/base_darcula.css")
      }
      else {
        template = setResourcePath(template, "\${hint_base}", "/style/hint/base.css")
        template = setResourcePath(template, "\${css_oldcodemirror}", "/code-mirror/codemirror-old.css")
        template = setResourcePath(template, "\${css_codemirror}", "/code-mirror/codemirror.css")
      }
      template = template.replace("\${code}", content)

      return template
    }

    private fun setResourcePath(template: String, name: String, recoursePath: String): String {
      var templateCopy = template
      val classLoader = BrowserWindow::class.java.classLoader
      val codemirrorScript = classLoader.getResource(recoursePath)
      if (codemirrorScript != null) {
        templateCopy = template.replace(name, codemirrorScript.toExternalForm())
      }
      else {
        LOG.warn("Resource not found: $recoursePath")
      }
      return templateCopy
    }
  }
}
