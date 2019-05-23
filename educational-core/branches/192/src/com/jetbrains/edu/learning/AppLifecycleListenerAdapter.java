package com.jetbrains.edu.learning;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.util.Ref;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface AppLifecycleListenerAdapter extends AppLifecycleListener {

  @Override
  default void appFrameCreated(@NotNull List<String> commandLineArgs, @NotNull Ref<? super Boolean> willOpenProject) {
    appFrameCreated();
  }

  default void appFrameCreated() {}
}
