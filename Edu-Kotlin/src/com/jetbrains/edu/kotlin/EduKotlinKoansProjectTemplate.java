package com.jetbrains.edu.kotlin;

import com.intellij.ide.util.projectWizard.AbstractModuleBuilder;
import com.intellij.openapi.ui.ValidationInfo;
import com.jetbrains.edu.intellij.EduIntelliJProjectTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinIcons;

import javax.swing.*;

public class EduKotlinKoansProjectTemplate implements EduIntelliJProjectTemplate {

    @NotNull
    @Override
    public String getName() {
        return "Kotlin Koans";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "Kotlin Koans course. \n You can also try it <a href=\"http://try.kotl.in/koans\">online</a>";
    }

    @Override
    public Icon getIcon() {
        return KotlinIcons.SMALL_LOGO;
    }

    @NotNull
    @Override
    public AbstractModuleBuilder createModuleBuilder() {
        return new EduKotlinKoansModuleBuilder();
    }

    @Nullable
    @Override
    public ValidationInfo validateSettings() {
        return null;
    }
}