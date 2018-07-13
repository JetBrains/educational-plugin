package com.jetbrains.edu.learning.checkio.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.jetbrains.edu.learning.checkio.model.CheckiOUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "CheckiOSettings", storages = @Storage("other.xml"))
public class CheckiOSettings implements PersistentStateComponent<CheckiOSettings> {
  private CheckiOUser user;

  @Nullable
  @Override
  public CheckiOSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull CheckiOSettings settings) {
    XmlSerializerUtil.copyBean(settings, this);
  }

  public static CheckiOSettings getInstance() {
    return ServiceManager.getService(CheckiOSettings.class);
  }

  @Nullable
  public CheckiOUser getUser() {
    return user;
  }

  public void setUser(@Nullable final CheckiOUser user) {
    this.user = user;
  }
}
