### First steps
Functions are one of the fundamental building blocks in software development. 
They help us to group blocks of code that perform a specific task. 
Functions provide a way to break our code into manageable, reusable, and testable parts.

### Task

Let's implement the task by declaring **two** functions:
1. The function `calculateDogAgeInDogYears`. It doesn't take any parameters and returns nothing.
2. The function to verify the input `verifyHumanYearsInput`. 
This function should have a parameter `humanYears` with type `Int` and it should return a `Boolean` value.



<div class="hint" title="The basic syntax for declaring a function in Kotlin">

```kotlin
fun functionName(parameter1: Type, parameter2: Type): ReturnType {
    TODO("Not implemented yet")
}
```
* `fun` - the keyword used to declare a function.
* `functionName` - the name of the function.
* `parameter1`: Type, `parameter2`: Type – the parameters of the function (if any) and their types. You can have any number of parameters and their types listed, separated by commas
* `ReturnType` - the type of the value returned by the function. If the function doesn’t return anything, its type would be `Unit`, which is usually omitted.

</div>
