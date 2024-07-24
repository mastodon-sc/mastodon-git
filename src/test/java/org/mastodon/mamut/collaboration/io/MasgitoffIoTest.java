package org.mastodon.mamut.collaboration.io;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.collaboration.utils.ModelAsserts;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

public class MasgitoffIoTest
{

	@Test
	public void test() throws SpimDataException, IOException
	{
		try (final Context context = new Context())
		{
			final ProjectModel projectModel = ProjectLoader.open( "/home/arzt/devel/mastodon/mastodon-git/src/test/resources/org/mastodon/mamut/collaboration/tiny/tiny-project.mastodon", context );
			final ModelGraph graph = projectModel.getModel().getGraph();
			Iterator< Spot > iterator = graph.vertices().iterator();
			iterator.next().setLabel( "some spot" );
			iterator.next().setLabel( "some other spot" );
			final File file = new File( "tmp.masgitoff" );
			if ( file.isDirectory() )
				FileUtils.deleteDirectory( file );
			MasgitoffIo.writeMasgitoff( projectModel.getModel(), file, new MasgitoffIo.MasgitoffIds( graph ) );
			final ModelGraph newGraph = MasgitoffIo.readMasgitoff( file );
			ModelAsserts.assertGraphEquals( graph, newGraph );
		}
	}

}
