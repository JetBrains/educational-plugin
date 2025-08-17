package com.jetbrains.edu.learning.actions.changeHost

import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager.SelectedServiceHost
import com.jetbrains.edu.learning.testAction
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("Junit4RunWithInspection")
@RunWith(Parameterized::class)
abstract class ChangeServiceHostActionTestBase<E>(
  private val initialValue: SelectedServiceHost<E>?,
  private val dialogValue: SelectedServiceHost<E>?,
  private val expectedValue: SelectedServiceHost<E>
) : EduActionTestCase() where E: Enum<E>, E: ServiceHostEnum {

  protected abstract val manager: ServiceHostManager<E>
  protected abstract val actionId: String

  @Test
  fun `test change service host action`() {
    // given
    if (initialValue != null) {
      manager.selectedHost = initialValue
    }
    mockkConstructor(ChangeServiceHostDialog::class)
    every { anyConstructed<ChangeServiceHostDialog<*>>().showAndGetSelectedHost() } returns dialogValue
    // when
    testAction(actionId)
    // then
    assertEquals(expectedValue, manager.selectedHost)
  }

  override fun tearDown() {
    try {
      unmockkConstructor(ChangeServiceHostDialog::class)
      manager.reset()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  companion object {
    @JvmStatic
    protected fun <E> ServiceHostManager<E>.data(): Collection<Array<Any?>>
      where E : Enum<E>,
            E : ServiceHostEnum {
      // Usually, it's `STAGING` option
      val thirdOption = hostEnumClass.enumConstants.find { it != default && it != other }

      val additionalCases: List<Array<Any?>> = if (thirdOption != null) {
        listOf(
          arrayOf(SelectedServiceHost(thirdOption), null, SelectedServiceHost(thirdOption)),
          arrayOf(null, SelectedServiceHost(thirdOption), SelectedServiceHost(thirdOption)),
        )
      }
      else {
        emptyList()
      }

      return listOf<Array<Any?>>(
        arrayOf(null, null, SelectedServiceHost(default)),
        arrayOf(null, SelectedServiceHost(default), SelectedServiceHost(default)),
        arrayOf(null, SelectedServiceHost(other), SelectedServiceHost(other)),
        arrayOf(null, SelectedServiceHost(other, "http://foo.bar"), SelectedServiceHost(other, "http://foo.bar")),
        arrayOf(
          SelectedServiceHost(other, "http://foo.bar"),
          SelectedServiceHost(other, "http://foo2.bar"),
          SelectedServiceHost(other, "http://foo2.bar")
        ),
      ) + additionalCases
    }

  }
}
