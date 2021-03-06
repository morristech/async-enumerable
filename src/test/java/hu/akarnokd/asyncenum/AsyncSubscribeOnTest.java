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

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsyncSubscribeOnTest {

    @Test
    public void simple() {
        ExecutorService exec = Executors.newSingleThreadExecutor(r -> new Thread(r, "CustomPool"));
        try {
            List<String> list = AsyncEnumerable.defer(() ->
                    AsyncEnumerable.just(Thread.currentThread().getName()))
                .subscribeOn(exec)
                .toList()
                .blockingFirst();

            assertEquals(1, list.size());
            for (String s : list) {
                assertTrue(s, s.contains("CustomPool"));
            }
        } finally {
            exec.shutdownNow();
        }
    }

    @Test
    public void cancel() {
        TestHelper.withScheduler(executor ->{
            AtomicReference<Future<?>> f = new AtomicReference<>();
            AsyncEnumerable.range(1, 5)
                .subscribeOn(r -> {
                    f.set(executor.schedule(r, 100, TimeUnit.MILLISECONDS));
                })
                .enumerator()
                .cancel();

            try {
                f.get().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }
}
