package com.jetbrains.edu.kotlin;

import com.jetbrains.edu.learning.StudyLanguageManager;
import org.jetbrains.annotations.NotNull;


public class EduKotlinStudyLanguageManager implements StudyLanguageManager {
    @NotNull
    @Override
    public String getTestFileName() {
        return "Tests.kt";
    }

    @NotNull
    @Override
    public String getTestHelperFileName() {
        //TODO: allow nullable
        return "no test_helper";
    }

    @NotNull
    @Override
    public String getUserTester() {
        return "userTester.kt";
    }
}
