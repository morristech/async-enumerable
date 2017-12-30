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

import java.util.ArrayDeque;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

final class AsyncTakeLast<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final int n;

    AsyncTakeLast(AsyncEnumerable<T> source, int n) {
        this.source = source;
        this.n = n;
    }


    @Override
    public AsyncEnumerator<T> enumerator() {
        return new TakeLastEnumerator<>(source.enumerator(), n);
    }

    static final class TakeLastEnumerator<T>
            extends AtomicInteger
            implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final ArrayDeque<T> deque;

        final int n;

        T result;

        CompletableFuture<Boolean> completable;

        volatile boolean cancelled;

        boolean replayMode;

        TakeLastEnumerator(AsyncEnumerator<T> source, int n) {
            this.source = source;
            this.n = n;
            this.deque = new ArrayDeque<>();
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            result = null;
            if (replayMode) {
                if (deque.isEmpty()) {
                    return FALSE;
                }
                result = deque.poll();
                return TRUE;
            } else {
                CompletableFuture<Boolean> cf = new CompletableFuture<>();
                completable = cf;
                nextSource();
                return cf;
            }
        }

        @Override
        public T current() {
            return result;
        }

        void nextSource() {
            if (getAndIncrement() == 0) {
                do {
                    if (cancelled) {
                        return;
                    }
                    source.moveNext().whenComplete(this);
                } while (decrementAndGet() != 0);
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            source.cancel();
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                deque.clear();
                completable.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                if (n == deque.size()) {
                    deque.poll();
                }
                deque.offer(source.current());
                nextSource();
            } else {
                if (deque.isEmpty()) {
                    completable.complete(false);
                }
                result = deque.poll();
                replayMode = true;
                completable.complete(true);
            }
        }
    }
}
