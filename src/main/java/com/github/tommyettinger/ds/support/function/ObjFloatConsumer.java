/*
 * Copyright (c) 2013-2022 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.github.tommyettinger.ds.support.function;

import java.util.function.BiConsumer;

/**
 * Represents an operation that accepts an object-valued and a
 * {@code float}-valued argument, and returns no result.  This is the
 * {@code (reference, float)} specialization of {@link BiConsumer}.
 * Unlike most other functional interfaces, {@code ObjFloatConsumer} is
 * expected to operate via side-effects.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #accept(Object, float)}.
 *
 * @param <T> the type of the object argument to the operation
 *
 * @see BiConsumer
 */
@FunctionalInterface
public interface ObjFloatConsumer<T> {

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param value the second input argument
     */
    void accept(T t, float value);
}
