package com.jetbrains.edu.python.learning.checkio;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Property;
import com.jetbrains.edu.learning.checkio.model.CheckiOAccount;
import com.jetbrains.edu.learning.checkio.model.CheckiOAccountHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "CheckiOSettings", storages = @Storage("other.xml"))
public class PyCheckiOAccountHolder implements PersistentStateComponent<PyCheckiOAccountHolder>, CheckiOAccountHolder {
  @Property private CheckiOAccount myCheckiOAccount = new CheckiOAccount();

  @Nullable
  @Override
  public PyCheckiOAccountHolder getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull PyCheckiOAccountHolder settings) {
    XmlSerializerUtil.copyBean(settings, this);
  }

  @NotNull
  public static PyCheckiOAccountHolder getInstance() {
    return ServiceManager.getService(PyCheckiOAccountHolder.class);
  }

  @NotNull
  @Override
  public CheckiOAccount getAccount() {
    return myCheckiOAccount;
  }

  @Override
  public void setAccount(@NotNull CheckiOAccount account) {
    myCheckiOAccount = account;
  }
}
