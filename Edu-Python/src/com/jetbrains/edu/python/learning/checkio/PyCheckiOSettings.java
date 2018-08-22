package com.jetbrains.edu.python.learning.checkio;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "PyCheckiOSettings", storages = @Storage("other.xml"))
public class PyCheckiOSettings implements PersistentStateComponent<PyCheckiOSettings> {
  @Nullable
  @Tag("PyAccount")
  private CheckiOAccount myCheckiOAccount;

  @Nullable
  @Override
  public PyCheckiOSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull PyCheckiOSettings settings) {
    XmlSerializerUtil.copyBean(settings, this);
  }

  @NotNull
  public static PyCheckiOSettings getInstance() {
    return ServiceManager.getService(PyCheckiOSettings.class);
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
