package org.mastodon.mamut.collaboration.io;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.collaboration.utils.ModelAsserts;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.model.ModelGraph;
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
			final File file = new File( "tmp.masgitoff" );
			if ( file.isDirectory() )
				FileUtils.deleteDirectory( file );
			MasgitoffIo.writeMasgitoff( graph, file );
			final ModelGraph newGraph = MasgitoffIo.readMasgitoff( file );
			ModelAsserts.assertGraphEquals( graph, newGraph );
		}
	}

}
