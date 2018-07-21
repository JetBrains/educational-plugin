package com.jetbrains.edu.learning.authUtils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Android Studio doesn't allow to use built-in server
 * without credentials, so {@link CustomAuthorizationServer}
 * is used for OAuth authorization from Android Studio.
 */
public class CustomAuthorizationServer {
  private static final Logger LOG = Logger.getInstance(CustomAuthorizationServer.class);

  private HttpServer myServer;
  private final String myPlatformName;

  public CustomAuthorizationServer(@NotNull String platformName) {
    this.myPlatformName = platformName;
  }

  public int handle(@NotNull ContextHandler handler) {
    int port = IntStream.rangeClosed(36656, 36665).filter(CustomAuthorizationServer::isPortAvailable).findFirst().orElse(-1);

    if (port != -1) {
      SocketConfig socketConfig = SocketConfig.custom()
        .setSoTimeout(15000)
        .setTcpNoDelay(true)
        .build();

      myServer = ServerBootstrap.bootstrap()
        .setListenerPort(port)
        .setServerInfo(myPlatformName + " authorization server")
        .registerHandler("*", handler)
        .setSocketConfig(socketConfig)
        .create();

      try {
        myServer.start();
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

  private void stopServerInNewThread() {
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

  private static boolean isPortAvailable(int port) {
    try (Socket ignored = new Socket("localhost", port)) {
      return false;
    }
    catch (IOException ignored) {
      return true;
    }
  }

  public int getPort() {
    return myServer.getLocalPort();
  }

  public abstract static class ContextHandler implements HttpRequestHandler {
    private final CustomAuthorizationServer myServer;

    public ContextHandler(@NotNull CustomAuthorizationServer server) {
      myServer = server;
    }


    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws IOException {
      LOG.info("Handling auth response");

      try {
        List<NameValuePair> parse = URLEncodedUtils.parse(new URI(httpRequest.getRequestLine().getUri()), "UTF-8");
        for (NameValuePair pair : parse) {
          if (pair.getName().equals("code")) {
            String code = pair.getValue();
            String errorMessage = afterCodeReceived(code);

            if (errorMessage == null) {
              sendOkResponse(httpResponse, myServer.myPlatformName);
            } else {
              LOG.warn(errorMessage);
              sendErrorResponse(httpResponse, myServer.myPlatformName, errorMessage);
            }

            break;
          }
        }
      }
      catch (URISyntaxException e) {
        LOG.warn(e.getMessage());
        sendErrorResponse(httpResponse, myServer.myPlatformName, "Invalid response");
      }
      finally {
        myServer.stopServerInNewThread();
      }
    }

    /**
     * This function encapsulate authorization process after receiving
     * OAuth code. It applies to OAuth code and has to
     * return null in case of successful authorization and not null
     * error message otherwise.
     * */
    public abstract String afterCodeReceived(@NotNull String code);

    public int getPort() {
      return myServer.getPort();
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
}
