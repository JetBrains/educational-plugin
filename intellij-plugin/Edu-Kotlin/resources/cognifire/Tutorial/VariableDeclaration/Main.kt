package jetbrains.course.tutorial

import org.jetbrains.academy.cognifire.dsl.*

fun calculateDogAgeInDogYears() {
  prompt("""
        Get the user input and save the result to a variable named `humanYears`.
    """)
}

fun verifyHumanYearsInput(humanYears: Int): Boolean {
  TODO("Not implemented yet")
}

fun main() {
  calculateDogAgeInDogYears()
}