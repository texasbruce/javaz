/* ____  ______________  ________________________  __________
 * \   \/   /      \   \/   /   __/   /      \   \/   /      \
 *  \______/___/\___\______/___/_____/___/\___\______/___/\___\
 *
 * Copyright 2020 Vavr, http://vavr.io
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
package io.vavr;

import io.vavr.collection.Iterator;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import java.util.function.Function;

import static io.vavr.API.*;

/**
 * Functional programming is all about values and transformation of values using functions. The {@code Value}
 * type reflects the values in a functional setting. It can be seen as the result of a partial function application.
 * Hence the result may be undefined. If a value is undefined, we say it is empty.
 * <p>
 * How the empty state is interpreted depends on the context, i.e. it may be <em>undefined</em>, <em>failed</em>,
 * <em>no elements</em>, etc.
 * <p>
 * Iterable extensions:
 *
 * <ul>
 * <li>{@link #forEach(Consumer)}</li>
 * <li>{@link #iterator()}</li>
 * </ul>
 *
 * Type conversion:
 *
 * <ul>
 * <li>{@link #toEither(Object)}</li>
 * <li>{@link #toEither(Supplier)}</li>
 * <li>{@link #toInvalid(Object)}</li>
 * <li>{@link #toInvalid(Supplier)}</li>
 * <li>{@link #toJavaOptional()}</li>
 * <li>{@link #toLeft(Object)}</li>
 * <li>{@link #toLeft(Supplier)}</li>
 * <li>{@link #toOption()}</li>
 * <li>{@link #toRight(Object)}</li>
 * <li>{@link #toRight(Supplier)}</li>
 * <li>{@link #toString()}</li>
 * <li>{@link #toTry()}</li>
 * <li>{@link #toTry(Supplier)}</li>
 * <li>{@link #toValid(Object)}</li>
 * <li>{@link #toValid(Supplier)}</li>
 * <li>{@link #toValidation(Object)}</li>
 * <li>{@link #toValidation(Supplier)}</li>
 * </ul>
 *
 * <strong>Please note:</strong> flatMap signatures are manifold and have to be declared by subclasses of Value.
 *
 * @param <T> The type of the wrapped value.
 */
public interface Value<T> extends Iterable<T> {

    /**
     * Narrows a widened {@code Value<? extends T>} to {@code Value<T>}
     * by performing a type-safe cast. This is eligible because immutable/read-only
     * types are covariant.
     *
     * @param value A {@code Value}.
     * @param <T>   Component type of the {@code Value}.
     * @return the given {@code value} instance as narrowed type {@code Value<T>}.
     */
    @SuppressWarnings("unchecked")
    static <T> Value<T> narrow(Value<? extends T> value) {
        return (Value<T>) value;
    }

    /**
     * Gets the underlying value or throws if no value is present.
     * <p>
     * <strong>IMPORTANT! This method will throw an undeclared {@link Throwable} if {@code isEmpty() == true} is true.</strong>
     * <p>
     * Because the 'empty' state indicates that there is no value present that can be returned,
     * {@code get()} has to throw in such a case. Generally, implementing classes should throw a
     * {@link java.util.NoSuchElementException} if {@code isEmpty()} returns true.
     * <p>
     * However, there exist use-cases, where implementations may throw other exceptions. See {@link Try#get()}.
     * <p>
     * <strong>Additional note:</strong> Dynamic proxies will wrap an undeclared exception in a {@link java.lang.reflect.UndeclaredThrowableException}.
     *
     * @return the underlying value if this is not empty, otherwise {@code get()} throws a {@code Throwable}
     * @deprecated get() will be removed from collections. It should be used on single-valued types only.
     */
    @Deprecated
    default T get() {
        return null;
    }
    
    /**
     * Returns the underlying value if present, otherwise {@code other}.
     *
     * @param other An alternative value.
     * @return A value of type {@code T}
     * @deprecated will be removed from io.vavr.collection should be used by single-valued types only
     */
    @Deprecated
    default T getOrElse(T other) {
        return isEmpty() ? other : get();
    }

    /**
     * Returns the underlying value if present, otherwise {@code supplier.get()}.
     *
     * @param supplier An alternative value supplier.
     * @return A value of type {@code T}
     * @throws NullPointerException if supplier is null
     * @deprecated will be removed from io.vavr.collection should be used by single-valued types only
     */
    @Deprecated
    default T getOrElse(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return isEmpty() ? supplier.get() : get();
    }

    /**
     * Returns the underlying value if present, otherwise throws {@code supplier.get()}.
     *
     * @param <X>      a Throwable type
     * @param supplier An exception supplier.
     * @return A value of type {@code T}.
     * @throws NullPointerException if supplier is null
     * @throws X                    if no value is present
     * @deprecated will be removed from io.vavr.collection should be used by single-valued types only
     */
    @Deprecated
    default <X extends Throwable> T getOrElseThrow(Supplier<X> supplier) throws X {
        Objects.requireNonNull(supplier, "supplier is null");
        if (isEmpty()) {
            throw supplier.get();
        } else {
            return get();
        }
    }

    /**
     * Returns the underlying value if present, otherwise returns the result of {@code Try.of(supplier).get()}.
     *
     * @param supplier An alternative value supplier.
     * @return A value of type {@code T}.
     * @throws NullPointerException if supplier is null
     * @deprecated will be removed from io.vavr.collection should be used by single-valued types only. Use getOrElse(() -&gt; Try.of(supplier).get()) instead.
     */
    @Deprecated
    default T getOrElseTry(CheckedFunction0<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return isEmpty() ? Try.of(supplier).get() : get();
    }

    /**
     * Returns the underlying value if present, otherwise {@code null}.
     *
     * @return A value of type {@code T} or {@code null}.
     * @deprecated will be removed from io.vavr.collection should be used by single-valued types only. Use getOrElse(null) instead.
     */
    @Deprecated
    default T getOrNull() {
        return isEmpty() ? null : get();
    }

    /**
     * Checks, this {@code Value} is empty, i.e. if the underlying value is absent.
     *
     * @return false, if no underlying value is present, true otherwise.
     * @deprecated will be moved to controls and collections
     */
    @Deprecated
    boolean isEmpty();

    /**
     * Maps the underlying value to a different component type.
     *
     * @param mapper A mapper
     * @param <U>    The new component type
     * @return A new value
     * @deprecated will be moved to controls and collections
     */
    @Deprecated
    <U> Value<U> map(Function<? super T, ? extends U> mapper);

    // -- Adjusted return types of Iterable

    /**
     * Returns a rich {@code io.vavr.collection.Iterator}.
     *
     * @return A new Iterator
     */
    @Override
    Iterator<T> iterator();

    /**
     * Converts this to a {@link CompletableFuture}
     *
     * @return A new {@link CompletableFuture} containing the value
     */
    default CompletableFuture<T> toCompletableFuture() {
        final CompletableFuture<T> completableFuture = new CompletableFuture<>();
        Try.of(this::get)
                .onSuccess(completableFuture::complete)
                .onFailure(completableFuture::completeExceptionally);
        return completableFuture;
    }

    /**
     * Converts this to a {@link Validation}.
     *
     * @param <U>   value type of a {@code Valid}
     * @param value An instance of a {@code Valid} value
     * @return A new {@link Validation.Valid} containing the given {@code value} if this is empty, otherwise
     * a new {@link Validation.Invalid} containing this value.
     * @deprecated Use {@link #toValidation(Object)} instead.
     */
    @Deprecated
    default <U> Validation<T, U> toInvalid(U value) {
        return isEmpty() ? Validation.valid(value) : Validation.invalid(get());
    }

    /**
     * Converts this to a {@link Validation}.
     *
     * @param <U>           value type of a {@code Valid}
     * @param valueSupplier A supplier of a {@code Valid} value
     * @return A new {@link Validation.Valid} containing the result of {@code valueSupplier} if this is empty,
     * otherwise a new {@link Validation.Invalid} containing this value.
     * @throws NullPointerException if {@code valueSupplier} is null
     * @deprecated Use {@link #toValidation(Supplier)} instead.
     */
    @Deprecated
    default <U> Validation<T, U> toInvalid(Supplier<? extends U> valueSupplier) {
        Objects.requireNonNull(valueSupplier, "valueSupplier is null");
        return isEmpty() ? Validation.valid(valueSupplier.get()) : Validation.invalid(get());
    }

    /**
     * Converts this to an {@link java.util.Optional}.
     *
     * <pre>{@code
     * // = Optional.empty
     * Future.of(() -> { throw new Error(); })
     *       .toJavaOptional()
     *
     * // = Optional[ok]
     * Try.of(() -> "ok")
     *     .toJavaOptional()
     *
     * // = Optional[1]
     * List.of(1, 2, 3)
     *     .toJavaOptional()
     * }</pre>
     *
     * @return A new {@link java.util.Optional}.
     */
    default Optional<T> toJavaOptional() {
        return isEmpty() ? Optional.empty() : Optional.ofNullable(get());
    }

    /**
     * Converts this to a {@link Either}.
     *
     * @param <R>   right type
     * @param right An instance of a right value
     * @return A new {@link Either.Right} containing the value of {@code right} if this is empty, otherwise
     * a new {@link Either.Left} containing this value.
     * @deprecated Use {@link #toEither(Object)} instead.
     */
    @Deprecated
    default <R> Either<T, R> toLeft(R right) {
        return isEmpty() ? Either.right(right) : Either.left(get());
    }

    /**
     * Converts this to a {@link Either}.
     *
     * @param <R>   right type
     * @param right A supplier of a right value
     * @return A new {@link Either.Right} containing the result of {@code right} if this is empty, otherwise
     * a new {@link Either.Left} containing this value.
     * @throws NullPointerException if {@code right} is null
     * @deprecated Use {@link #toEither(Supplier)} instead.
     */
    @Deprecated
    default <R> Either<T, R> toLeft(Supplier<? extends R> right) {
        Objects.requireNonNull(right, "right is null");
        return isEmpty() ? Either.right(right.get()) : Either.left(get());
    }

    /**
     * Converts this to an {@link Option}.
     *
     * @return A new {@link Option}.
     * @deprecated will be moved to single-valued types
     */
    @Deprecated
    default Option<T> toOption() {
        if (this instanceof Option) {
            return (Option<T>) this;
        } else {
            return isEmpty() ? Option.none() : Option.some(get());
        }
    }

    /**
     * Converts this to an {@link Either}.
     *
     * @param left A left value for the {@link Either}
     * @param <L>  Either left component type
     * @return A new {@link Either}.
     * @deprecated will be moved to single-valued types
     */
    @Deprecated
    default <L> Either<L, T> toEither(L left) {
        if (this instanceof Either) {
            return ((Either<?, T>) this).mapLeft(ignored -> left);
        } else {
            return isEmpty() ? Left(left) : Right(get());
        }
    }

    /**
     * Converts this to an {@link Either}.
     *
     * @param leftSupplier A {@link Supplier} for the left value for the {@link Either}
     * @param <L>          Validation error component type
     * @return A new {@link Either}.
     * @deprecated will be moved to single-valued types
     */
    @Deprecated
    default <L> Either<L, T> toEither(Supplier<? extends L> leftSupplier) {
        Objects.requireNonNull(leftSupplier, "leftSupplier is null");
        if (this instanceof Either) {
            return ((Either<?, T>) this).mapLeft(ignored -> leftSupplier.get());
        } else {
            return isEmpty() ? Left(leftSupplier.get()) : Right(get());
        }
    }

    /**
     * Converts this to an {@link Validation}.
     *
     * @param invalid An invalid value for the {@link Validation}
     * @param <E>     Validation error component type
     * @return A new {@link Validation}.
     */
    default <E> Validation<E, T> toValidation(E invalid) {
        if (this instanceof Validation) {
            return ((Validation<?, T>) this).mapError(ignored -> invalid);
        } else {
            return isEmpty() ? Invalid(invalid) : Valid(get());
        }
    }

    /**
     * Converts this to an {@link Validation}.
     *
     * @param invalidSupplier A {@link Supplier} for the invalid value for the {@link Validation}
     * @param <E>             Validation error component type
     * @return A new {@link Validation}.
     */
    default <E> Validation<E, T> toValidation(Supplier<? extends E> invalidSupplier) {
        Objects.requireNonNull(invalidSupplier, "invalidSupplier is null");
        if (this instanceof Validation) {
            return ((Validation<?, T>) this).mapError(ignored -> invalidSupplier.get());
        } else {
            return isEmpty() ? Invalid(invalidSupplier.get()) : Valid(get());
        }
    }

    /**
     * Converts this to a {@link Either}.
     *
     * @param <L>  left type
     * @param left An instance of a left value
     * @return A new {@link Either.Left} containing the value of {@code left} if this is empty, otherwise
     * a new {@link Either.Right} containing this value.
     * @deprecated Use {@link #toEither(Object)} instead.
     */
    @Deprecated
    default <L> Either<L, T> toRight(L left) {
        return isEmpty() ? Either.left(left) : Either.right(get());
    }

    /**
     * Converts this to a {@link Either}.
     *
     * @param <L>  left type
     * @param left A supplier of a left value
     * @return A new {@link Either.Left} containing the result of {@code left} if this is empty, otherwise
     * a new {@link Either.Right} containing this value.
     * @throws NullPointerException if {@code left} is null
     * @deprecated Use {@link #toEither(Supplier)} instead.
     */
    @Deprecated
    default <L> Either<L, T> toRight(Supplier<? extends L> left) {
        Objects.requireNonNull(left, "left is null");
        return isEmpty() ? Either.left(left.get()) : Either.right(get());
    }

    /**
     * Converts this to a {@link Try}.
     * <p>
     * If this value is undefined, i.e. empty, then a new {@code Failure(NoSuchElementException)} is returned,
     * otherwise a new {@code Success(value)} is returned.
     *
     * @return A new {@link Try}.
     * @deprecated will be moved to single-valued types
     */
    @Deprecated
    default Try<T> toTry() {
        if (this instanceof Try) {
            return (Try<T>) this;
        } else {
            return Try.of(this::get);
        }
    }

    /**
     * Converts this to a {@link Try}.
     * <p>
     * If this value is undefined, i.e. empty, then a new {@code Failure(ifEmpty.get())} is returned,
     * otherwise a new {@code Success(value)} is returned.
     *
     * @param ifEmpty an exception supplier
     * @return A new {@link Try}.
     * @deprecated will be moved to single-valued types
     */
    @Deprecated
    default Try<T> toTry(Supplier<? extends Throwable> ifEmpty) {
        Objects.requireNonNull(ifEmpty, "ifEmpty is null");
        return isEmpty() ? Try.failure(ifEmpty.get()) : toTry();
    }

    /**
     * Converts this to a {@link Validation}.
     *
     * @param <E>   error type of an {@code Invalid}
     * @param error An error
     * @return A new {@link Validation.Invalid} containing the given {@code error} if this is empty, otherwise
     * a new {@link Validation.Valid} containing this value.
     * @deprecated Use {@link #toValidation(Object)} instead.
     */
    @Deprecated
    default <E> Validation<E, T> toValid(E error) {
        return isEmpty() ? Validation.invalid(error) : Validation.valid(get());
    }

    /**
     * Converts this to a {@link Validation}.
     *
     * @param <E>           error type of an {@code Invalid}
     * @param errorSupplier A supplier of an error
     * @return A new {@link Validation.Invalid} containing the result of {@code errorSupplier} if this is empty,
     * otherwise a new {@link Validation.Valid} containing this value.
     * @throws NullPointerException if {@code valueSupplier} is null
     * @deprecated Use {@link #toValidation(Supplier)} instead.
     */
    @Deprecated
    default <E> Validation<E, T> toValid(Supplier<? extends E> errorSupplier) {
        Objects.requireNonNull(errorSupplier, "errorSupplier is null");
        return isEmpty() ? Validation.invalid(errorSupplier.get()) : Validation.valid(get());
    }

    @Override
    default Spliterator<T> spliterator() {
        return Spliterators.spliterator(iterator(), isEmpty() ? 0 : 1,
                Spliterator.IMMUTABLE | Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED);
    }

    // -- Object

    /**
     * Clarifies that values have a proper equals() method implemented.
     * <p>
     * See <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#equals-java.lang.Object-">Object.equals(Object)</a>.
     *
     * @param o An object
     * @return true, if this equals o, false otherwise
     */
    @Override
    boolean equals(Object o);

    /**
     * Clarifies that values have a proper hashCode() method implemented.
     * <p>
     * See <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#hashCode--">Object.hashCode()</a>.
     *
     * @return The hashcode of this object
     */
    @Override
    int hashCode();

    /**
     * Clarifies that values have a proper toString() method implemented.
     * <p>
     * See <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#toString--">Object.toString()</a>.
     *
     * @return A String representation of this object
     */
    @Override
    String toString();

}
