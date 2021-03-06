/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.util;

import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Utilities with operations that do not (yet) exist
 */
public final class OperationUtils {

    private OperationUtils() {
    }

    /**
     * Casts an item from one type to another
     *
     * @param <IN>  the source type
     * @param <OUT> the target type
     * @return the same instance
     */
    public static <IN extends OUT, OUT> Function<IN, OUT> cast() {
        return in -> in;
    }

    /**
     * Produces a Mono transformer that preserves the type of the source {@code Mono<IN>}.
     *
     * <p> The Mono produced expects a single element from the source, passes this to the function (as in {@code .then}) and requests an element from the resulting {@code Mono<OUT>}. When successful,
     * the result (if any) is discarded and input value is signalled. </p>
     *
     * <p> <b>Summary:</b> does a {@code .then} on the new Mono but keeps the input to pass on unchanged. </p>
     *
     * <p> <b>Usage:</b> Can be used inline thus: {@code .as(thenKeep(in -> funcOf(in)))} </p>
     *
     * @param thenFunction from source input element to some {@code Mono<OUT>}
     * @param <T>          the source element type
     * @param <U>          the element type of the Mono produced by {@code thenFunction}
     * @return a Mono transformer
     */
    public static <T, U> Function<Mono<T>, Mono<T>> thenKeep(Function<T, Mono<U>> thenFunction) {
        return source -> source
            .then(in -> thenFunction
                .apply(in)
                .after(Mono.just(in)));
    }

}
