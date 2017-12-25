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

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

public class AsyncTimerTest {

    @Test
    public void simple() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        try {
            List<Long> list =
                    AsyncEnumerable.timer(100, TimeUnit.MILLISECONDS, scheduler)
                    .toList()
                    .blockingFirst();

            assertEquals(Collections.singletonList(0L), list);
        } finally {
            scheduler.shutdownNow();
        }
    }
}
