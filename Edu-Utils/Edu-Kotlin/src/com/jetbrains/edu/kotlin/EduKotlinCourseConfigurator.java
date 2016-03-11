package com.jetbrains.edu.kotlin;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationsConfiguration;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.jetbrains.edu.intellij.EduCourseConfigurator;
import com.sun.istack.internal.NotNull;
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtilsKt;
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator;
import org.jetbrains.kotlin.idea.configuration.KotlinWithLibraryConfigurator;
import org.jetbrains.kotlin.idea.framework.ui.FileUIUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

public class EduKotlinCourseConfigurator implements EduCourseConfigurator {
    private static final Logger LOG = Logger.getInstance(EduKotlinCourseConfigurator.class);

    @Override
    public void configureModule(@NotNull Project project) {
        StartupManager.getInstance(project).runWhenProjectIsInitialized(new Runnable() {
            @Override
            public void run() {
                NotificationsConfiguration.getNotificationsConfiguration().changeSettings("Configure Kotlin: info notification",
                        NotificationDisplayType.NONE, true, false);
                KotlinProjectConfigurator configuratorByName = ConfigureKotlinInProjectUtilsKt.getConfiguratorByName("java");
                if (configuratorByName == null) {
                    LOG.info("Failed to find configurator");
                    return;
                }
                Class<?> confClass = configuratorByName.getClass();
                while (confClass != KotlinWithLibraryConfigurator.class) {
                    confClass = confClass.getSuperclass();
                }
                try {
                    String lib = FileUIUtils.createRelativePath(project, project.getBaseDir(), "lib");

                    Method method = confClass.getDeclaredMethod("configureModuleWithLibrary", Module.class, String.class, String.class);
                    method.setAccessible(true);
                    for (Module module : ModuleManager.getInstance(project).getModules()) {
                        method.invoke(configuratorByName, module, lib, null);
                    }
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    LOG.info(e);
                    configuratorByName.configure(project, Collections.emptyList());
                }
                finally {
                    NotificationsConfiguration.getNotificationsConfiguration().changeSettings("Configure Kotlin: info notification",
                            NotificationDisplayType.STICKY_BALLOON, true, false);
                }

                //TODO: uncomment this for new API
//                KotlinJavaModuleConfigurator.getInstance().configureSilently(project);
            }
        });
    }
}
