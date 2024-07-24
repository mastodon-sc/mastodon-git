package org.mastodon.mamut.collaboration.io;

import java.util.Map;
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

	private final Map< Spot, UUID > uuids;

	private final Index< Spot > spotIndex;

	private final Index< Link > linkIndex;

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
		// fixme make sure that label indices are unique
		for ( final Spot spot : spots )
		{
			if ( !spot.isLabelSet() )
				continue;
			final String label = spot.getLabel();
			if ( !labelIndex.containsKey( label ) )
				labelIndex.put( label, labelIndex.size() );
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
