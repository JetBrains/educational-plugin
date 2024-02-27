Now, we will implement a function that builds a new string to display after a user's guess.

### Task

Implement the `generateNewUserWord` function, 
which generates a new sequence of underscores and already guessed letters
using a string for `secret`, a char for the user's `guess`, and a string for the `currentUserWord`. 

<div class="hint" title="Click me to see the new signature of the generateNewUserWord function">

The signature of the function is:
```kotlin
fun generateNewUserWord(secret: String, guess: Char, currentUserWord: String): String
```
</div>

**Note**, the `currentUserWord` must be stored with `separator`s, e.g., `B _ N _`.

You can implement this function in any way you choose, but we _recommend_ looking into the [`indices`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/indices.html) property, 
and the [`removeSuffix`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/remove-suffix.html) function.

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="Hint" title="Click me to see examples of how the currentUserWord function works">

Here are several examples of the _currentUserWord_ function's work:

- secret = `"BOOK"`, guess = `'A'`, currentUserWord = `"_ _ _ _"`, result = `"_ _ _ _"`;
- secret = `"BOOK"`, guess = `'A'`, currentUserWord = `"_ O O _"`, result = `"_ O O _"`;
- secret = `"BOOK"`, guess = `'A'`, currentUserWord = `"_ _ _ K"`, result = `"_ _ _ K"`;
- secret = `"BOOK"`, guess = `'B'`, currentUserWord = `"_ _ _ _"`, result = `"B _ _ _"`;
- secret = `"BOOK"`, guess = `'B'`, currentUserWord = `"_ O O _"`, result = `"B O O _"`;
- secret = `"BOOK"`, guess = `'K'`, currentUserWord = `"_ _ _ K"`, result = `"_ _ _ K"`;
</div>

<div class="Hint" title="Click me to learn more about the indices property">

To loop over each char in a string, you can use the <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/indices.html">`indices`</a> property:
   ```
   for (i in "abcd".indices) { ... } // i will be 0, 1, 2, 3
   ```
It is the same as:
   ```
   for (i in 0 until "abcd".length) { ... } // i will be 0, 1, 2, 3
   ```

It's a more convenient and concise way to represent a range of indices.
</div>

<div class="Hint" title="Click me to learn more about the removeSuffix function">

The [`removeSuffix`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/remove-suffix.html) function
helps to delete a suffix from a string:
```kotlin
println("abcdef".removeSuffix("f")) // abcde
```

It may be useful if you need to delete some extra separators from the end of the string.
</div>

<div class="Hint" title="Click me to check the main idea of the algorithm">

To implement the `generateNewUserWord` function, you just need to check if each letter from the `secret`
matches the `guess`.
If the current `secret`'s char matches the `guess`'s char in the same position,
add the respective `secret`'s char to `newUserWord`; otherwise, add the `currentUserWord`'s char in the `i * 2` position, 
where `i` is the position of the current char.

**Also, don't forget to add a separator at each step of the loop, since the resulting string needs to include spaces:**
```kotlin
"${secret[i]}$separator" or "${currentUserWord[i * 2]}$separator" // CORRECT

"${secret[i]}" or "${currentUserWord[i * 2]}" // INCORRECT
```

When following this algorithm, don't forget to remove an extra space from the end of the new string.
</div>
