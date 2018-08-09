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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Android Studio doesn't allow to use built-in server,
 * so {@link CustomAuthorizationServer}
 * is used for OAuth authorization from Android Studio.
 */
public class CustomAuthorizationServer {
  private static final Logger LOG = Logger.getInstance(CustomAuthorizationServer.class);

  private static final Map<String, CustomAuthorizationServer> ourServerByName = new HashMap<>();
  private static final ReentrantLock ourLock = new ReentrantLock();

  private final HttpServer myServer;

  private CustomAuthorizationServer(@NotNull HttpServer server) {
    myServer = server;
  }

  private void stopServer() {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        LOG.info("Stopping server");
        myServer.stop();
        LOG.info("Server stopped");
      }
      catch (Exception e) {
        LOG.warn(e.getMessage());
      }
    });
  }

  public int getPort() {
    return myServer.getLocalPort();
  }

  public static CustomAuthorizationServer getServerIfStarted(@NotNull String platformName) {
    return ourServerByName.get(platformName);
  }

  public static int create(
    @NotNull String platformName,
    @NotNull Range<Integer> portsToTry,
    @NotNull String handlerPath,
    @NotNull Function<String, String> afterCodeReceived
  ) {
    return create(
      platformName,
      IntStream.rangeClosed(portsToTry.getFrom(), portsToTry.getTo()).boxed().collect(Collectors.toList()),
      handlerPath,
      afterCodeReceived
    );
  }

  public static int create(
    @NotNull String platformName,
    @NotNull Collection<Integer> portsToTry,
    @NotNull String handlerPath,
    @NotNull Function<String, String> afterCodeReceived
  ) {
    ourLock.lock();
    int port = createServer(platformName, portsToTry, handlerPath, afterCodeReceived);
    ourLock.unlock();
    return port;
  }

  private static int createServer(
    @NotNull String platformName,
    @NotNull Collection<Integer> portsToTry,
    @NotNull String handlerPath,
    @NotNull Function<String, String> afterCodeReceived
  ) {
    int port = portsToTry.stream().filter(CustomAuthorizationServer::isPortAvailable).findFirst().orElse(-1);

    if (port != -1) {
      SocketConfig socketConfig = SocketConfig.custom()
        .setSoTimeout(15000)
        .setTcpNoDelay(true)
        .build();

      final HttpServer newServer = ServerBootstrap.bootstrap()
        .setListenerPort(port)
        .setServerInfo(platformName)
        .registerHandler(handlerPath, createContextHandler(platformName, afterCodeReceived))
        .setSocketConfig(socketConfig)
        .create();

      try {
        newServer.start();
        ourServerByName.put(platformName, new CustomAuthorizationServer(newServer));
        return port;
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
        return -1;
      }
    }

    LOG.warn("No available ports");
    return -1;
  }

  private static boolean isPortAvailable(int port) {
    try (Socket ignored = new Socket("localhost", port)) {
      return false;
    }
    catch (IOException ignored) {
      return true;
    }
  }

  private static HttpRequestHandler createContextHandler(@NotNull String platformName, @NotNull Function<String, String> afterCodeReceived) {
    return (request, response, context) -> {
      LOG.info("Handling auth response");

      try {
        final List<NameValuePair> parse = URLEncodedUtils.parse(new URI(request.getRequestLine().getUri()), Charset.forName("UTF-8"));
        for (NameValuePair pair : parse) {
          if (pair.getName().equals("code")) {
            String code = pair.getValue();
            String errorMessage = afterCodeReceived.apply(code);

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
        ourServerByName.get(platformName).stopServer();
      }
    };
  }

  private static void sendOkResponse(@NotNull HttpResponse httpResponse, @NotNull String platformName) throws IOException {
    final String okPageContent = BuiltinServerUtils.getOkPageContent(platformName);
    sendResponse(httpResponse, okPageContent);
  }

  private static void sendErrorResponse(@NotNull HttpResponse httpResponse, @NotNull String platformName, @NotNull String errorMessage) throws IOException {
    final String errorPageContent = BuiltinServerUtils.getErrorPageContent(platformName, errorMessage);
    sendResponse(httpResponse, errorPageContent);
  }

  private static void sendResponse(@NotNull HttpResponse httpResponse, @NotNull String pageContent) throws IOException {
    httpResponse.setHeader("Content-Type", "text/html");
    httpResponse.setEntity(new StringEntity(pageContent));
  }
}
