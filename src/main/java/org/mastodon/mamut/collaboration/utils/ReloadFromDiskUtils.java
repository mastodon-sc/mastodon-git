package org.mastodon.mamut.collaboration.utils;

import java.io.IOException;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.io.project.MamutProject;
import org.mastodon.mamut.model.Model;
import org.mastodon.model.AbstractModelImporter;

/**
 * Utility class that provides "reload from disk" functionality for
 * Mastodon's {@link ProjectModel}/
 */
public class ReloadFromDiskUtils
{
	private ReloadFromDiskUtils()
	{
		// Prevent instantiation of utility class.
	}

	/**
	 * Reloads the model from disk.
	 * <br>
	 * This clears the undo history and reloads the ModelGraph and tags.
	 * and tags. It does not reload the feature model, project XML or dataset XML.
	 */
	public static void reloadFromDisk( ProjectModel projectModel ) throws IOException
	{
		try (
				MamutProject.ProjectReader reader = projectModel.getProject().openForReading();
				ModelImporter ignored = new ModelImporter( projectModel.getModel() ); // this pauses listeners and resets the undo history
		)
		{
			projectModel.getModel().loadRaw( reader );
		}
	}

	private static class ModelImporter extends AbstractModelImporter< Model > implements AutoCloseable
	{
		protected ModelImporter( Model model )
		{
			super( model );
			startImport();
		}

		@Override
		public void close()
		{
			finishImport();
		}
	}
}
