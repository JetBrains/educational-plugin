package com.jetbrains.edu.learning;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException;
import com.jetbrains.edu.learning.stepik.StepikUser;
import com.jetbrains.edu.learning.stepik.StepikUserInfo;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.intellij.ide.plugins.PluginManagerCore.isUnitTestMode;
import static com.jetbrains.edu.learning.authUtils.OAuthAccountKt.deserializeAccount;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*;

@State(name = "EduSettings", storages = @Storage("other.xml"))
public class EduSettings implements PersistentStateComponent<Element> {
  private static final String RESET_JAVA_UI_LIBRARY = "EduTools.ResetJavaUiLibrary";
  public static final Topic<EduLogInListener> SETTINGS_CHANGED = Topic.create("Edu.UserSet", EduLogInListener.class);
  @Transient
  @Nullable
  private StepikUser myUser;
  private long myLastTimeChecked;
  @Property private JavaUILibrary javaUiLibrary = initialJavaUiLibrary();

  private Set<Integer> myShownCourseIds;

  public EduSettings() {
    init();
  }

  @VisibleForTesting
  public void init() {
    myLastTimeChecked = System.currentTimeMillis();
    myShownCourseIds = Collections.emptySet();
  }

  public long getLastTimeChecked() {
    return myLastTimeChecked;
  }

  public void setLastTimeChecked(long timeChecked) {
    myLastTimeChecked = timeChecked;
  }

  public Set<Integer> getShownCourseIds() {
    return new HashSet<>(myShownCourseIds);
  }

  public void setShownCourseIds(@NotNull Set<Integer> shownCourseIds) {
    myShownCourseIds = new HashSet<>(shownCourseIds);
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

    resetJavaUiLibrary();
  }

  private void deserialize(@NotNull Element state) throws StudyUnrecognizedFormatException {
    XmlSerializer.deserializeInto(this, state);

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
    ApplicationManager.getApplication().getMessageBus().syncPublisher(SETTINGS_CHANGED).userLoggedIn();
  }

  private JavaUILibrary initialJavaUiLibrary() {
    if (javaUiLibrary != null && javaUiLibrary != JavaUILibrary.JAVAFX) {
      return javaUiLibrary;
    }
    if (EduUtils.hasJCEF()) {
      return JavaUILibrary.JCEF;
    }
    return JavaUILibrary.SWING;
  }

  @NotNull
  public JavaUILibrary getJavaUiLibrary() {
    return javaUiLibrary;
  }

  @NotNull
  public JavaUILibrary getJavaUiLibraryWithCheck() {
    if (javaUiLibrary == JavaUILibrary.JCEF && EduUtils.hasJCEF()) {
      return JavaUILibrary.JCEF;
    }
    return JavaUILibrary.SWING;
  }

  public void setJavaUiLibrary(@NotNull JavaUILibrary javaUiLibrary) {
    this.javaUiLibrary = javaUiLibrary;
  }

  /**
   * EDU-4061: get rid of this after 2020.1.4 release
   */
  private void resetJavaUiLibrary() {
    if (isUnitTestMode) return;
    PropertiesComponent propertyComponent = PropertiesComponent.getInstance();
    if (!propertyComponent.getBoolean(RESET_JAVA_UI_LIBRARY)) {
      javaUiLibrary = null;
      javaUiLibrary = initialJavaUiLibrary();
      propertyComponent.setValue(RESET_JAVA_UI_LIBRARY, true);
    }
  }

  public static boolean isLoggedIn() {
    return getInstance().myUser != null;
  }
}
