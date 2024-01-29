package org.mastodon.mamut.collaboration.utils;

import java.io.IOException;

import org.mastodon.graph.io.RawGraphIO;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.io.project.MamutProject;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;

/**
 * Utility class that provides "reload from disk" functionality for
 * Mastodon's {@link ProjectModel}/
 */
public class ReloadFromDiskUtils
{

	/**
	 * Reloads the model from disk.
	 * <p>
	 * It does so by opening the Mastodon project again, and copying all the
	 * spots and links from the new model to the existing one.
	 */
	public static void reloadFromDisk( ProjectModel projectModel ) throws IOException
	{
		try (MamutProject.ProjectReader reader = projectModel.getProject().openForReading())
		{
			projectModel.getModel().loadRaw( reader );
		}
	}

	public static Model loadModel( MamutProject project ) throws IOException
	{
		final Model model = new Model( project.getSpaceUnits(), project.getTimeUnits() );
		try (final MamutProject.ProjectReader reader = project.openForReading())
		{
			final RawGraphIO.FileIdToGraphMap< Spot, Link > idmap = model.loadRaw( reader );
		}
		return model;
	}

}
