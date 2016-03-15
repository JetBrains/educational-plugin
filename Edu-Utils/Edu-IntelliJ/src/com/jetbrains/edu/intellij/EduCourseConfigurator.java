package com.jetbrains.edu.intellij;

import com.intellij.lang.LanguageExtension;
import com.intellij.openapi.project.Project;
import com.sun.istack.internal.NotNull;

public interface EduCourseConfigurator {
    String EP_NAME = "com.jetbrains.edu.intellij.courseConfigurator";
    public static final LanguageExtension<EduCourseConfigurator> INSTANCE = new LanguageExtension<EduCourseConfigurator>(EP_NAME);

    public default void configureModule(@NotNull final Project project) {}
}
