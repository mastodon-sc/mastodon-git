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
package org.mastodon.mamut.collaboration.commands;

import java.io.File;

import org.mastodon.mamut.collaboration.dialogs.ErrorDialog;
import org.mastodon.mamut.collaboration.MastodonGitRepository;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

// TODOs:
// - warn if parentDirectory already exists
// - warn if repositoryName already exists and the corresponding directory is not empty
// - fill repositoryName with a default value based on the repositoryURL
@Plugin( type = Command.class,
		label = "Mastodon Git - Download Shared Project (clone)",
		menuPath = "Plugins > Tracking > Mastodon > Mastodon Collaborative (Git) > Download Shared Project" )
public class MastodonGitCloneRepository extends AbstractCancellable implements Command
{
	@Parameter
	private Context context;

	@Parameter( label = "URL on github or gitlab", description = URL_DESCRIPTION )
	private String repositoryURL;

	private static final String URL_DESCRIPTION = "<html><body>"
			+ "Here are two examples of valid URLs:<br>"
			+ "<ul>"
			+ "<li>https://github.com/username/repositoryname.git</li>"
			+ "<li>git@github.com:username/repositoryname.git (if you use SSH to authenticate)</li>"
			+ "</ul>"
			+ "</body></html>";

	@Parameter( label = "Directory, to store the project:", style = "directory", description = DIRECTORY_DESCRIPTION )
	private File directory;

	private static final String DIRECTORY_DESCRIPTION = "<html><body>"
			+ "A copy of the shared project will be downloaded to your computer.<br>"
			+ "Please select a directory where to store it.<br>"
			+ "The directory should be empty, or select \"Create new subdirectory\"."
			+ "</body></html>";

	@Parameter( label = "Create new subdirectory", required = false, description = CREATE_SUBDIRECTORY_DESCRIPTION )
	private boolean createSubdirectory = false;

	private static final String CREATE_SUBDIRECTORY_DESCRIPTION = "<html><body>"
			+ "If selected, a new subdirectory will be created in the selected directory.<br>"
			+ "The name of the subdirectory will be the name of the repository."
			+ "</body></html>";

	@Override
	public void run()
	{
		try
		{
			directory = NewDirectoryUtils.createRepositoryDirectory( createSubdirectory, directory, repositoryURL );
			MastodonGitRepository.cloneRepository( repositoryURL, directory );
			MastodonGitRepository.openProjectInRepository( context, directory );
		}
		catch ( final Exception e )
		{
			ErrorDialog.showErrorMessage( "Download Shared Project (Clone)", e );
		}
	}

}
