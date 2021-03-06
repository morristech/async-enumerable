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

import java.util.concurrent.CompletionStage;

final class AsyncJust<T> implements AsyncEnumerable<T> {

    final T value;

    AsyncJust(T value) {
        this.value = value;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new JustEnumerator<>(value);
    }

    static final class JustEnumerator<T> implements AsyncEnumerator<T> {

        final T value;

        boolean once;

        JustEnumerator(T value) {
            this.value = value;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (once) {
                return FALSE;
            }
            once = true;
            return TRUE;
        }

        @Override
        public T current() {
            return value;
        }

        @Override
        public void cancel() {
            // No action, consumer should stop calling moveNext().
        }
    }
}
