package com.jetbrains.edu.learning;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.jetbrains.edu.learning.stepik.StepicUser;
import com.jetbrains.edu.learning.stepik.StepikUserWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@State(name = "EduSettings", storages = @Storage("other.xml"))
public class EduSettings implements PersistentStateComponent<EduSettings> {
  public static final Topic<StudySettingsListener> SETTINGS_CHANGED = Topic.create("Edu.UserSet", StudySettingsListener.class);
  private StepicUser myUser;
  public long LAST_TIME_CHECKED = 0;
  public boolean myShouldUseJavaFx = EduUtils.hasJavaFx();

  public EduSettings() {
  }

  public long getLastTimeChecked() {
    return LAST_TIME_CHECKED;
  }

  public void setLastTimeChecked(long timeChecked) {
    LAST_TIME_CHECKED = timeChecked;
  }

  @Nullable
  @Override
  public EduSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull EduSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public static EduSettings getInstance() {
    return ServiceManager.getService(EduSettings.class);
  }

  @Nullable
  public StepicUser getUser() {
    return myUser;
  }

  public void setUser(@Nullable final StepicUser user) {
    myUser = user;
    ApplicationManager.getApplication().getMessageBus().syncPublisher(SETTINGS_CHANGED).settingsChanged();

    for (StepikUserWidget widget : getStepikWidgets()) {
      widget.update();
    }
  }

  public boolean shouldUseJavaFx() {
    return myShouldUseJavaFx;
  }

  public void setShouldUseJavaFx(boolean shouldUseJavaFx) {
    this.myShouldUseJavaFx = shouldUseJavaFx;
  }

  public static boolean isLoggedIn() {
    return getInstance().myUser != null;
  }

  @NotNull
  private static List<StepikUserWidget> getStepikWidgets() {
    IdeFrame[] frames = WindowManager.getInstance().getAllProjectFrames();
    return Arrays.stream(frames)
      .filter(frame -> frame instanceof IdeFrameImpl)
      .map(frame -> frame.getStatusBar().getWidget(StepikUserWidget.ID))
      .filter(widget -> widget != null)
      .map(widget -> (StepikUserWidget)widget)
      .collect(Collectors.toList());
  }

  @FunctionalInterface
  public interface StudySettingsListener {
    void settingsChanged();
  }
}
