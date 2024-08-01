package org.mastodon.mamut.collaboration.io.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.io.ProjectSaver;
import org.mastodon.mamut.model.Model;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

class DefaultSaver implements Saver< ProjectModel >
{
	private final Path path;

	private final Context context;

	public DefaultSaver( final Context context, final Path path )
	{
		this.context = context;
		this.path = path;
	}

	@Override
	public Pair< Model, ProjectModel > open() throws IOException
	{
		return open( path );
	}

	private Pair< Model, ProjectModel > open( final Path path ) throws IOException
	{
		try
		{
			final ProjectModel pm = ProjectLoader.open( path.toFile().getAbsolutePath(), context );
			return Pair.of( pm.getModel(), pm );
		}
		catch ( final SpimDataException e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public void save( final Model model, final ProjectModel details ) throws IOException
	{
		if ( Files.exists( path ) )
			FileUtils.deleteDirectory( path.toFile() );

		Files.createDirectory( path );
		ProjectSaver.saveProject( path.toFile(), details );
	}

	@Override
	public Pair< Model, ProjectModel > createEmpty() throws IOException
	{
		return open( Paths.get( "/home/arzt/Datasets/Mette/empty.mastodon" ) );
	}
}
