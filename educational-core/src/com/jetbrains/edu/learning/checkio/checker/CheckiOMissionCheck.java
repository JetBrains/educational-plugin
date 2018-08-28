package com.jetbrains.edu.learning.checkio.checker;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import com.jetbrains.edu.learning.checkio.notifications.errors.handlers.CheckiOErrorHandler;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.ui.taskDescription.BrowserWindow;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import netscape.javascript.JSObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLInputElement;
import org.w3c.dom.html.HTMLTextAreaElement;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CheckiOMissionCheck implements Callable<CheckResult> {
  private final Project myProject;
  private final Task myTask;

  private final BrowserWindow myBrowserWindow;
  private final CheckiOTestResultHandler myResultHandler;

  @Nullable private CheckResult myCheckResult;
  private final CountDownLatch myLatch = new CountDownLatch(1);
  private final CheckiOOAuthConnector myOAuthConnector;
  private final String myInterpreterName;
  private final String myTestFormTargetUrl;

  protected CheckiOMissionCheck(
    @NotNull Task task,
    @NotNull Project project,
    @NotNull CheckiOOAuthConnector oAuthConnector,
    @NotNull String interpreterName,
    @NotNull String testFormTargetUrl
  ) {
    myProject = project;
    myTask = task;
    myOAuthConnector = oAuthConnector;
    myInterpreterName = interpreterName;
    myTestFormTargetUrl = testFormTargetUrl;

    myResultHandler = new CheckiOTestResultHandler();
    myBrowserWindow = new BrowserWindow(myProject, false, false);
  }

  @NotNull
  @Override
  public CheckResult call() {
    try {
      final String accessToken = myOAuthConnector.getAccessToken();
      final String taskId = String.valueOf(myTask.getId());
      final String code = getCodeFromTask();

      return doCheck(accessToken, taskId, code);
    } catch (InterruptedException e) {
      return new CheckResult(CheckStatus.Unchecked, "Checking was cancelled");
    } catch (Exception e) {
      new CheckiOErrorHandler(
        "Failed to check the task",
        myOAuthConnector
      ).handle(e);
      return CheckResult.FAILED_TO_CHECK;
    }
  }

  private String getCodeFromTask() throws IOException {
    final TaskFile taskFile = ((CheckiOMission) myTask).getTaskFile();
    final VirtualFile missionDir = myTask.getDir(myProject);
    if (missionDir == null) {
      throw new IOException("Directory is not found for mission: " + myTask.getStepId() + ", " + myTask.getName());
    }

    final VirtualFile virtualFile = EduUtils.findTaskFileInDir(taskFile, missionDir);
    if (virtualFile == null) {
      throw new IOException("Virtual file is not found for mission: " + myTask.getStepId() + ", " + myTask.getName());
    }

    final Document document = ApplicationManager.getApplication().runReadAction((Computable<Document>) () ->
      FileDocumentManager.getInstance().getDocument(virtualFile)
    );

    if (document == null) {
      throw new IOException("Document isn't provided for VirtualFile: " + virtualFile.getName());
    }

    return document.getText();
  }

  @NotNull
  private CheckResult doCheck(@NotNull String accessToken, @NotNull String taskId, @NotNull String code)
    throws InterruptedException, NetworkException {

    Platform.runLater(() -> {
      setTestFormLoadedListener(accessToken, taskId, code);
      setCheckDoneListener();
      loadTestForm();
    });

    boolean timeoutExceeded = !myLatch.await(30L, TimeUnit.SECONDS);
    if (timeoutExceeded) {
      return new CheckResult(CheckStatus.Unchecked, "Checking took too much time");
    }

    if (myCheckResult == CheckResult.CONNECTION_FAILED) {
      throw new NetworkException();
    }

    //noinspection ConstantConditions cannot be null because of handler implementation
    return myCheckResult;
  }

  private void loadTestForm() {
    final String formUrl = getClass().getResource(CheckiONames.CHECKIO_TEST_FORM_URL).toExternalForm();
    myBrowserWindow.getEngine().load(formUrl);
  }

  private void setCheckDoneListener() {
    final Ref<Boolean> visited = new Ref<>(Boolean.FALSE);

    myBrowserWindow.getEngine().getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
      if (newState == Worker.State.FAILED) {
        setConnectionError();
        return;
      }

      if (myBrowserWindow.getEngine().getLocation().contains(CheckiONames.CHECKIO_URL) && newState == Worker.State.SUCCEEDED && !visited.get()) {
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
  }

  private void setTestFormLoadedListener(@NotNull String accessToken,
                                         @NotNull String taskId,
                                         @NotNull String code) {
    myBrowserWindow.getEngine().getLoadWorker().stateProperty().addListener(((observable, oldState, newState) -> {
      if (newState == Worker.State.FAILED) {
        setConnectionError();
        return;
      }

      if (newState == Worker.State.SUCCEEDED && myBrowserWindow.getEngine().getLocation().contains("checkioTestForm.html")) {
        final org.w3c.dom.Document documentWithForm = myBrowserWindow.getEngine().getDocument();
        ((HTMLInputElement)documentWithForm.getElementById("access-token")).setValue(accessToken);
        ((HTMLInputElement)documentWithForm.getElementById("task-id")).setValue(taskId);
        ((HTMLInputElement)documentWithForm.getElementById("interpreter")).setValue(myInterpreterName);
        ((HTMLTextAreaElement)documentWithForm.getElementById("code")).setValue(code);

        final HTMLFormElement testForm = (HTMLFormElement) documentWithForm.getElementById("test-form");
        testForm.setAction(myTestFormTargetUrl);
        testForm.submit();
      }
    }));
  }

  private void setConnectionError() {
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
