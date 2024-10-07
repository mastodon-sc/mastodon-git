package org.mastodon.mamut.collaboration.io.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mastodon.mamut.collaboration.io.MasgitoffIds;
import org.mastodon.mamut.collaboration.io.MasgitoffIo;
import org.mastodon.mamut.model.Model;

public class MasgitoffSaver implements Saver< Void >
{

	private final Path path;

	public MasgitoffSaver( final Path path )
	{
		this.path = path;
	}

	@Override
	public Pair< Model, Void > open() throws IOException
	{
		return Pair.of( MasgitoffIo.readMasgitoff( path.toFile() ), null );
	}

	@Override
	public void save( final Model model, final Void details ) throws IOException
	{
		if ( Files.isDirectory( path ) )
			FileUtils.deleteDirectory( path.toFile() );
		MasgitoffIo.writeMasgitoff( model, path.toFile() );
	}

	@Override
	public Pair< Model, Void > createEmpty()
	{
		final Model model = new Model();
		return Pair.of( model, null );
	}
}
