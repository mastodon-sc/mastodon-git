package org.mastodon.mamut.collaboration.io.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.io.ProjectSaver;

class DefaultSaver implements Saver
{
	private final ProjectModel projectModel;

	private final Path path;

	public DefaultSaver( final ProjectModel projectModel, final Path path )
	{
		this.projectModel = projectModel;
		this.path = path;
	}

	@Override
	public void save() throws IOException
	{
		if ( Files.exists( path ) )
			FileUtils.deleteDirectory( path.toFile() );

		Files.createDirectory( path );
		ProjectSaver.saveProject( path.toFile(), projectModel );
	}
}
