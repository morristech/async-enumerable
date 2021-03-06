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

public class AsyncConcataMapTest {

    @Test
    public void simple() {
        List<Integer> list = AsyncEnumerable.range(1, 5)
                .concatMap(v -> AsyncEnumerable.range(v, 2))
                .toList()
                .blockingFirst();

        assertEquals(Arrays.asList(1, 2, 2, 3, 3, 4, 4, 5, 5, 6), list);
    }

    @Test
    public void simpleLong() {
        List<Integer> list = AsyncEnumerable.range(0, 1_000_000)
                .concatMap(AsyncEnumerable::just)
                .toList()
                .blockingFirst();

        for (int i = 0; i < 1_000_000; i++) {
            assertEquals(i, list.get(i).intValue());
        }
    }

    @Test
    public void simpleLongEmpty() {
        List<Integer> list = AsyncEnumerable.range(0, 1_000_000)
                .concatMap(v -> AsyncEnumerable.<Integer>empty())
                .toList()
                .blockingFirst();

        assertEquals(Collections.emptyList(), list);
    }

    @Test
    public void crossMap() {
        List<Integer> list = AsyncEnumerable.range(0, 1_000)
                .concatMap(v -> AsyncEnumerable.range(v, 1000))
                .toList()
                .blockingFirst();
        for (int i = 0; i < 1_000; i++) {
            for (int j = i; j < i + 1000; j++) {
                assertEquals(j, list.get(i * 1_000 + (j - i)).intValue());
            }
        }
    }

    @Test
    public void mainError() {
        TestHelper.assertFailure(
                AsyncEnumerable.error(new RuntimeException("forced failure"))
                .concatMap(v -> AsyncEnumerable.just(1)),
                RuntimeException.class, "forced failure"
        );
    }

    @Test
    public void innerError() {
        TestHelper.assertFailure(
                AsyncEnumerable.range(1, 5)
                        .concatMap(v -> AsyncEnumerable.error(new RuntimeException("forced failure"))),
                RuntimeException.class, "forced failure"
        );
    }
}
