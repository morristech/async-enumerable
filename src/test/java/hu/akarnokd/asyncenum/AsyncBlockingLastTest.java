/*
 * Copyright 2017 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AsyncBlockingLastTest {

    @Test
    public void interrupted() {
        Thread.currentThread().interrupt();

        try {
            AsyncEnumerable.never()
                    .blockingLast();
            fail("Should have thrown");
        } catch (RuntimeException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof InterruptedException);
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void asyncEmpty() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            AsyncEnumerable.timer(200, TimeUnit.MILLISECONDS, executor)
                    .filter(v -> false)
                    .blockingLast();
        } finally {
            executor.shutdownNow();
        }
    }
}
