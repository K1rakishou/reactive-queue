# reactive-queue

Reactive queue behaves like ReplaySubject but may contain only one
subscriber.

Unlike the ReplaySubject it remembers accepted items in the internal ArrayList
when there is no subscriber and emits everything it have remembered to new subscriber.
It also removes elements from ArrayList after emitting them.

### Example

Add some elements in the queue then subscribe and resubscribe

```kotlin
val queue = SingleSubscriberReactiveQueue.create<Int>()
queue.accept(0)
queue.accept(1)
queue.accept(2)
queue.accept(3)

queue.subscribe({
    println("subscriber1: $it")
}).dispose()

queue.accept(4)
queue.accept(5)
queue.accept(6)
queue.accept(7)

println("queue elements count: ${queue.getValues().size}")

queue.subscribe({
    println("subscriber2: $it")
})

println("queue elements count: ${queue.getValues().size}")
```

Will output:

    subscriber1: 0
    subscriber1: 1
    subscriber1: 2
    subscriber1: 3
    queue elements count: 4
    subscriber2: 4
    subscriber2: 5
    subscriber2: 6
    subscriber2: 7
    queue elements count: 0
