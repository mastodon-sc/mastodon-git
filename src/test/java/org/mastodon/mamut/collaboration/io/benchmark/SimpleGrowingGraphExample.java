package org.mastodon.mamut.collaboration.io.benchmark;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.collaboration.utils.ModelAsserts;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

class SimpleGrowingGraphExample implements GrowingGraphExample
{
	private static final String original = "/home/arzt/Datasets/Mette/E2.mastodon";

	private static final String empty = "/home/arzt/Datasets/Mette/empty.mastodon";

	private final ModelGraph fullGraph;

	private final ProjectModel growingProject;

	private final ModelGraph growingGraph;

	private final GraphCopier copier;

	public SimpleGrowingGraphExample( final Context context ) throws SpimDataException, IOException
	{
		final Path open1 = Paths.get( original );
		final ProjectModel fullProject = ProjectLoader.open( open1.toFile().getAbsolutePath(), context );
		fullGraph = fullProject.getModel().getGraph();
		GrowingGraphExample.removeWrongEdges( fullProject.getModel().getGraph() );
		final Path open = Paths.get( empty );
		growingProject = ProjectLoader.open( open.toFile().getAbsolutePath(), context );
		growingGraph = growingProject.getModel().getGraph();
		copier = new GraphCopier( fullGraph, growingGraph );
	}

	@Override
	public int getCompletion()
	{
		return growingGraph.vertices().size() * 100 / fullGraph.vertices().size();
	}

	@Override
	public boolean hasNext()
	{
		return copier.hasNextSpot();// && getCompletion() < 25;
	}

	@Override
	public void grow()
	{
		for ( int i = 0; i < 900 && copier.hasNextSpot(); i++ )
			copier.copyNextSpot();
	}

	@Override
	public ProjectModel getProject()
	{
		return growingProject;
	}

	@Override
	public void assertEqualsOriginal( Model model )
	{
		ModelAsserts.assertGraphEquals( fullGraph, model.getGraph() );
	}
}
