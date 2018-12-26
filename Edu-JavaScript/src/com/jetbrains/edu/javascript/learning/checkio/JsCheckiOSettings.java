package com.jetbrains.edu.javascript.learning.checkio;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = JsCheckiOSettings.SERVICE_NAME, storages = @Storage("other.xml"))
public class JsCheckiOSettings implements PersistentStateComponent<Element> {
  protected static final String SERVICE_NAME = "JsCheckiOSettings";
  @Nullable
  @Tag("JsAccount")
  private CheckiOAccount myCheckiOAccount;

  @Nullable
  @Override
  public Element getState() {
    return myCheckiOAccount != null ? myCheckiOAccount.serializeIntoService(SERVICE_NAME): null;
  }

  @Override
  public void loadState(@NotNull Element settings) {
    myCheckiOAccount = CheckiOAccount.fromElement(settings);
  }

  @NotNull
  public static JsCheckiOSettings getInstance() {
    return ServiceManager.getService(JsCheckiOSettings.class);
  }

  @Nullable
  @Transient
  public CheckiOAccount getAccount() {
    return myCheckiOAccount;
  }

  @Transient
  public void setAccount(@Nullable CheckiOAccount account) {
    myCheckiOAccount = account;
  }
}
