package com.jetbrains.edu.learning.checkio.checker

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.exceptions.CheckiOLoginRequiredException
import com.jetbrains.edu.learning.checkio.notifications.errors.CheckiOErrorReporter
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.jetbrains.annotations.NonNls
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.JComponent

class CheckiOMissionCheck(private val project: Project,
                          private val task: Task,
                          private val oAuthConnector: CheckiOOAuthConnector,
                          @NonNls private val interpreterName: String,
                          @NonNls private val testFormTargetUrl: String
) : Callable<CheckResult> {
  private val jbCefBrowser = JCEFHtmlPanel(JBCefApp.getInstance().createClient(), null)
  private val jbCefJSQuery = JBCefJSQuery.create(jbCefBrowser as JBCefBrowserBase)
  private lateinit var checkResult: CheckResult
  private val latch = CountDownLatch(1)
  val panel: JComponent
    get() = jbCefBrowser.component

  init {
    jbCefBrowser.cefBrowser.createImmediately()
    jbCefBrowser.jbCefClient.addLoadHandler(TestFormLoadHandler(), jbCefBrowser.cefBrowser)

    jbCefJSQuery.addHandler { value ->
      val result = value.toIntOrNull() ?: return@addHandler null

      ApplicationManager.getApplication().executeOnPooledThread {
        setCheckResult(result)
      }
      null
    }
    // TODO: pass another disposable with shorter lifetime
    // for example, content manager of the corresponding tool window
    // otherwise, jcef browser objects can be leaked until the project is closed.
    // But it's better than nothing
    Disposer.register(StudyTaskManager.getInstance(project), jbCefBrowser)
  }

  @Throws(InterruptedException::class, NetworkException::class)
  override fun call(): CheckResult {
    return try {
      doCheck(collectResources())

      val timeoutExceeded: Boolean = !latch.await(30L, TimeUnit.SECONDS)
      if (timeoutExceeded) {
        return CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("edu.check.took.too.much.time"))
      }

      if (checkResult === CheckResult.CONNECTION_FAILED) {
        throw NetworkException()
      }

      return checkResult
    }
    catch (e: CheckiOLoginRequiredException) {
      CheckiOErrorReporter(project, EduCoreBundle.message("label.login.required"), oAuthConnector).handle(e)
      CheckResult.LOGIN_NEEDED
    }
    catch (e: InterruptedException) {
      CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("edu.check.was.cancelled"))
    }
    catch (e: Exception) {
      CheckiOErrorReporter(project, EduCoreBundle.message("notification.title.failed.to.check.task"), oAuthConnector).handle(e)
      CheckResult.failedToCheck
    }
    finally {
      latch.countDown()
    }
  }

  private fun doCheck(resources: Map<String, String>) {
    invokeLater {
      val html = getTestFormHtml(resources)
      jbCefBrowser.loadHTML(html)
    }
  }

  private fun getTestFormHtml(resources: Map<String, String>): String =
    GeneratorUtils.getInternalTemplateText(CHECKIO_TEST_FORM_TEMPLATE, resources)

  private fun setCheckResult(result: Int) {
    checkResult = when (result) {
      1 -> CheckResult(CheckStatus.Solved, EduCoreBundle.message("edu.check.all.tests.passed"))
      else -> CheckResult(CheckStatus.Failed, EduCoreBundle.message("edu.check.tests.failed"))
    }
    latch.countDown()
  }

  private fun collectResources(): Map<String, String> = mapOf(
    "testFormTargetUrl" to testFormTargetUrl,
    "accessToken" to oAuthConnector.getAccessToken(),
    "taskId" to task.id.toString(),
    "interpreterName" to interpreterName,
    "code" to getCodeFromTask()
  )

  private fun setConnectionError() {
    checkResult = CheckResult.CONNECTION_FAILED
    latch.countDown()
  }

  private fun getCodeFromTask(): String {
    val taskFile = (task as CheckiOMission).getTaskFile()
    val missionDir = task.getDir(project.courseDir)
                     ?: throw IOException("Directory is not found for mission: ${task.id}, ${task.name}")
    val virtualFile = EduUtils.findTaskFileInDir(taskFile, missionDir)
                      ?: throw IOException("Virtual file is not found for mission: ${task.id}, ${task.name}")

    val document = ApplicationManager.getApplication().runReadAction(
      Computable {
        FileDocumentManager.getInstance().getDocument(virtualFile)
      }
    ) ?: throw IOException("Document isn't provided for VirtualFile: ${virtualFile.name}")

    return document.text
  }

  private inner class TestFormLoadHandler : CefLoadHandlerAdapter() {
    override fun onLoadError(browser: CefBrowser?,
                             frame: CefFrame?,
                             errorCode: CefLoadHandler.ErrorCode?,
                             errorText: String?,
                             failedUrl: String?) {
      ApplicationManager.getApplication().executeOnPooledThread {
        setConnectionError()
      }
    }

    override fun onLoadEnd(browser: CefBrowser, frame: CefFrame?, httpStatusCode: Int) {
      // actually it means test form
      if (browser.url.contains("about:blank")) {
        browser.mainFrame.executeJavaScript(
          """
          form = document.getElementById('test-form');
          if (form) {
            form.hidden = true;
            form.submit();
          }
          """.trimIndent(), jbCefBrowser.cefBrowser.url, 0
        )
      }

      if (browser.url.contains("check-html-output")) {
        browser.mainFrame.executeJavaScript(
          """
          function handleEvent(e) {
            let value = e.detail.success;
            ${jbCefJSQuery.inject("value")}
          }
          window.addEventListener('checkio:checkDone', handleEvent, false);  
            
          document.documentElement.setAttribute("style", "background-color : #DEE7F6;")
          """.trimIndent(), jbCefBrowser.cefBrowser.url, 0
        )
      }
    }
  }

  companion object {
    @NonNls
    private const val CHECKIO_TEST_FORM_TEMPLATE = "checkioTestForm.html"
  }
}
