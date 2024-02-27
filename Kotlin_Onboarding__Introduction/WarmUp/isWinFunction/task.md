It's time to implement functions that indicate if the user win or lose. Let's start from the first one.

### Task

Add a function `isWon`, which accepts three arguments: `complete`, `attempts`, and `maxAttemptsCount`
and returns `true` only if the user guessed the word (the `complete` variable stores `true`) 
**and** spent _not more_ than `maxAttemptsCount` attempts.

<div class="hint" title="Click me to see the signature of the isWon function">

The signature of the function is:
```kotlin
fun isWon(complete: Boolean, attempts: Int, maxAttemptsCount: Int): Boolean
```
</div>
