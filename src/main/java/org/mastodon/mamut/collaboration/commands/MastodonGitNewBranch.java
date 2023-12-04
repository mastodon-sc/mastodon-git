package org.mastodon.mamut.collaboration.commands;

import org.mastodon.mamut.collaboration.ErrorDialog;
import org.mastodon.mamut.collaboration.MastodonGitRepository;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin( type = Command.class, label = "Create New Branch", visible = false )
public class MastodonGitNewBranch extends AbstractCancellable implements Command
{

	@Parameter
	private MastodonGitRepository repository;

	@Parameter( label = "Branch name", persist = false )
	private String branchName;

	@Override
	public void run()
	{
		try
		{
			repository.createNewBranch( branchName );
		}
		catch ( Exception e )
		{
			ErrorDialog.showErrorMessage( "Create New Branch", e );
		}
	}
}
