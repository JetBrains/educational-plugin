package com.jetbrains.edu.python.learning.checkio.checker;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.ui.taskDescription.BrowserWindow;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import netscape.javascript.JSObject;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLInputElement;
import org.w3c.dom.html.HTMLTextAreaElement;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PyCheckiOMissionChecker implements Callable<CheckResult> {
  private static final Logger LOG = Logger.getInstance(PyCheckiOMissionChecker.class);

  private final Project myProject;
  private final Task myTask;

  private final BrowserWindow myBrowserWindow;
  private final CheckiOTestResultHandler myResultHandler = new CheckiOTestResultHandler();

  private CheckResult myCheckResult;
  private CountDownLatch myLatch = new CountDownLatch(1);

  public PyCheckiOMissionChecker(@NotNull Project project, @NotNull Task task) {
    myProject = project;
    myTask = task;

    myBrowserWindow = new BrowserWindow(myProject, false, false);
  }

  @NotNull
  @Override
  public CheckResult call() {
    final Editor selectedEditor = EduUtils.getSelectedEditor(myProject);
    if (selectedEditor == null) {
      return CheckResult.FAILED_TO_CHECK;
    }

    final String accessToken = PyCheckiOOAuthConnector.getInstance().getAccessToken();
    final String taskId = String.valueOf(myTask.getId());
    final String interpreter = EduNames.CHECKIO_PYTHON_INTERPRETER;
    final String code = selectedEditor.getDocument().getText();

    if (accessToken == null) {
      return new CheckResult(CheckStatus.Unchecked, "Log in to Py CheckiO to check the task");
    }

    try {
      return doCheck(accessToken, taskId, interpreter, code);
    }
    catch (Exception e) {
      LOG.warn(e.getMessage());
      return CheckResult.FAILED_TO_CHECK;
    }
  }

  @SuppressWarnings("SameParameterValue")
  @NotNull
  private CheckResult doCheck(@NotNull String accessToken, @NotNull String taskId, @NotNull String interpreter, @NotNull String code)
    throws InterruptedException {
    setTestFormLoadedListener(accessToken, taskId, interpreter, code);
    setCheckDoneListener();
    loadTestForm();

    boolean timeoutExceeded = !myLatch.await(15L, TimeUnit.SECONDS);
    if (timeoutExceeded) {
      myCheckResult = new CheckResult(CheckStatus.Unchecked, "Checking took too much time");
    }
    return myCheckResult;
  }

  private void loadTestForm() {
    Platform.runLater(() -> {
      final String formUrl = CheckiOApiConnector.class.getResource(CheckiONames.CHECKIO_TEST_FORM_URL).toExternalForm();
      myBrowserWindow.getEngine().load(formUrl);
    });
  }

  private void setCheckDoneListener() {
    Platform.runLater(() -> {
      final Ref<Boolean> visited = new Ref<>(Boolean.FALSE);

      myBrowserWindow.getEngine().getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
        if (newState == Worker.State.FAILED) {
          setConnectionError();
          return;
        }

        if (myBrowserWindow.getEngine().getLocation().contains("checkio.org") && newState == Worker.State.SUCCEEDED && !visited.get()) {
          visited.set(Boolean.TRUE);

          final JSObject windowObject = (JSObject)myBrowserWindow.getEngine().executeScript("window");
          windowObject.setMember("javaHandler", myResultHandler);

          myBrowserWindow.getEngine().executeScript(
            "function handleEvent(e) {\n" +
            "\twindow.javaHandler.handleTestEvent(e.detail.success)\n" +
            "}\n" +
            "window.addEventListener(\"checkio:checkDone\", handleEvent, false)"
          );
        }
      });
    });
  }

  private void setTestFormLoadedListener(@NotNull String accessToken,
                                         @NotNull String taskId,
                                         @NotNull String interpreter,
                                         @NotNull String code) {
    Platform.runLater(() -> myBrowserWindow.getEngine().getLoadWorker().stateProperty().addListener(((observable, oldState, newState) -> {
      if (newState == Worker.State.FAILED) {
        setConnectionError();
        return;
      }

      if (newState == Worker.State.SUCCEEDED && myBrowserWindow.getEngine().getLocation().contains("checkioTestForm.html")) {
        final Document documentWithForm = myBrowserWindow.getEngine().getDocument();
        ((HTMLInputElement)documentWithForm.getElementById("access-token")).setValue(accessToken);
        ((HTMLInputElement)documentWithForm.getElementById("task-id")).setValue(taskId);
        ((HTMLInputElement)documentWithForm.getElementById("interpreter")).setValue(interpreter);
        ((HTMLTextAreaElement)documentWithForm.getElementById("code")).setValue(code);

        ((HTMLFormElement)documentWithForm.getElementById("test-form")).submit();
      }
    })));
  }

  private void setConnectionError() {
    LOG.warn(CheckResult.CONNECTION_FAILED.getMessage());
    myCheckResult = CheckResult.CONNECTION_FAILED;
    myLatch.countDown();
  }

  public JFXPanel getBrowserPanel() {
    return myBrowserWindow.getPanel();
  }

  public class CheckiOTestResultHandler {
    @SuppressWarnings("unused") // used in JS code
    public void handleTestEvent(int result) {
      myCheckResult = result == 1 ?
                      new CheckResult(CheckStatus.Solved, "All tests passed") :
                      new CheckResult(CheckStatus.Failed, "Tests failed");
      myLatch.countDown();
    }
  }
}
