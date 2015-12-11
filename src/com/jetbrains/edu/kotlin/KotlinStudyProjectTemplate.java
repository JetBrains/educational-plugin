package com.jetbrains.edu.kotlin;

import com.intellij.ide.util.projectWizard.AbstractModuleBuilder;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.ProjectTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class KotlinStudyProjectTemplate implements ProjectTemplate {

    @NotNull
    @Override
    public String getName() {
        return "Education Kotlin Project";
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @NotNull
    @Override
    public AbstractModuleBuilder createModuleBuilder() {
        return new KotlinStudyModuleBuilder();
    }

    @Nullable
    @Override
    public ValidationInfo validateSettings() {
        return null;
    }
}