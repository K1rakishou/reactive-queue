import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.internal.functions.ObjectHelper
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SingleSubscriberReactiveQueue<T>
private constructor(
        capacity: Int
) : Observable<T>(), Consumer<T> {

    private val lock = ReentrantLock()
    private val elementsQueue = ArrayList<T>(capacity)
    private var subscriber = AtomicReference<QueueDisposable<T>?>(null)

    override fun subscribeActual(observer: Observer<in T>) {
        val disposable = QueueDisposable(observer, this)
        observer.onSubscribe(disposable)

        if (!disposable.isDisposed) {
            if (set(disposable)) {
                if (disposable.isDisposed) {
                    remove(disposable)
                } else {
                    lock.withLock {
                        drainTo(disposable)
                    }
                }
            }
        }
    }

    override fun accept(element: T) {
        ObjectHelper.requireNonNull<T>(element, "onNext called with null. Null values are generally not allowed in 2.x operators and sources.")

        lock.withLock {
            val sub = subscriber.get()
            if (sub != null) {
                drainTo(sub)
                sub.actual.onNext(element)
            } else {
                elementsQueue.add(0, element)
            }
        }
    }

    fun getValues(): List<T> {
        return elementsQueue
    }

    fun hasSubscriber(): Boolean {
        return subscriber.get() != null
    }

    private fun set(disposable: QueueDisposable<T>): Boolean {
        if (subscriber.get() != null) {
            throw IllegalStateException("Cannot replace subscriber! You have to dispose of the previous one first!")
        }

        while (true) {
            if (subscriber.compareAndSet(null, disposable)) {
                return true
            }
        }
    }

    private fun remove(disposable: QueueDisposable<T>): Boolean {
        while (true) {
            if (subscriber.compareAndSet(disposable, null)) {
                return true
            }
        }
    }

    private fun drainTo(subscriber: QueueDisposable<T>) {
        for (i in elementsQueue.lastIndex downTo 0) {
            subscriber.actual.onNext(elementsQueue[i])
        }

        elementsQueue.clear()
    }

    companion object {
        fun <T> create(capacity: Int = 16): SingleSubscriberReactiveQueue<T> {
            if (capacity <= 0) {
                throw IllegalStateException("Capacity cannot be <= 0")
            }

            return SingleSubscriberReactiveQueue(capacity)
        }
    }

    private class QueueDisposable<T>(
            val actual: Observer<in T>,
            val parent: SingleSubscriberReactiveQueue<T>) : AtomicBoolean(false), Disposable {

        override fun dispose() {
            if (this.compareAndSet(false, true)) {
                this.parent.remove(this)
            }
        }

        override fun isDisposed(): Boolean {
            return this.get()
        }
    }
}