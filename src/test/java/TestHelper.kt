/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

import io.reactivex.exceptions.CompositeException
import io.reactivex.internal.util.ExceptionHelper
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import io.reactivex.Scheduler

object TestHelper {
    fun race(r1: Runnable, r2: Runnable, s: Scheduler) {
        val count = AtomicInteger(2)
        val cdl = CountDownLatch(2)

        val errors = arrayOf<Throwable?>(null, null)

        s.scheduleDirect {
            if (count.decrementAndGet() != 0) {
                while (count.get() != 0) {
                }
            }

            try {
                try {
                    r1.run()
                } catch (ex: Throwable) {
                    errors[0] = ex
                }

            } finally {
                cdl.countDown()
            }
        }

        if (count.decrementAndGet() != 0) {
            while (count.get() != 0) {
            }
        }

        try {
            try {
                r2.run()
            } catch (ex: Throwable) {
                errors[1] = ex
            }

        } finally {
            cdl.countDown()
        }

        try {
            if (!cdl.await(5, TimeUnit.SECONDS)) {
                throw AssertionError("The wait timed out!")
            }
        } catch (ex: InterruptedException) {
            throw RuntimeException(ex)
        }

        if (errors[0] != null && errors[1] == null) {
            throw ExceptionHelper.wrapOrThrow(errors[0])
        }

        if (errors[0] == null && errors[1] != null) {
            throw ExceptionHelper.wrapOrThrow(errors[1])
        }

        if (errors[0] != null && errors[1] != null) {
            throw CompositeException(*errors)
        }
    }
}