package com.jetbrains.edu.python.learning;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.util.PlatformUtils;
import com.jetbrains.edu.python.learning.messages.EduPythonBundle;

@SuppressWarnings("ComponentNotRegistered") // Edu-Python.xml
public class PyShowTutorial implements ProjectComponent {

  private static final String POPUP_SHOWN = "StudyShowPopup";
  private final Project myProject;

  protected PyShowTutorial(Project project) {
    myProject = project;
  }

  @Override
  public void projectOpened() {
    if (PropertiesComponent.getInstance().isValueSet(POPUP_SHOWN) || !PlatformUtils.isPyCharmEducational()) {
      return;
    }
    ApplicationManager.getApplication().invokeLater((DumbAwareRunnable)() -> ApplicationManager.getApplication().runWriteAction(
      (DumbAwareRunnable)() -> {
        final Notification notification = new Notification(
          EduPythonBundle.message("watch.tutorials.title"),
          "",
          EduPythonBundle.message("watch.tutorials.message.html", "https://www.jetbrains.com/pycharm-edu/quickstart/"),
          NotificationType.INFORMATION,
          new NotificationListener.UrlOpeningListener(true)
        );
        StartupManager.getInstance(myProject).registerPostStartupActivity(() -> {
          Notifications.Bus.notify(notification);
          PropertiesComponent.getInstance().setValue(POPUP_SHOWN, true);
        });
      }));
  }
}