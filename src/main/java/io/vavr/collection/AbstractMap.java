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

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import lombok.NonNull;

import java.util.Comparator;
import java.util.function.*;

import static io.vavr.API.Tuple;

/**
 * INTERNAL: Common {@code Map} functions (not intended to be public).
 */
abstract class AbstractMap<K, V, M extends Map<K, V>> implements Map<K, V> {
    private static final long serialVersionUID = 1L;

    abstract Function<Iterable<? extends Tuple2<K, V>>, M> ofEntriesInstance();

    abstract Supplier<M> emptyInstance();

    @SuppressWarnings("unchecked")
    protected M self() {
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public Tuple2<V, M> computeIfAbsent(K key, @NonNull Function<? super K, ? extends V> ingFunction) {
        final Option<V> value = get(key);
        if (value.isDefined()) {
            return Tuple.of(value.get(), self());
        } else {
            final V newValue = ingFunction.apply(key);
            final M newMap = (M) put(key, newValue);
            return Tuple.of(newValue, newMap);
        }
    }

    @SuppressWarnings("unchecked")
    public Tuple2<Option<V>, M> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> reingFunction) {
        final Option<V> value = get(key);
        if (value.isDefined()) {
            final V newValue = reingFunction.apply(key, value.get());
            final M newMap = (M) put(key, newValue);
            return Tuple.of(Option.of(newValue), newMap);
        } else {
            return Tuple.of(Option.none(), self());
        }
    }

    @Override
    public M distinct() {
        return self();
    }

    @Override
    public M distinctBy(@NonNull Comparator<? super Tuple2<K, V>> comparator) {
        return ofEntriesInstance().apply(iterator().distinctBy(comparator));
    }

    @Override
    public <U> M distinctBy(@NonNull Function<? super Tuple2<K, V>, ? extends U> keyExtractor) {
        return ofEntriesInstance().apply(iterator().distinctBy(keyExtractor));
    }

    @Override
    public M drop(int n) {
        if (n <= 0) {
            return self();
        } else if (n >= size()) {
            return emptyInstance().get();
        } else {
            return ofEntriesInstance().apply(iterator().drop(n));
        }
    }

    @Override
    public M dropRight(int n) {
        if (n <= 0) {
            return self();
        } else if (n >= size()) {
            return emptyInstance().get();
        } else {
            return ofEntriesInstance().apply(iterator().dropRight(n));
        }
    }

    @Override
    public M dropUntil(@NonNull Predicate<? super Tuple2<K, V>> predicate) {
        return dropWhile(predicate.negate());
    }

    @Override
    public M dropWhile(@NonNull Predicate<? super Tuple2<K, V>> predicate) {
        return ofEntriesInstance().apply(iterator().dropWhile(predicate));
    }

    @Override
    public M filter(@NonNull BiPredicate<? super K, ? super V> predicate) {
        return filter(t -> predicate.test(t._1, t._2));
    }

    @Override
    public M filter(@NonNull Predicate<? super Tuple2<K, V>> predicate) {
        return ofEntriesInstance().apply(iterator().filter(predicate));
    }

    @Override
    public M filterKeys(@NonNull Predicate<? super K> predicate) {
        return filter(t -> predicate.test(t._1));
    }

    @Override
    public M filterValues(@NonNull Predicate<? super V> predicate) {
        return filter(t -> predicate.test(t._2));
    }

    @Override
    public <C> Map<C, M> groupBy(Function<? super Tuple2<K, V>, ? extends C> classifier) {
        return Collections.groupBy(self(), classifier, ofEntriesInstance());
    }

    @Override
    public Iterator<M> grouped(int size) {
        return sliding(size, size);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Option<M> initOption() {
        return isEmpty() ? Option.none() : Option.some((M) init());
    }

    @Override
    public M merge(@NonNull Map<? extends K, ? extends V> that) {
        if (isEmpty()) {
            return ofEntriesInstance().apply(Map.narrow(that));
        } else if (that.isEmpty()) {
            return self();
        } else {
            return that.foldLeft(self(), (result, entry) -> !result.containsKey(entry._1) ? put(entry) : result);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U extends V> M merge(@NonNull Map<? extends K, U> that, @NonNull BiFunction<? super V, ? super U, ? extends V> collisionResolution) {
        if (isEmpty()) {
            return ofEntriesInstance().apply(Map.narrow(that));
        } else if (that.isEmpty()) {
            return self();
        } else {
            return that.foldLeft(self(), (result, entry) -> {
                final K key = entry._1;
                final U value = entry._2;
                final V newValue = result.get(key).map(v -> (V) collisionResolution.apply(v, value)).getOrElse(value);
                return (M) result.put(key, newValue);
            });
        }
    }

    @SuppressWarnings("unchecked")
    public <T> M ofStream(@NonNull java.util.stream.Stream<? extends T> stream,
                          @NonNull Function<? super T, ? extends K> keyMapper,
                          @NonNull Function<? super T, ? extends V> valueMapper) {
        return Stream.ofAll(stream).foldLeft(self(), (m, el) -> (M) m.put(keyMapper.apply(el), valueMapper.apply(el)));
    }

    @SuppressWarnings("unchecked")
    public <T> M ofStream(@NonNull java.util.stream.Stream<? extends T> stream,
                          @NonNull Function<? super T, Tuple2<? extends K, ? extends V>> entryMapper) {
        return Stream.ofAll(stream).foldLeft(self(), (m, el) -> (M) m.put(entryMapper.apply(el)));
    }

    @Override
    public Tuple2<M, M> partition(@NonNull Predicate<? super Tuple2<K, V>> predicate) {
        final Tuple2<Iterator<Tuple2<K, V>>, Iterator<Tuple2<K, V>>> p = iterator().partition(predicate);
        return Tuple.of(ofEntriesInstance().apply(p._1), ofEntriesInstance().apply(p._2));
    }

    @Override
    public M peek(@NonNull Consumer<? super Tuple2<K, V>> action) {
        if (!isEmpty()) {
            action.accept(head());
        }
        return self();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U extends V> M put(K key, U value,
                               @NonNull BiFunction<? super V, ? super U, ? extends V> merge) {
        final Option<V> currentValue = get(key);
        if (currentValue.isEmpty()) {
            return (M) put(key, value);
        } else {
            return (M) put(key, merge.apply(currentValue.get(), value));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public M put(@NonNull Tuple2<? extends K, ? extends V> entry) {
        return (M) put(entry._1, entry._2);
    }

    @Override
    public <U extends V> M put(Tuple2<? extends K, U> entry,
                               @NonNull BiFunction<? super V, ? super U, ? extends V> merge) {
        final Option<V> currentValue = get(entry._1);
        if (currentValue.isEmpty()) {
            return put(entry);
        } else {
            return put(entry.map2(value -> merge.apply(currentValue.get(), value)));
        }
    }

    @Override
    public M filterNot(@NonNull Predicate<? super Tuple2<K, V>> predicate) {
        return filter(predicate.negate());
    }

    @Override
    public M filterNot(@NonNull BiPredicate<? super K, ? super V> predicate) {
        return filter(predicate.negate());
    }

    @Override
    public M filterNotKeys(@NonNull Predicate<? super K> predicate) {
        return filterKeys(predicate.negate());
    }

    @Override
    public M filterNotValues(@NonNull Predicate<? super V> predicate) {
        return filterValues(predicate.negate());
    }

    @Deprecated
    @Override
    public M reject(Predicate<? super Tuple2<K, V>> predicate) {
        return filterNot(predicate);
    }

    @SuppressWarnings("unchecked")
    @Override
    public M replace(K key, V oldValue, V newValue) {
        return contains(Tuple(key, oldValue)) ? (M) put(key, newValue) : self();
    }

    @SuppressWarnings("unchecked")
    @Override
    public M replace(@NonNull Tuple2<K, V> currentElement, @NonNull Tuple2<K, V> newElement) {
        return containsKey(currentElement._1) ? (M) remove(currentElement._1).put(newElement) : self();
    }

    @SuppressWarnings("unchecked")
    @Override
    public M replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        return (M) map((k, v) -> Tuple(k, function.apply(k, v)));
    }

    @Override
    public M replaceAll(Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
        return replace(currentElement, newElement);
    }

    @SuppressWarnings("unchecked")
    @Override
    public M replaceValue(K key, V value) {
        return containsKey(key) ? (M) put(key, value) : self();
    }

    @Override
    public M scan(Tuple2<K, V> zero,
                  BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> operation) {
        return Collections.scanLeft(self(), zero, operation, ofEntriesInstance()::apply);
    }

    @SuppressWarnings("unchecked")
    public M scan(Tuple2<K, V> zero,
                  BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> operation,
                  Function<Iterator<Tuple2<K, V>>, Traversable<Tuple2<K, V>>> finisher) {
        return (M) Collections.scanLeft(self(), zero, operation, finisher);
    }

    @Override
    public Iterator<M> slideBy(Function<? super Tuple2<K, V>, ?> classifier) {
        return iterator().slideBy(classifier).map(ofEntriesInstance());
    }

    @Override
    public Iterator<M> sliding(int size) {
        return sliding(size, 1);
    }

    @Override
    public Iterator<M> sliding(int size, int step) {
        return iterator().sliding(size, step).map(ofEntriesInstance());
    }

    @Override
    public Tuple2<M, M> span(@NonNull Predicate<? super Tuple2<K, V>> predicate) {
        final Tuple2<Iterator<Tuple2<K, V>>, Iterator<Tuple2<K, V>>> t = iterator().span(predicate);
        return Tuple.of(ofEntriesInstance().apply(t._1), ofEntriesInstance().apply(t._2));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Option<M> tailOption() {
        return isEmpty() ? Option.none() : Option.some((M) tail());
    }

    @Override
    public M take(int n) {
        if (n >= size()) {
            return self();
        } else {
            return ofEntriesInstance().apply(iterator().take(n));
        }
    }

    @Override
    public M takeRight(int n) {
        if (n >= size()) {
            return self();
        } else {
            return ofEntriesInstance().apply(iterator().takeRight(n));
        }
    }

    @Override
    public M takeUntil(@NonNull Predicate<? super Tuple2<K, V>> predicate) {
        return takeWhile(predicate.negate());
    }

    @Override
    public M takeWhile(@NonNull Predicate<? super Tuple2<K, V>> predicate) {
        final M taken = ofEntriesInstance().apply(iterator().takeWhile(predicate));
        return taken.size() == size() ? self() : taken;
    }

}
