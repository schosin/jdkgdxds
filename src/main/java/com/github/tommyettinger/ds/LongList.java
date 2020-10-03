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
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Random;

/** A resizable, ordered or unordered long list. Primitive-backed, so it avoids the boxing that occurs with an ArrayList of Long.
 * If unordered, this class avoids a memory copy when removing elements (the last element is moved to the removed element's position).
 * This tries to imitate most of the {@link java.util.List} interface, though it can't implement it without boxing its items.
 * Has a Java 8 {@link PrimitiveIterator} accessible via {@link #iterator()}.
 * 
 * @author Nathan Sweet */
public class LongList implements Arrangeable, Serializable {
	private static final long serialVersionUID = 0L;
	public long[] items;
	public int size;
	public boolean ordered;
	protected @Nullable LongListIterator iterator1, iterator2;

	/** Creates an ordered array with a capacity of 16. */
	public LongList () {
		this(true, 16);
	}

	/** Creates an ordered array with the specified capacity. */
	public LongList (int capacity) {
		this(true, capacity);
	}

	/** @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
	 *           memory copy.
	 * @param capacity Any elements added beyond this will cause the backing array to be grown. */
	public LongList (boolean ordered, int capacity) {
		this.ordered = ordered;
		items = new long[capacity];
	}

	/** Creates a new array containing the elements in the specific array. The new array will be ordered if the specific array is
	 * ordered. The capacity is set to the number of elements, so any subsequent elements added will cause the backing array to be
	 * grown. */
	public LongList (LongList array) {
		this.ordered = array.ordered;
		size = array.size;
		items = new long[size];
		System.arraycopy(array.items, 0, items, 0, size);
	}

	/** Creates a new ordered array containing the elements in the specified array. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown. */
	public LongList (long[] array) {
		this(true, array, 0, array.length);
	}

	/** Creates a new array containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 * @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
	 *           memory copy. */
	public LongList (boolean ordered, long[] array, int startIndex, int count) {
		this(ordered, count);
		size = count;
		System.arraycopy(array, startIndex, items, 0, count);
	}

	// Newly-added
	public int size(){
		return size;
	}

	// Modified from libGDX
	public boolean add (long value) {
		long[] items = this.items;
		if (size == items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
		items[size++] = value;
		return true;
	}

	public void add (long value1, long value2) {
		long[] items = this.items;
		if (size + 1 >= items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
		items[size] = value1;
		items[size + 1] = value2;
		size += 2;
	}

	public void add (long value1, long value2, long value3) {
		long[] items = this.items;
		if (size + 2 >= items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
		items[size] = value1;
		items[size + 1] = value2;
		items[size + 2] = value3;
		size += 3;
	}

	public void add (long value1, long value2, long value3, long value4) {
		long[] items = this.items;
		if (size + 3 >= items.length) items = resize(Math.max(8, (int)(size * 1.8f))); // 1.75 isn't enough when size=5.
		items[size] = value1;
		items[size + 1] = value2;
		items[size + 2] = value3;
		items[size + 3] = value4;
		size += 4;
	}

	// Modified from libGDX
	public boolean addAll (LongList array) {
		return addAll(array.items, 0, array.size);
	}

	// Modified from libGDX
	public boolean addAll (LongList array, int offset, int length) {
		if (offset + length > array.size)
			throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
		return addAll(array.items, offset, length);
	}

	// Modified from libGDX
	public boolean addAll (long... array) {
		return addAll(array, 0, array.length);
	}

	// Modified from libGDX
	public boolean addAll (long[] array, int offset, int length) {
		long[] items = this.items;
		int sizeNeeded = size + length;
		if (sizeNeeded > items.length) items = resize(Math.max(Math.max(8, sizeNeeded), (int)(size * 1.75f)));
		System.arraycopy(array, offset, items, size, length);
		size += length;
		return true;
	}

	//Kotlin-friendly operator
	public long get (int index) {
		if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		return items[index];
	}

	//Kotlin-friendly operator
	public void set (int index, long value) {
		if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		items[index] = value;
	}

	// Modified from libGDX
	public void plus (int index, long value) {
		if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		items[index] += value;
	}

	/**
	 * Adds {@code value} to each item in this LongList, stores it in this and returns it.
	 * The presence of this method allows Kotlin code to use the {@code +} operator (though it
	 * shouldn't be used more than once in an expression, because this method modifies this LongList).
	 * @param value each item in this will be assigned {@code item + value}
	 * @return this for chaining and Kotlin compatibility
	 */
	// Modified from libGDX
	// Kotlin-friendly operator
	public LongList plus (long value) {
		long[] items = this.items;
		for (int i = 0, n = size; i < n; i++)
			items[i] += value;
		return this;
	}

	// Modified from libGDX
	public void times (int index, long value) {
		if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		items[index] *= value;
	}


	/**
	 * Multiplies each item in this LongList by {@code value}, stores it in this and returns it.
	 * The presence of this method allows Kotlin code to use the {@code *} operator (though it
	 * shouldn't be used more than once in an expression, because this method modifies this LongList).
	 * @param value each item in this will be assigned {@code item * value}
	 * @return this for chaining and Kotlin compatibility
	 */
	// Modified from libGDX
	// Kotlin-friendly operator
	public LongList times (long value) {
		long[] items = this.items;
		for (int i = 0, n = size; i < n; i++)
			items[i] *= value;
		return this;
	}

	// Newly-added
	public void minus (int index, long value) {
		if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		items[index] -= value;
	}

	/**
	 * Takes each item in this LongList and subtracts {@code value}, stores it in this and returns it.
	 * This is just a minor convenience in Java, but the presence of this method allows Kotlin code to use
	 * the {@code -} operator (though it shouldn't be used more than once in an expression, because
	 * this method modifies this LongList).
	 * @param value each item in this will be assigned {@code item - value}
	 * @return this for chaining and Kotlin compatibility
	 */
	// Newly-added
	// Kotlin-friendly operator
	public LongList minus (long value) {
		long[] items = this.items;
		for (int i = 0, n = size; i < n; i++)
			items[i] -= value;
		return this;
	}

	// Newly-added
	public void div (int index, long value) {
		if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		items[index] /= value;
	}

	/**
	 * Divides each item in this LongList by {@code value}, stores it in this and returns it.
	 * The presence of this method allows Kotlin code to use the {@code /} operator (though it
	 * shouldn't be used more than once in an expression, because this method modifies this LongList).
	 * @param value each item in this will be assigned {@code item / value}
	 * @return this for chaining and Kotlin compatibility
	 */
	// Newly-added
	// Kotlin-friendly operator
	public LongList div (long value) {
		long[] items = this.items;
		for (int i = 0, n = size; i < n; i++)
			items[i] /= value;
		return this;
	}

	// Newly-added
	public void rem (int index, long value) {
		if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		items[index] %= value;
	}

	/**
	 * Gets the remainder of each item in this LongList with {@code value}, stores it in this and returns it.
	 * The presence of this method allows Kotlin code to use the {@code %} operator (though it
	 * shouldn't be used more than once in an expression, because this method modifies this LongList).
	 * @param value each item in this will be assigned {@code item % value}
	 * @return this for chaining and Kotlin compatibility
	 */
	// Newly-added
	// Kotlin-friendly operator
	public LongList rem (long value) {
		long[] items = this.items;
		for (int i = 0, n = size; i < n; i++)
			items[i] %= value;
		return this;
	}

	public void insert (int index, long value) {
		if (index > size) throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);
		long[] items = this.items;
		if (size == items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
		if (ordered)
			System.arraycopy(items, index, items, index + 1, size - index);
		else
			items[size] = items[index];
		size++;
		items[index] = value;
	}

	/** Inserts the specified number of items at the specified index. The new items will have values equal to the values at those
	 * indices before the insertion. */
	public void insertRange (int index, int count) {
		if (index > size) throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);
		int sizeNeeded = size + count;
		if (sizeNeeded > items.length) items = resize(Math.max(Math.max(8, sizeNeeded), (int)(size * 1.75f)));
		System.arraycopy(items, index, items, index + count, size - index);
		size = sizeNeeded;
	}

	@Override
	public void swap (int first, int second) {
		if (first >= size) throw new IndexOutOfBoundsException("first can't be >= size: " + first + " >= " + size);
		if (second >= size) throw new IndexOutOfBoundsException("second can't be >= size: " + second + " >= " + size);
		long[] items = this.items;
		long firstValue = items[first];
		items[first] = items[second];
		items[second] = firstValue;
	}

	public boolean contains (long value) {
		int i = size - 1;
		long[] items = this.items;
		while (i >= 0)
			if (items[i--] == value) return true;
		return false;
	}

	/**
	 * Returns true if this LongList contains, at least once, every item in {@code other}; otherwise returns false.
	 * @param other an LongList
	 * @return true if this contains every item in {@code other}, otherwise false
	 */
	// Newly-added
	public boolean containsAll(LongList other) {
		long[] others = other.items;
		int otherSize = other.size;
		for (int i = 0; i < otherSize; i++) {
			if (!contains(others[i]))
				return false;
		}
		return true;
	}

	public int indexOf (long value) {
		long[] items = this.items;
		for (int i = 0, n = size; i < n; i++)
			if (items[i] == value) return i;
		return -1;
	}

	public int lastIndexOf (long value) {
		long[] items = this.items;
		for (int i = size - 1; i >= 0; i--)
			if (items[i] == value) return i;
		return -1;
	}

	/**
	 * Removes the first occurrence of {@code value} from this LongList, returning true if anything was removed.
	 * Otherwise, this returns false.
	 * @param value the value to (attempt to) remove
	 * @return true if a value was removed, false if the LongList is unchanged
	 */
	// Modified from libGDX
	public boolean remove (long value) {
		long[] items = this.items;
		for (int i = 0, n = size; i < n; i++) {
			if (items[i] == value) {
				removeIndex(i);
				return true;
			}
		}
		return false;
	}

	/** Removes and returns the item at the specified index.
	 * Note that this is equivalent to {@link java.util.List#remove(int)}, but can't have that name because
	 * we also have {@link #remove(long)} that removes a value, rather than an index.
	 * @param index the index of the item to remove and return
	 * @return the removed item */
	public long removeIndex (int index) {
		if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		long[] items = this.items;
		long value = items[index];
		size--;
		if (ordered)
			System.arraycopy(items, index + 1, items, index, size - index);
		else
			items[index] = items[size];
		return value;
	}

	/** Removes the items between the specified indices, inclusive. */
	public void removeRange (int startIndex, int endIndex) {
		int n = size;
		if (endIndex >= n) throw new IndexOutOfBoundsException("end can't be >= size: " + endIndex + " >= " + size);
		if (startIndex > endIndex) throw new IndexOutOfBoundsException("start can't be > end: " + startIndex + " > " + endIndex);
		int count = endIndex - startIndex + 1, lastIndex = n - count;
		if (ordered)
			System.arraycopy(items, startIndex + count, items, startIndex, n - (startIndex + count));
		else {
			int i = Math.max(lastIndex, endIndex + 1);
			System.arraycopy(items, i, items, startIndex, n - i);
		}
		size = n - count;
	}

	/** Removes from this array all of elements contained in the specified array.
	 * Note that if a value is present more than once in this LongList, only one of those occurrences
	 * will be removed for each occurrence of that value in {@code array}. If {@code array} has the same
	 * contents as this LongList or has additional items, then removing all of {@code array} will clear this.
	 * @return true if this array was modified. */
	public boolean removeAll (LongList array) {
		int size = this.size;
		int startSize = size;
		long[] items = this.items;
		for (int i = 0, n = array.size; i < n; i++) {
			long item = array.get(i);
			for (int ii = 0; ii < size; ii++) {
				if (item == items[ii]) {
					removeIndex(ii);
					size--;
					break;
				}
			}
		}
		return size != startSize;
	}

	/**
	 * Removes all items from this LongList that are not present somewhere in {@code other}, any number of times.
	 * @param other an LongList that contains the items that this should keep, whenever present
	 * @return true if this LongList changed as a result of this call, otherwise false
	 */
	// Newly-added
	public boolean retainAll (LongList other) {
		final int size = this.size;
		final long[] items = this.items;
		int r = 0, w = 0;
		for (; r < size; r++) {
			if (other.contains(items[r])) {
				items[w++] = items[r];
			}
		}

		return size != (this.size = w);
	}

	/** Removes and returns the last item. */
	public long pop () {
		return items[--size];
	}

	/** Returns the last item. */
	public long peek () {
		return items[size - 1];
	}

	/** Returns the first item. */
	// Modified from libGDX
	public long first () {
		if (size == 0) throw new IndexOutOfBoundsException("Array is empty.");
		return items[0];
	}

	/** Returns true if the array has one or more items. */
	public boolean notEmpty () {
		return size > 0;
	}

	/** Returns true if the array is empty. */
	public boolean isEmpty () {
		return size == 0;
	}

	public void clear () {
		size = 0;
	}

	/** Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
	 * have been removed, or if it is known that more items will not be added.
	 * @return {@link #items} */
	public long[] shrink () {
		if (items.length != size) resize(size);
		return items;
	}

	/** Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes.
	 * @return {@link #items} */
	public long[] ensureCapacity (int additionalCapacity) {
		if (additionalCapacity < 0) throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity);
		int sizeNeeded = size + additionalCapacity;
		if (sizeNeeded > items.length) resize(Math.max(Math.max(8, sizeNeeded), (int)(size * 1.75f)));
		return items;
	}

	/** Sets the array size, leaving any values beyond the current size undefined.
	 * @return {@link #items} */
	public long[] setSize (int newSize) {
		if (newSize < 0) throw new IllegalArgumentException("newSize must be >= 0: " + newSize);
		if (newSize > items.length) resize(Math.max(8, newSize));
		size = newSize;
		return items;
	}

	protected long[] resize (int newSize) {
		long[] newItems = new long[newSize];
		long[] items = this.items;
		System.arraycopy(items, 0, newItems, 0, Math.min(size, newItems.length));
		this.items = newItems;
		return newItems;
	}

	public void sort () {
		Arrays.sort(items, 0, size);
	}

	@Override
	public void reverse () {
		long[] items = this.items;
		for (int i = 0, lastIndex = size - 1, n = size / 2; i < n; i++) {
			int ii = lastIndex - i;
			long temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	// Modified from libGDX
	@Override
	public void shuffle (Random random) {
		long[] items = this.items;
		for (int i = size - 1; i >= 0; i--) {
			int ii = random.nextInt(i+1);
			long temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	/** Reduces the size of the array to the specified size. If the array is already smaller than the specified size, no action is
	 * taken. */
	public void truncate (int newSize) {
		if (size > newSize) size = newSize;
	}

	/** Returns a random item from the array, or zero if the array is empty. */
	public long random (Random random) {
		if (size == 0) return 0;
		return items[random.nextInt(size)];
	}

	public long[] toArray () {
		long[] array = new long[size];
		System.arraycopy(items, 0, array, 0, size);
		return array;
	}

	public int hashCode () {
		long[] items = this.items;
		long h;
		if (!ordered) {
			h = 1L;
			for (int i = 0, n = size; i < n; i++) {
				h += items[i];
			}
		}
		else {
			h = 0xC13FA9A902A6328FL;
			for (int i = 0, n = size; i < n; i++) {
				h = h * 0x9E3779B97F4A7C15L + items[i];
			}
		}
		return (int)(h ^ h >>> 32);
	}

	/** Returns false if either array is unordered. */
	public boolean equals (Object object) {
		if (object == this) return true;
		if (!ordered) return false;
		if (!(object instanceof LongList)) return false;
		LongList array = (LongList)object;
		if (!array.ordered) return false;
		int n = size;
		if (n != array.size) return false;
		long[] items1 = this.items, items2 = array.items;
		for (int i = 0; i < n; i++)
			if (items1[i] != items2[i]) return false;
		return true;
	}

	public String toString () {
		if (size == 0) return "[]";
		long[] items = this.items;
		StringBuilder buffer = new StringBuilder(32);
		buffer.append('[');
		buffer.append(items[0]);
		for (int i = 1; i < size; i++) {
			buffer.append(", ");
			buffer.append(items[i]);
		}
		buffer.append(']');
		return buffer.toString();
	}

	public String toString (String separator) {
		if (size == 0) return "";
		long[] items = this.items;
		StringBuilder buffer = new StringBuilder(32);
		buffer.append(items[0]);
		for (int i = 1; i < size; i++) {
			buffer.append(separator);
			buffer.append(items[i]);
		}
		return buffer.toString();
	}

	/**
	 * Returns a Java 8 primitive iterator over the int items in this LongList. Iterates in order if {@link #ordered}
	 * is true, otherwise this is not guaranteed to iterate in the same order as items were added.
	 * <br>
	 * This will reuse one of two iterators in this IntList; this does not allow nested iteration.
	 * Use {@link LongListIterator#LongListIterator(LongList)} to nest iterators.
	 * @return a {@link PrimitiveIterator.OfLong}; use its nextLong() method instead of next()
	 */
	public LongListIterator iterator(){
		if (iterator1 == null) {
			iterator1 = new LongListIterator(this);
			iterator2 = new LongListIterator(this);
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

	public static class LongListIterator implements PrimitiveIterator.OfLong {
		protected int index = 0;
		protected LongList list;
		protected boolean valid = true;

		public LongListIterator (LongList list) {
			this.list = list;
		}

		/**
		 * Returns the next {@code int} element in the iteration.
		 *
		 * @return the next {@code int} element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		public long nextLong () {
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			if (index >= list.size)
				throw new NoSuchElementException();
			return list.get(index++);
		}

		/**
		 * Returns {@code true} if the iteration has more elements.
		 * (In other words, returns {@code true} if {@link #next} would
		 * return an element rather than throwing an exception.)
		 *
		 * @return {@code true} if the iteration has more elements
		 */
		@Override
		public boolean hasNext () {
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			return index < list.size;
		}

		public void reset () {
			index = -1;
		}
	}

	/** @see #LongList(long[]) */
	static public LongList with (long... array) {
		return new LongList(array);
	}
}
