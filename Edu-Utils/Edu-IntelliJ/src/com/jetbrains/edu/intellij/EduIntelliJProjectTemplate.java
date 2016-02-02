package com.jetbrains.edu.intellij;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.platform.ProjectTemplate;

public interface EduIntelliJProjectTemplate extends ProjectTemplate {
    ExtensionPointName<EduIntelliJProjectTemplate> EP_NAME = ExtensionPointName.create("com.jetbrains.edu.intellij.eduIntelliJProjectTemplate");
}
