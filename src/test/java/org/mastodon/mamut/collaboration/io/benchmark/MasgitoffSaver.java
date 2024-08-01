package org.mastodon.mamut.collaboration.io.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mastodon.mamut.collaboration.io.MasgitoffIds;
import org.mastodon.mamut.collaboration.io.MasgitoffIo;
import org.mastodon.mamut.model.Model;

public class MasgitoffSaver implements Saver< MasgitoffIds >
{

	private final Path path;

	public MasgitoffSaver( final Path path )
	{
		this.path = path;
	}

	@Override
	public Pair< Model, MasgitoffIds > open() throws IOException
	{
		return MasgitoffIo.readMasgitoff( path.toFile() );
	}

	@Override
	public void save( final Model model, final MasgitoffIds details ) throws IOException
	{
		if ( Files.isDirectory( path ) )
			FileUtils.deleteDirectory( path.toFile() );
		MasgitoffIo.writeMasgitoff( model, path.toFile(), details );
	}

	@Override
	public Pair< Model, MasgitoffIds > createEmpty()
	{
		final Model model = new Model();
		final MasgitoffIds masgitoffIds = new MasgitoffIds( model.getGraph() );
		return Pair.of( model, masgitoffIds );
	}
}
