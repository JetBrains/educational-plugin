package com.jetbrains.edu.learning;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.jetbrains.edu.learning.stepic.StepicUser;
import com.jetbrains.edu.learning.stepic.StepicUserWidget;
import org.jetbrains.annotations.Nullable;

@State(name = "EduSettings", storages = @Storage("other.xml"))
public class EduSettings implements PersistentStateComponent<EduSettings> {
  public static final Topic<StudySettingsListener> SETTINGS_CHANGED = Topic.create("Edu.UserSet", StudySettingsListener.class);
  private StepicUser myUser;
  public long LAST_TIME_CHECKED = 0;
  private boolean myEnableTestingFromSamples = false;
  public boolean myShouldUseJavaFx = StudyUtils.hasJavaFx();

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
  public void loadState(EduSettings state) {
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
    updateStepicUserWidget();
  }

  public boolean shouldUseJavaFx() {
    return myShouldUseJavaFx;
  }

  public void setShouldUseJavaFx(boolean shouldUseJavaFx) {
    this.myShouldUseJavaFx = shouldUseJavaFx;
  }

  private static void updateStepicUserWidget() {
    StepicUserWidget widget = StudyUtils.getStepicWidget();
    if (widget != null) {
      widget.update();
    }
  }

  public boolean isEnableTestingFromSamples() {
    return myEnableTestingFromSamples;
  }

  public void setEnableTestingFromSamples(boolean enableTestingFromSamples) {
    myEnableTestingFromSamples = enableTestingFromSamples;
    ApplicationManager.getApplication().getMessageBus().syncPublisher(SETTINGS_CHANGED).settingsChanged();
  }

  @FunctionalInterface
  public interface StudySettingsListener {
    void settingsChanged();
  }
}
