package org.mastodon.mamut.collaboration.io;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.mastodon.collection.ref.RefObjectHashMap;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.pool.PoolCollectionWrapper;
import org.mastodon.pool.PoolObject;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class MasgitoffIds
{

	/**
	 * Map Spot to UUID.
	 * TODO this has to become a property that is undoable.
	 */
	private final Map< Spot, UUID > uuids;

	/**
	 * Bimap between file IDs and spots.
	 */
	private final Index< Spot > spotIndex;

	/**
	 * Bimap between link IDs and links.
	 */
	private final Index< Link > linkIndex;

	/**
	 * Map for string to fileID.
	 */
	private final TObjectIntMap< String > stringIndex;

	public MasgitoffIds( final ModelGraph graph )
	{
		this.uuids = new RefObjectHashMap<>( graph.vertices().getRefPool() );
		this.spotIndex = new Index<>( graph.vertices().getRefPool() );
		this.linkIndex = new Index<>( graph.edges().getRefPool() );
		this.stringIndex = new TObjectIntHashMap<>( 100, 0.5f, -1 );
	}

	private static void fillUuids( final Model model, final Map< Spot, UUID > spotUuids )
	{
		for ( final Spot spot : model.getGraph().vertices() )
			if ( !spotUuids.containsKey( spot ) )
				spotUuids.put( spot, UUID.randomUUID() );
	}

	private static < T extends PoolObject< T, ?, ? > > void fillIndex( final PoolCollectionWrapper< T > collection, final Index< T > ids )
	{
		for ( final T object : collection )
			ids.getOrCreateId( object );
	}

	private static void fillLabelIndex( final PoolCollectionWrapper< Spot > spots, final TObjectIntMap< String > labelIndex )
	{
		final Set< String > labels = getUsedLabels( spots );
		updateStringIndex( labelIndex, labels );
	}

	private static Set< String > getUsedLabels( final PoolCollectionWrapper< Spot > spots )
	{
		// get a set of all labels in use
		final Set< String > labels = new HashSet<>();
		for ( final Spot spot : spots )
			if ( spot.isLabelSet() )
				labels.add( spot.getLabel() );
		return labels;
	}

	/**
	 * This method expects {@code indexMap} to be a map that assigns a unique integer to each
	 * key in the map. The {@code indexMap} is modified. Entries that are in {@code indexMap}
	 * and not in {@code newKeys} are removed from {@code indexMap}. Entries that are in
	 * {@code newKeys} but not in {@code indexMap} are added to the {@code indexMap}.
	 * All the new entries get a unique index.
	 * <p>
	 * WARNING: The set {@code newKeys} is modified too.
	 */
	static void updateStringIndex( final TObjectIntMap< String > indexMap, final Set< String > newKeys )
	{
		// remove all unused newKeys from the map & create a bitset of indices in use
		final BitSet usedIndicis = new BitSet();
		indexMap.retainEntries( ( label, index ) -> {
			final boolean needed = newKeys.contains( label );
			if ( needed )
			{
				usedIndicis.set( index );
				newKeys.remove( label );
			}
			return needed;
		} );
		// add entries the newKeys that had previously on entry
		int newIndex = -1;
		for ( final String label : newKeys )
		{
			newIndex = usedIndicis.nextClearBit( newIndex + 1 );
			indexMap.put( label, newIndex );
		}
	}

	void fillIds( final Model model )
	{
		fillUuids( model, uuids );
		fillIndex( model.getGraph().vertices(), spotIndex );
		fillIndex( model.getGraph().edges(), linkIndex );
		fillLabelIndex( model.getGraph().vertices(), stringIndex );
	}

	public Map< Spot, UUID > getSpotUuids()
	{
		return uuids;
	}

	public Index< Spot > getSpotIndex()
	{
		return spotIndex;
	}

	public Index< Link > getLinkIndex()
	{
		return linkIndex;
	}

	public TObjectIntMap< String > getLabelIndex()
	{
		return stringIndex;
	}
}
