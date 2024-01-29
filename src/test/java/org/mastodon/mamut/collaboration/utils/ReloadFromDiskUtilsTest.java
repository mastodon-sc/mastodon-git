package org.mastodon.mamut.collaboration.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.Test;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.collaboration.TestResources;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.io.project.MamutProjectIO;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

/**
 * Test for {@link ReloadFromDiskUtils}.
 */
public class ReloadFromDiskUtilsTest
{

	@Test
	public void testReloadFromDisk() throws IOException, URISyntaxException, SpimDataException
	{
		try (Context context = new Context())
		{
			Path tmp = Files.createTempDirectory( "test" );
			Files.copy( TestResources.asPath( "reload/tiny-dataset.h5" ), tmp.resolve( "tiny-dataset.h5" ) );
			Files.copy( TestResources.asPath( "reload/tiny-dataset.xml" ), tmp.resolve( "tiny-dataset.xml" ) );
			Path projectFile = tmp.resolve( "project.mastodon" );
			Files.copy( TestResources.asPath( "reload/project1.mastodon" ), projectFile );
			ProjectModel open = ProjectLoader.open( projectFile.toString(), context );
			for ( int i = 2; i <= 6; i++ )
			{
				Path resource = TestResources.asPath( "reload/project" + i + ".mastodon" );
				Files.copy( resource, projectFile, StandardCopyOption.REPLACE_EXISTING );
				ReloadFromDiskUtils.reloadFromDisk( open );
				Model expected = ReloadFromDiskUtils.loadModel( MamutProjectIO.load( resource.toString() ) );
				ModelAsserts.assertModelEquals( expected, open.getModel() );
			}
		}
	}
}
