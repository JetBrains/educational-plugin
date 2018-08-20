package com.jetbrains.edu.learning.authUtils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
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
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Android Studio doesn't allow using built-in server,
 * without credentials, so {@link CustomAuthorizationServer}
 * is used for OAuth authorization from Android Studio.
 */
public class CustomAuthorizationServer {
  private static final Logger LOG = Logger.getInstance(CustomAuthorizationServer.class);

  private static final Map<String, CustomAuthorizationServer> SERVER_BY_NAME = new HashMap<>();
  private static final ReentrantLock LOCK = new ReentrantLock();
  private static final Collection<Integer> DEFAULT_PORTS_TO_TRY = IntStream.rangeClosed(36656, 36665).boxed().collect(Collectors.toList());

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
    @NotNull CodeHandler afterCodeReceived
  ) throws IOException {
    LOCK.lock();
    try {
      final CustomAuthorizationServer server = createServer(platformName, handlerPath, afterCodeReceived);
      SERVER_BY_NAME.put(platformName, server);
      return server;
    } finally {
      LOCK.unlock();
    }
  }

  @NotNull
  private static CustomAuthorizationServer createServer(
    @NotNull String platformName,
    @NotNull String handlerPath,
    @NotNull CodeHandler afterCodeReceived
  ) throws IOException {
    int port = DEFAULT_PORTS_TO_TRY.stream().filter(CustomAuthorizationServer::isPortAvailable).findFirst().orElse(-1);

    if (port == -1) {
      throw new IOException("No ports available");
    }

    final SocketConfig socketConfig = SocketConfig.custom()
      .setSoTimeout(15000)
      .setTcpNoDelay(true)
      .build();

    final HttpServer newServer = ServerBootstrap.bootstrap()
      .setListenerPort(port)
      .setServerInfo(platformName)
      .registerHandler(handlerPath, createContextHandler(platformName, afterCodeReceived))
      .setSocketConfig(socketConfig)
      .create();

    newServer.start();
    return new CustomAuthorizationServer(newServer, handlerPath);
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
    @NotNull CodeHandler afterCodeReceived
  ) {
    return (request, response, context) -> {
      LOG.info("Handling auth response");

      try {
        final List<NameValuePair> parse = URLEncodedUtils.parse(new URI(request.getRequestLine().getUri()), Charset.forName("UTF-8"));
        for (NameValuePair pair : parse) {
          if (pair.getName().equals("code")) {
            final String code = pair.getValue();

            final CustomAuthorizationServer currentServer = getServerIfStarted(platformName);
            assert currentServer != null; // cannot be null: if this concrete handler is working then corresponding server is working too

            final String errorMessage = afterCodeReceived.apply(code, currentServer.getHandlingUri());

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

  // 1. Receives `oauth code` and `redirect uri` (i.e. the path `oauth code` was handled on)
  // 2. Authorizes new user
  // 3. Returns error message or null in case of successful authorization
  @FunctionalInterface
  public interface CodeHandler extends BiFunction<String, String, String> {

  }
}
