package org.mastodon.mamut.collaboration.io.benchmark;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

import org.mastodon.collection.RefRefMap;
import org.mastodon.collection.RefSet;
import org.mastodon.collection.ref.RefRefHashMap;
import org.mastodon.collection.ref.RefSetImp;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.collaboration.utils.ModelAsserts;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

class AddDeleteGrowingGraphExample implements GrowingGraphExample
{
	private static final String original = "/home/arzt/Datasets/Mette/E2.mastodon";

	public static final String empty = "/home/arzt/Datasets/Mette/empty.mastodon";

	private final Random random = new Random( 0 );

	private final ModelGraph fullGraph;

	private final ProjectModel growingProject;

	private final ModelGraph growingGraph;

	private final RefRefMap< Spot, Spot > mapFG; // maps spot from full graph to growing graph

	private final RefSet< Link > growCandidates; // links to be added to growing graph.
	// Links for which the source spot is in the growing graph. Or the source spot is a root spot.

	private final RefSet< Link > shrinkCandidates; // links to be deleted from growing graph. Links for which the tarhet
	// Links for which the target spot has no outgoing links.

	private final double[] position = new double[ 3 ];

	private final double[][] cov = new double[ 3 ][ 3 ];

	private static final int ADDITIONS_PER_STEP = 9000;

	private static final int DELETIONS_PER_STEP = 1000;

	private boolean first = true;

	public AddDeleteGrowingGraphExample( final Context context ) throws SpimDataException, IOException
	{
		final ProjectModel fullProject = ProjectLoader.open( original, context );
		fullGraph = fullProject.getModel().getGraph();
		GrowingGraphExample.removeWrongEdges( fullProject.getModel().getGraph() );
		growingProject = ProjectLoader.open( empty, context );
		growingGraph = growingProject.getModel().getGraph();
		mapFG = new RefRefHashMap<>( fullGraph.vertices().getRefPool(), growingGraph.vertices().getRefPool() );
		growCandidates = new RefSetImp<>( fullGraph.edges().getRefPool() );
		shrinkCandidates = new RefSetImp<>( fullGraph.edges().getRefPool() );
	}

	private boolean copyLink( final Link link )
	{
		final Spot vrefF1 = fullGraph.vertexRef();
		final Spot vrefF2 = fullGraph.vertexRef();
		final Spot vrefG1 = growingGraph.vertexRef();
		final Spot vrefG2 = growingGraph.vertexRef();
		final Link erefG = growingGraph.edgeRef();
		try
		{
			growCandidates.remove( link );
			final Spot source = link.getSource( vrefF1 );
			final Spot target = link.getTarget( vrefF2 );
			Spot sourceG = mapFG.get( source, vrefG1 );
			Spot targetG = mapFG.get( target, vrefG2 );
			if ( targetG != null )
				return false;
			if ( sourceG == null && source.incomingEdges().isEmpty() )
				sourceG = copySpot( source, vrefG1 );
			targetG = copySpot( target, vrefG2 );
			growingGraph.insertEdge( sourceG, link.getSourceOutIndex(), targetG, link.getTargetInIndex(), erefG ).init();
			growCandidates.remove( link );
			shrinkCandidates.add( link );
			return true;
		}
		finally
		{
			fullGraph.releaseRef( vrefF1 );
			fullGraph.releaseRef( vrefF2 );
			growingGraph.releaseRef( vrefG1 );
			growingGraph.releaseRef( vrefG2 );
			growingGraph.releaseRef( erefG );
		}
	}

	private Spot copySpot( final Spot spotF, final Spot ref )
	{
		spotF.localize( position );
		spotF.getCovariance( cov );
		final Spot spotG = growingGraph.addVertex( ref );
		spotG.init( spotF.getTimepoint(), position, cov );
		if ( spotF.isLabelSet() )
			spotG.setLabel( spotF.getLabel() );
		mapFG.put( spotF, spotG );
		for ( final Link sLink : spotF.outgoingEdges() )
			growCandidates.add( sLink );
		return spotG;
	}

	/**
	 * Remove the link from the growing graph that corresponds to linkF in the full graph.
	 * Only remove the link if the target spot has no outgoing links.
	 */
	public boolean removeLink( final Link linkF )
	{
		final Spot spotRefF1 = fullGraph.vertexRef();
		final Spot spotRefF2 = fullGraph.vertexRef();
		final Spot spotRefG1 = growingGraph.vertexRef();
		final Spot spotRefG2 = growingGraph.vertexRef();
		try
		{
			shrinkCandidates.remove( linkF );
			final Spot sourceF = linkF.getSource( spotRefF1 );
			final Spot targetF = linkF.getTarget( spotRefF2 );
			final Spot sourceG = mapFG.get( sourceF, spotRefG2 );
			final Spot targetG = mapFG.get( targetF, spotRefG1 );
			if ( targetG == null )
				throw new AssertionError();
			if ( !targetG.outgoingEdges().isEmpty() )
				return false;
			removeSpot( targetF, targetG );
			shrinkCandidates.remove( linkF );
			growCandidates.add( linkF );
			if ( sourceG.outgoingEdges().isEmpty() )
				for ( final Link sLink : sourceF.incomingEdges() )
					shrinkCandidates.add( sLink );
			return true;
		}
		finally
		{
			fullGraph.releaseRef( spotRefF1 );
			fullGraph.releaseRef( spotRefF2 );
			growingGraph.releaseRef( spotRefG1 );
		}
	}

	private void removeSpot( Spot spotF, Spot spotG )
	{
		mapFG.remove( spotF );
		growingGraph.remove( spotG );
		for ( final Link sLink : spotF.outgoingEdges() )
			growCandidates.remove( sLink );
	}

	@Override
	public int getCompletion()
	{
		return growingGraph.vertices().size() * 100 / fullGraph.vertices().size();
	}

	@Override
	public boolean hasNext()
	{
		return growingGraph.vertices().size() < fullGraph.vertices().size();
	}

	@Override
	public void grow( final ProjectModel model )
	{
		grow();
		GraphAdjuster.adjust( growingProject.getModel(), model.getModel() );
	}

	private void grow()
	{
		if ( first )
		{
			first = false;
			copyIsolatedSpots();
		}

		for ( int i = 0; i < DELETIONS_PER_STEP; )
		{
			if ( shrinkCandidates.isEmpty() )
				break;
			final Link link = pseudoRandomlyPickFromSet( shrinkCandidates );
			if ( removeLink( link ) )
				i++;
		}

		for ( int i = 0; i < ADDITIONS_PER_STEP; )
		{
			if ( growCandidates.isEmpty() )
			{
				if ( hasNext() )
					throw new AssertionError( "There should always be grow candidates when there are spots to add.");
				break;
			}
			final Link link = pseudoRandomlyPickFromSet( growCandidates );
			if ( copyLink( link ) )
				i++;
		}
	}

	private void copyIsolatedSpots()
	{
		final Spot ref = growingGraph.vertexRef();
		try
		{
			for ( final Spot spotF : fullGraph.vertices() )
				if ( spotF.incomingEdges().isEmpty() )
				{
					final Spot spotG = copySpot( spotF, ref );
					mapFG.put( spotF, spotG );
				}
		}
		finally
		{
			growingGraph.releaseRef( ref );
		}
	}

	private Link pseudoRandomlyPickFromSet( final RefSet< Link > set )
	{
		if ( set.isEmpty() )
			throw new NoSuchElementException();
		final int n = set.size();
		final int k = random.nextInt( Math.min( 4, n ) );
		final Iterator< Link > iterator = set.iterator();
		for ( int i = 0; i < k; i++ )
		{
			iterator.next();
		}
		return iterator.next();
	}

	@Override
	public void assertEqualsOriginal( Model model )
	{
		ModelAsserts.assertGraphEquals( fullGraph, model.getGraph() );
	}
}
