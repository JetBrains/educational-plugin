package com.jetbrains.edu.kotlin;

import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.ProjectTemplate;
import com.jetbrains.edu.stepic.EduStepicConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class KotlinEduProjectTemplate implements ProjectTemplate {
//    private static EduStepicConnector connector;
    private static final Logger LOG = Logger.getInstance(KotlinEduProjectTemplate.class);
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
        return new KotlinEduModuleBuilder();
    }

    @Nullable
    @Override
    public ValidationInfo validateSettings() {
        return null;
    }
}