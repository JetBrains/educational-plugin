package com.jetbrains.edu.learning.intellij;

import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.PathUtil;
import com.jetbrains.edu.learning.EduConfigurator;
import org.jetbrains.annotations.NotNull;

public abstract class EduConfiguratorBase implements EduConfigurator<JdkProjectSettings> {

  @Override
  public boolean excludeFromArchive(@NotNull String path) {
    final String name = PathUtil.getFileName(path);
    return "out".equals(name) || ".idea".equals(name) || "iml".equals(FileUtilRt.getExtension(name)) || "EduTestRunner.java".equals(name);
  }
}
