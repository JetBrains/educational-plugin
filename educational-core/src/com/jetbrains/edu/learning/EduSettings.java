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
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException;
import com.jetbrains.edu.learning.stepik.StepikUser;
import com.jetbrains.edu.learning.stepik.StepikUserInfo;
import com.jetbrains.edu.learning.stepik.StepikUserWidget;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.jetbrains.edu.learning.authUtils.OAuthAccountKt.deserializeAccount;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*;

@State(name = "EduSettings", storages = @Storage("other.xml"))
public class EduSettings implements PersistentStateComponent<Element> {
  public static final Topic<StudySettingsListener> SETTINGS_CHANGED = Topic.create("Edu.UserSet", StudySettingsListener.class);
  @Transient
  @Nullable
  private StepikUser myUser;
  private long myLastTimeChecked = 0;
  private boolean myShouldUseJavaFx = EduUtils.hasJavaFx();

  public EduSettings() {
  }

  public long getLastTimeChecked() {
    return myLastTimeChecked;
  }

  public void setLastTimeChecked(long timeChecked) {
    myLastTimeChecked = timeChecked;
  }

  @Nullable
  @Override
  public Element getState() {
    return serialize();
  }

  @NotNull
  private Element serialize() {
    Element mainElement = new Element(SETTINGS_NAME);
    XmlSerializer.serializeInto(this, mainElement);
    if (myUser != null) {
      Element userOption = new Element(OPTION);
      userOption.setAttribute(NAME, USER);
      Element userElement = myUser.serialize();
      userOption.addContent(userElement);
      mainElement.addContent(userOption);
    }
    return mainElement;
  }

  @Override
  public void loadState(@NotNull Element state) {
    try {
      deserialize(state);
    }
    catch (StudyUnrecognizedFormatException ignored) {
    }
  }

  private void deserialize(@NotNull Element state) throws StudyUnrecognizedFormatException {
    Element lastTimeChecked = getChildWithName(state, LAST_TIME_CHECKED, true);
    if (lastTimeChecked != null) {
      String value = lastTimeChecked.getAttributeValue(VALUE);
      myLastTimeChecked = Long.parseLong(value);
    }
    Element shouldUseJavaFx = getChildWithName(state, USE_JAVA_FX, true);
    if (shouldUseJavaFx != null) {
    String value = shouldUseJavaFx.getAttributeValue(VALUE);
      myShouldUseJavaFx = Boolean.parseBoolean(value);
    }

    Element user = getChildWithName(state, USER, true);
    if (user != null) {
      Element userXml = user.getChild(STEPIK_USER);
      if (userXml != null) {
        myUser = deserializeAccount(userXml, StepikUser.class, StepikUserInfo.class);
      }
    }
  }

  public static EduSettings getInstance() {
    return ServiceManager.getService(EduSettings.class);
  }

  @Nullable
  @Transient
  public StepikUser getUser() {
    return myUser;
  }

  @Transient
  public void setUser(@Nullable final StepikUser user) {
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
