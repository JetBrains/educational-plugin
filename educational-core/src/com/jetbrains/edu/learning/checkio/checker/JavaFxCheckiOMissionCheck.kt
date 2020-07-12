package com.jetbrains.edu.learning.checkio.checker

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskDescription.ui.BrowserWindow
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker
import netscape.javascript.JSObject
import org.w3c.dom.html.HTMLFormElement
import javax.swing.JComponent

class JavaFxCheckiOMissionCheck(
  project: Project,
  task: Task,
  oAuthConnector: CheckiOOAuthConnector,
  interpreterName: String,
  testFormTargetUrl: String
) : CheckiOMissionCheck(project, task, oAuthConnector, interpreterName, testFormTargetUrl) {
  private val browserWindow: BrowserWindow = BrowserWindow(project, false)
  private val resultHandler: CheckiOTestResultHandler = CheckiOTestResultHandler()

  override fun doCheck() {
    Platform.runLater {
      setTestFormLoadedListener()
      setCheckDoneListener()
      loadTestForm()
    }
  }

  override fun getPanel(): JComponent = browserWindow.panel

  private fun loadTestForm() {
    val html = getTestFormHtml()
    browserWindow.engine.loadContent(html)
  }

  private fun setCheckDoneListener() {
    val visited = Ref(false)
    browserWindow.engine.loadWorker.stateProperty().addListener { _: ObservableValue<out Worker.State>?,
                                                                  _: Worker.State?,
                                                                  newState: Worker.State ->
      if (newState == Worker.State.FAILED) {
        setConnectionError()
        return@addListener
      }

      if (browserWindow.engine.location.contains(CheckiONames.CHECKIO_URL)
          && newState == Worker.State.SUCCEEDED && !visited.get()) {
        visited.set(true)
        val windowObject = browserWindow.engine.executeScript("window") as JSObject
        windowObject.setMember("javaHandler", resultHandler)
        browserWindow.engine.executeScript(
          """
            function handleEvent(e) {
	            window.javaHandler.handleTestEvent(e.detail.success)
            }
            window.addEventListener("checkio:checkDone", handleEvent, false)
          """.trimIndent()
        )
      }
    }
  }

  private fun setTestFormLoadedListener() {
    browserWindow.engine.loadWorker.stateProperty().addListener { _: ObservableValue<out Worker.State>?,
                                                                  _: Worker.State?,
                                                                  newState: Worker.State ->
      if (newState == Worker.State.FAILED) {
        setConnectionError()
        return@addListener
      }
      if (newState != Worker.State.SUCCEEDED) {
        return@addListener
      }

      val location = browserWindow.engine.location
      val document = browserWindow.engine.document
      val testForm = document.getElementById("test-form") as? HTMLFormElement
      testForm?.submit()

      if (location.contains("check-html-output")) {
        document.documentElement.setAttribute("style", "background-color : #DEE7F6;")
      }
    }
  }

  inner class CheckiOTestResultHandler {
    @Suppress("unused") // used in JS code
    fun handleTestEvent(result: Int) {
      setCheckResult(result)
      latch.countDown()
    }
  }
}