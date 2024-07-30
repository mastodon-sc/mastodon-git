package org.mastodon.mamut.collaboration.io.benchmark;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

class GrowingGraphExample
{
	private static final String original = "/home/arzt/Datasets/Mette/E2.mastodon";

	private static final String empty = "/home/arzt/Datasets/Mette/empty.mastodon";

	private final ModelGraph fullGraph;

	private final ProjectModel growingProject;

	private final ModelGraph growingGraph;

	private final GraphCopier copier;

	public GrowingGraphExample( final Context context ) throws SpimDataException, IOException
	{
		final Path open1 = Paths.get( original );
		final ProjectModel fullProject = ProjectLoader.open( open1.toFile().getAbsolutePath(), context );
		fullGraph = fullProject.getModel().getGraph();
		removeWrongEdges( fullProject.getModel().getGraph() );
		final Path open = Paths.get( empty );
		growingProject = ProjectLoader.open( open.toFile().getAbsolutePath(), context );
		growingGraph = growingProject.getModel().getGraph();
		copier = new GraphCopier( fullGraph, growingGraph );
	}

	private static void removeWrongEdges( final ModelGraph graph )
	{
		final Spot ref1 = graph.vertexRef();
		final Spot ref2 = graph.vertexRef();
		try
		{
			for ( final Link link : graph.edges() )
			{
				final Spot source = link.getSource( ref1 );
				final Spot target = link.getTarget( ref2 );
				if ( source.getTimepoint() >= target.getTimepoint() )
					graph.remove( link );
			}
		}
		finally
		{
			graph.releaseRef( ref1 );
			graph.releaseRef( ref2 );
		}
	}

	public int getCompletion()
	{
		return growingGraph.vertices().size() * 100 / fullGraph.vertices().size();
	}

	public boolean hasNext()
	{
		return copier.hasNextSpot();// && getCompletion() < 25;
	}

	public void grow()
	{
		for ( int i = 0; i < 900 && copier.hasNextSpot(); i++ )
			copier.copyNextSpot();
	}

	public ProjectModel getProject()
	{
		return growingProject;
	}
}
