package com.jetbrains.edu.learning.checkio.checker;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.taskDescription.ui.BrowserWindow;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.html.HTMLFormElement;

import javax.swing.*;

import static com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText;

public class JavaFxCheckiOMissionCheck extends CheckiOMissionCheck {
  private final BrowserWindow myBrowserWindow;
  private final CheckiOTestResultHandler myResultHandler;

  protected JavaFxCheckiOMissionCheck(
    @NotNull Task task,
    @NotNull Project project,
    @NotNull CheckiOOAuthConnector oAuthConnector,
    @NotNull String interpreterName,
    @NotNull String testFormTargetUrl
  ) {
    super(project, task, oAuthConnector, interpreterName, testFormTargetUrl);
    myBrowserWindow = new BrowserWindow(project, false);
    myResultHandler = new CheckiOTestResultHandler();
  }

  @Override
  protected void doCheck() {
    Platform.runLater(() -> {
      setTestFormLoadedListener();
      setCheckDoneListener();
      loadTestForm();
    });
  }

  @NotNull
  @Override
  public JComponent getPanel() {
    return myBrowserWindow.getPanel();
  }

  private void loadTestForm() {
    final String html = getInternalTemplateText(CHECKIO_TEST_FORM_TEMPLATE, getResources());
    myBrowserWindow.getEngine().loadContent(html);
  }

  private void setCheckDoneListener() {
    final Ref<Boolean> visited = new Ref<>(Boolean.FALSE);

    myBrowserWindow.getEngine().getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
      if (newState == Worker.State.FAILED) {
        setConnectionError();
        return;
      }

      if (myBrowserWindow.getEngine().getLocation().contains(CheckiONames.CHECKIO_URL)
          && newState == Worker.State.SUCCEEDED
          && !visited.get()) {
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

  private void setTestFormLoadedListener() {
    myBrowserWindow.getEngine().getLoadWorker().stateProperty().addListener(((observable, oldState, newState) -> {
      if (newState == Worker.State.FAILED) {
        setConnectionError();
        return;
      }

      if (newState != Worker.State.SUCCEEDED) {
        return;
      }

      String location = myBrowserWindow.getEngine().getLocation();
      final Document document = myBrowserWindow.getEngine().getDocument();
      final HTMLFormElement testForm = (HTMLFormElement) document.getElementById("test-form");
      if (testForm != null) {
        testForm.submit();
      }

      if (location.contains("check-html-output")) {
        applyCheckiOBackgroundColor(document);
      }
    }));
  }

  private static void applyCheckiOBackgroundColor(@NotNull final Document document) {
    document.getDocumentElement().setAttribute("style", "background-color : #DEE7F6;");
  }

  private void setConnectionError() {
    checkResult = CheckResult.CONNECTION_FAILED;
    getLatch().countDown();
  }

  public class CheckiOTestResultHandler {
    @SuppressWarnings("unused") // used in JS code
    public void handleTestEvent(int result) {
      checkResult = result == 1 ?
                      new CheckResult(CheckStatus.Solved, "All tests passed") :
                      new CheckResult(CheckStatus.Failed, "Tests failed");
      getLatch().countDown();
    }
  }
}
