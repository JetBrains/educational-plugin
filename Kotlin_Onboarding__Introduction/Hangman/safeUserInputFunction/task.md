In this task, we will implement the `safeUserInput` function to ensure the user only enters correct inputs.

### Task

Implement the `safeUserInput` function,
which accepts no arguments and returns an uppercase letter that was input by the user.
This function has to check the user input with the `isCorrectInput` function to avoid incorrect inputs.

<div class="hint" title="Click me to see the new signature of the safeUserInput function">

The signature of the function is:
```kotlin
fun safeUserInput(): Char
```
</div>

- before reading the user input, print the prompt:

```text
Please input your guess.
```

**Note**: to avoid typos, just copy the text from here and paste it into your code.

- to read the line of user input, use the `safeReadLine` function, which is already defined in the project:

```kotlin
guess = safeReadLine()
```

- to check the correctness of the user's input, use the `isCorrectInput` function, which was implemented in the previous step.

To make a letter uppercase, you can implement your own function, but we _recommend_ looking into the [`uppercase`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/uppercase.html) built-in function.

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="Hint" title="Click me to learn more about the uppercase built-in function">

The built-in function [`uppercase`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/uppercase.html)
converts all letter of a string to uppercase format:
```kotlin
println("abc".uppercase()) // ABC
```

Since the function returns `String`, to get a `Char` we need to get this letter by index, e.g.:
```kotlin
println("abc".uppercase()[0]) // A
println("abc".uppercase()[1]) // B
println("abc".uppercase()[2]) // C
```
</div>
