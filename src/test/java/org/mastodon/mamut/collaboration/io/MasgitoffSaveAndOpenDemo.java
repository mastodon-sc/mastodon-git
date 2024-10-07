package org.mastodon.mamut.collaboration.io;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.collaboration.TestResources;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.io.project.MamutProject;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

public class MasgitoffSaveAndOpenDemo
{
	public static void main( final String... args ) throws SpimDataException, IOException, URISyntaxException
	{
		// initialize
		final Context context = new Context();
		final String exampleMastodonFile = TestResources.asPath( "reload/project6.mastodon" ).toString();
		final ProjectModel projectModel = ProjectLoader.open( exampleMastodonFile, context );
		final File folder = Files.createTempDirectory( "masgitoff" ).toFile();
		folder.deleteOnExit();

		// save and open
		MasgitoffProjectSaver.saveProject( folder, projectModel );
		final ProjectModel projectModelReopened = MasgitoffProjectLoader.open( new MamutProject( folder ), context, true, false );

		// launch gui
		final MainWindow mainWindow = new MainWindow( projectModelReopened );
		mainWindow.setLocationByPlatform( true );
		mainWindow.setVisible( true );
	}

}
