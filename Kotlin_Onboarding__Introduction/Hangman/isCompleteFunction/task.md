It's time to practice! Let's start with a simple function.

### Task

Implement the `isComplete` function, which accepts two string arguments - `secret` and `currentGuess`, 
and checks if the game is complete. The game is complete only if `secret` and `currentGuess` are equal.

<div class="hint" title="Click me to see the new signature of the isComplete function">

The signature of the function is:
```kotlin
fun isComplete(secret: String, currentGuess: String): Boolean
```
</div>

**Note** that `currentGuess` contains spaces between letters. 
Therefore, it's not enough to merely compare  `secret` and `currentGuess`.
You'll need to remove all spaces from `currentGuess` first.

You can also use the already defined variable `separator`, which stores a space:
```kotlin
println("This is the value from the separator variable: $separator.") // This is the value from the separator variable:  .
```

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="Hint" title="Click me to see examples of how the isComplete function works">

Here are several examples of the _isComplete_ function's work:

- secret = "ABC", currentGuess = "A B C", result = true;
- secret = "ABC", currentGuess = "A B B", result = false;
- secret = "ABC", currentGuess = "A A A", result = false;
</div>

<div class="Hint" title="Click me to learn how to remove the separator in the current user's guess">

The easiest way to remove `separator` in `currentGuess` is to use the built-in function [`replace`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/replace.html):
```kotlin
println("aabbccdd".replace("a", "e")) // eebbccdd
```
In this task, you can just replace `separator` with an empty string `"""`.
</div>
