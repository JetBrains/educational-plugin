Let's implement a function to interact with the user.

### Task

Implement the `chooseFilter` function, which asks the user to choose 
a filter, either `borders` or `squared`. The full text of the prompt is `Please choose the filter: 'borders' or 'squared'.` 
The function should then return the chosen filter.

<div class="hint" title="Click me to see the signature of the chooseFilter function">

The signature of the function is:
```kotlin
fun chooseFilter(): String
```
</div>

This function must use the `safeReadLine` function. 
If the user inputs an incorrect filter name, the function should prompt the user again with `Please input 'borders' or 'squared'` to ensure the correct filter is entered:

![`chooseFilter` function work](../../utils/src/main/resources/images/part1/almost.done/choose_filter.gif "`chooseFilter` function work")

**Note**: to avoid typos, just copy the text from here and paste it into your code.

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="hint" title="Click me to learn an efficient way to use `when`">

The <code>when</code> expression allows you to use several values in one branch and define a variable in place:

```kotlin
when (val input = safeReadLine()) {
    "firstValue", "secondValue" -> {
        TODO()
    }
    else -> TODO()
}
```
</div>
