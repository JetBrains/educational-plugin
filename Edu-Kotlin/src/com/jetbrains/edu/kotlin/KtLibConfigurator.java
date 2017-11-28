package com.jetbrains.edu.kotlin;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationsConfiguration;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.configuration.*;
import org.jetbrains.kotlin.idea.framework.ui.FileUIUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

public class KtLibConfigurator {
  private static final Logger LOG = Logger.getInstance(KtLibConfigurator.class);

  public static void configureLib(@NotNull Project project) {
    if (tryConfigureSilently(project)) {
      return;
    }
    if (!tryOldAPI(project)) {
      LOG.warn("Failed to configure kotlin lib for edu project");
    }
  }

    //bad way to configure kotlin lib
    private static boolean tryOldAPI(Project project) {

        String settingName = "Configure Kotlin: info notification";
        NotificationsConfiguration.getNotificationsConfiguration().changeSettings(settingName,
                NotificationDisplayType.NONE, true, false);
        KotlinProjectConfigurator configuratorByName = ConfigureKotlinInProjectUtilsKt.getConfiguratorByName("java");
        if (configuratorByName == null) {
            LOG.info("Failed to find configurator");
            return false;
        }
        Class<?> confClass = configuratorByName.getClass();
        while (confClass != KotlinWithLibraryConfigurator.class) {
            confClass = confClass.getSuperclass();
        }
        String lib = FileUIUtils.createRelativePath(project, project.getBaseDir(), "lib");
        //collector arg was added in Kotlin plugin 1.0.1

        IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId("org.jetbrains.kotlin"));
        if (plugin == null) {
            return false;
        }

        if (VersionComparatorUtil.compare(plugin.getVersion(), "1.0.1") > 0) {
            if (configureWithCollector(project, confClass, configuratorByName, lib)) {
              return true;
            }
        } else {
            if (!configureWithoutCollector(project, confClass, configuratorByName, lib)) {
                configuratorByName.configure(project, Collections.emptyList());
            }
        }
        NotificationsConfiguration.getNotificationsConfiguration().changeSettings(settingName,
                NotificationDisplayType.STICKY_BALLOON, true, false);
        return true;
    }

    private static boolean configureWithCollector(Project project, Class<?> confClass, KotlinProjectConfigurator configurator, String lib) {
        try {
            Method method = confClass.getDeclaredMethod("configureModuleWithLibrary", Module.class, String.class, String.class, NotificationMessageCollector.class);
            method.setAccessible(true);
            NotificationMessageCollector collector = NotificationMessageCollectorKt.createConfigureKotlinNotificationCollector(project);
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                method.invoke(configurator, module, lib, null, collector);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean configureWithoutCollector(Project project, Class<?> confClass, KotlinProjectConfigurator configurator, String lib) {
        try {
            Method method = confClass.getDeclaredMethod("configureModuleWithLibrary", Module.class, String.class, String.class);
            method.setAccessible(true);
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                method.invoke(configurator, module, lib, null);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // should work since Kotlin 1.0.2
    private static boolean tryConfigureSilently(Project project) {
        KotlinJavaModuleConfigurator configurator;
        try {
            configurator = KotlinJavaModuleConfigurator.Companion.getInstance();
        } catch (Exception e) {
            LOG.info("Failed to get `KotlinJavaModuleConfigurator` instance through Companion object", e);
            // Try to use old API
            configurator = getModuleConfiguratorInstance();
        }
        if (configurator != null) {
            Class<?> confClass = configurator.getClass();
            while (confClass != KotlinWithLibraryConfigurator.class) {
                confClass = confClass.getSuperclass();
            }
            try {
                Method configureSilently = confClass.getDeclaredMethod("configureSilently", Project.class);
                configureSilently.invoke(configurator, project);
                return true;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                LOG.info("Failed to invoke `KotlinJavaModuleConfigurator.configureSilently` method", e);
            }
        }
        return false;
    }

    /**
     * Get instance of {@link KotlinJavaModuleConfigurator} for kotlin plugin 1.0.2 - 1.1.2.
     * Uses reflection.
     *
     * @return {@link KotlinJavaModuleConfigurator} instance
     */
    @Nullable
    private static KotlinJavaModuleConfigurator getModuleConfiguratorInstance() {
        Class<KotlinJavaModuleConfigurator> configuratorClass = KotlinJavaModuleConfigurator.class;
        try {
            Method instanceMethod = configuratorClass.getDeclaredMethod("getInstance");
            return (KotlinJavaModuleConfigurator) instanceMethod.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOG.info("Failed to get `KotlinJavaModuleConfigurator` instance through `getInstance` method", e);
            return null;
        }
    }
}
