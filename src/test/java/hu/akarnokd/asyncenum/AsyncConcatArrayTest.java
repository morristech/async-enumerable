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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class AsyncConcatArrayTest {

    @Test
    public void simple() throws Exception {
        List<Integer> list = new ArrayList<>();
        AsyncEnumerable.concatArray(
                AsyncEnumerable.range(1, 3),
                AsyncEnumerable.range(4, 2))
                .forEach(list::add)
                .toCompletableFuture()
                .get();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void simpleLong() throws Exception {
        List<Integer> list = new ArrayList<>();
        @SuppressWarnings("unchecked")
        AsyncEnumerable<Integer>[] sources = new AsyncEnumerable[1_000_000];
        for (int i = 1; i < sources.length - 1; i++) {
            sources[i] = AsyncEnumerable.empty();
        }
        sources[0] = AsyncEnumerable.range(1, 3);
        sources[999_999] = AsyncEnumerable.range(4, 2);

        AsyncEnumerable.concatArray(sources)
                .forEach(list::add)
                .toCompletableFuture()
                .get();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void startWith() {
        List<Integer> list =
                AsyncEnumerable.range(1, 3)
                .startWith(AsyncEnumerable.range(4, 2))
                .toList()
                .blockingFirst();

        assertEquals(Arrays.asList(4, 5, 1, 2, 3), list);
    }

    @Test
    public void concatWith() {
        List<Integer> list =
                AsyncEnumerable.range(1, 3)
                        .concatWith(AsyncEnumerable.range(4, 2))
                        .toList()
                        .blockingFirst();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void emptyArray() {
        TestHelper.assertResult(AsyncEnumerable.concatArray());
    }

    @Test
    public void cancelThenMove() {
        TestHelper.withExecutor(executor -> {
            for (int i = 0; i < 10000; i++) {
                AsyncEnumerator<Integer> en = AsyncEnumerable.concatArray(AsyncEnumerable.range(1, 5))
                        .enumerator();

                TestHelper.race(en::cancel, en::moveNext, executor);
            }
        });
    }
}
