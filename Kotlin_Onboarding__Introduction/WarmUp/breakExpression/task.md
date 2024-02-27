If you are working with a loop (whether `for` or `while`),
you can stop it earlier with a special [`break`](https://kotlinlang.org/docs/returns.html) expression:
```kotlin
while (x > 0) {
    // to do something
    if (x == 10) {
        break // break the loop
    }
}
```
