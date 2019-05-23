package com.jetbrains.edu.learning;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.util.Ref;
import org.jetbrains.annotations.NotNull;

public interface AppLifecycleListenerAdapter extends AppLifecycleListener {

  @Override
  default void appFrameCreated(String[] commandLineArgs, @NotNull Ref<Boolean> willOpenProject) {
    appFrameCreated();
  }

  default void appFrameCreated() {}
}
