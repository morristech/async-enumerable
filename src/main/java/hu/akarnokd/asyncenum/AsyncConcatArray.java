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

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BiConsumer;

final class AsyncConcatArray<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T>[] sources;

    AsyncConcatArray(AsyncEnumerable<T>[] sources) {
        this.sources = sources;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new ConcatArrayEnumerator<>(sources);
    }

    static final class ConcatArrayEnumerator<T> extends AtomicInteger
            implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerable<T>[] sources;

        final AtomicReference<AsyncEnumerator<T>> currentEnumerator;

        CompletableFuture<Boolean> currentStage;

        int index;

        ConcatArrayEnumerator(AsyncEnumerable<T>[] sources) {
            this.sources = sources;
            this.currentEnumerator = new AtomicReference<>();
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (currentEnumerator.get() == null) {
                if (index == sources.length) {
                    return FALSE;
                }
                if (!AsyncEnumeratorHelper.replace(currentEnumerator, sources[index++].enumerator())) {
                    return CANCELLED;
                }
            }

            currentStage = new CompletableFuture<>();
            currentEnumerator.getPlain().moveNext().whenComplete(this);
            return currentStage;
        }

        @Override
        public T current() {
            return currentEnumerator.getPlain().current();
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                currentStage.completeExceptionally(throwable);
                return;
            }
            if (aBoolean) {
                currentStage.complete(true);
            } else {
                if (getAndIncrement() == 0) {
                    do {
                        if (index == sources.length) {
                            currentStage.complete(false);
                            break;
                        }
                        AsyncEnumerator<T> en = sources[index++].enumerator();
                        if (AsyncEnumeratorHelper.replace(currentEnumerator, en)) {
                            en.moveNext().whenComplete(this);
                        } else {
                            break;
                        }
                    } while (decrementAndGet() != 0);
                }
            }
        }

        @Override
        public void cancel() {
            AsyncEnumeratorHelper.cancel(currentEnumerator);
        }
    }
}
