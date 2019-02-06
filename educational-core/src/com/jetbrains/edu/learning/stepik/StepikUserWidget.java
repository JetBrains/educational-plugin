package com.jetbrains.edu.learning.stepik;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IconLikeCustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.ui.ClickListener;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.actions.SyncCourseAction;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class StepikUserWidget implements IconLikeCustomStatusBarWidget {
  public static final String ID = "StepikUserWidget";
  private JLabel myComponent;

  public StepikUserWidget(@NotNull Project project) {
    Icon icon = getWidgetIcon(EduSettings.getInstance().getUser());
    myComponent = new JLabel(icon);

    final MessageBusConnection busConnection = ApplicationManager.getApplication().getMessageBus().connect();
    busConnection.subscribe(EduSettings.SETTINGS_CHANGED, () -> update());

    new ClickListener() {
      @Override
      public boolean onClick(@NotNull MouseEvent e, int clickCount) {
        StepikUser user = EduSettings.getInstance().getUser();
        ListPopup popup = createPopup(user, project);
        Dimension preferredSize = popup.getContent().getPreferredSize();
        Point point = new Point(0, 0);
        point = new Point(point.x - preferredSize.width, point.y - preferredSize.height);
        popup.show(new RelativePoint(myComponent, point));

        return true;
      }
    }.installOn(myComponent);
  }

  @NotNull
  @Override
  public String ID() {
    return ID;
  }

  @Nullable
  @Override
  public WidgetPresentation getPresentation(@NotNull PlatformType type) {
    return null;
  }

  @Override
  public void install(@NotNull StatusBar statusBar) {
  }

  @Override
  public void dispose() {
    Disposer.dispose(this);
  }

  private void update() {
    StepikUser user = EduSettings.getInstance().getUser();
    Icon icon = getWidgetIcon(user);
    myComponent.setIcon(icon);
  }

  private static Icon getWidgetIcon(@Nullable StepikUser user) {
    return user == null ? EducationalCoreIcons.StepikOff : EducationalCoreIcons.Stepik;
  }

  private static ListPopup createPopup(@Nullable StepikUser user, @NotNull Project project) {
    String loginText = "Log in ";
    String logOutText = "Log out";
    String syncCourseStep = "Synchronize course";
    String userActionStep = user == null ? loginText : logOutText;
    ArrayList<String> steps = new ArrayList<>();
    if (user != null && SyncCourseAction.isAvailable(project)) {
      steps.add(syncCourseStep);
    }
    steps.add(userActionStep);

    BaseListPopupStep stepikStep = new BaseListPopupStep<String>(null, steps) {
      @Override
      public PopupStep onChosen(String selectedValue, boolean finalChoice) {
        return doFinalStep(() -> {
          if (syncCourseStep.equals(selectedValue)) {
            EduUsagesCollector.progressFromWidget();
            SyncCourseAction.doUpdate(project);
          }
          else if (loginText.equals(selectedValue)) {
            EduUsagesCollector.loginFromWidget();
            StepikAuthorizer.doAuthorize(EduUtils::showOAuthDialog);
          }
          else if (logOutText.equals(selectedValue)) {
            EduUsagesCollector.logoutFromWidget();
            EduSettings.getInstance().setUser(null);
          }
        });
      }
    };
    return JBPopupFactory.getInstance().createListPopup(stepikStep);
  }

  @Override
  public JComponent getComponent() {
    return myComponent;
  }
}
