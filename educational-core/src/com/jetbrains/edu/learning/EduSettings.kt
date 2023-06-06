package com.jetbrains.edu.learning;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException;
import com.jetbrains.edu.learning.stepik.StepikUser;
import com.jetbrains.edu.learning.stepik.StepikUserInfo;
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.jetbrains.edu.learning.authUtils.OAuthAccountKt.deserializeOAuthAccount;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*;

@State(name = "EduSettings", storages = @Storage("other.xml"))
public class EduSettings implements PersistentStateComponent<Element> {
  @Transient
  @Nullable
  private volatile StepikUser myUser;
  @Property private JavaUILibrary javaUiLibrary = initialJavaUiLibrary();

  public EduSettings() {
    init();
  }

  @VisibleForTesting
  public void init() {
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
      if (userElement != null) {
        userOption.addContent(userElement);
        mainElement.addContent(userOption);
      }
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
    XmlSerializer.deserializeInto(this, state);

    Element user = getChildWithName(state, USER, true);
    if (user != null) {
      Element userXml = user.getChild(STEPIK_USER);
      if (userXml != null) {
        myUser = deserializeOAuthAccount(userXml, StepikUser.class, StepikUserInfo.class);
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
    if (user != null) {
      StepikConnector.getInstance().notifyUserLoggedIn();
    }
    else {
      StepikConnector.getInstance().notifyUserLoggedOut();
    }
  }

  private JavaUILibrary initialJavaUiLibrary() {
    if (javaUiLibrary != null && javaUiLibrary != JavaUILibrary.JAVAFX) {
      return javaUiLibrary;
    }
    if (JBCefApp.isSupported()) {
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
    if (javaUiLibrary == JavaUILibrary.JCEF && JBCefApp.isSupported()) {
      return JavaUILibrary.JCEF;
    }
    return JavaUILibrary.SWING;
  }

  public void setJavaUiLibrary(@NotNull JavaUILibrary javaUiLibrary) {
    this.javaUiLibrary = javaUiLibrary;
  }

  public static boolean isLoggedIn() {
    return getInstance().myUser != null;
  }
}
