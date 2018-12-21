package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.QueryStringDecoder
import org.jetbrains.ide.RestService
import org.jetbrains.io.send
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.util.regex.Pattern

class HyperskillRestService : OAuthRestService(HYPERSKILL) {
  override fun getServiceName(): String = EDU_HYPERSKILL_SERVICE_NAME

  @Throws(InterruptedException::class, InvocationTargetException::class)
  override fun isHostTrusted(request: FullHttpRequest): Boolean {
    val uri = request.uri()
    val codeMatcher = OAUTH_CODE_PATTERN.matcher(uri)
    val openCourseMatcher = OPEN_COURSE_PATTERN.matcher(uri)
    return if (request.method() === HttpMethod.GET && (codeMatcher.matches() || openCourseMatcher.matches())) {
      true
    }
    else super.isHostTrusted(request)
  }

  @Throws(IOException::class)
  override fun execute(decoder: QueryStringDecoder,
                       request: FullHttpRequest,
                       context: ChannelHandlerContext): String? {
    val uri = decoder.uri()

    val matcher = OPEN_COURSE_PATTERN.matcher(uri)
    if (matcher.matches()) {
      val stageId = RestService.getStringParameter("stage_id", decoder)?.toInt() ?: return "The stage_id parameter was not found"
      val projectId = RestService.getStringParameter("project_id", decoder)?.toInt() ?: return "The project_id parameter was not found"
      LOG.info("Opening a stage $stageId from project $projectId")
      //TODO: try to find existing project
      createProject(projectId, stageId)
      RestService.sendOk(request, context)
      LOG.info("Hyperskill project opened: $projectId")
      return null
    }

    if (OAUTH_CODE_PATTERN.matcher(uri).matches()) {
      val code = RestService.getStringParameter("code", decoder)!! // cannot be null because of pattern

      val success = HyperskillConnector.login(code)
      if (success) {
        RestService.LOG.info("$myPlatformName: OAuth code is handled")
        val pageContent = FileTemplateManager.getDefaultInstance().getInternalTemplate("hyperskill.redirectPage.html").text
        createResponse(pageContent).send(context.channel(), request)
        return null
      }
      return sendErrorResponse(request, context, "Failed to login using provided code")
    }

    RestService.sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel())
    return "Unknown command: $uri"
  }

  private fun createProject(projectId: Int, stageId: Int) {
    // TODO: open stage with stageId
    // TODO: handle not logged in users
    runInEdt {
      val hyperskillCourse = ProgressManager.getInstance().run(object : Task.WithResult<HyperskillCourse?, Exception>
                                                                        (null, "Loading project", true) {
        override fun compute(indicator: ProgressIndicator): HyperskillCourse? {
          val stages = HyperskillConnector.getStages(projectId) ?: return null
          val account = HyperskillSettings.INSTANCE.account ?: return null
          val hyperskillProject = stages[0].hyperskillProject ?: return null
          account.userInfo.hyperskillProject = hyperskillProject
          if (!hyperskillProject.useIde) {
            // TODO: show notification
            return null
          }
          val languageId = EduNames.JAVA
          val hyperskillCourse = HyperskillCourse(hyperskillProject.title, languageId)
          hyperskillCourse.stages = stages
          return hyperskillCourse
        }
      }) ?: return@runInEdt

      val dialog = JoinCourseDialog(hyperskillCourse)
      dialog.show()
    }
  }

  companion object {
    private const val EDU_HYPERSKILL_SERVICE_NAME = "edu/hyperskill"
    private val OAUTH_CODE_PATTERN = Pattern.compile("/api/$EDU_HYPERSKILL_SERVICE_NAME/oauth\\?code=(\\w+)")
    private val OPEN_COURSE_PATTERN = Pattern.compile("/api/$EDU_HYPERSKILL_SERVICE_NAME\\?stage_id=.+&project_id=.+")
  }
}
