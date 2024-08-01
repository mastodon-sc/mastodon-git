package org.mastodon.mamut.collaboration.io;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.collaboration.utils.ModelAsserts;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.util.TagHelper;
import org.mastodon.util.TagSetUtils;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

public class MasgitoffIoTest
{

	@Test
	public void test() throws SpimDataException, IOException
	{
		try (final Context context = new Context())
		{
			final Model model = initializeModel( context );
			final File file = new File( "tmp.masgitoff" );
			if ( file.isDirectory() )
				FileUtils.deleteDirectory( file );
			MasgitoffIo.writeMasgitoff( model, file, new MasgitoffIds( model.getGraph() ) );
			final Model newModel = MasgitoffIo.readMasgitoff( file ).getLeft();
			ModelAsserts.assertModelEquals( model, newModel );
		}
	}

	private static Model initializeModel( final Context context ) throws IOException, SpimDataException
	{
		final ProjectModel projectModel = ProjectLoader.open( "/home/arzt/devel/mastodon/mastodon-git/src/test/resources/org/mastodon/mamut/collaboration/tiny/tiny-project.mastodon", context );
		final Model model = projectModel.getModel();
		final ModelGraph graph = model.getGraph();
		final Spot someSpot = graph.vertices().getRefPool().getObject( 0, graph.vertexRef() );
		final Spot someOtherSpot = graph.vertices().getRefPool().getObject( 1, graph.vertexRef() );
		someSpot.setLabel( "some spot" );
		someOtherSpot.setLabel( "some other spot" );
		TagSetUtils.addNewTagSetToModel( model, "tagset", Arrays.asList( Pair.of( "a", 0xff0000 ), Pair.of( "b", 0x00ff00 ) ) );
		TagSetUtils.addNewTagSetToModel( model, "other tagset", Arrays.asList( Pair.of( "c", 0x0000ff ) ) );
		final TagHelper tagA = new TagHelper( model, "tagset", "a" );
		final TagHelper tagB = new TagHelper( model, "tagset", "b" );
		final TagHelper tagC = new TagHelper( model, "other tagset", "c" );
		tagA.tagSpot( someSpot );
		tagB.tagSpot( someOtherSpot );
		tagC.tagSpot( someOtherSpot );
		Link aLink = someSpot.outgoingEdges().get( 0 );
		tagA.tagLink( aLink );
		tagB.tagLink( aLink );
		return model;
	}

}
