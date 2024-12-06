package com.jetbrains.edu.learning

import com.intellij.ide.plugins.ContainerDescriptor
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.runInEdtAndWait
import io.github.classgraph.AnnotationEnumValue
import io.github.classgraph.ClassGraph
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

/**
 * Helps manage state of services marked with [EduTestAware] between tests cases
 */
object EduTestServiceStateHelper {

  fun restoreState(project: Project?) {
    performForAllServices(project, EduTestAware::restoreState)
  }

  fun cleanUpState(project: Project?) {
    // Some services may be used in scheduled tasks (via `invokeLater`, for example).
    // And since we can't rely on service/project disposing in light tests, let's wait for all these tasks manually
    runInEdtAndWait {
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }

    performForAllServices(project, EduTestAware::cleanUpState)
  }

  private fun performForAllServices(project: Project?, action: EduTestAware.() -> Unit) {
    val serviceClasses = ServiceClasses.collect()

    val application = ApplicationManager.getApplication()
    for (serviceClass in serviceClasses.applicationServices) {
      // Do not create service if it wasn't created earlier
      (application.getServiceIfCreated(serviceClass) as? EduTestAware)?.action()
    }
    // There is no necessity to do anything for non-light projects
    // since in heavy tests a new project is created for each test case
    if (project != null && project.isLight) {
      for (serviceClass in serviceClasses.projectServices) {
        // Do not create service if it wasn't created earlier
        (project.getServiceIfCreated(serviceClass) as? EduTestAware)?.action()
      }
    }
  }
}

private class ServiceClasses private constructor(
  val applicationServices: List<Class<*>>,
  val projectServices: List<Class<*>>
) {

  companion object {

    private const val BASE_PACKAGE = "com.jetbrains.edu"

    private val cache: MutableMap<IdeaPluginDescriptorImpl, ServiceClasses> = ConcurrentHashMap()

    fun collect(): ServiceClasses {
      // Here we take into account that all tests in single Gradle module have the same plugin descriptor.
      // As a result, we can reuse calculated values from the previous test with the same descriptor
      val pluginDescriptor = PluginManagerCore.plugins.find {
        it.isEnabled && it.pluginId.idString.startsWith(BASE_PACKAGE)
      } as? IdeaPluginDescriptorImpl ?: error("Failed to find any plugin descriptor related to the plugin")

      return cache.getOrPut(pluginDescriptor) {
        val collectedClasses: ServiceClasses
        val duration = measureTimeMillis {
          collectedClasses = collect(pluginDescriptor)
        }
        println("Service classes collecting took $duration ms")
        collectedClasses
      }
    }

    private fun collect(pluginDescriptor: IdeaPluginDescriptorImpl): ServiceClasses {
      val appServiceClassesHolder = ServiceClassesHolder.create(pluginDescriptor) { appContainerDescriptor }
      val projectServiceClassesHolder = ServiceClassesHolder.create(pluginDescriptor) { projectContainerDescriptor }
      val scanResult = ClassGraph()
        .enableClassInfo()
        .enableAnnotationInfo()
        .acceptPackages(BASE_PACKAGE)
        .scan()

      scanResult.use {
        for (classInfo in it.getClassesImplementing(EduTestAware::class.java)) {
          val serviceAnnotationInfo = classInfo.getAnnotationInfo(Service::class.java)
          if (serviceAnnotationInfo != null) {
            // According to `Service` annotation definition,
            // it has only one `value` parameter with `Array` type
            // which is not empty and contains objects of `Service.Level` type
            val valueParameter = serviceAnnotationInfo.parameterValues["value"].value as Array<*>
            for (value in valueParameter) {
              val scope = Service.Level.valueOf((value as AnnotationEnumValue).valueName)
              when (scope) {
                Service.Level.APP -> appServiceClassesHolder.addLightService(classInfo.name)
                Service.Level.PROJECT -> projectServiceClassesHolder.addLightService(classInfo.name)
              }
            }
          }
          else {
            appServiceClassesHolder.addNonLightService(classInfo.name)
            projectServiceClassesHolder.addNonLightService(classInfo.name)
          }
        }
      }

      // `StudyTaskManager` is intentionally moved to the end of the list
      // since it holds the essential knowledge about project course
      // that may be needed to properly clean up other services
      val projectServices =
        (projectServiceClassesHolder.serviceClasses - StudyTaskManager::class.java).toList() + StudyTaskManager::class.java
      val applicationServices = appServiceClassesHolder.serviceClasses.toList()
      return ServiceClasses(applicationServices, projectServices)
    }
  }
}

private class ServiceClassesHolder private constructor(private val nonLightServices: Map<String, String>) {
  private val _serviceClasses: MutableSet<Class<*>> = hashSetOf()

  val serviceClasses: Set<Class<*>> get() = _serviceClasses

  fun addLightService(className: String) {
    // It's possible to have `@Service` annotation above non-light service class,
    // so let's process this case properly as well
    val baseNonLightServiceClass = nonLightServices[className]
    if (baseNonLightServiceClass != null) return

    _serviceClasses.add(Class.forName(className))
  }

  fun addNonLightService(className: String) {
    val baseClassName = nonLightServices[className] ?: return
    _serviceClasses.add(Class.forName(baseClassName))
  }

  companion object {
    fun create(
      pluginDescriptor: IdeaPluginDescriptorImpl,
      containerDescriptor: IdeaPluginDescriptorImpl.() -> ContainerDescriptor
    ): ServiceClassesHolder {
      val services = mutableMapOf<String, String>()
      collectServicesFromPluginManifest(pluginDescriptor, containerDescriptor, services)
      return ServiceClassesHolder(services)
    }

    private fun collectServicesFromPluginManifest(
      pluginDescriptor: IdeaPluginDescriptorImpl,
      containerDescriptor: IdeaPluginDescriptorImpl.() -> ContainerDescriptor,
      serviceMap: MutableMap<String, String>
    ) {
      for (serviceDescriptor in pluginDescriptor.containerDescriptor().services) {
        val baseServiceClass = serviceDescriptor.serviceInterface ?: serviceDescriptor.serviceImplementation ?: continue

        val allServiceClasses = listOfNotNull(
          serviceDescriptor.serviceInterface,
          serviceDescriptor.serviceImplementation,
          serviceDescriptor.testServiceImplementation,
          serviceDescriptor.headlessImplementation
        )
        for (serviceClass in allServiceClasses) {
          serviceMap[serviceClass] = baseServiceClass
        }
      }

      for (module in pluginDescriptor.content.modules) {
        collectServicesFromPluginManifest(module.requireDescriptor(), containerDescriptor, serviceMap)
      }
    }
  }
}
