/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.core3.util;

import java.util.*;


/**
 * A lighter (on memory) hash map
 * 
 * Please note the following performance drawbacks:
 * -removal is implemented with thombstone-keys, this can significantly increase the lookup time if many values are removed. Use compactify() for scrubbing
 * -key set iterators and thus transfers to other maps are slower than compareable implementations
 * 
 * @author Aaron Grunthal
 * @create 28.11.2007
 */
public class LightHashMap extends AbstractMap {
	private static final Object	THOMBSTONE			= new Object();
	private static final float	DEFAULT_LOAD_FACTOR	= 0.75f;
	private static final int	DEFAULT_CAPACITY	= 8;

	public LightHashMap()
	{
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public LightHashMap(final int initialCapacity)
	{
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	public LightHashMap(final Map m)
	{
		this(0);
		putAll(m);
	}

	public LightHashMap(final int initialCapacity, final float loadFactor)
	{
		if (loadFactor > 1)
			throw new IllegalArgumentException("Load factor must not be > 1");
		this.loadFactor = loadFactor;
		int capacity = 1;
		while (capacity < initialCapacity)
			capacity <<= 1;
		values = new Object[capacity];
		keys = new Object[capacity];
	}

	final float	loadFactor;
	int			size;
	Object[]	values;
	Object[]	keys;

	public Set entrySet() {
		return new EntrySet();
	}

	private abstract class HashIterator implements Iterator {
		protected int	nextIdx		= -1;
		protected int	currentIdx	= -1;

		public HashIterator()
		{
			findNext();
		}

		private void findNext() {
			do
				nextIdx++;
			while (nextIdx < keys.length && ((values[nextIdx] == null && keys[nextIdx] == null) || keys[nextIdx] == THOMBSTONE));
		}

		public void remove() {
			if (currentIdx == -1)
				new IllegalStateException("No entry to delete, use next() first");
			LightHashMap.this.remove(keys[currentIdx]);
			currentIdx = -1;
		}

		public boolean hasNext() {
			return nextIdx < keys.length;
		}

		public Object next() {
			if (!hasNext())
				throw new IllegalStateException("No more entries");
			currentIdx = nextIdx;
			findNext();
			return nextIntern();
		}

		abstract Object nextIntern();
	}

	private class EntrySet extends AbstractSet {
		public Iterator iterator() {
			return new EntrySetIterator();
		}

		public int size() {
			return size;
		}

		private final class Entry implements Map.Entry {
			final int	entryIndex;

			public Entry(final int idx)
			{
				entryIndex = idx;
			}

			public Object getKey() {
				return keys[entryIndex];
			}

			public Object getValue() {
				return values[entryIndex];
			}

			public Object setValue(final Object value) {
				final Object oldValue = values[entryIndex];
				values[entryIndex] = value;
				return oldValue;
			}
		}

		private class EntrySetIterator extends HashIterator {
			public Object nextIntern() {
				return new Entry(currentIdx);
			}
		}
	}

	private class KeySet extends AbstractSet {
		public Iterator iterator() {
			return new KeySetIterator();
		}

		private class KeySetIterator extends HashIterator {
			Object nextIntern() {
				return keys[currentIdx];
			}
		}

		public int size() {
			return size;
		}
	}

	private class Values extends AbstractCollection {
		public Iterator iterator() {
			return new ValueIterator();
		}

		private class ValueIterator extends HashIterator {
			Object nextIntern() {
				return values[currentIdx];
			}
		}

		public int size() {
			return size;
		}
	}

	public Object put(final Object key, final Object value) {
		checkCapacity(1);
		return add(key, value);
	}

	public void putAll(final Map m) {
		checkCapacity(m.size());
		for (final Iterator it = m.entrySet().iterator(); it.hasNext();)
		{
			final Map.Entry entry = (Map.Entry) it.next();
			add(entry.getKey(), entry.getValue());
		}
	}

	public Set keySet() {
		return new KeySet();
	}

	public Collection values() {
		return new Values();
	}

	public Object get(final Object key) {
		return values[findIndex(key)];
	}

	private Object add(final Object key, final Object value) {
		final int idx = findIndex(key);
		final Object oldValue = values[idx];
		if (!keysEqual(key, keys[idx]))
		{
			keys[idx] = key;
			size++;
		}
		values[idx] = value;
		return oldValue;
	}

	public Object remove(final Object key) {
		final int idx = findIndex(key);
		if (keysEqual(key, keys[idx]))
		{
			final Object oldValue = values[idx];
			if (key == null && oldValue == null) // sanity check for null
				// keys
				return null;
			keys[idx] = THOMBSTONE;
			values[idx] = null;
			size--;
			return oldValue;
		}
		return null;
	}

	public void clear() {
		size = 0;
		int capacity = 1;
		while (capacity < DEFAULT_CAPACITY)
			capacity <<= 1;
		values = new Object[capacity];
		keys = new Object[capacity];
	}

	public boolean containsKey(final Object key) {
		return keysEqual(key, keys[findIndex(key)]);
	}

	public boolean containsValue(final Object value) {
		if (value != null)
		{
			for (int i = 0; i < values.length; i++)
				if (value.equals(values[i]))
					return true;
		} else
			for (int i = 0; i < values.length; i++)
				if (values[i] == null && keys[i] != null && keys[i] != THOMBSTONE)
					return true;
		return false;
	}

	private boolean keysEqual(final Object o1, final Object o2) {
		if (o1 != null)
			if (o2 != null && o1.hashCode() != o2.hashCode())
				return false;
			else
				return o1.equals(o2);
		return o2 == null;
	}

	private int findIndex(final Object keyToFind) {
		final int hash = keyToFind == null ? 0 : keyToFind.hashCode();
		/* hash ^= (hash >>> 20) ^ (hash >>> 12);
		 * hash ^= (hash >>> 7) ^ (hash >>> 4);
		 */
		int probe = 1;
		int newIndex = hash & (keys.length - 1);
		int thombStoneIndex = -1;
		// search until we find a free entry or an entry matching the key to insert
		while (keys[newIndex] != null && !keysEqual(keys[newIndex], keyToFind))
		{
			if (keys[newIndex] == THOMBSTONE && thombStoneIndex == -1)
				thombStoneIndex = newIndex;
			newIndex = (hash + (probe + probe * probe) >> 1) & (keys.length - 1);
			probe++;
		}
		// if we didn't find an exact match then the first thombstone will do too for insert
		if (!keysEqual(keys[newIndex], keyToFind) && thombStoneIndex != -1)
			return thombStoneIndex;
		return newIndex;
	}

	private void checkCapacity(final int n) {
		if ((size + n) < keys.length * loadFactor)
			return;
		int newCapacity = keys.length;
		do
			newCapacity <<= 1;
		while (newCapacity * loadFactor < (size + n));
		adjustCapacity(newCapacity);
	}

	/**
	 * will shrink the internal storage size to the least possible amount,
	 * should be used after removing many entries for example
	 * 
	 * @param compactingLoadFactor
	 *            load factor for the compacting operation, use 0 to compact
	 *            with the load factor specified during instantiation
	 */
	public void compactify(float compactingLoadFactor) {
		int newCapacity = 1;
		if (compactingLoadFactor == 0.f)
			compactingLoadFactor = loadFactor;
		while (newCapacity * compactingLoadFactor < size)
			newCapacity <<= 1;
		adjustCapacity(newCapacity);
	}

	private void adjustCapacity(final int newSize) {
		final Object[] oldValues = values;
		final Object[] oldKeys = keys;
		values = new Object[newSize];
		keys = new Object[newSize];
		size = 0;
		for (int i = 0; i < oldKeys.length; i++)
		{
			if ((oldValues[i] == null && oldKeys[i] == null) || oldKeys[i] == THOMBSTONE)
				continue;
			add(oldKeys[i], oldValues[i]);
		}
	}

	static void test() {
		final Random rnd = new Random();
		final byte[] buffer = new byte[25];
		final String[] fillData = new String[1048575];
		for (int i = 0; i < fillData.length; i++)
		{
			rnd.nextBytes(buffer);
			fillData[i] = new String(buffer);
			fillData[i].hashCode();
		}
		long time;
		final Map m1 = new HashMap();
		final Map m2 = new LightHashMap();
		System.out.println("fill:");
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			m1.put(fillData[i], buffer);
		System.out.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			m2.put(fillData[i], buffer);
		System.out.println(System.currentTimeMillis() - time);
		System.out.println("replace-fill:");
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			m1.put(fillData[i], buffer);
		System.out.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			m2.put(fillData[i], buffer);
		System.out.println(System.currentTimeMillis() - time);
		System.out.println("get:");
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			m1.get(fillData[i]);
		System.out.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			m2.get(fillData[i]);
		System.out.println(System.currentTimeMillis() - time);
		System.out.println("compactify light map");
		time = System.currentTimeMillis();
		((LightHashMap) m2).compactify(0.9f);
		System.out.println(System.currentTimeMillis() - time);
		System.out.println("transfer to hashmap");
		time = System.currentTimeMillis();
		new HashMap(m1);
		System.out.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		new HashMap(m2);
		System.out.println(System.currentTimeMillis() - time);
		System.out.println("transfer to lighthashmap");
		time = System.currentTimeMillis();
		new LightHashMap(m1);
		System.out.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		new LightHashMap(m2);
		System.out.println(System.currentTimeMillis() - time);
		System.out.println("remove entry by entry");
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			if (m1.remove(fillData[i]) == null)
				System.out.println("removeerror");
		System.out.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			if (m2.remove(fillData[i]) == null)
				System.out.println("removeerror");
		System.out.println(System.currentTimeMillis() - time);
	}

	public static void main(final String[] args) {
		System.out.println("Call with -Xmx300m -Xcomp -server");
		try
		{
			Thread.sleep(300);
		} catch (final InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		test();
		System.out.println("-------------------------------------");
		System.gc();
		try
		{
			Thread.sleep(300);
		} catch (final InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		test();
	}
}
