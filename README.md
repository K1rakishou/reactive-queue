# reactive-queue

Reactive queue behaves like PublishSubject but may contain only one subscriber.

Unlike the PublishSubject it remembers accepted items in the internal ArrayList
when the subscriber is not set and emits everything when someone subscribes.

# Example

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

queue.subscribe({
    println("subscriber2: $it")
})
```

Will output:

    subscriber1: 0
    subscriber1: 1
    subscriber1: 2
    subscriber1: 3
    subscriber2: 4
    subscriber2: 5
    subscriber2: 6
    subscriber2: 7
