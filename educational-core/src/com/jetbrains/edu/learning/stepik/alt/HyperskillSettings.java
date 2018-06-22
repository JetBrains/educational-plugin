package com.jetbrains.edu.learning.stepik.alt;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "HyperskillSettings", storages = @Storage("other.xml"))
public class HyperskillSettings implements PersistentStateComponent<HyperskillSettings> {
  @Nullable
  private HyperskillAccount myAccount;

  @Nullable
  @Override
  public HyperskillSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull HyperskillSettings settings) {
    XmlSerializerUtil.copyBean(settings, this);
  }

  @NotNull
  public static HyperskillSettings getInstance() {
    return ServiceManager.getService(HyperskillSettings.class);
  }

  @Nullable
  public HyperskillAccount getAccount() {
    return myAccount;
  }

  public void setAccount(@Nullable HyperskillAccount account) {
    myAccount = account;
  }
}
