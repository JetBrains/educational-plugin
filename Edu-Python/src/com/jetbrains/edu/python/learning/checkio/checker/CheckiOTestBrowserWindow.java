package com.jetbrains.edu.python.learning.checkio.checker;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOConnector;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.ui.taskDescription.BrowserWindow;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLInputElement;
import org.w3c.dom.html.HTMLTextAreaElement;

import java.util.concurrent.SynchronousQueue;

public class CheckiOTestBrowserWindow extends BrowserWindow {
  private static Logger LOG = Logger.getInstance(CheckiOTestBrowserWindow.class);

  public CheckiOTestBrowserWindow(@NotNull Project project) {
    super(project, false, false);
  }

  private final SynchronousQueue<Integer> checkResult = new SynchronousQueue<>();

  @NotNull
  public CheckResult checkOnBackground(@NotNull String accessToken,
                                       @NotNull String taskId,
                                       @NotNull String interpreter,
                                       @NotNull String code) {
    LOG.info("Start checking");
    doCheck(accessToken, taskId, interpreter, code);
    try {
      return createResult(checkResult.take());
    }
    catch (InterruptedException e) {
      LOG.warn(e);
      return CheckResult.FAILED_TO_CHECK;
    }
  }

  private static CheckResult createResult(int result) {
    return result == 1
           ? new CheckResult(CheckStatus.Solved, "Successfully passed all test cases")
           : new CheckResult(CheckStatus.Failed, "Tests failed");
  }

  private void doCheck(@NotNull String accessToken,
                       @NotNull String taskId,
                       @NotNull String interpreter,
                       @NotNull String code) {
    setFormLoadedListener(accessToken, taskId, interpreter, code);
    LOG.info("Form listener set");
    setCheckDoneListener();
    LOG.info("Check done listener set");
    loadForm();
    LOG.info("Form loaded");
  }

  private void loadForm() {
    Platform.runLater(() -> {
      final String formUrl = CheckiOConnector.class.getResource("/checkio/checkioTestForm.html").toExternalForm();
      getEngine().load(formUrl);
    });
  }

  private void setFormLoadedListener(@NotNull String accessToken,
                                     @NotNull String taskId,
                                     @NotNull String interpreter,
                                     @NotNull String code) {
    Platform.runLater(() -> getEngine().getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
      if (newState == Worker.State.SUCCEEDED && getEngine().getLocation().contains("checkioTestForm.html")) {
        final Document documentWithForm = getEngine().getDocument();
        ((HTMLInputElement) documentWithForm.getElementById("access-token")).setValue(accessToken);
        ((HTMLInputElement) documentWithForm.getElementById("task-id")).setValue(taskId);
        ((HTMLInputElement) documentWithForm.getElementById("interpreter")).setValue(interpreter);
        ((HTMLTextAreaElement) documentWithForm.getElementById("code")).setValue(code);

        ((HTMLFormElement) documentWithForm.getElementById("test-form")).submit();
        LOG.info("Form submitted");
      }
    }));
  }

  private final TestResultHandler myHandler = (res) ->
    ApplicationManager.getApplication().invokeLater(() -> {
      LOG.info("Handling");
      checkResult.offer(res);
    });

  private void setCheckDoneListener() {
    Platform.runLater(() -> {
      final boolean[] visited = {false};
      getEngine().getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
        if (getEngine().getLocation().contains("checkio.org") && newState == Worker.State.SUCCEEDED && !visited[0]) {
          Platform.runLater(() -> {
            visited[0] = true;
            try {
              final JSObject jsObject = (JSObject) getEngine().executeScript("window");

              jsObject.setMember("java", myHandler);

              LOG.info("Before script");

              getEngine().setOnAlert((e) -> ApplicationManager.getApplication().invokeLater(() -> Messages.showInfoMessage(e.getData(), "Alert")));

              getEngine().executeScript(
                "function handleEvent(e) {\n" +
                "\twindow.java.handleTestEvent(e.detail.success)\n" +
                "}\n" +
                "alert(123)\n" +
                "window.addEventListener(\"checkio:checkDone\", handleEvent, false)"
              );
              LOG.info("After script");
            } catch (JSException e) {
              LOG.warn(e);
            }
          });
        }
      });
    });
  }

  private WebEngine getEngine() {
    return myWebComponent.getEngine();
  }

  @FunctionalInterface
  public interface TestResultHandler {
    @SuppressWarnings("unused") // used in JS code
    void handleTestEvent(int result);
  }
}
