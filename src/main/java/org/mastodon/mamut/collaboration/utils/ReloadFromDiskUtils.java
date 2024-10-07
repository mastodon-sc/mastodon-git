/*-
 * #%L
 * mastodon-git
 * %%
 * Copyright (C) 2023 - 2024 Matthias Arzt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.mamut.collaboration.utils;

import java.io.File;
import java.io.IOException;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.collaboration.io.MasgitoffIo;
import org.mastodon.mamut.io.importer.ModelImporter;
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
		Model model = projectModel.getModel();
		try (
				AutoClosableModelImporter ignored = new AutoClosableModelImporter( model ); // this pauses listeners and resets the undo history
		)
		{
			MasgitoffIo.readMasgitoff( model, new File( projectModel.getProject().getProjectRoot(), "model" ) );
		}
	}

	private static class AutoClosableModelImporter extends ModelImporter implements AutoCloseable
	{
		protected AutoClosableModelImporter( Model model )
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
