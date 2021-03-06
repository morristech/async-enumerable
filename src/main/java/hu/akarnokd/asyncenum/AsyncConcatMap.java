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
import java.util.function.*;

final class AsyncConcatMap<T, R> implements AsyncEnumerable<R> {

    final AsyncEnumerable<T> source;

    final Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper;

    public AsyncConcatMap(AsyncEnumerable<T> source, Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public AsyncEnumerator<R> enumerator() {
        return new ConcatMapEnumerator<>(source.enumerator(), mapper);
    }

    static final class ConcatMapEnumerator<T, R>
            implements AsyncEnumerator<R>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper;

        final AtomicInteger wipMain;

        final AtomicInteger wipInner;

        final AtomicReference<AsyncEnumerator<R>> currentSource;

        volatile CompletableFuture<Boolean> completable;

        R current;

        ConcatMapEnumerator(AsyncEnumerator<T> source, Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper) {
            this.source = source;
            this.mapper = mapper;
            this.currentSource = new AtomicReference<>();
            this.wipMain = new AtomicInteger();
            this.wipInner = new AtomicInteger();
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            if (currentSource.getPlain() == null) {
                nextMain();
            } else {
                nextInner();
            }
            return cf;
        }

        @Override
        public R current() {
            return current;
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                source.cancel();
                completable.completeExceptionally(throwable);
                return;
            }
            if (aBoolean) {
                current = currentSource.getPlain().current();
                completable.complete(true);
            } else {
                nextMain();
            }
        }

        @SuppressWarnings("unchecked")
        public void acceptMain(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                completable.completeExceptionally(throwable);
                return;
            }
            if (aBoolean) {
                if (AsyncEnumeratorHelper.replace(currentSource, (AsyncEnumerator<R>)mapper.apply(source.current()).enumerator())) {
                    nextInner();
                }
            } else {
                completable.complete(false);
            }
        }

        void nextMain() {
            if (wipMain.getAndIncrement() == 0) {
                do {
                    source.moveNext().whenComplete(this::acceptMain);
                } while (wipMain.decrementAndGet() != 0);
            }
        }

        void nextInner() {
            if (wipInner.getAndIncrement() == 0) {
                do {
                    currentSource.getPlain().moveNext().whenComplete(this);
                } while (wipInner.decrementAndGet() != 0);
            }
        }

        @Override
        public void cancel() {
            AsyncEnumeratorHelper.cancel(currentSource);
        }
    }
}
