In this task, we will check if the user inputs are correct.

### Task

Implement the `isCorrectInput` function,
which accepts `userInput`, `wordLength`, and `alphabet` and returns `true` 
if the input is correct, and `false` otherwise.

<div class="hint" title="Click me to see the new signature of the isCorrectInput function">

The signature of the function is:
```kotlin
fun isCorrectInput(userInput: String, wordLength: Int, alphabet: String): Boolean
```
</div>

This function should have the following behavior:
- inform the user if the length of the input is incorrect:
  ```text
  The length of your guess should be <wordLength> characters! Try again!
  ```
  Here, instead of `<wordLength>`, you need to print the value from the `wordLength` function argument: e.g., if the value is `4`,
  the text `The length of your guess should be 4 characters! Try again!` will be printed.
  
- inform the user if the input contains wrong symbols:
  ```text
  All symbols in your guess should be the <alphabet> alphabet characters! Try again!
  ```
  Here, instead of `<alphabet>`, you need to print the value from the `alphabet` function argument: e.g., if the value is `ABCDEFGH`,
  the text `All symbols in your guess should be the ABCDEFGH alphabet characters! Try again!` will be printed.

**Note**: to avoid typos, just copy the text from here and paste it into your code.

You can implement this function in any way you choose, but we _recommend_ looking into the `filter` and `isNotEmpty` built-in functions.

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="Hint" title="Click me to learn how to check if the size of the userInput is incorrect">

You can use `length` to get the number of letters in `userInput`:
```kotlin
val size = userInput.length
```
Then, you need to compare it with `wordLength`.
</div>

<div class="Hint" title="Click me to learn more about the isNotEmpty built-in function">

As we already know, Kotlin has many built-in functions.
If you need to check whether a list (or string) is empty,
you can use the [`isEmpty`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/is-empty.html) and [`isNotEmpty`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/is-not-empty.html) built-in functions:
```kotlin
if (someString.length == 0) {
    TODO("Not implemented yet")
}
```
It is the same as:
```kotlin
if (someString.isEmpty()) {
    TODO("Not implemented yet")
}
```

Another example:
```kotlin
if (someString.length != 0) {
    TODO("Not implemented yet")
}
```
It is the same as:
```kotlin
if (someString.isNotEmpty()) {
    TODO("Not implemented yet")
}
```
</div>
