package com.jetbrains.edu.learning.authUtils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Range;
import com.jetbrains.edu.learning.stepik.StepikNames;
import kotlin.text.Charsets;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpRequestHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Android Studio doesn't allow using built-in server,
 * without credentials, so {@link CustomAuthorizationServer}
 * is used for OAuth authorization from Android Studio.
 */
public class CustomAuthorizationServer {
  private static final Logger LOG = Logger.getInstance(CustomAuthorizationServer.class);

  private static final Map<String, CustomAuthorizationServer> SERVER_BY_NAME = new HashMap<>();
  private static final Range<Integer> DEFAULT_PORTS_TO_TRY = new Range<>(36656, 36665);

  private final HttpServer myServer;
  private final String myHandlerPath;

  private CustomAuthorizationServer(@NotNull HttpServer server, @NotNull String handlerPath) {
    myServer = server;
    myHandlerPath = handlerPath;
  }

  private void stopServer() {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      LOG.info("Stopping server");
      myServer.stop();
      LOG.info("Server stopped");
    });
  }

  public int getPort() {
    return myServer.getLocalPort();
  }

  @NotNull
  public String getHandlingUri() {
    return "http://localhost:" + getPort() + myHandlerPath;
  }

  @Nullable
  public static CustomAuthorizationServer getServerIfStarted(@NotNull String platformName) {
    return SERVER_BY_NAME.get(platformName);
  }

  @NotNull
  public static CustomAuthorizationServer create(
    @NotNull String platformName,
    @NotNull String handlerPath,
    @NotNull CodeHandler codeHandler
  ) throws IOException {
      final CustomAuthorizationServer server = createServer(platformName, handlerPath, codeHandler);
      SERVER_BY_NAME.put(platformName, server);
      return server;
  }

  @NotNull
  private static synchronized CustomAuthorizationServer createServer(
    @NotNull String platformName,
    @NotNull String handlerPath,
    @NotNull CodeHandler codeHandler
  ) throws IOException {
    int port = getAvailablePort();

    if (port == -1) {
      throw new IOException("No ports available");
    }

    final SocketConfig socketConfig = SocketConfig.custom()
      .setSoTimeout(15000)
      .setTcpNoDelay(true)
      .build();

    // In case of Stepik our redirect_uri is `http://localhost:port`
    // but authorization code request is sent on `http://localhost:port/`
    // So we have to add additional slash
    final String slashIfNeeded = (platformName.equals(StepikNames.STEPIK) ? "/" : "");

    final HttpServer newServer = ServerBootstrap.bootstrap()
      .setListenerPort(port)
      .setServerInfo(platformName)
      .registerHandler(handlerPath + slashIfNeeded, createContextHandler(platformName, codeHandler))
      .setSocketConfig(socketConfig)
      .create();

    newServer.start();
    return new CustomAuthorizationServer(newServer, handlerPath);
  }

  public static int getAvailablePort() {
    return IntStream.rangeClosed(DEFAULT_PORTS_TO_TRY.getFrom(), DEFAULT_PORTS_TO_TRY.getTo())
      .filter(CustomAuthorizationServer::isPortAvailable)
      .findFirst()
      .orElse(-1);
  }

  private static boolean isPortAvailable(int port) {
    try (Socket ignored = new Socket("localhost", port)) {
      return false;
    }
    catch (IOException ignored) {
      return true;
    }
  }

  @NotNull
  private static HttpRequestHandler createContextHandler(
    @NotNull String platformName,
    @NotNull CodeHandler codeHandler
  ) {
    return (request, response, context) -> {
      LOG.info("Handling auth response");

      try {
        final List<NameValuePair> parse = URLEncodedUtils.parse(new URI(request.getRequestLine().getUri()), Charsets.UTF_8);
        for (NameValuePair pair : parse) {
          if (pair.getName().equals("code")) {
            final String code = pair.getValue();

            final CustomAuthorizationServer currentServer = getServerIfStarted(platformName);
            assert currentServer != null; // cannot be null: if this concrete handler is working then corresponding server is working too

            final String errorMessage = codeHandler.handle(code, currentServer.getHandlingUri());

            if (errorMessage == null) {
              sendOkResponse(response, platformName);
            } else {
              LOG.warn(errorMessage);
              sendErrorResponse(response, platformName, errorMessage);
            }

            break;
          }
        }
      }
      catch (URISyntaxException e) {
        LOG.warn(e.getMessage());
        sendErrorResponse(response, platformName, "Invalid response");
      }
      finally {
        SERVER_BY_NAME.get(platformName).stopServer();
        SERVER_BY_NAME.remove(platformName);
      }
    };
  }

  private static void sendOkResponse(@NotNull HttpResponse httpResponse, @NotNull String platformName) throws IOException {
    final String okPageContent = OAuthUtils.getOkPageContent(platformName);
    sendResponse(httpResponse, okPageContent);
  }

  private static void sendErrorResponse(@NotNull HttpResponse httpResponse, @NotNull String platformName, @NotNull String errorMessage) throws IOException {
    final String errorPageContent = OAuthUtils.getErrorPageContent(platformName, errorMessage);
    sendResponse(httpResponse, errorPageContent);
  }

  private static void sendResponse(@NotNull HttpResponse httpResponse, @NotNull String pageContent) throws IOException {
    httpResponse.setHeader("Content-Type", "text/html");
    httpResponse.setEntity(new StringEntity(pageContent));
  }


  @FunctionalInterface
  public interface CodeHandler {
    /**
     * @see CustomAuthorizationServer#createContextHandler(String, CodeHandler)
     *
     * Is called when oauth authorization code is handled by the context handler.
     * Encapsulates authorization process and returns error message or null.
     *
     * @param code oauth authorization code
     * @param handlingUri uri the code wah handled on (is used as redirect_uri in tokens request)
     *
     * @return non-null error message in case of error, null otherwise
     * */
    @Nullable
    String handle(@NotNull String code, @NotNull String handlingUri);
  }
}
