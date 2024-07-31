package org.mastodon.mamut.collaboration.io.benchmark;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Localizable;
import net.imglib2.RealLocalizable;
import net.imglib2.util.Localizables;

import org.mastodon.RefPool;
import org.mastodon.mamut.model.Spot;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

public class ExactPositionToSpotIndex
{
	private final TIntIntHashMap hashToIndex;

	private final List< TIntList > buckets;

	private final RefPool< Spot > pool;

	public ExactPositionToSpotIndex( RefPool< Spot > pool )
	{
		this.pool = pool;
		this.hashToIndex = new TIntIntHashMap( 100, 0.5f, -1, -1 );
		this.buckets = new ArrayList<>();
		this.buckets.add( null );
		this.buckets.add( null );
	}

	public void put( final Spot spot )
	{
		final int hash = hash( spot );
		int index = hashToIndex.get( hash );
		if ( index == -1 )
		{
			hashToIndex.put( hash, spot.getInternalPoolIndex() );
			return;
		}
		if ( index >= 0 )
		{
			// collision
			final TIntList bucket = new TIntArrayList();
			bucket.add( index );
			bucket.add( spot.getInternalPoolIndex() );
			buckets.add( bucket );
			hashToIndex.put( hash, buckets.size() - 1 );
			return;
		}
		// index < 0
		index = -index;
		buckets.get( index ).add( spot.getInternalPoolIndex() );
	}

	public Spot get( final RealLocalizable key, final Spot ref )
	{
		final int hash = hash( key );
		int index = hashToIndex.get( hash );
		if ( index == -1 )
			return null;
		if ( index >= 0 )
		{
			final Spot spot = pool.getObject( index, ref );
			return ( Localizables.equals( key, spot ) ) ? spot : null;
		}
		// index < -1
		index = -index;
		final TIntList bucket = buckets.get( index );
		for ( int i = 0; i < bucket.size(); i++ )
		{
			final Spot spot = pool.getObject( bucket.get( i ), ref );
			if ( Localizables.equals( key, spot ) )
				return spot;
		}
		return null;
	}

	private static int hash( final RealLocalizable position )
	{
		final int hash = Double.hashCode( position.getDoublePosition( 0 ) ) +
				31 * Double.hashCode( position.getDoublePosition( 1 ) ) +
				37 * Double.hashCode( position.getDoublePosition( 2 ) );
		return hash == -1 ? 493579437 : hash; // -1 is not a valid hash code
	}

}
