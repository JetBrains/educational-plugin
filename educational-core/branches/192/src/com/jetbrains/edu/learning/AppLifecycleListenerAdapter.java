package com.jetbrains.edu.learning;

import com.intellij.ide.AppLifecycleListener;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface AppLifecycleListenerAdapter extends AppLifecycleListener {

  @Override
  default void appFrameCreated(@NotNull List<String> commandLineArgs) {
    appFrameCreated();
  }

  default void appFrameCreated() {}
}
