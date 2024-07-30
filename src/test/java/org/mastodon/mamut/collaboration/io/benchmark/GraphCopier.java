package org.mastodon.mamut.collaboration.io.benchmark;

import java.util.Iterator;

import org.mastodon.collection.RefRefMap;
import org.mastodon.collection.ref.RefRefHashMap;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;

class GraphCopier implements AutoCloseable
{

	private final ModelGraph source;

	private final ModelGraph target;

	private final Spot refA;

	private final Spot refB;

	private final Spot refB2;

	private final RefRefMap< Spot, Spot > mapAB;

	private final double[] position = new double[ 3 ];

	private final double[][] cov = new double[ 3 ][ 3 ];

	private final Iterator< Spot > iteratorA;

	GraphCopier( final ModelGraph source, final ModelGraph target )
	{
		this.source = source;
		this.target = target;
		refA = source.vertexRef();
		refB = target.vertexRef();
		refB2 = target.vertexRef();
		mapAB = new RefRefHashMap<>( source.vertices().getRefPool(), target.vertices().getRefPool() );
		iteratorA = source.vertices().iterator();
	}

	public boolean hasNextSpot()
	{
		return iteratorA.hasNext();
	}

	public void copyNextSpot()
	{
		final Spot spotA = iteratorA.next();
		final int timepoint = spotA.getTimepoint();
		spotA.localize( position );
		spotA.getCovariance( cov );
		final Spot spotB = target.addVertex( refB );
		spotB.init( timepoint, position, cov );
		spotB.setLabel( spotA.getLabel() );
		mapAB.put( spotA, spotB );
		for ( final Link sLink : spotA.incomingEdges() )
		{
			final Spot sourceA = sLink.getSource( refA );
			final Spot sourceB = mapAB.get( sourceA, refB2 );
			if ( sourceB != null )
				target.addEdge( sourceB, spotB ).init();
		}
		for ( final Link sLink : spotA.outgoingEdges() )
		{
			final Spot targetA = sLink.getTarget( refA );
			final Spot targetB = mapAB.get( targetA, refB2 );
			if ( targetB != null )
				target.addEdge( spotB, targetB ).init();
		}
	}

	@Override
	public void close()
	{
		source.releaseRef( refA );
		target.releaseRef( refB );
		target.releaseRef( refB2 );
	}
}
