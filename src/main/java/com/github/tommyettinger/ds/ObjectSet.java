/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
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
 ******************************************************************************/

package com.github.tommyettinger.ds;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * An unordered set where the keys are objects. Null keys are not allowed. No allocation is done except when growing the table
 * size.
 * <p>
 * This class performs fast contains and remove (typically O(1), worst case O(n) but that is rare in practice). Add may be
 * slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce collisions and the need to resize. Load factors
 * greater than 0.91 greatly increase the chances to resize to the next higher POT size.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with OrderedSet and
 * OrderedMap.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal. Hashcodes are rehashed using Fibonacci
 * hashing, instead of the more common power-of-two mask, to better distribute poor hashCodes (see <a href=
 * "https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/">Malte
 * Skarupke's blog post</a>). Linear probing continues to work even when all hashCodes collide, just more slowly.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class ObjectSet<T> implements Iterable<T>, Set<T>, Serializable {
	private static final long serialVersionUID = 0L;

	public int size;

	protected T[] keyTable;

	protected float loadFactor;
	protected int threshold;

	protected int shift;

	/**
	 * A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1.
	 */
	protected int mask;
	protected ObjectSetIterator<T> iterator1, iterator2;

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of 0.8.
	 */
	public ObjectSet () {
		this(51, 0.8f);
	}

	/**
	 * Creates a new set with a load factor of 0.8.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public ObjectSet (int initialCapacity) {
		this(initialCapacity, 0.8f);
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public ObjectSet (int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f)
			throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
		this.loadFactor = loadFactor;

		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int)(tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);

		keyTable = (T[])new Object[tableSize];
	}

	/**
	 * Creates a new set identical to the specified set.
	 */
	public ObjectSet (ObjectSet<? extends T> set) {
		this((int)(set.keyTable.length * set.loadFactor), set.loadFactor);
		System.arraycopy(set.keyTable, 0, keyTable, 0, set.keyTable.length);
		size = set.size;
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 */
	public ObjectSet (Collection<? extends T> coll) {
		this(coll.size());
		addAll(coll);
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}.
	 *
	 * @param item a non-null Object; its hashCode() method should be used by most implementations.
	 */
	protected int place (Object item) {
		final int h = item.hashCode() * 0x9E377;
		return (h ^ h >>> shift) & mask;
	}

	/**
	 * Returns the index of the key if already present, else {@code ~index} for the next empty index. This can be overridden
	 * to compare for equality differently than {@link Object#equals(Object)}.
	 *
	 * @param key a non-null Object that should probably be a T
	 */
	protected int locateKey (Object key) {
		T[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			T other = keyTable[i];
			if (other == null)
				return ~i; // Empty space is available.
			if (other.equals(key))
				return i; // Same key was found.
		}
	}

	/**
	 * Returns true if the key was not already in the set. If this set already contains the key, the call leaves the set unchanged
	 * and returns false.
	 */
	@Override
	public boolean add (T key) {
		int i = locateKey(key);
		if (i >= 0)
			return false; // Existing key was found.
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		if (++size >= threshold)
			resize(keyTable.length << 1);
		return true;
	}

	@Override
	public boolean containsAll (Collection<?> c) {
		for (Object o : c) {
			if (!contains(o))
				return false;
		}
		return true;
	}

	@Override
	public boolean addAll (Collection<? extends T> coll) {
		final int length = coll.size();
		ensureCapacity(length);
		int oldSize = size;
		for (T t : coll)
			add(t);
		return oldSize != size;

	}

	@Override
	public boolean retainAll (Collection<?> c) {
		boolean modified = false;
		for (Object o : this) {
			if (!c.contains(o))
				modified |= remove(o);

		}
		return modified;
	}

	@Override
	public boolean removeAll (Collection<?> c) {
		boolean modified = false;
		for (Object o : c) {
			modified |= remove(o);
		}
		return modified;
	}

	public boolean addAll (T[] array) {
		return addAll(array, 0, array.length);
	}

	public boolean addAll (T[] array, int offset, int length) {
		ensureCapacity(length);
		int oldSize = size;
		for (int i = offset, n = i + length; i < n; i++)
			add(array[i]);
		return oldSize != size;
	}

	public void addAll (ObjectSet<T> set) {
		ensureCapacity(set.size);
		T[] keyTable = set.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			T key = keyTable[i];
			if (key != null)
				add(key);
		}
	}

	/**
	 * Skips checks for existing keys, doesn't increment size.
	 */
	private void addResize (T key) {
		T[] keyTable = this.keyTable;
		for (int i = place(key); ; i = (i + 1) & mask) {
			if (keyTable[i] == null) {
				keyTable[i] = key;
				return;
			}
		}
	}

	/**
	 * Returns true if the key was removed.
	 */
	@Override
	public boolean remove (Object key) {
		int i = locateKey(key);
		if (i < 0)
			return false;
		T[] keyTable = this.keyTable;
		int mask = this.mask, next = i + 1 & mask;
		while ((key = keyTable[next]) != null) {
			int placement = place(key);
			if ((next - placement & mask) > (i - placement & mask)) {
				keyTable[i] = (T)key;
				i = next;
			}
			next = next + 1 & mask;
		}
		keyTable[i] = null;
		size--;
		return true;
	}

	/**
	 * Returns true if the set has one or more items.
	 */
	public boolean notEmpty () {
		return size > 0;
	}

	/**
	 * Returns the number of elements in this set (its cardinality).  If this
	 * set contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
	 * <tt>Integer.MAX_VALUE</tt>.
	 *
	 * @return the number of elements in this set (its cardinality)
	 */
	@Override
	public int size () {
		return size;
	}

	/**
	 * Returns true if the set is empty.
	 */
	@Override
	public boolean isEmpty () {
		return size == 0;
	}

	/**
	 * Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less,
	 * nothing is done. If the set contains more items than the specified capacity, the next highest power of two capacity is used
	 * instead.
	 */
	public void shrink (int maximumCapacity) {
		if (maximumCapacity < 0)
			throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
		int tableSize = tableSize(maximumCapacity, loadFactor);
		if (keyTable.length > tableSize)
			resize(tableSize);
	}

	/**
	 * Clears the set and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger.
	 * The reduction is done by allocating new arrays, though for large arrays this can be faster than clearing the existing
	 * array.
	 */
	public void clear (int maximumCapacity) {
		int tableSize = tableSize(maximumCapacity, loadFactor);
		if (keyTable.length <= tableSize) {
			clear();
			return;
		}
		size = 0;
		resize(tableSize);
	}

	/**
	 * Clears the set, leaving the backing arrays at the current capacity. When the capacity is high and the population is low,
	 * iteration can be unnecessarily slow. {@link #clear(int)} can be used to reduce the capacity.
	 */
	@Override
	public void clear () {
		if (size == 0)
			return;
		size = 0;
		Arrays.fill(keyTable, null);
	}

	@Override
	public boolean contains (Object key) {
		return locateKey(key) >= 0;
	}

	public @Nullable T get (T key) {
		int i = locateKey(key);
		return i < 0 ? null : keyTable[i];
	}

	public T first () {
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++)
			if (keyTable[i] != null)
				return keyTable[i];
		throw new IllegalStateException("ObjectSet is empty.");
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 */
	public void ensureCapacity (int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize)
			resize(tableSize);
	}

	private void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = Long.numberOfLeadingZeros(mask);
		T[] oldKeyTable = keyTable;

		keyTable = (T[])(new Object[newSize]);

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				T key = oldKeyTable[i];
				if (key != null)
					addResize(key);
			}
		}
	}

	@Override
	public Object[] toArray() {
		return toArray(new Object[size()]);
	}

	/**
	 * Returns an array containing all of the elements in this set; the
	 * runtime type of the returned array is that of the specified array.
	 * If the set fits in the specified array, it is returned therein.
	 * Otherwise, a new array is allocated with the runtime type of the
	 * specified array and the size of this set.
	 * <br>
	 * Implementation is mostly copied from GWT, but uses Arrays.copyOf() instead of their internal APIs.
	 *
	 * @param a the array into which the elements of this set are to be
	 *        stored, if it is big enough; otherwise, a new array of the same
	 *        runtime type is allocated for this purpose.
	 * @param <E> must be the same as {@code T} or a superclass/interface of it; not checked
	 * @return an array containing all the elements in this set
	 */
	@Override
	public <E> E[] toArray(E[] a) {
		int size = size();
		if (a.length < size) {
			a = Arrays.copyOf(a, size);
		}
		Object[] result = a;
		Iterator<T> it = iterator();
		for (int i = 0; i < size; ++i) {
			result[i] = it.next();
		}
		if (a.length > size) {
			a[size] = null;
		}
		return a;
	}

	public int hashCode () {
		int h = size;
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			T key = keyTable[i];
			if (key != null)
				h += key.hashCode();
		}
		return h;
	}

	public boolean equals (Object obj) {
		if (!(obj instanceof ObjectSet))
			return false;
		ObjectSet other = (ObjectSet)obj;
		if (other.size != size)
			return false;
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++)
			if (keyTable[i] != null && !other.contains(keyTable[i]))
				return false;
		return true;
	}

	public String toString () {
		return '{' + toString(", ") + '}';
	}

	public String toString (String separator) {
		if (size == 0)
			return "";
		StringBuilder buffer = new StringBuilder(32);
		T[] keyTable = this.keyTable;
		int i = keyTable.length;
		while (i-- > 0) {
			T key = keyTable[i];
			if (key == null)
				continue;
			buffer.append(key == this ? "(this)" : key);
			break;
		}
		while (i-- > 0) {
			T key = keyTable[i];
			if (key == null)
				continue;
			buffer.append(separator);
			buffer.append(key == this ? "(this)" : key);
		}
		return buffer.toString();
	}

	/**
	 * Returns an iterator for the keys in the set. Remove is supported.
	 * <p>
	 * Reuses one of two iterators for this set. For nested or multithreaded
	 * iteration, use {@link ObjectSetIterator#ObjectSetIterator(ObjectSet)}.
	 */
	@Override
	public Iterator<T> iterator () {
		if (iterator1 == null) {
			iterator1 = new ObjectSetIterator<>(this);
			iterator2 = new ObjectSetIterator<>(this);
		}
		if (!iterator1.valid) {
			iterator1.reset();
			iterator1.valid = true;
			iterator2.valid = false;
			return iterator1;
		}
		iterator2.reset();
		iterator2.valid = true;
		iterator1.valid = false;
		return iterator2;
	}

	@SafeVarargs
	static public <T> ObjectSet<T> with (T... array) {
		ObjectSet<T> set = new ObjectSet<T>();
		set.addAll(array);
		return set;
	}

	static public class ObjectSetIterator<K> implements Iterable<K>, Iterator<K> {
		public boolean hasNext;

		final ObjectSet<K> set;
		int nextIndex, currentIndex;
		boolean valid = true;

		public ObjectSetIterator (ObjectSet<K> set) {
			this.set = set;
			reset();
		}

		public void reset () {
			currentIndex = -1;
			nextIndex = -1;
			findNextIndex();
		}

		private void findNextIndex () {
			K[] keyTable = set.keyTable;
			for (int n = set.keyTable.length; ++nextIndex < n; ) {
				if (keyTable[nextIndex] != null) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}

		@Override
		public void remove () {
			int i = currentIndex;
			if (i < 0)
				throw new IllegalStateException("next must be called before remove.");
			K[] keyTable = set.keyTable;
			int mask = set.mask, next = i + 1 & mask;
			K key;
			while ((key = keyTable[next]) != null) {
				int placement = set.place(key);
				if ((next - placement & mask) > (i - placement & mask)) {
					keyTable[i] = key;
					i = next;
				}
				next = next + 1 & mask;
			}
			keyTable[i] = null;
			set.size--;
			if (i != currentIndex)
				--nextIndex;
			currentIndex = -1;
		}

		@Override
		public boolean hasNext () {
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			return hasNext;
		}

		@Override
		public K next () {
			if (!hasNext)
				throw new NoSuchElementException();
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			K key = set.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}

		@Override
		public ObjectSetIterator<K> iterator () {
			return this;
		}
	}
}
