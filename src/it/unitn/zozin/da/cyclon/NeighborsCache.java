package it.unitn.zozin.da.cyclon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import akka.actor.ActorRef;

public class NeighborsCache {

	public static final long SEED = 1234l;
	private final int MAX_SIZE;

	private final List<Neighbor> cache;

	private final Random rand;
	private Set<Integer> replaceableEntries;

	public NeighborsCache(int maxSize) {
		replaceableEntries = new HashSet<Integer>();
		rand = new Random(SEED);
		this.MAX_SIZE = maxSize;

		cache = new ArrayList<Neighbor>();
	}

	public void increaseNeighborsAge() {
		for (Neighbor n : cache)
			n.age += 1;
	}

	public Neighbor selectOldestNeighbor() {
		Neighbor oldest = cache.get(0);
		for (int i = 1; i < cache.size(); i++)
			if (cache.get(i).age > oldest.age)
				oldest = cache.get(i);

		return oldest;
	}

	public List<Neighbor> selectRandomNeighbors(int shuffleLength) {
		return selectRandomNeighbors(shuffleLength, null);
	}

	public List<Neighbor> selectRandomNeighbors(int shuffleLength, Neighbor exclude) {
		List<Neighbor> out = new ArrayList<Neighbor>(cache);
		if (exclude != null)
			out.remove(exclude);

		Collections.shuffle(out, rand);

		List<Neighbor> neighbors = out.subList(0, Math.min(shuffleLength, out.size()));

		neighbors.stream().forEach((n) -> replaceableEntries.add(n.cacheEntryIndex));

		return neighbors;
	}

	public void updateNeighbors(Collection<Neighbor> newEntries, boolean overwrite) {

		Iterator<Integer> replaceableEntriesIter = replaceableEntries.iterator();

		Iterator<Integer> overwritableIter = null;
		Set<Integer> overwritable = cache.stream().map((n) -> n.cacheEntryIndex).collect(Collectors.toSet());

		overwritable.removeAll(replaceableEntries);
		overwritableIter = overwritable.iterator();

		for (Neighbor newNeighbor : newEntries) {
			// If the are no free cache slots, remove a replaceable entry before
			// inserting the new one

			int entryIndex = (cache.size() == 0) ? 0 : cache.size() - 1;

			if (freeSlots() == 0) {
				if (!replaceableEntriesIter.hasNext()) {
					entryIndex = overwritableIter.next();
				} else {
					entryIndex = replaceableEntriesIter.next();
					replaceableEntriesIter.remove();
				}

				// INVARIANCE: The replaceable entry has always to be present in
				// current neighbors (remove returns true if present)
				assert (cache.remove(entryIndex) != null) : entryIndex + " not in cache: " + cache;
			}

			newNeighbor.cacheEntryIndex = entryIndex;
			cache.add(entryIndex, newNeighbor);
		}

		// INVARIANCE: cache size never exceeds maximum size
		assert (cache.size() <= MAX_SIZE);
	}

	@Override
	public String toString() {
		return "NeighborsCache " + cache;
	}

	/**
	 * Neighbor objects are sorted only by age (Comparable interface) but the
	 * equals method compares only the address (This class is inconsistent with
	 * the Comparable interface specification)
	 *
	 */
	public static class Neighbor implements Comparable<Neighbor>, Cloneable {

		// Entry index inside the local cache
		int cacheEntryIndex;

		Integer age;
		ActorRef address;

		public Neighbor(int age, ActorRef address) {
			this.age = age;
			this.address = address;
		}

		@Override
		public String toString() {
			return cacheEntryIndex + " (" + age + ", " + address.path().name() + ")";
		}

		@Override
		public int compareTo(Neighbor o2) {
			return age.compareTo(o2.age);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((address == null) ? 0 : address.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Neighbor other = (Neighbor) obj;
			if (address == null) {
				if (other.address != null)
					return false;
			} else if (!address.equals(other.address))
				return false;
			return true;
		}

		@Override
		public Neighbor clone() {
			return new Neighbor(age, address);
		}

	}

	public int freeSlots() {
		return MAX_SIZE - cache.size();
	}

	public int size() {
		return cache.size();
	}

	public List<Neighbor> getNeighbors() {
		return Collections.unmodifiableList(cache);
	}
}
