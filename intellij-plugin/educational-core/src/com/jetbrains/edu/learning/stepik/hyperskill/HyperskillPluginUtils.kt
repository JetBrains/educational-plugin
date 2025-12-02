package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.plugins.*
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
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.checkCanceled
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.updateSettings.impl.PluginDownloader
import com.intellij.openapi.wm.WelcomeScreenLeftPanel
import com.intellij.openapi.wm.impl.welcomeScreen.FlatWelcomeFrame
import com.intellij.openapi.wm.impl.welcomeScreen.TabbedWelcomeScreen
import com.intellij.openapi.wm.impl.welcomeScreen.TabbedWelcomeScreen.DefaultWelcomeScreenTab
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.ui.components.JBLabel
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.containers.TreeTraversal
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.util.ui.tree.TreeUtil.invalidateCacheAndRepaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode
import kotlin.coroutines.CoroutineContext

private const val HYPERSKILL_PLUGIN_ID: String = "org.hyperskill.academy"

private val hyperskillPluginId: PluginId
  get() = PluginId.getId(HYPERSKILL_PLUGIN_ID)

/**
 * Copied from [com.intellij.openapi.wm.impl.welcomeScreen.learnIde.jbAcademy.InstallJBAcademyTask.install]
 */
@Suppress("removal", "DEPRECATION", "UsagesOfObsoleteApi")
suspend fun installAndEnableHyperskillPlugin(
  modalityContext: CoroutineContext = ModalityState.defaultModalityState().asContextElement(),
  shouldLoadPlugin: Boolean = false,
) {
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
        operation.setAllowInstallWithoutRestart(shouldLoadPlugin)
        operation.run()
        operation
      }
    }

    if (!operation.isSuccess) return

    reporter.nextStep(endFraction = 100) {
      if (!shouldLoadPlugin) return@nextStep
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

@RequiresEdt
fun restartIde(withConfirmationDialog: Boolean = true) {
  if (withConfirmationDialog) {
    PluginManagerConfigurable.shutdownOrRestartApp()
  }
  else {
    ApplicationManagerEx.getApplicationEx().restart(true)
  }
}

var Project.isHyperskillProject: Boolean
  get() = PropertiesComponent.getInstance(this).getBoolean(IS_HYPERSKILL_COURSE_PROPERTY)
  set(value) {
    PropertiesComponent.getInstance(this).setValue(IS_HYPERSKILL_COURSE_PROPERTY, value)
  }

private const val IS_HYPERSKILL_COURSE_PROPERTY: String = "edu.course.is.hyperskill"

@RequiresEdt
fun openHyperskillBrowseCourses() {
  val welcomeFrame = WelcomeFrame.getInstance() as? FlatWelcomeFrame
  val tabbedWelcomeScreen = welcomeFrame?.screen as? TabbedWelcomeScreen

  if (tabbedWelcomeScreen == null) {
    openHyperskillBrowseCoursesAction()
    return
  }

  refreshWelcomeScreen(tabbedWelcomeScreen)
  navigateToHyperskillAcademyTab()
}

private fun refreshWelcomeScreen(tabbedWelcomeScreen: TabbedWelcomeScreen) {
  tabbedWelcomeScreen.loadTabs()

  (tabbedWelcomeScreen.leftPanel?.component as? JTree)?.apply {
    updateUI()
    revalidate()
    invalidateCacheAndRepaint(ui)
  }
}

private fun navigateToHyperskillAcademyTab() {
  val welcomeFrame = WelcomeFrame.getInstance() as? FlatWelcomeFrame
  val tabbedWelcomeScreen = welcomeFrame?.screen as? TabbedWelcomeScreen
  val hyperskillTab = tabbedWelcomeScreen?.findTabByTitle("Hyperskill Academy")
  if (hyperskillTab != null) {
    tabbedWelcomeScreen.selectTab(hyperskillTab)
  }
  else {
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

@VisibleForTesting
fun TabbedWelcomeScreen.findTabByTitle(title: String): DefaultWelcomeScreenTab? {
  val root = leftPanel?.root ?: return null

  val targetNode = TreeUtil.treeNodeTraverser(root).traverse(TreeTraversal.POST_ORDER_DFS).find { node: TreeNode? ->
    if (node is DefaultMutableTreeNode) {
      val currentTab = node.userObject
      if (currentTab is DefaultWelcomeScreenTab && currentTab.title == title) {
        return@find true
      }
    }
    false
  }

  return (targetNode as? DefaultMutableTreeNode)?.userObject as? DefaultWelcomeScreenTab
}

@get:VisibleForTesting
val TabbedWelcomeScreen.leftPanel: WelcomeScreenLeftPanel?
  get() = getPrivateField("myLeftSidebar")

@get:VisibleForTesting
val WelcomeScreenLeftPanel.root: DefaultMutableTreeNode?
  get() = getPrivateField("root")

@get:VisibleForTesting
val DefaultWelcomeScreenTab.title: String?
  get() = getPrivateField<JBLabel>("myLabel", javaClass.superclass)?.text

private inline fun <reified T> Any.getPrivateField(fieldName: String, clazz: Class<*> = javaClass): T? = runCatching {
  clazz.getDeclaredField(fieldName).apply { isAccessible = true }.get(this) as? T
}.getOrNull()

private val LOG: Logger = Logger.getInstance("HyperskillPluginUtils")