import io.reactivex.schedulers.Schedulers
import org.junit.Test
import kotlin.test.assertEquals

class SingleSubscriberReactiveQueueTest {

    @Test
    fun testSubscribeHasSubscriber() {
        val queue = SingleSubscriberReactiveQueue.create<Int>()
        queue.subscribe()
        assertEquals(true, queue.hasSubscriber())
    }

    @Test
    fun testSubscribeUnsubscribeHasNoSubscriber() {
        val queue = SingleSubscriberReactiveQueue.create<Int>()
        queue.subscribe().dispose()
        assertEquals(false, queue.hasSubscriber())
    }

    @Test
    fun testQueueRememberElements() {
        val queue = SingleSubscriberReactiveQueue.create<Int>()
        queue.accept(0)
        queue.accept(1)
        queue.accept(2)
        queue.accept(3)

        queue.test().assertValues(0, 1, 2, 3)
    }

    @Test
    fun testMultipleSubscribers() {
        val queue = SingleSubscriberReactiveQueue.create<Int>()
        queue.test()

        try {
            queue.test()
        } catch (error: Throwable) {
            assertEquals("Cannot replace subscriber! You have to dispose of the previous one first!", error.cause!!.message)
        }
    }

    @Test
    fun testSubscribeUnsubscribe() {
        val queue = SingleSubscriberReactiveQueue.create<Int>()
        queue.accept(0)

        var testObserver = queue.test()
        testObserver.assertValues(0)
        testObserver.dispose()

        queue.accept(1)

        testObserver = queue.test()
        testObserver.assertValues(1)
        testObserver.dispose()

        queue.accept(2)
        queue.accept(3)
        queue.accept(4)

        testObserver = queue.test()
        testObserver.assertValues(2, 3, 4)
        testObserver.dispose()
    }

    @Test
    fun testEmptyQueueHasNoValues() {
        val queue = SingleSubscriberReactiveQueue.create<Int>()
        assertEquals(true, queue.getValues().isEmpty())
    }

    @Test
    fun testQueueWithNoSubscribersContainsValues() {
        val queue = SingleSubscriberReactiveQueue.create<Int>()
        queue.accept(0)
        queue.accept(1)
        queue.accept(2)
        val values = queue.getValues()

        assertEquals(3, values.size)
        assertEquals(0, values[2])
        assertEquals(1, values[1])
        assertEquals(2, values[0])
    }

    @Test
    fun testAcceptAcceptRace() {
        for (i in 0..50000) {
            val queue = SingleSubscriberReactiveQueue.create<Int>()
            val ts = queue.test()

            val r1 = Runnable {
                queue.accept(1)
            }

            val r2 = Runnable {
                queue.accept(2)
            }

            TestHelper.race(r1, r2, Schedulers.single())

            ts.assertSubscribed()
                    .assertNoErrors()
                    .assertNotComplete()
                    .assertValueSet(setOf(1, 2))
        }
    }

    @Test
    fun testNoErrors() {
        val queue = SingleSubscriberReactiveQueue.create<Int>()

        queue.accept(0)
        queue.doOnNext { throw IllegalStateException("123") }
        queue.accept(1)

        queue.test()
                .assertNoErrors()
                .assertValues(0, 1)
    }
}