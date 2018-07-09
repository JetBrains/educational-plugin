package com.jetbrains.edu.learning.checkio.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.jetbrains.edu.learning.checkio.model.CheckioUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "CheckioSettings", storages = @Storage("other.xml"))
public class CheckioSettings implements PersistentStateComponent<CheckioSettings> {
  private CheckioUser user;

  @Nullable
  @Override
  public CheckioSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull CheckioSettings settings) {
    XmlSerializerUtil.copyBean(settings, this);
  }

  public static CheckioSettings getInstance() {
    return ServiceManager.getService(CheckioSettings.class);
  }

  @Nullable
  public CheckioUser getUser() {
    return user;
  }

  public void setUser(@Nullable final CheckioUser user) {
    this.user = user;
  }
}
