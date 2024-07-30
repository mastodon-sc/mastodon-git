package org.mastodon.mamut.collaboration.io.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.collaboration.io.MasgitoffIds;
import org.mastodon.mamut.collaboration.io.MasgitoffIo;
import org.mastodon.mamut.model.Model;

public class MasgitoffSaver implements Saver
{

	private final ProjectModel projectModel;

	private final Path path;

	private final MasgitoffIds masgitoffIds;

	public MasgitoffSaver( final ProjectModel projectModel, final Path path )
	{
		this.projectModel = projectModel;
		this.path = path;
		this.masgitoffIds = new MasgitoffIds( projectModel.getModel().getGraph() );
	}

	@Override
	public void save() throws IOException
	{
		final Model model = projectModel.getModel();
		if ( Files.isDirectory( path ) )
			FileUtils.deleteDirectory( path.toFile() );
		MasgitoffIo.writeMasgitoff( model, path.toFile(), masgitoffIds );
	}
}
