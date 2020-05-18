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
package io.vavr.collection;

import io.vavr.*;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import lombok.NonNull;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import java.util.stream.Collector;

/**
 * An immutable {@code Map} interface.
 *
 * <p>
 * Basic operations:
 *
 * <ul>
 * <li>{@link #containsKey(Object)}</li>
 * <li>{@link #containsValue(Object)}</li>
 * <li>{@link #get(Object)}</li>
 * <li>{@link #keySet()}</li>
 * <li>{@link #merge(Map)}</li>
 * <li>{@link #merge(Map, BiFunction)}</li>
 * <li>{@link #put(Object, Object)}</li>
 * <li>{@link #put(Tuple2)}</li>
 * <li>{@link #put(Object, Object, BiFunction)}</li>
 * <li>{@link #put(Tuple2, BiFunction)}</li>
 * <li>{@link #values()}</li>
 * </ul>
 *
 * Conversion:
 *
 * <ul>
 * <li>{@link #toJavaMap()}</li>
 * </ul>
 *
 * Filtering:
 *
 * <ul>
 * <li>{@link #filter(BiPredicate)}</li>
 * <li>{@link #filterKeys(Predicate)}</li>
 * <li>{@link #filterValues(Predicate)}</li>
 * <li>{@link #filterNot(BiPredicate)}</li>
 * <li>{@link #filterNotKeys(Predicate)}</li>
 * <li>{@link #filterNotValues(Predicate)}</li>
 * <li>{@link #remove(Object)}</li>
 * </ul>
 *
 * Iteration:
 *
 * <ul>
 * <li>{@link #forEach(BiConsumer)}</li>
 * <li>{@link #iterator(BiFunction)}</li>
 * <li>{@link #keysIterator()}</li>
 * <li>{@link #valuesIterator()}</li>
 * </ul>
 *
 * Transformation:
 *
 * <ul>
 * <li>{@link #bimap(Function, Function)}</li>
 * <li>{@link #flatMap(BiFunction)}</li>
 * <li>{@link #lift()}</li>
 * <li>{@link #map(BiFunction)}</li>
 * <li>{@link #mapKeys(Function)}</li>
 * <li>{@link #mapKeys(Function, BiFunction)}</li>
 * <li>{@link #mapValues(Function)}</li>
 * <li>{@link #transform(Function)}</li>
 * <li>{@link #unzip(BiFunction)}</li>
 * <li>{@link #unzip3(BiFunction)}</li>
 * <li>{@link #withDefault(Function)}</li>
 * <li>{@link #withDefaultValue(Object)}</li>
 * </ul>
 *
 * @param <K> Key type
 * @param <V> Value type
 */
@SuppressWarnings("deprecation")
public interface Map<K, V> extends Traversable<Tuple2<K, V>>, PartialFunction<K, V>, Serializable {

    long serialVersionUID = 1L;

    /**
     * Narrows a widened {@code Map<? extends K, ? extends V>} to {@code Map<K, V>}
     * by performing a type-safe cast. This is eligible because immutable/read-only
     * collections are covariant.
     *
     * @param map A {@code Map}.
     * @param <K> Key type
     * @param <V> Value type
     * @return the given {@code map} instance as narrowed type {@code Map<K, V>}.
     */
    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> narrow(Map<? extends K, ? extends V> map) {
        return (Map<K, V>) map;
    }

    /**
     * Convenience factory method to create a key/value pair.
     * <p>
     * If imported statically, this method allows to create a {@link Map} with arbitrary entries in a readable and
     * type-safe way, e.g.:
     * <pre>
     * {@code
     *
     * HashMap.ofEntries(
     *     entry(k1, v1),
     *     entry(k2, v2),
     *     entry(k3, v3)
     * );
     *
     * }
     * </pre>
     *
     * @param key   the entry's key
     * @param value the entry's value
     * @param <K>   Key type
     * @param <V>   Value type
     * @return a key/value pair
     */
    static <K, V> Tuple2<K, V> entry(K key, V value) {
        return Tuple.of(key, value);
    }

    @Override
    default V apply(K key) {
        return get(key).getOrElseThrow(() -> new NoSuchElementException(String.valueOf(key)));
    }

    /**
     * Turns this {@code Map} into a {@link PartialFunction} which is defined at a specific index, if this {@code Map}
     * contains the given key. When applied to a defined key, the partial function will return
     * the value of this {@code Map} that is associated with the key.
     *
     * @return a new {@link PartialFunction}
     * @throws NoSuchElementException when a non-existing key is applied to the partial function
     */
    default PartialFunction<K, V> asPartialFunction() throws IndexOutOfBoundsException {
        return new PartialFunction<K, V>() {
            private static final long serialVersionUID = 1L;
            @Override
            public V apply(K key) {
                return get(key).getOrElseThrow(() -> new NoSuchElementException(String.valueOf(key)));
            }
            @Override
            public boolean isDefinedAt(K key) {
                return containsKey(key);
            }
        };
    }

    @Override
    default <R> Seq<R> collect(PartialFunction<? super Tuple2<K, V>, ? extends R> partialFunction) {
        return io.vavr.collection.Vector.ofAll(iterator().<R> collect(partialFunction));
    }

    /**
     * Maps this {@code Map} to a new {@code Map} with different component type by applying a function to its elements.
     *
     * @param <K2>        key's component type of the map result
     * @param <V2>        value's component type of the map result
     * @param keyMapper   a {@code Function} that maps the keys of type {@code K} to keys of type {@code K2}
     * @param valueMapper a {@code Function} that the values of type {@code V} to values of type {@code V2}
     * @return a new {@code Map}
     * @throws NullPointerException if {@code keyMapper} or {@code valueMapper} is null
     */
    <K2, V2> Map<K2, V2> bimap(Function<? super K, ? extends K2> keyMapper, Function<? super V, ? extends V2> valueMapper);

    @Override
    default boolean contains(Tuple2<K, V> element) {
        return get(element._1).map(v -> Objects.equals(v, element._2)).getOrElse(false);
    }

    /**
     * If the specified key is not already associated with a value,
     * attempts to compute its value using the given mapping
     * function and enters it into this map.
     *
     * @param key             key whose presence in this map is to be tested
     * @param mappingFunction mapping function
     * @return the {@link Tuple2} of current or modified map and existing or computed value associated with the specified key
     */
    Tuple2<V, ? extends Map<K, V>> computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);

    /**
     * If the value for the specified key is present, attempts to
     * compute a new mapping given the key and its current mapped value.
     *
     * @param key               key whose presence in this map is to be tested
     * @param remappingFunction remapping function
     * @return the {@link Tuple2} of current or modified map and the {@code Some} of the value associated
     * with the specified key, or {@code None} if none
     */
    Tuple2<Option<V>, ? extends Map<K, V>> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    /**
     * Returns <code>true</code> if this map contains a mapping for the specified key.
     *
     * @param key key whose presence in this map is to be tested
     * @return <code>true</code> if this map contains a mapping for the specified key
     */
    boolean containsKey(K key);

    /**
     * Returns <code>true</code> if this map maps one or more keys to the
     * specified value. This operation will require time linear in the map size.
     *
     * @param value value whose presence in this map is to be tested
     * @return <code>true</code> if this map maps one or more keys to the
     * specified value
     */
    default boolean containsValue(V value) {
        return iterator().map(Tuple2::_2).contains(value);
    }

    /**
     * Returns a new Map consisting of all elements which satisfy the given predicate.
     *
     * @param predicate the predicate used to test elements
     * @return a new Map
     * @throws NullPointerException if {@code predicate} is null
     */
    Map<K, V> filter(BiPredicate<? super K, ? super V> predicate);

    /**
     * Returns a new Map consisting of all elements which do not satisfy the given predicate.
     *
     * @param predicate the predicate used to test elements
     * @return a new Map
     * @throws NullPointerException if {@code predicate} is null
     */
    Map<K, V> filterNot(BiPredicate<? super K, ? super V> predicate);

    /**
     * Returns a new Map consisting of all elements with keys which satisfy the given predicate.
     *
     * @param predicate the predicate used to test keys of elements
     * @return a new Map
     * @throws NullPointerException if {@code predicate} is null
     */
    Map<K, V> filterKeys(Predicate<? super K> predicate);

    /**
     * Returns a new Map consisting of all elements with keys which do not satisfy the given predicate.
     *
     * @param predicate the predicate used to test keys of elements
     * @return a new Map
     * @throws NullPointerException if {@code predicate} is null
     */
    Map<K, V> filterNotKeys(Predicate<? super K> predicate);

    /**
     * Returns a new Map consisting of all elements with values which satisfy the given predicate.
     *
     * @param predicate the predicate used to test values of elements
     * @return a new Map
     * @throws NullPointerException if {@code predicate} is null
     */
    Map<K, V> filterValues(Predicate<? super V> predicate);

    /**
     * Returns a new Map consisting of all elements with values which do not satisfy the given predicate.
     *
     * @param predicate the predicate used to test values of elements
     * @return a new Map
     * @throws NullPointerException if {@code predicate} is null
     */
    Map<K, V> filterNotValues(Predicate<? super V> predicate);

    /**
     * FlatMaps this {@code Map} to a new {@code Map} with different component type.
     *
     * @param mapper A mapper
     * @param <K2>   key's component type of the mapped {@code Map}
     * @param <V2>   value's component type of the mapped {@code Map}
     * @return A new {@code Map}.
     * @throws NullPointerException if {@code mapper} is null
     */
    <K2, V2> Map<K2, V2> flatMap(BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper);

    /**
     * Flat-maps this entries to a sequence of values.
     * <p>
     * Please use {@link #flatMap(BiFunction)} if the result should be a {@code Map}
     *
     * @param mapper A mapper
     * @param <U>    Component type
     * @return A sequence of flat-mapped values.
     */
    @SuppressWarnings("unchecked")
    @Override
    default <U> Seq<U> flatMap(@NonNull Function<? super Tuple2<K, V>, ? extends Iterable<? extends U>> mapper) {
        // don't remove cast, doesn't compile in Eclipse without it
        return (Seq<U>) iterator().flatMap(mapper).toStream();
    }

    @Override
    default <U> U foldRight(U zero, @NonNull BiFunction<? super Tuple2<K, V>, ? super U, ? extends U> f) {
        return iterator().foldRight(zero, f);
    }

    /**
     * Performs an action on key, value pair.
     *
     * @param action A {@code BiConsumer}
     * @throws NullPointerException if {@code action} is null
     */
    default void forEach(@NonNull BiConsumer<K, V> action) {
        for (Tuple2<K, V> t : this) {
            action.accept(t._1, t._2);
        }
    }

    /**
     * Returns the {@code Some} of value to which the specified key
     * is mapped, or {@code None} if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the {@code Some} of value to which the specified key
     * is mapped, or {@code None} if this map contains no mapping
     * for the key
     */
    Option<V> get(K key);

    /**
     * Returns the value associated with a key, or a default value if the key is not contained in the map.
     *
     * @param key          the key
     * @param defaultValue a default value
     * @return the value associated with key if it exists, otherwise the default value.
     */
    V getOrElse(K key, V defaultValue);

    @Override
    default boolean hasDefiniteSize() {
        return true;
    }

    @Override
    default boolean isTraversableAgain() {
        return true;
    }

    @Override
    Iterator<Tuple2<K, V>> iterator();

    /**
     * Iterates this Map sequentially, mapping the (key, value) pairs to elements.
     *
     * @param mapper A function that maps (key, value) pairs to elements of type U
     * @param <U> The type of the resulting elements
     * @return An iterator through the mapped elements.
     */
    default <U> Iterator<U> iterator(@NonNull BiFunction<K, V, ? extends U> mapper) {
        return iterator().map(t -> mapper.apply(t._1, t._2));
    }

    /**
     * Returns the keys contained in this map.
     *
     * @return {@code Set} of the keys contained in this map.
     */
    io.vavr.collection.Set<K> keySet();

    /**
     * Returns the keys contained in this map as an iterator.
     *
     * @return {@code Iterator} of the keys contained in this map.
     */
    default Iterator<K> keysIterator() {
        return iterator().map(Tuple2::_1);
    }

    @Override
    default int length() {
        return size();
    }

    /**
     * Turns this map into a plain function returning an Option result.
     *
     * @return a function that takes a key k and returns its value in a Some if found, otherwise a None.
     */
    default Function1<K, Option<V>> lift() {
        return this::get;
    }

    /**
     * Maps the {@code Map} entries to a sequence of values.
     * <p>
     * Please use {@link #map(BiFunction)} if the result has to be of type {@code Map}.
     *
     * @param mapper A mapper
     * @param <U>    Component type
     * @return A sequence of mapped values.
     */
    @SuppressWarnings("unchecked")
    @Override
    default <U> Seq<U> map(@NonNull Function<? super Tuple2<K, V>, ? extends U> mapper) {
        // don't remove cast, doesn't compile in Eclipse without it
        return (Seq<U>) iterator().map(mapper).toStream();
    }

    /**
     * Maps the entries of this {@code Map} to form a new {@code Map}.
     *
     * @param <K2>   key's component type of the map result
     * @param <V2>   value's component type of the map result
     * @param mapper a {@code Function} that maps entries of type {@code (K, V)} to entries of type {@code (K2, V2)}
     * @return a new {@code Map}
     * @throws NullPointerException if {@code mapper} is null
     */
    <K2, V2> Map<K2, V2> map(BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper);

    /**
     * Maps the keys of this {@code Map} while preserving the corresponding values.
     * <p>
     * The size of the result map may be smaller if {@code keyMapper} maps two or more distinct keys to the same new key.
     * In this case the value at the {@code latest} of the original keys is retained.
     * Order of keys is predictable in {@code TreeMap} (by comparator) and {@code LinkedHashMap} (insertion-order) and not predictable in {@code HashMap}.
     *
     * @param <K2>      the new key type
     * @param keyMapper a {@code Function} that maps keys of type {@code V} to keys of type {@code V2}
     * @return a new {@code Map}
     * @throws NullPointerException if {@code keyMapper} is null
     */
    <K2> Map<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper);

    /**
     * Maps the keys of this {@code Map} while preserving the corresponding values and applying a value merge function on collisions.
     * <p>
     * The size of the result map may be smaller if {@code keyMapper} maps two or more distinct keys to the same new key.
     * In this case the associated values will be combined using {@code valueMerge}.
     *
     * @param <K2>       the new key type
     * @param keyMapper  a {@code Function} that maps keys of type {@code V} to keys of type {@code V2}
     * @param valueMerge a {@code BiFunction} that merges values
     * @return a new {@code Map}
     * @throws NullPointerException if {@code keyMapper} is null
     */
    <K2> Map<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper, BiFunction<? super V, ? super V, ? extends V> valueMerge);

    /**
     * Maps the values of this {@code Map} while preserving the corresponding keys.
     *
     * @param <V2>        the new value type
     * @param valueMapper a {@code Function} that maps values of type {@code V} to values of type {@code V2}
     * @return a new {@code Map}
     * @throws NullPointerException if {@code valueMapper} is null
     */
    <V2> Map<K, V2> mapValues(Function<? super V, ? extends V2> valueMapper);

    /**
     * Creates a new map which by merging the entries of {@code this} map and {@code that} map.
     * <p>
     * If collisions occur, the value of {@code this} map is taken.
     *
     * @param that the other map
     * @return A merged map
     * @throws NullPointerException if that map is null
     */
    Map<K, V> merge(Map<? extends K, ? extends V> that);

    /**
     * Creates a new map which by merging the entries of {@code this} map and {@code that} map.
     * <p>
     * Uses the specified collision resolution function if two keys are the same.
     * The collision resolution function will always take the first argument from <code>this</code> map
     * and the second from <code>that</code> map.
     *
     * @param <U>                 value type of that Map
     * @param that                the other map
     * @param collisionResolution the collision resolution function
     * @return A merged map
     * @throws NullPointerException if that map or the given collision resolution function is null
     */
    <U extends V> Map<K, V> merge(Map<? extends K, U> that, BiFunction<? super V, ? super U, ? extends V> collisionResolution);

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old value is
     * replaced by the specified value.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return A new Map containing these elements and that entry.
     */
    Map<K, V> put(K key, V value);

    /**
     * Convenience method for {@code put(entry._1, entry._2)}.
     *
     * @param entry A Tuple2 containing the key and value
     * @return A new Map containing these elements and that entry.
     */
    Map<K, V> put(Tuple2<? extends K, ? extends V> entry);

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the merge
     * function is used to combine the previous value to the value to
     * be inserted, and the result of that call is inserted in the map.
     *
     * @param <U>   the value type
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @param merge function taking the old and new values and merging them.
     * @return A new Map containing these elements and that entry.
     */
    <U extends V> Map<K, V> put(K key, U value, BiFunction<? super V, ? super U, ? extends V> merge);

    /**
     * Convenience method for {@code put(entry._1, entry._2, merge)}.
     *
     * @param <U>   the value type
     * @param entry A Tuple2 containing the key and value
     * @param merge function taking the old and new values and merging them.
     * @return A new Map containing these elements and that entry.
     */
    <U extends V> Map<K, V> put(Tuple2<? extends K, U> entry, BiFunction<? super V, ? super U, ? extends V> merge);

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key key whose mapping is to be removed from the map
     * @return A new Map containing these elements without the entry
     * specified by that key.
     */
    Map<K, V> remove(K key);


    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param keys keys are to be removed from the map
     * @return A new Map containing these elements without the entries
     * specified by that keys.
     */
    Map<K, V> removeAll(Iterable<? extends K> keys);

    @Override
    default <U> Seq<U> scanLeft(U zero, BiFunction<? super U, ? super Tuple2<K, V>, ? extends U> operation) {
        return io.vavr.collection.Collections.scanLeft(this, zero, operation, io.vavr.collection.Iterator::toVector);
    }

    @Override
    default <U> Seq<U> scanRight(U zero, BiFunction<? super Tuple2<K, V>, ? super U, ? extends U> operation) {
        return io.vavr.collection.Collections.scanRight(this, zero, operation, io.vavr.collection.Iterator::toVector);
    }

    @Override
    int size();

    /**
     * Converts this Vavr {@code Map} to a {@code java.util.Map} while preserving characteristics
     * like insertion order ({@code LinkedHashMap}) and sort order ({@code SortedMap}).
     *
     * @return a new {@code java.util.Map} instance
     */
    java.util.Map<K, V> toJavaMap();

    /**
     * Transforms this {@code Map}.
     *
     * @param f   A transformation
     * @param <U> Type of transformation result
     * @return An instance of type {@code U}
     * @throws NullPointerException if {@code f} is null
     */
    default <U> U transform(@NonNull Function<? super Map<K, V>, ? extends U> f) {
        return f.apply(this);
    }

    default Tuple2<Seq<K>, Seq<V>> unzip() {
        return unzip(Function.identity());
    }

    default <T1, T2> Tuple2<Seq<T1>, Seq<T2>> unzip(@NonNull BiFunction<? super K, ? super V, Tuple2<? extends T1, ? extends T2>> unzipper) {
        return unzip(entry -> unzipper.apply(entry._1, entry._2));
    }

    @Override
    default <T1, T2> Tuple2<Seq<T1>, Seq<T2>> unzip(@NonNull Function<? super Tuple2<K, V>, Tuple2<? extends T1, ? extends T2>> unzipper) {
        return iterator().unzip(unzipper).map(Stream::ofAll, Stream::ofAll);
    }

    default <T1, T2, T3> Tuple3<Seq<T1>, Seq<T2>, Seq<T3>> unzip3(@NonNull BiFunction<? super K, ? super V, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
        return unzip3(entry -> unzipper.apply(entry._1, entry._2));
    }

    @Override
    default <T1, T2, T3> Tuple3<Seq<T1>, Seq<T2>, Seq<T3>> unzip3(
            @NonNull Function<? super Tuple2<K, V>, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
        return iterator().unzip3(unzipper).map(Stream::ofAll, Stream::ofAll, Stream::ofAll);
    }

    /**
     * Returns a new {@link Seq} that contains the values of this {@code Map}.
     *
     * <pre>{@code
     * // = Seq("a", "b", "c")
     * HashMap.of(1, "a", 2, "b", 3, "c").values()
     * }</pre>
     *
     * @return a new {@link Seq}
     */
    Seq<V> values();

    /**
     * Returns the values in this map.
     *
     * <pre>{@code
     * // = Iterator.of("a", "b", "c")
     * HashMap.of(1, "a", 2, "b", 3, "c").values()
     * }</pre>
     *
     * @return a new {@link Iterator}
     */
    default Iterator<V> valuesIterator() {
        return iterator().map(Tuple2::_2);
    }

    class WithDefault<K, V> implements Map<K, V> {
        private static final long serialVersionUID = 1L;

        private final Map<K, V> underlying;
        private final Function<? super K, ? extends V> defaultFunction;

        WithDefault(Map<K, V> underlying, Function<? super K, ? extends V> defaultFunction) {
            this.underlying = underlying;
            this.defaultFunction = defaultFunction;
        }

        @Override
        public Option<V> get(K key) {
            return underlying.get(key);
        }

        @Override
        public V apply(K key) {
            return WithDefault.this.getOrElse(key, defaultFunction.apply(key));
        }

        @Override
        public PartialFunction<K, V> asPartialFunction() throws IndexOutOfBoundsException {
            return underlying.asPartialFunction();
        }

        @Override
        public <R> Seq<R> collect(PartialFunction<? super Tuple2<K, V>, ? extends R> partialFunction) {
            return underlying.collect(partialFunction);
        }

        @Override
        public <K2, V2> Map<K2, V2> bimap(Function<? super K, ? extends K2> keyMapper, Function<? super V, ? extends V2> valueMapper) {
            return underlying.bimap(keyMapper, valueMapper);
        }

        @Override
        public boolean contains(Tuple2<K, V> element) {
            return underlying.contains(element);
        }

        @Override
        public Tuple2<V, ? extends Map<K, V>> computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            return underlying.computeIfAbsent(key, mappingFunction);
        }

        @Override
        public Tuple2<Option<V>, ? extends Map<K, V>> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return underlying.computeIfPresent(key, remappingFunction);
        }

        @Override
        public boolean containsKey(K key) {
            return underlying.containsKey(key);
        }

        @Override
        public boolean containsValue(V value) {
            return underlying.containsValue(value);
        }

        @Override
        public Map<K, V> filter(BiPredicate<? super K, ? super V> predicate) {
            return underlying.filter(predicate);
        }

        @Override
        public Map<K, V> filterNot(BiPredicate<? super K, ? super V> predicate) {
            return underlying.filterNot(predicate);
        }

        @Override
        public Map<K, V> filterKeys(Predicate<? super K> predicate) {
            return underlying.filterKeys(predicate);
        }

        @Override
        public Map<K, V> filterNotKeys(Predicate<? super K> predicate) {
            return underlying.filterNotKeys(predicate);
        }

        @Override
        public Map<K, V> filterValues(Predicate<? super V> predicate) {
            return underlying.filterValues(predicate);
        }

        @Override
        public Map<K, V> filterNotValues(Predicate<? super V> predicate) {
            return underlying.filterNotValues(predicate);
        }

        @Override
        public <K2, V2> Map<K2, V2> flatMap(BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper) {
            return underlying.flatMap(mapper);
        }

        @Override
        public <U> Seq<U> flatMap(Function<? super Tuple2<K, V>, ? extends Iterable<? extends U>> mapper) {
            return underlying.flatMap(mapper);
        }

        @Override
        public <U> U foldRight(U zero, BiFunction<? super Tuple2<K, V>, ? super U, ? extends U> f) {
            return underlying.foldRight(zero, f);
        }

        @Override
        public void forEach(BiConsumer<K, V> action) {
            underlying.forEach(action);
        }

        @Override
        public V getOrElse(K key, V defaultValue) {
            return underlying.getOrElse(key, defaultValue);
        }

        @Override
        public boolean hasDefiniteSize() {
            return underlying.hasDefiniteSize();
        }

        @Override
        public boolean isTraversableAgain() {
            return underlying.isTraversableAgain();
        }

        @Override
        public Iterator<Tuple2<K, V>> iterator() {
            return underlying.iterator();
        }

        @Override
        public <U> Iterator<U> iterator(BiFunction<K, V, ? extends U> mapper) {
            return underlying.iterator(mapper);
        }

        @Override
        public Set<K> keySet() {
            return underlying.keySet();
        }

        @Override
        public Iterator<K> keysIterator() {
            return underlying.keysIterator();
        }

        @Override
        public int length() {
            return underlying.length();
        }

        @Override
        public Function1<K, Option<V>> lift() {
            return underlying.lift();
        }

        @Override
        public <U> Seq<U> map(Function<? super Tuple2<K, V>, ? extends U> mapper) {
            return underlying.map(mapper);
        }

        @Override
        public <K2, V2> Map<K2, V2> map(BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper) {
            return underlying.map(mapper);
        }

        @Override
        public <K2> Map<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper) {
            return underlying.mapKeys(keyMapper);
        }

        @Override
        public <K2> Map<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper, BiFunction<? super V, ? super V, ? extends V> valueMerge) {
            return underlying.mapKeys(keyMapper, valueMerge);
        }

        @Override
        public <V2> Map<K, V2> mapValues(Function<? super V, ? extends V2> valueMapper) {
            return underlying.mapValues(valueMapper);
        }

        @Override
        public Map<K, V> merge(Map<? extends K, ? extends V> that) {
            return underlying.merge(that);
        }

        @Override
        public <U extends V> Map<K, V> merge(Map<? extends K, U> that, BiFunction<? super V, ? super U, ? extends V> collisionResolution) {
            return underlying.merge(that, collisionResolution);
        }

        @Override
        public Map<K, V> put(K key, V value) {
            return underlying.put(key, value);
        }

        @Override
        public Map<K, V> put(Tuple2<? extends K, ? extends V> entry) {
            return underlying.put(entry);
        }

        @Override
        public <U extends V> Map<K, V> put(K key, U value, BiFunction<? super V, ? super U, ? extends V> merge) {
            return underlying.put(key, value, merge);
        }

        @Override
        public <U extends V> Map<K, V> put(Tuple2<? extends K, U> entry, BiFunction<? super V, ? super U, ? extends V> merge) {
            return underlying.put(entry, merge);
        }

        @Override
        public Map<K, V> remove(K key) {
            return underlying.remove(key);
        }

        @Override
        public Map<K, V> removeAll(Iterable<? extends K> keys) {
            return underlying.removeAll(keys);
        }

        @Override
        public <U> Seq<U> scanLeft(U zero, BiFunction<? super U, ? super Tuple2<K, V>, ? extends U> operation) {
            return underlying.scanLeft(zero, operation);
        }

        @Override
        public <U> Seq<U> scanRight(U zero, BiFunction<? super Tuple2<K, V>, ? super U, ? extends U> operation) {
            return underlying.scanRight(zero, operation);
        }

        @Override
        public int size() {
            return underlying.size();
        }

        @Override
        public java.util.Map<K, V> toJavaMap() {
            return underlying.toJavaMap();
        }

        @Override
        public <U> U transform(Function<? super Map<K, V>, ? extends U> f) {
            return underlying.transform(f);
        }

        @Override
        public Tuple2<Seq<K>, Seq<V>> unzip() {
            return underlying.unzip();
        }

        @Override
        public <T1, T2> Tuple2<Seq<T1>, Seq<T2>> unzip(BiFunction<? super K, ? super V, Tuple2<? extends T1, ? extends T2>> unzipper) {
            return underlying.unzip(unzipper);
        }

        @Override
        public <T1, T2> Tuple2<Seq<T1>, Seq<T2>> unzip(Function<? super Tuple2<K, V>, Tuple2<? extends T1, ? extends T2>> unzipper) {
            return underlying.unzip(unzipper);
        }

        @Override
        public <T1, T2, T3> Tuple3<Seq<T1>, Seq<T2>, Seq<T3>> unzip3(BiFunction<? super K, ? super V, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
            return underlying.unzip3(unzipper);
        }

        @Override
        public <T1, T2, T3> Tuple3<Seq<T1>, Seq<T2>, Seq<T3>> unzip3(Function<? super Tuple2<K, V>, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
            return underlying.unzip3(unzipper);
        }

        @Override
        public Seq<V> values() {
            return underlying.values();
        }

        @Override
        public Iterator<V> valuesIterator() {
            return underlying.valuesIterator();
        }

        @Override
        public Map<K, V> withDefault(Function<? super K, ? extends V> defaultFunction) {
            return underlying.withDefault(defaultFunction);
        }

        @Override
        public Map<K, V> withDefaultValue(V defaultValue) {
            return underlying.withDefaultValue(defaultValue);
        }

        @Override
        public <U> Seq<Tuple2<Tuple2<K, V>, U>> zip(Iterable<? extends U> that) {
            return underlying.zip(that);
        }

        @Override
        public <U, R> Seq<R> zipWith(Iterable<? extends U> that, BiFunction<? super Tuple2<K, V>, ? super U, ? extends R> mapper) {
            return underlying.zipWith(that, mapper);
        }

        @Override
        public <U> Seq<Tuple2<Tuple2<K, V>, U>> zipAll(Iterable<? extends U> that, Tuple2<K, V> thisElem, U thatElem) {
            return underlying.zipAll(that, thisElem, thatElem);
        }

        @Override
        public Seq<Tuple2<Tuple2<K, V>, Integer>> zipWithIndex() {
            return underlying.zipWithIndex();
        }

        @Override
        public <U> Seq<U> zipWithIndex(BiFunction<? super Tuple2<K, V>, ? super Integer, ? extends U> mapper) {
            return underlying.zipWithIndex(mapper);
        }

        @Override
        public Map<K, V> distinct() {
            return underlying.distinct();
        }

        @Override
        public Map<K, V> distinctBy(Comparator<? super Tuple2<K, V>> comparator) {
            return underlying.distinctBy(comparator);
        }

        @Override
        public <U> Map<K, V> distinctBy(Function<? super Tuple2<K, V>, ? extends U> keyExtractor) {
            return underlying.distinctBy(keyExtractor);
        }

        @Override
        public Map<K, V> drop(int n) {
            return underlying.drop(n);
        }

        @Override
        public Map<K, V> dropRight(int n) {
            return underlying.dropRight(n);
        }

        @Override
        public Map<K, V> dropUntil(Predicate<? super Tuple2<K, V>> predicate) {
            return underlying.dropUntil(predicate);
        }

        @Override
        public Map<K, V> dropWhile(Predicate<? super Tuple2<K, V>> predicate) {
            return underlying.dropWhile(predicate);
        }

        @Override
        public Map<K, V> filter(Predicate<? super Tuple2<K, V>> predicate) {
            return underlying.filter(predicate);
        }

        @Override
        public Map<K, V> filterNot(Predicate<? super Tuple2<K, V>> predicate) {
            return underlying.filterNot(predicate);
        }

        @Override
        public Map<K, V> reject(Predicate<? super Tuple2<K, V>> predicate) {
            return underlying.reject(predicate);
        }

        @Override
        public <C> Map<C, ? extends Map<K, V>> groupBy(Function<? super Tuple2<K, V>, ? extends C> classifier) {
            return underlying.groupBy(classifier);
        }

        @Override
        public Iterator<? extends Map<K, V>> grouped(int size) {
            return underlying.grouped(size);
        }

        @Override
        public boolean isDefinedAt(K key) {
            return underlying.isDefinedAt(key);
        }

        @Override
        public boolean isDistinct() {
            return underlying.isDistinct();
        }

        @Override
        public Map<K, V> init() {
            return underlying.init();
        }

        @Override
        public Option<? extends Map<K, V>> initOption() {
            return underlying.initOption();
        }

        @Override
        public Map<K, V> orElse(Iterable<? extends Tuple2<K, V>> other) {
            return underlying.orElse(other);
        }

        @Override
        public Map<K, V> orElse(Supplier<? extends Iterable<? extends Tuple2<K, V>>> supplier) {
            return underlying.orElse(supplier);
        }

        @Override
        public Tuple2<? extends Map<K, V>, ? extends Map<K, V>> partition(Predicate<? super Tuple2<K, V>> predicate) {
            return underlying.partition(predicate);
        }

        @Override
        public Map<K, V> peek(Consumer<? super Tuple2<K, V>> action) {
            return underlying.peek(action);
        }

        @Override
        public Map<K, V> replace(Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
            return underlying.replace(currentElement, newElement);
        }

        @Override
        public Map<K, V> replaceValue(K key, V value) {
            return underlying.replaceValue(key, value);
        }

        @Override
        public Map<K, V> replace(K key, V oldValue, V newValue) {
            return underlying.replace(key, oldValue, newValue);
        }

        @Override
        public Map<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
            return underlying.replaceAll(function);
        }

        @Override
        public Map<K, V> replaceAll(Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
            return underlying.replaceAll(currentElement, newElement);
        }

        @Override
        public Map<K, V> retainAll(Iterable<? extends Tuple2<K, V>> elements) {
            return underlying.retainAll(elements);
        }

        @Override
        public Map<K, V> scan(Tuple2<K, V> zero, BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> operation) {
            return underlying.scan(zero, operation);
        }

        @Override
        public Iterator<? extends Map<K, V>> slideBy(Function<? super Tuple2<K, V>, ?> classifier) {
            return underlying.slideBy(classifier);
        }

        @Override
        public Iterator<? extends Map<K, V>> sliding(int size) {
            return underlying.sliding(size);
        }

        @Override
        public Iterator<? extends Map<K, V>> sliding(int size, int step) {
            return underlying.sliding(size, step);
        }

        @Override
        public Tuple2<? extends Map<K, V>, ? extends Map<K, V>> span(Predicate<? super Tuple2<K, V>> predicate) {
            return underlying.span(predicate);
        }

        @Override
        public Map<K, V> tail() {
            return underlying.tail();
        }

        @Override
        public Option<? extends Map<K, V>> tailOption() {
            return underlying.tailOption();
        }

        @Override
        public Map<K, V> take(int n) {
            return underlying.take(n);
        }

        @Override
        public Map<K, V> takeRight(int n) {
            return underlying.takeRight(n);
        }

        @Override
        public Map<K, V> takeUntil(Predicate<? super Tuple2<K, V>> predicate) {
            return underlying.takeUntil(predicate);
        }

        @Override
        public Map<K, V> takeWhile(Predicate<? super Tuple2<K, V>> predicate) {
            return underlying.takeWhile(predicate);
        }

        @Override
        public <K2> Option<Map<K2, Tuple2<K, V>>> arrangeBy(Function<? super Tuple2<K, V>, ? extends K2> getKey) {
            return underlying.arrangeBy(getKey);
        }

        @Override
        public Option<Double> average() {
            return underlying.average();
        }

        @Override
        public boolean containsAll(Iterable<? extends Tuple2<K, V>> elements) {
            return underlying.containsAll(elements);
        }

        @Override
        public int count(Predicate<? super Tuple2<K, V>> predicate) {
            return underlying.count(predicate);
        }

        @Override
        public boolean existsUnique(Predicate<? super Tuple2<K, V>> predicate) {
            return underlying.existsUnique(predicate);
        }

        @Override
        public Option<Tuple2<K, V>> find(Predicate<? super Tuple2<K, V>> predicate) {
            return underlying.find(predicate);
        }

        @Override
        public Option<Tuple2<K, V>> findLast(Predicate<? super Tuple2<K, V>> predicate) {
            return underlying.findLast(predicate);
        }

        @Override
        public Tuple2<K, V> fold(Tuple2<K, V> zero, BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> combine) {
            return underlying.fold(zero, combine);
        }

        @Override
        public <U> U foldLeft(U zero, BiFunction<? super U, ? super Tuple2<K, V>, ? extends U> combine) {
            return underlying.foldLeft(zero, combine);
        }

        @Override
        public void forEachWithIndex(ObjIntConsumer<? super Tuple2<K, V>> action) {
            underlying.forEachWithIndex(action);
        }

        @Override
        public Tuple2<K, V> get() {
            return underlying.get();
        }

        @Override
        public Tuple2<K, V> head() {
            return underlying.head();
        }

        @Override
        public Option<Tuple2<K, V>> headOption() {
            return underlying.headOption();
        }

        @Override
        public boolean isEmpty() {
            return underlying.isEmpty();
        }

        @Override
        public boolean isOrdered() {
            return underlying.isOrdered();
        }

        @Override
        public boolean isSequential() {
            return underlying.isSequential();
        }

        @Override
        public boolean isSingleValued() {
            return underlying.isSingleValued();
        }

        @Override
        public Tuple2<K, V> last() {
            return underlying.last();
        }

        @Override
        public Option<Tuple2<K, V>> lastOption() {
            return underlying.lastOption();
        }

        @Override
        public Option<Tuple2<K, V>> max() {
            return underlying.max();
        }

        @Override
        public Option<Tuple2<K, V>> maxBy(Comparator<? super Tuple2<K, V>> comparator) {
            return underlying.maxBy(comparator);
        }

        @Override
        public <U extends Comparable<? super U>> Option<Tuple2<K, V>> maxBy(Function<? super Tuple2<K, V>, ? extends U> f) {
            return underlying.maxBy(f);
        }

        @Override
        public Option<Tuple2<K, V>> min() {
            return underlying.min();
        }

        @Override
        public Option<Tuple2<K, V>> minBy(Comparator<? super Tuple2<K, V>> comparator) {
            return underlying.minBy(comparator);
        }

        @Override
        public <U extends Comparable<? super U>> Option<Tuple2<K, V>> minBy(Function<? super Tuple2<K, V>, ? extends U> f) {
            return underlying.minBy(f);
        }

        @Override
        public CharSeq mkCharSeq() {
            return underlying.mkCharSeq();
        }

        @Override
        public CharSeq mkCharSeq(CharSequence delimiter) {
            return underlying.mkCharSeq(delimiter);
        }

        @Override
        public CharSeq mkCharSeq(CharSequence prefix, CharSequence delimiter, CharSequence suffix) {
            return underlying.mkCharSeq(prefix, delimiter, suffix);
        }

        @Override
        public String mkString() {
            return underlying.mkString();
        }

        @Override
        public String mkString(CharSequence delimiter) {
            return underlying.mkString(delimiter);
        }

        @Override
        public String mkString(CharSequence prefix, CharSequence delimiter, CharSequence suffix) {
            return underlying.mkString(prefix, delimiter, suffix);
        }

        @Override
        public boolean nonEmpty() {
            return underlying.nonEmpty();
        }

        @Override
        public Number product() {
            return underlying.product();
        }

        @Override
        public Tuple2<K, V> reduce(BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> op) {
            return underlying.reduce(op);
        }

        @Override
        public Option<Tuple2<K, V>> reduceOption(BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> op) {
            return underlying.reduceOption(op);
        }

        @Override
        public Tuple2<K, V> reduceLeft(BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> op) {
            return underlying.reduceLeft(op);
        }

        @Override
        public Option<Tuple2<K, V>> reduceLeftOption(BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> op) {
            return underlying.reduceLeftOption(op);
        }

        @Override
        public Tuple2<K, V> reduceRight(BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> op) {
            return underlying.reduceRight(op);
        }

        @Override
        public Option<Tuple2<K, V>> reduceRightOption(BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> op) {
            return underlying.reduceRightOption(op);
        }

        @Override
        public Tuple2<K, V> single() {
            return underlying.single();
        }

        @Override
        public Option<Tuple2<K, V>> singleOption() {
            return underlying.singleOption();
        }

        @Override
        public Spliterator<Tuple2<K, V>> spliterator() {
            return underlying.spliterator();
        }

        @Override
        public Number sum() {
            return underlying.sum();
        }

        @Override
        public void forEach(Consumer<? super Tuple2<K, V>> action) {
            underlying.forEach(action);
        }

        @Override
        public <R, A> R collect(Collector<? super Tuple2<K, V>, A, R> collector) {
            return underlying.collect(collector);
        }

        @Override
        public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super Tuple2<K, V>> accumulator, BiConsumer<R, R> combiner) {
            return underlying.collect(supplier, accumulator, combiner);
        }

        @Override
        public <U> boolean corresponds(Iterable<U> that, BiPredicate<? super Tuple2<K, V>, ? super U> predicate) {
            return underlying.corresponds(that, predicate);
        }

        @Override
        public boolean eq(Object o) {
            return underlying.eq(o);
        }

        @Override
        public boolean exists(Predicate<? super Tuple2<K, V>> predicate) {
            return underlying.exists(predicate);
        }

        @Override
        public boolean forAll(Predicate<? super Tuple2<K, V>> predicate) {
            return underlying.forAll(predicate);
        }

        @Override
        public Tuple2<K, V> getOrElse(Tuple2<K, V> other) {
            return underlying.getOrElse(other);
        }

        @Override
        public Tuple2<K, V> getOrElse(Supplier<? extends Tuple2<K, V>> supplier) {
            return underlying.getOrElse(supplier);
        }

        @Override
        public <X extends Throwable> Tuple2<K, V> getOrElseThrow(Supplier<X> supplier) throws X {
            return underlying.getOrElseThrow(supplier);
        }

        @Override
        public Tuple2<K, V> getOrElseTry(CheckedFunction0<? extends Tuple2<K, V>> supplier) {
            return underlying.getOrElseTry(supplier);
        }

        @Override
        public Tuple2<K, V> getOrNull() {
            return underlying.getOrNull();
        }

        @Override
        public boolean isAsync() {
            return underlying.isAsync();
        }

        @Override
        public boolean isLazy() {
            return underlying.isLazy();
        }

        @Override
        public String stringPrefix() {
            return underlying.stringPrefix();
        }

        @Override
        public void out(PrintStream out) {
            underlying.out(out);
        }

        @Override
        public void out(PrintWriter writer) {
            underlying.out(writer);
        }

        @Override
        public void stderr() {
            underlying.stderr();
        }

        @Override
        public void stdout() {
            underlying.stdout();
        }

        @Override
        public Array<Tuple2<K, V>> toArray() {
            return underlying.toArray();
        }

        @Override
        public CharSeq toCharSeq() {
            return underlying.toCharSeq();
        }

        @Override
        public CompletableFuture<Tuple2<K, V>> toCompletableFuture() {
            return underlying.toCompletableFuture();
        }

        @Override
        @Deprecated
        public <U> Validation<Tuple2<K, V>, U> toInvalid(U value) {
            return underlying.toInvalid(value);
        }

        @Override
        @Deprecated
        public <U> Validation<Tuple2<K, V>, U> toInvalid(Supplier<? extends U> valueSupplier) {
            return underlying.toInvalid(valueSupplier);
        }

        @Override
        public Object[] toJavaArray() {
            return underlying.toJavaArray();
        }

        @Override
        @Deprecated
        public Tuple2<K, V>[] toJavaArray(Class<Tuple2<K, V>> componentType) {
            return underlying.toJavaArray(componentType);
        }

        @Override
        public Tuple2<K, V>[] toJavaArray(IntFunction<Tuple2<K, V>[]> arrayFactory) {
            return underlying.toJavaArray(arrayFactory);
        }

        @Override
        public <C extends Collection<Tuple2<K, V>>> C toJavaCollection(Function<Integer, C> factory) {
            return underlying.toJavaCollection(factory);
        }

        @Override
        public List<Tuple2<K, V>> toJavaList() {
            return underlying.toJavaList();
        }

        @Override
        public <LIST extends List<Tuple2<K, V>>> LIST toJavaList(Function<Integer, LIST> factory) {
            return underlying.toJavaList(factory);
        }

        public <K2, V2> java.util.Map<K2, V2> toJavaMap(Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
            return underlying.toJavaMap(f);
        }

        public <K2, V2, MAP extends java.util.Map<K2, V2>> MAP toJavaMap(Supplier<MAP> factory, Function<? super Tuple2<K, V>, ? extends K2> keyMapper, Function<? super Tuple2<K, V>, ? extends V2> valueMapper) {
            return underlying.toJavaMap(factory, keyMapper, valueMapper);
        }

        public <K2, V2, MAP extends java.util.Map<K2, V2>> MAP toJavaMap(Supplier<MAP> factory, Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
            return underlying.toJavaMap(factory, f);
        }

        @Override
        public Optional<Tuple2<K, V>> toJavaOptional() {
            return underlying.toJavaOptional();
        }

        @Override
        public java.util.Set<Tuple2<K, V>> toJavaSet() {
            return underlying.toJavaSet();
        }

        @Override
        public <SET extends java.util.Set<Tuple2<K, V>>> SET toJavaSet(Function<Integer, SET> factory) {
            return underlying.toJavaSet(factory);
        }

        @Override
        public java.util.stream.Stream<Tuple2<K, V>> toJavaStream() {
            return underlying.toJavaStream();
        }

        @Override
        public java.util.stream.Stream<Tuple2<K, V>> toJavaParallelStream() {
            return underlying.toJavaParallelStream();
        }

        @Override
        @Deprecated
        public <R> Either<Tuple2<K, V>, R> toLeft(R right) {
            return underlying.toLeft(right);
        }

        @Override
        @Deprecated
        public <R> Either<Tuple2<K, V>, R> toLeft(Supplier<? extends R> right) {
            return underlying.toLeft(right);
        }

        @Override
        public io.vavr.collection.List<Tuple2<K, V>> toList() {
            return underlying.toList();
        }

        public <K2, V2> Map<K2, V2> toMap(Function<? super Tuple2<K, V>, ? extends K2> keyMapper, Function<? super Tuple2<K, V>, ? extends V2> valueMapper) {
            return underlying.toMap(keyMapper, valueMapper);
        }

        public <K2, V2> Map<K2, V2> toMap(Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
            return underlying.toMap(f);
        }

        public <K2, V2> Map<K2, V2> toLinkedMap(Function<? super Tuple2<K, V>, ? extends K2> keyMapper, Function<? super Tuple2<K, V>, ? extends V2> valueMapper) {
            return underlying.toLinkedMap(keyMapper, valueMapper);
        }

        public <K2, V2> Map<K2, V2> toLinkedMap(Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
            return underlying.toLinkedMap(f);
        }

        public <K2 extends Comparable<? super K2>, V2> SortedMap<K2, V2> toSortedMap(Function<? super Tuple2<K, V>, ? extends K2> keyMapper, Function<? super Tuple2<K, V>, ? extends V2> valueMapper) {
            return underlying.toSortedMap(keyMapper, valueMapper);
        }

        public <K2 extends Comparable<? super K2>, V2> SortedMap<K2, V2> toSortedMap(Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
            return underlying.toSortedMap(f);
        }

        public <K2, V2> SortedMap<K2, V2> toSortedMap(Comparator<? super K2> comparator, Function<? super Tuple2<K, V>, ? extends K2> keyMapper, Function<? super Tuple2<K, V>, ? extends V2> valueMapper) {
            return underlying.toSortedMap(comparator, keyMapper, valueMapper);
        }

        public <K2, V2> SortedMap<K2, V2> toSortedMap(Comparator<? super K2> comparator, Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
            return underlying.toSortedMap(comparator, f);
        }

        @Override
        public Option<Tuple2<K, V>> toOption() {
            return underlying.toOption();
        }

        @Override
        public <L> Either<L, Tuple2<K, V>> toEither(L left) {
            return underlying.toEither(left);
        }

        @Override
        public <L> Either<L, Tuple2<K, V>> toEither(Supplier<? extends L> leftSupplier) {
            return underlying.toEither(leftSupplier);
        }

        @Override
        public <E> Validation<E, Tuple2<K, V>> toValidation(E invalid) {
            return underlying.toValidation(invalid);
        }

        @Override
        public <E> Validation<E, Tuple2<K, V>> toValidation(Supplier<? extends E> invalidSupplier) {
            return underlying.toValidation(invalidSupplier);
        }

        @Override
        public Queue<Tuple2<K, V>> toQueue() {
            return underlying.toQueue();
        }

        @Override
        public PriorityQueue<Tuple2<K, V>> toPriorityQueue() {
            return underlying.toPriorityQueue();
        }

        @Override
        public PriorityQueue<Tuple2<K, V>> toPriorityQueue(Comparator<? super Tuple2<K, V>> comparator) {
            return underlying.toPriorityQueue(comparator);
        }

        @Override
        @Deprecated
        public <L> Either<L, Tuple2<K, V>> toRight(L left) {
            return underlying.toRight(left);
        }

        @Override
        @Deprecated
        public <L> Either<L, Tuple2<K, V>> toRight(Supplier<? extends L> left) {
            return underlying.toRight(left);
        }

        @Override
        public Set<Tuple2<K, V>> toSet() {
            return underlying.toSet();
        }

        @Override
        public Set<Tuple2<K, V>> toLinkedSet() {
            return underlying.toLinkedSet();
        }

        @Override
        public SortedSet<Tuple2<K, V>> toSortedSet() throws ClassCastException {
            return underlying.toSortedSet();
        }

        @Override
        public SortedSet<Tuple2<K, V>> toSortedSet(Comparator<? super Tuple2<K, V>> comparator) {
            return underlying.toSortedSet(comparator);
        }

        @Override
        public Stream<Tuple2<K, V>> toStream() {
            return underlying.toStream();
        }

        @Override
        public Try<Tuple2<K, V>> toTry() {
            return underlying.toTry();
        }

        @Override
        public Try<Tuple2<K, V>> toTry(Supplier<? extends Throwable> ifEmpty) {
            return underlying.toTry(ifEmpty);
        }

        @Override
        public Tree<Tuple2<K, V>> toTree() {
            return underlying.toTree();
        }

        @Override
        public <ID> io.vavr.collection.List<Tree.Node<Tuple2<K, V>>> toTree(Function<? super Tuple2<K, V>, ? extends ID> idMapper, Function<? super Tuple2<K, V>, ? extends ID> parentMapper) {
            return underlying.toTree(idMapper, parentMapper);
        }

        @Override
        @Deprecated
        public <E> Validation<E, Tuple2<K, V>> toValid(E error) {
            return underlying.toValid(error);
        }

        @Override
        @Deprecated
        public <E> Validation<E, Tuple2<K, V>> toValid(Supplier<? extends E> errorSupplier) {
            return underlying.toValid(errorSupplier);
        }

        @Override
        public Vector<Tuple2<K, V>> toVector() {
            return underlying.toVector();
        }

        @Override
        public int arity() {
            return underlying.arity();
        }

        @Override
        public Function1<K, V> curried() {
            return underlying.curried();
        }

        @Override
        public Function1<Tuple1<K>, V> tupled() {
            return underlying.tupled();
        }

        @Override
        public Function1<K, V> reversed() {
            return underlying.reversed();
        }

        @Override
        public Function1<K, V> memoized() {
            return underlying.memoized();
        }

        @Override
        public boolean isMemoized() {
            return underlying.isMemoized();
        }

        @Override
        public PartialFunction<K, V> partial(Predicate<? super K> isDefinedAt) {
            return underlying.partial(isDefinedAt);
        }

        @Override
        public <V1> Function1<K, V1> andThen(Function<? super V, ? extends V1> after) {
            return underlying.andThen(after);
        }

        @Override
        public <V1> Function1<V1, V> compose(Function<? super V1, ? extends K> before) {
            return underlying.compose(before);
        }

    }

    /**
     * Turns this map from a partial function into a total function that
     * returns a value computed by defaultFunction for all keys
     * absent from the map.
     *
     * @param defaultFunction function to evaluate for all keys not present in the map
     * @return a total function from K to T
     */
    default Map<K, V> withDefault(Function<? super K, ? extends V> defaultFunction) {
        return new WithDefault<>(this, defaultFunction);
    }

    /**
     * Turns this map from a partial function into a total function that
     * returns defaultValue for all keys absent from the map.
     *
     * @param defaultValue default value to return for all keys not present in the map
     * @return a total function from K to T
     */
    default Map<K, V> withDefaultValue(V defaultValue) {
        return new WithDefault<K, V>(this, $ -> defaultValue);
    }

    @Override
    default <U> Seq<Tuple2<Tuple2<K, V>, U>> zip(Iterable<? extends U> that) {
        return zipWith(that, Tuple::of);
    }

    @Override
    default <U, R> Seq<R> zipWith(@NonNull Iterable<? extends U> that,
                                  @NonNull BiFunction<? super Tuple2<K, V>, ? super U, ? extends R> mapper) {
        return Stream.ofAll(iterator().zipWith(that, mapper));
    }

    @Override
    default <U> Seq<Tuple2<Tuple2<K, V>, U>> zipAll(@NonNull Iterable<? extends U> that, Tuple2<K, V> thisElem, U thatElem) {
        return Stream.ofAll(iterator().zipAll(that, thisElem, thatElem));
    }

    @Override
    default Seq<Tuple2<Tuple2<K, V>, Integer>> zipWithIndex() {
        return zipWithIndex(Tuple::of);
    }

    @Override
    default <U> Seq<U> zipWithIndex(@NonNull BiFunction<? super Tuple2<K, V>, ? super Integer, ? extends U> mapper) {
        return Stream.ofAll(iterator().zipWithIndex(mapper));
    }

    // -- Adjusted return types of Traversable methods

    @Override
    Map<K, V> distinct();

    @Override
    Map<K, V> distinctBy(Comparator<? super Tuple2<K, V>> comparator);

    @Override
    <U> Map<K, V> distinctBy(Function<? super Tuple2<K, V>, ? extends U> keyExtractor);

    @Override
    Map<K, V> drop(int n);

    @Override
    Map<K, V> dropRight(int n);

    @Override
    Map<K, V> dropUntil(Predicate<? super Tuple2<K, V>> predicate);

    @Override
    Map<K, V> dropWhile(Predicate<? super Tuple2<K, V>> predicate);

    @Override
    Map<K, V> filter(Predicate<? super Tuple2<K, V>> predicate);

    @Override
    Map<K, V> filterNot(Predicate<? super Tuple2<K, V>> predicate);

    @Deprecated
    @Override
    Map<K, V> reject(Predicate<? super Tuple2<K, V>> predicate);

    @Override
    <C> Map<C, ? extends Map<K, V>> groupBy(Function<? super Tuple2<K, V>, ? extends C> classifier);

    @Override
    io.vavr.collection.Iterator<? extends Map<K, V>> grouped(int size);

    @Override
    default boolean isDefinedAt(K key) {
        return containsKey(key);
    }

    @Override
    default boolean isDistinct() {
        return true;
    }

    @Override
    Map<K, V> init();

    @Override
    Option<? extends Map<K, V>> initOption();

    @Override
    Map<K, V> orElse(Iterable<? extends Tuple2<K, V>> other);

    @Override
    Map<K, V> orElse(Supplier<? extends Iterable<? extends Tuple2<K, V>>> supplier);

    @Override
    Tuple2<? extends Map<K, V>, ? extends Map<K, V>> partition(Predicate<? super Tuple2<K, V>> predicate);

    @Override
    Map<K, V> peek(Consumer<? super Tuple2<K, V>> action);

    @Override
    Map<K, V> replace(Tuple2<K, V> currentElement, Tuple2<K, V> newElement);

    /**
     * Replaces the entry for the specified key only if it is currently mapped to some value.
     *
     * @param key   the key of the element to be substituted.
     * @param value the new value to be associated with the key
     * @return a new map containing key mapped to value if key was contained before. The old map otherwise.
     */
    Map<K, V> replaceValue(K key, V value);

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified value.
     *
     * @param key      the key of the element to be substituted.
     * @param oldValue the expected current value that the key is currently mapped to
     * @param newValue the new value to be associated with the key
     * @return a new map containing key mapped to newValue if key was contained before and oldValue matched. The old map otherwise.
     */
    Map<K, V> replace(K key, V oldValue, V newValue);

    /**
     * Replaces each entry's value with the result of invoking the given function on that entry until all entries have been processed or the function throws an exception.
     *
     * @param function function transforming key and current value to a new value
     * @return a new map with the same keySet but transformed values.
     */
    Map<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> function);

    @Override
    Map<K, V> replaceAll(Tuple2<K, V> currentElement, Tuple2<K, V> newElement);

    @Override
    Map<K, V> retainAll(Iterable<? extends Tuple2<K, V>> elements);

    @Override
    Map<K, V> scan(Tuple2<K, V> zero,
            BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> operation);

    @Override
    io.vavr.collection.Iterator<? extends Map<K, V>> slideBy(Function<? super Tuple2<K, V>, ?> classifier);

    @Override
    io.vavr.collection.Iterator<? extends Map<K, V>> sliding(int size);

    @Override
    io.vavr.collection.Iterator<? extends Map<K, V>> sliding(int size, int step);

    @Override
    Tuple2<? extends Map<K, V>, ? extends Map<K, V>> span(Predicate<? super Tuple2<K, V>> predicate);

    @Override
    Map<K, V> tail();

    @Override
    Option<? extends Map<K, V>> tailOption();

    @Override
    Map<K, V> take(int n);

    @Override
    Map<K, V> takeRight(int n);

    @Override
    Map<K, V> takeUntil(Predicate<? super Tuple2<K, V>> predicate);

    @Override
    Map<K, V> takeWhile(Predicate<? super Tuple2<K, V>> predicate);

}
