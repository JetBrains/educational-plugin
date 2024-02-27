Let's implement the second function.

### Task

Add a function `isLost`, which accepts three arguments: `complete`, `attempts`, and `maxAttemptsCount`
and returns `true` only if the user **did not** guess the word (the `complete` variable stores `false`) **and** spent _more_ than `maxAttemptsCount` attempts.

<div class="hint" title="Click me to see the signature of the isLost function">

The signature of the function is:
```kotlin
fun isLost(complete: Boolean, attempts: Int, maxAttemptsCount: Int): Boolean
```
</div>
