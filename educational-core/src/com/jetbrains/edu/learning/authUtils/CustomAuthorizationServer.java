package com.jetbrains.edu.learning.authUtils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Range;
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

  private static final Map<String, CustomAuthorizationServer> serverByName = new HashMap<>();
  private static final ReentrantLock lock = new ReentrantLock();
  private static final Range<Integer> defaultPortsTotry = new Range<>(36656, 36665);

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
    return serverByName.get(platformName);
  }

  @NotNull
  public static CustomAuthorizationServer create(
    @NotNull String platformName,
    @NotNull String handlerPath,
    @NotNull CodeHandler afterCodeReceived
  ) throws IOException {
    return create(
      platformName,
      defaultPortsTotry,
      handlerPath,
      afterCodeReceived
    );
  }

  @NotNull
  public static CustomAuthorizationServer create(
    @NotNull String platformName,
    @NotNull Range<Integer> portsToTry,
    @NotNull String handlerPath,
    @NotNull CodeHandler afterCodeReceived
  ) throws IOException {
    return create(
      platformName,
      IntStream.rangeClosed(portsToTry.getFrom(), portsToTry.getTo()).boxed().collect(Collectors.toList()),
      handlerPath,
      afterCodeReceived
    );
  }

  @NotNull
  public static CustomAuthorizationServer create(
    @NotNull String platformName,
    @NotNull Collection<Integer> portsToTry,
    @NotNull String handlerPath,
    @NotNull CodeHandler afterCodeReceived
  ) throws IOException {
    lock.lock();
    final CustomAuthorizationServer server = createServer(platformName, portsToTry, handlerPath, afterCodeReceived);
    serverByName.put(platformName, server);
    lock.unlock();
    return server;
  }

  @NotNull
  private static CustomAuthorizationServer createServer(
    @NotNull String platformName,
    @NotNull Collection<Integer> portsToTry,
    @NotNull String handlerPath,
    @NotNull CodeHandler afterCodeReceived
  ) throws IOException {
    int port = portsToTry.stream().filter(CustomAuthorizationServer::isPortAvailable).findFirst().orElse(-1);

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
            String code = pair.getValue();
            String errorMessage = afterCodeReceived.apply(code, getServerIfStarted(platformName).getHandlingUri());

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
        serverByName.get(platformName).stopServer();
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
