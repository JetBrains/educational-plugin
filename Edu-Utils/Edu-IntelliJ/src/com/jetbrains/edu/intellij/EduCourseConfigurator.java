package com.jetbrains.edu.intellij;

import com.intellij.lang.LanguageExtension;
import com.intellij.openapi.project.Project;
import com.sun.istack.internal.NotNull;

public interface EduCourseConfigurator {
    LanguageExtension EP_NAME = new LanguageExtension("com.jetbrains.edu.intellij.courseConfigurator");
    void configureModule(@NotNull final Project project);
}
