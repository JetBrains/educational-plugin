// UnitTesting/NoFramework.kt
package unittesting
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun fortyTwo() = 42

fun testFortyTwo(n: Int = 42) {
  assertEquals(
    expected = n,
    actual = fortyTwo(),
    message = "Incorrect,")
}

fun main(args: Array<String>){
  testFortyTwo()
  }
