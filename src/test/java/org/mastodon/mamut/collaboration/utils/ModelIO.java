package org.mastodon.mamut.collaboration.utils;

import java.io.IOException;

import org.mastodon.graph.io.RawGraphIO;
import org.mastodon.mamut.io.project.MamutProject;
import org.mastodon.mamut.io.project.MamutProjectIO;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;

public class ModelIO
{
	private ModelIO()
	{
		// prevent from instantiation
	}

	public static Model open( String file ) throws IOException
	{
		MamutProject project = MamutProjectIO.load( file );
		final Model model = new Model( project.getSpaceUnits(), project.getTimeUnits() );
		try (final MamutProject.ProjectReader reader = project.openForReading())
		{
			final RawGraphIO.FileIdToGraphMap< Spot, Link > idmap = model.loadRaw( reader );
		}
		return model;
	}
}
