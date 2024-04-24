### Advanced topic!
Let's implement the verification function!
It should return true if the `humanYearsInput` is a non-negative integer and false otherwise.\
We can use `toIntOrNull()` to attempt converting a string to an integer. If the casting fails, it will result in a `null` value.
<div class="hint" title="Example of code in a prompt">

```kotlin
fun verifyHumanYearsInput(humanYearsInput: String): Boolean {
  prompt("""
        If `humanYearsInput.toIntOrNull()` is greater than zero and not null, return true,
        Otherwise return false.
    """)
}
```
</div>