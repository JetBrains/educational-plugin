package com.jetbrains.edu.kotlin;

import com.intellij.ide.util.projectWizard.AbstractModuleBuilder;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.ProjectTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinIcons;

import javax.swing.*;

public class EduKotlinProjectTemplate implements ProjectTemplate {

    @NotNull
    @Override
    public String getName() {
        return "Introduction to Kotlin";
    }

    @Nullable
    @Override
    public String getDescription() {
        //TODO: get correct course description
        return "Introduction to Kotlin Course";
    }

    @Override
    public Icon getIcon() {
        return KotlinIcons.SMALL_LOGO;
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