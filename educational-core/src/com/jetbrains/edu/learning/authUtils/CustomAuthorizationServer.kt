package com.jetbrains.edu.learning.authUtils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.authUtils.OAuthUtils.getErrorPageContent
import com.jetbrains.edu.learning.authUtils.OAuthUtils.getOkPageContent
import com.jetbrains.edu.learning.stepik.StepikNames
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.config.SocketConfig
import org.apache.http.entity.StringEntity
import org.apache.http.impl.bootstrap.HttpServer
import org.apache.http.impl.bootstrap.ServerBootstrap
import org.apache.http.protocol.HttpContext
import org.apache.http.protocol.HttpRequestHandler
import java.io.IOException
import java.net.Socket
import java.net.URI
import java.net.URISyntaxException

/**
 * Android Studio doesn't allow using built-in server,
 * without credentials, so [CustomAuthorizationServer]
 * is used for OAuth authorization from Android Studio.
 */
class CustomAuthorizationServer private constructor(private val server: HttpServer, handlerPath: String) {
  private fun stopServer() {
    ApplicationManager.getApplication().executeOnPooledThread {
      LOG.info("Stopping server")
      server.stop()
      LOG.info("Server stopped")
    }
  }

  val port: Int
    get() = server.localPort

  val handlingUri: String = "http://localhost:$port$handlerPath"

  fun interface CodeHandler {
    /**
     * @see CustomAuthorizationServer.createContextHandler
     * @param code oauth authorization code
     * @param handlingUri uri the code wah handled on (is used as redirect_uri in tokens request)
     *
     * @return non-null error message in case of error, null otherwise
     */
    fun handle(code: String, handlingUri: String): String?
  }

  companion object {
    private val LOG = logger<CustomAuthorizationServer>()
    private val SERVER_BY_NAME = mutableMapOf<String, CustomAuthorizationServer>()
    private val DEFAULT_PORTS_TO_TRY = (36656..36665)
    fun getServerIfStarted(platformName: String): CustomAuthorizationServer? = SERVER_BY_NAME[platformName]

    @Throws(IOException::class)
    fun create(
      platformName: String,
      handlerPath: String,
      codeHandler: CodeHandler
    ): CustomAuthorizationServer {
      val server = createServer(platformName, handlerPath, codeHandler)
      SERVER_BY_NAME[platformName] = server
      return server
    }

    @Synchronized
    @Throws(IOException::class)
    private fun createServer(
      platformName: String,
      handlerPath: String,
      codeHandler: CodeHandler
    ): CustomAuthorizationServer {
      val port = availablePort ?: throw IOException("No ports available")
      val socketConfig = SocketConfig.custom()
        .setSoTimeout(15000)
        .setTcpNoDelay(true)
        .build()

      // In case of Stepik our redirect_uri is `http://localhost:port`
      // but authorization code request is sent on `http://localhost:port/`
      // So we have to add additional slash
      val slashIfNeeded = if (platformName == StepikNames.STEPIK) "/" else ""
      val newServer = ServerBootstrap.bootstrap()
        .setListenerPort(port)
        .setServerInfo(platformName)
        .registerHandler(handlerPath + slashIfNeeded, createContextHandler(platformName, codeHandler))
        .setSocketConfig(socketConfig)
        .create()
      newServer.start()
      return CustomAuthorizationServer(newServer, handlerPath)
    }

    val availablePort: Int?
      get() = DEFAULT_PORTS_TO_TRY.firstOrNull { isPortAvailable(it) }

    private fun isPortAvailable(port: Int): Boolean {
      return try {
        Socket("localhost", port).use { _ -> false }
      }
      catch (ignored: IOException) {
        true
      }
    }

    private fun createContextHandler(
      platformName: String,
      codeHandler: CodeHandler
    ): HttpRequestHandler {
      return HttpRequestHandler { request: HttpRequest, response: HttpResponse, _: HttpContext? ->
        LOG.info("Handling auth response")
        try {
          val parsed = URLEncodedUtils.parse(URI(request.requestLine.uri), Charsets.UTF_8)
          for (pair in parsed) {
            if (pair.name == "code") {
              val code = pair.value
              // cannot be null: if this concrete handler is working then corresponding server is working too
              val currentServer = getServerIfStarted(platformName) ?: return@HttpRequestHandler
              val errorMessage = codeHandler.handle(code, currentServer.handlingUri)
              if (errorMessage == null) {
                sendOkResponse(response, platformName)
              }
              else {
                LOG.warn(errorMessage)
                sendErrorResponse(response, platformName, errorMessage)
              }
              break
            }
          }
        }
        catch (e: URISyntaxException) {
          LOG.warn(e.message)
          sendErrorResponse(response, platformName, "Invalid response")
        }
        finally {
          SERVER_BY_NAME[platformName]?.stopServer()
          SERVER_BY_NAME.remove(platformName)
        }
      }
    }

    @Throws(IOException::class)
    private fun sendOkResponse(httpResponse: HttpResponse, platformName: String) {
      val okPageContent = getOkPageContent(platformName)
      sendResponse(httpResponse, okPageContent)
    }

    @Throws(IOException::class)
    private fun sendErrorResponse(httpResponse: HttpResponse, platformName: String, errorMessage: String) {
      val errorPageContent = getErrorPageContent(platformName, errorMessage)
      sendResponse(httpResponse, errorPageContent)
    }

    @Throws(IOException::class)
    private fun sendResponse(httpResponse: HttpResponse, pageContent: String) {
      httpResponse.setHeader("Content-Type", "text/html")
      httpResponse.entity = StringEntity(pageContent)
    }
  }
}
