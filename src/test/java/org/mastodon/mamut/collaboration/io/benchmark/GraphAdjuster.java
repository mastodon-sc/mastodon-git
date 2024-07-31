package org.mastodon.mamut.collaboration.io.benchmark;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.util.Localizables;

import org.mastodon.collection.RefSet;
import org.mastodon.collection.ref.RefRefHashMap;
import org.mastodon.collection.ref.RefSetImp;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.util.TreeUtils;

/**
 * Feed in two graphs modalA and copy. The copy is adjusted to match the modalA.
 * A minimal number of add and delete operations are performed to make the copy match the modalA.
 */
public class GraphAdjuster
{

	private final Model modelA;

	private final Model modelB;

	private final double[][] covarianceA = new double[ 3 ][ 3 ];

	private final double[][] covarianceB = new double[ 3 ][ 3 ];

	private final RefSet< Spot > aOnlySpots;

	private final RefSet< Spot > bOnlySpots;

	private final RefRefHashMap< Spot, Spot > spotsAtoB;

	public GraphAdjuster( final Model modelA, final Model modelB )
	{
		this.modelA = modelA;
		this.modelB = modelB;
		this.aOnlySpots = new RefSetImp<>( modelA.getGraph().vertices().getRefPool() );
		this.bOnlySpots = new RefSetImp<>( modelB.getGraph().vertices().getRefPool() );
		this.spotsAtoB = new RefRefHashMap<>( modelA.getGraph().vertices().getRefPool(), modelB.getGraph().vertices().getRefPool() );
	}

	/**
	 * Adjust the copy to match the modalA.
	 *
	 * @param original the modalA graph.
	 * @param copy     the copy graph.
	 */
	public static void adjust( final Model original, final Model copy )
	{
		new GraphAdjuster( original, copy ).adjust();
	}

	private void adjust()
	{
		initializeSpotsAtoB();
		initializeAOnlySpots();
		initializeBOnlySpots();
		deleteBOnlySpots();
		copyAOnlySpots();
		deleteAndCopyLinks();
	}

	private void deleteAndCopyLinks()
	{
		final ModelGraph graphB = modelB.getGraph();
		final Link refEdgeB = graphB.edgeRef();
		final Set< Link > bOnlyLinks = new RefSetImp<>( graphB.edges().getRefPool() );
		bOnlyLinks.addAll( graphB.edges() );

		for ( final Link linkA : modelA.getGraph().edges() )
		{
			final Spot sourceA = linkA.getSource();
			final Spot targetA = linkA.getTarget();
			final Spot sourceB = spotsAtoB.get( sourceA );
			final Spot targetB = spotsAtoB.get( targetA );
			final Link linkB = graphB.getEdge( sourceB, targetB, refEdgeB );
			final boolean isAOnlyLink = linkB == null;
			if ( isAOnlyLink )
				graphB.insertEdge( sourceB, linkA.getSourceOutIndex(), targetB, linkA.getTargetInIndex(), refEdgeB ).init();
			else
				bOnlyLinks.remove( linkB );
		}

		for ( final Link linkB : bOnlyLinks )
			graphB.remove( linkB );
	}

	private void copyAOnlySpots()
	{
		final Spot refB = modelB.getGraph().vertexRef();
		final double[] position = new double[ 3 ];
		final double[][] covariance = new double[ 3 ][ 3 ];
		for ( final Spot spotA : aOnlySpots )
		{
			final Spot spotB = modelB.getGraph().addVertex( refB );
			spotA.localize( position );
			spotA.getCovariance( covariance );
			spotB.init( spotA.getTimepoint(), position, covariance );
			if ( spotA.isLabelSet() )
				spotB.setLabel( spotA.getLabel() );
			spotsAtoB.put( spotA, spotB );
		}
	}

	private void deleteBOnlySpots()
	{
		for ( final Spot spot : bOnlySpots )
			modelB.getGraph().remove( spot );
	}

	private void initializeBOnlySpots()
	{
		bOnlySpots.addAll( modelB.getGraph().vertices() );
		bOnlySpots.removeAll( spotsAtoB.values() );
	}

	private void initializeAOnlySpots()
	{
		aOnlySpots.addAll( modelA.getGraph().vertices() );
		aOnlySpots.removeAll( spotsAtoB.keySet() );
	}

	private void initializeSpotsAtoB()
	{
		// FIXME: this is a bit slow
		final SpatioTemporalIndex< Spot > temporalIndexA = modelA.getSpatioTemporalIndex();
		final SpatioTemporalIndex< Spot > temporalIndexB = modelB.getSpatioTemporalIndex();
		final int max = TreeUtils.getMaxTimepoint( modelB );
		for ( int t = 0; t <= max; t++ )
		{
			final SpatialIndex< Spot > spatialIndexA = temporalIndexA.getSpatialIndex( t );
			final SpatialIndex< Spot > spatialIndexB = temporalIndexB.getSpatialIndex( t );
			final NearestNeighborSearch< Spot > searchA = spatialIndexA.getNearestNeighborSearch();
			for ( final Spot spotB : spatialIndexB )
			{
				searchA.search( spotB );
				final Spot spotA = searchA.getSampler().get();
				if ( spotA == null )
					continue;
				final boolean same = spotEquals( spotA, spotB );
				if ( same )
					spotsAtoB.put( spotA, spotB );
			}
		}

//		final ExactPositionToSpotIndex index = new ExactPositionToSpotIndex( modelA.getGraph().vertices().getRefPool() );
//		for ( final Spot spotA : modelA.getGraph().vertices() )
//			index.put( spotA );
//
//		final Spot ref = modelA.getGraph().vertexRef();
//		for ( final Spot spotB : modelB.getGraph().vertices() )
//		{
//			final Spot spotA = index.get( spotB, ref );
//			if ( spotA == null )
//				continue;
//			final boolean same = spotEquals( spotB, spotA );
//			if ( same )
//				spotsAtoB.put( spotA, spotB );
//		}

	}

	private boolean spotEquals( final Spot a, final Spot b )
	{
		return a.getTimepoint() == b.getTimepoint()
				&& Localizables.equals( a, b )
				&& Objects.equals( getSpotLabel( a ), getSpotLabel( b ) )
				&& covarianceEquals( a, b );
	}

	private static String getSpotLabel( final Spot spot )
	{
		return spot.isLabelSet() ? spot.getLabel() : null;
	}

	private boolean covarianceEquals( final Spot a, final Spot b )
	{
		a.getCovariance( covarianceA );
		b.getCovariance( covarianceB );
		return Arrays.deepEquals( covarianceA, covarianceB );
	}

}
