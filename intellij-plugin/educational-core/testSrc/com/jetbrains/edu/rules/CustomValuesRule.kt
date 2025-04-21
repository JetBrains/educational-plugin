package com.jetbrains.edu.rules

import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.RegistryValue
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.setFeatureEnabled
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Allows setting a non-default value of registry value or experimental feature during test execution with annotations.
 *
 * Looks for annotations on the test class as well as the corresponding method.
 * In the case of multiple annotations with the same key, annotation above methods will have priority.
 *
 * @see [WithRegistryValue]
 * @see [WithExperimentalFeature]
 */
class CustomValuesRule : TestRule {
  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        val annotationDescriptors = (
          description.collectAnnotations(WithRegistryValue::key) +
          description.collectAnnotations(WithExperimentalFeature::id)
        ).map { ValueDescriptor.from(it) }

        try {
          for (descriptor in annotationDescriptors) {
            descriptor.apply()
          }
          base.evaluate()
        }
        finally {
          for (descriptor in annotationDescriptors) {
            descriptor.reset()
          }
        }
      }
    }
  }

  private inline fun <reified T : Annotation> Description.collectAnnotations(key: (T) -> String): List<T> {
    val registryAnnotations = HashMap<String, T>() // used to preserve order if it's
    testClass.getAnnotationsByType(T::class.java).associateByTo(registryAnnotations, key)
    annotations.filterIsInstance<T>().associateByTo(registryAnnotations, key)

    return registryAnnotations.values.toList()
  }

  private sealed class ValueDescriptor<T>(
    val initialValue: T,
    val newValue: T
  ) {

    fun apply() = apply(newValue)
    fun reset() = apply(initialValue)

    protected open fun apply(value: T) {}

    companion object {
      fun from(annotation: Annotation): ValueDescriptor<*> {
        return when (annotation) {
          is WithRegistryValue -> RegistryValueDescriptor(annotation.key, annotation.value)
          is WithExperimentalFeature -> ExperimentalFeatureValueDescriptor(annotation.id, annotation.value)
          else -> error("Unknown annotation ${annotation.javaClass}")
        }
      }
    }
  }

  private class RegistryValueDescriptor(key: String, newValue: String) : ValueDescriptor<String>(Registry.get(key).asString(), newValue) {

    private val registryValue: RegistryValue = Registry.get(key)

    override fun apply(value: String) {
      registryValue.setValue(value)
    }
  }

  private class ExperimentalFeatureValueDescriptor(
    private val id: String,
    newValue: Boolean
  ) : ValueDescriptor<Boolean>(isFeatureEnabled(id), newValue) {

    override fun apply(value: Boolean) {
      setFeatureEnabled(id, value)
    }
  }
}
