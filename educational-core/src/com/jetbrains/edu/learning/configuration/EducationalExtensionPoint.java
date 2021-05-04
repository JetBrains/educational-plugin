package com.jetbrains.edu.learning.configuration;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.serviceContainer.BaseKeyedLazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import com.jetbrains.edu.learning.EduNames;
import org.jetbrains.annotations.Nullable;

public class EducationalExtensionPoint<T> extends BaseKeyedLazyInstance<T> {
  public static final ExtensionPointName<EducationalExtensionPoint<EduConfigurator<?>>> EP_NAME =
    ExtensionPointName.create("Educational.configurator");

  @Attribute("implementationClass")
  public String implementationClass;

  @Attribute("language")
  public String language = "";

  @Attribute("courseType")
  public String courseType = EduNames.PYCHARM;

  @Attribute("environment")
  public String environment = "";

  @Attribute("displayName")
  public String displayName = null;

  @Override
  protected @Nullable String getImplementationClassName() {
    return implementationClass;
  }
}
