package com.jetbrains.edu.course.creator;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.coursecreator.CCLanguageManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class EduJavaCCLanguageManager implements CCLanguageManager {
    @Nullable
    @Override
    public String getDefaultTaskFileExtension() {
        return ".java";
    }

    @Nullable
    @Override
    public FileTemplate getTaskFileTemplateForExtension(@NotNull Project project, String extension) {
        return null;
    }

    @Nullable
    @Override
    public FileTemplate getTestsTemplate(@NotNull Project project) {
        return null;
    }

    @Override
    public boolean doNotPackFile(File pathname) {
        String name = pathname.getName();
        return "out".equals(name) || ".idea".equals(name);
    }
}
