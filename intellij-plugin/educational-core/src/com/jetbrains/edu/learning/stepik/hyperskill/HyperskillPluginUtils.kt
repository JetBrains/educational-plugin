package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginEnabler
import com.intellij.ide.plugins.PluginInstallOperation
import com.intellij.ide.plugins.PluginInstaller
import com.intellij.ide.plugins.PluginManagementPolicy
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.plugins.PluginNode
import com.intellij.ide.plugins.RepositoryHelper
import com.intellij.ide.plugins.marketplace.MarketplaceRequests
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.checkCanceled
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.updateSettings.impl.PluginDownloader
import com.intellij.platform.ide.CoreUiCoroutineScopeHolder
import com.intellij.platform.util.progress.reportSequentialProgress
import com.jetbrains.edu.learning.newproject.ui.BrowseCoursesDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

private const val HYPERSKILL_PLUGIN_ID: String = "org.hyperskill.academy"

private val hyperskillPluginId: PluginId
  get() = PluginId.getId(HYPERSKILL_PLUGIN_ID)

/**
 * Copied from [com.intellij.openapi.wm.impl.welcomeScreen.learnIde.jbAcademy.InstallJBAcademyTask.install]
 */
@Suppress("removal", "DEPRECATION", "UsagesOfObsoleteApi")
suspend fun installAndEnableHyperskillPlugin(modalityContext: CoroutineContext = ModalityState.defaultModalityState().asContextElement()) {
  reportSequentialProgress { reporter ->
    val descriptors = reporter.nextStep(endFraction = 20) {
      val marketplacePlugins = MarketplaceRequests.loadLastCompatiblePluginDescriptors(setOf(hyperskillPluginId))
      val customPlugins = coroutineToIndicator {
        val indicator = ProgressManager.getGlobalProgressIndicator()
        RepositoryHelper.loadPluginsFromCustomRepositories(indicator)
      }
      val descriptors: MutableList<IdeaPluginDescriptor> =
        RepositoryHelper.mergePluginsFromRepositories(marketplacePlugins, customPlugins, true).toMutableList()
      PluginManagerCore.plugins.filterTo(descriptors) {
        !it.isEnabled && PluginManagerCore.isCompatible(it) && PluginManagementPolicy.getInstance().canInstallPlugin(it)
      }
      checkCanceled()
      descriptors
    }

    val plugins: List<PluginNode> = reporter.nextStep(endFraction = 40) {
      val downloader = PluginDownloader.createDownloader(descriptors.first())
      val nodes = mutableListOf<PluginNode>()
      val plugin = downloader.descriptor
      if (plugin.isEnabled) {
        nodes.add(downloader.toPluginNode())
      }
      PluginEnabler.HEADLESS.enable(listOf(plugin))
      checkCanceled()
      nodes
    }

    if (plugins.isEmpty()) return

    val operation = reporter.nextStep(endFraction = 80) {
      coroutineToIndicator {
        val indicator = ProgressManager.getGlobalProgressIndicator()
        val operation = PluginInstallOperation(plugins, emptyList(), PluginEnabler.HEADLESS, indicator)
        indicator.checkCanceled()
        operation.setAllowInstallWithoutRestart(true)
        operation.run()
        operation
      }
    }

    if (!operation.isSuccess) return

    reporter.nextStep(endFraction = 100) {
      withContext(Dispatchers.EDT + modalityContext) {
        for ((file, pluginDescriptor) in operation.pendingDynamicPluginInstalls) {
          checkCanceled()
          PluginInstaller.installAndLoadDynamicPlugin(file, pluginDescriptor)
        }
      }
    }
  }
}

fun needInstallHyperskillPlugin(): Boolean {
  return !PluginManagerCore.isPluginInstalled(hyperskillPluginId) || PluginManagerCore.isDisabled(hyperskillPluginId)
}

var Project.isHyperskillProject: Boolean
  get() = PropertiesComponent.getInstance(this).getBoolean(IS_HYPERSKILL_COURSE_PROPERTY)
  set(value) {
    PropertiesComponent.getInstance(this).setValue(IS_HYPERSKILL_COURSE_PROPERTY, value)
  }

private const val IS_HYPERSKILL_COURSE_PROPERTY: String = "edu.course.is.hyperskill"

fun closeDialogAndOpenHyperskillBrowseCourses(modalityContext: CoroutineContext) {
  service<CoreUiCoroutineScopeHolder>().coroutineScope.launch(Dispatchers.EDT + modalityContext) {
    BrowseCoursesDialog.getInstance()?.close()

    openHyperskillBrowseCoursesAction()
  }
}

private const val HYPERSKILL_BROWSE_COURSES_ACTION_ID: String = "HyperskillEducational.BrowseCourses"

private fun openHyperskillBrowseCoursesAction() {
  val event = AnActionEvent.createEvent(DataContext.EMPTY_CONTEXT, null, ActionPlaces.WELCOME_SCREEN, ActionUiKind.NONE, null)
  val action = ActionManager.getInstance().getAction(HYPERSKILL_BROWSE_COURSES_ACTION_ID)
  if (action == null) {
    LOG.error("Cannot find browse courses action by id: $HYPERSKILL_BROWSE_COURSES_ACTION_ID")
    return
  }
  ActionUtil.performAction(action, event)
}

private val LOG: Logger = Logger.getInstance("HyperskillPluginUtils")