package com.jetbrains.edu.python.learning.checkio;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Property;
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "PyCheckiOSettings", storages = @Storage("other.xml"))
public class PyCheckiOSettings implements PersistentStateComponent<PyCheckiOSettings> {
  @Property private CheckiOAccount myCheckiOAccount = new CheckiOAccount();

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

  @NotNull
  public CheckiOAccount getAccount() {
    return myCheckiOAccount;
  }

  public void setAccount(@NotNull CheckiOAccount account) {
    myCheckiOAccount = account;
  }
}
