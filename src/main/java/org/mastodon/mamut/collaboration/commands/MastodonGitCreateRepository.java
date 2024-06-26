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
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin( type = Command.class,
		label = "Share Current Project via GitHub or GitLab",
		visible = false )
public class MastodonGitCreateRepository extends AbstractCancellable implements Command
{
	@Parameter( visibility = ItemVisibility.MESSAGE )
	private final String text = "<html><body>"
			+ "<h1>Share Project</h1>"
			+ "<p>Share the current project on github or gitlab.</p>"
			+ "<p>Go to github.com or to your institute's gitlab and create a new repository.</p>"
			+ "<p>Then copy the URL of the repository and paste it below.</p>"
			+ "<p>(GitLab by default protects the main branch. You might want to unprotect it.)</p>"
			+ "<p>A copy of the shared project will be saved in the directory that you specify below.</p>"
			+ "<p>This directory is used by Mastodon-Git as local repository, all changes are saved there</p>"
			+ "<p>before they are uploaded to the specified URL.</p>";

	@Parameter
	private Callback callback;

	@Parameter( label = "URL on github or gitlab" )
	private String repositoryURL;

	@Parameter( label = "Directory to contain the repository", style = "directory" )
	private File directory;

	@Parameter( label = "Create new subdirectory", required = false )
	private boolean createSubdirectory = false;

	@Override
	public void run()
	{
		try
		{
			directory = NewDirectoryUtils.createRepositoryDirectory( createSubdirectory, directory, repositoryURL );
			callback.run( directory, repositoryURL );
		}
		catch ( Exception e )
		{
			ErrorDialog.showErrorMessage( "Share Project", e );
		}
	}

	public interface Callback
	{
		void run( File directory, String repositoryURL ) throws Exception;
	}
}
