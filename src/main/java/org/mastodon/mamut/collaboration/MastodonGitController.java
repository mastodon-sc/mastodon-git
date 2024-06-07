/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2022 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.mamut.collaboration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.mastodon.mamut.KeyConfigScopes;
import org.mastodon.mamut.collaboration.commands.MastodonGitCloneRepository;
import org.mastodon.mamut.collaboration.commands.MastodonGitCreateRepository;
import org.mastodon.mamut.collaboration.commands.MastodonGitNewBranch;
import org.mastodon.mamut.collaboration.dialogs.SetAuthorDialog;
import org.mastodon.mamut.collaboration.dialogs.CommitMessageDialog;
import org.mastodon.mamut.collaboration.dialogs.ErrorDialog;
import org.mastodon.mamut.collaboration.dialogs.NotificationDialog;
import org.mastodon.mamut.collaboration.settings.MastodonGitSettingsService;
import org.mastodon.mamut.collaboration.utils.ActionDescriptions;
import org.mastodon.mamut.collaboration.utils.BasicDescriptionProvider;
import org.mastodon.mamut.collaboration.utils.BasicMamutPlugin;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.mamut.collaboration.exceptions.GraphMergeConflictException;
import org.mastodon.mamut.collaboration.exceptions.GraphMergeException;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;

@Plugin( type = MamutPlugin.class )
public class MastodonGitController extends BasicMamutPlugin
{
	@Parameter
	private CommandService commandService;

	@Parameter
	private MastodonGitSettingsService settingsService;

	public static final ActionDescriptions< MastodonGitController > actionDescriptions = new ActionDescriptions<>( MastodonGitController.class );

	private static final String SHARE_PROJECT_ACTION_KEY = actionDescriptions.addActionDescription(
			"[mastodon git] share project",
			"Plugins > Collaborative (Git) > Initialize > Share Project",
			"Upload Mastodon project to a newly created git repository.",
			MastodonGitController::shareProject );

	private static final String CLONE_REPOSITORY_ACTION_KEY = actionDescriptions.addActionDescription(
			"[mastodon git] download shared project (clone)",
			"Plugins > Collaborative (Git) > Initialize > Download Shared Project (clone)",
			"Download a shared project, save a copy on the local disc and open it with Mastodon.",
			MastodonGitController::cloneGitRepository );

	private static final String SET_AUTHOR_ACTION_KEY = actionDescriptions.addActionDescription(
			"[mastodon git] set author name",
			"Plugins > Collaborative (Git) > Initialize > Set Author Name",
			"Set the author name that is used for your commits.",
			MastodonGitController::setAuthor );

	private static final String SYNCHRONIZE_ACTION_KEY = actionDescriptions.addActionDescription(
			"[mastodon git] synchronize (commit, pull, push)",
			"Plugins > Collaborative (Git) > Synchronize (commit, pull, push)",
			"Download remote changes and upload local changes.",
			MastodonGitController::synchronize );

	private static final String COMMIT_ACTION_KEY = actionDescriptions.addActionDescription(
			"[mastodon git] add save point (commit)",
			"Plugins > Collaborative (Git) > Add Save Point (commit)",
			"Commit changes to the git repository.",
			MastodonGitController::commit );

	private static final String PUSH_ACTION_KEY = actionDescriptions.addActionDescription(
			"[mastodon git] upload changes (push)",
			"Plugins > Collaborative (Git) > Upload Changes (push)",
			"Push local changed to the remote server.",
			MastodonGitController::push );

	private static final String PULL_ACTION_KEY = actionDescriptions.addActionDescription(
			"[mastodon git] download changes (pull)",
			"Plugins > Collaborative (Git) > Download Changes (pull)",
			"Download changes from the remote server and merge them with my changes.",
			MastodonGitController::pull );

	private static final String RESET_ACTION_KEY = actionDescriptions.addActionDescription(
			"[mastodon git] go back to latest save point (reset)",
			"Plugins > Collaborative (Git) > Go Back To Latest Save Point (reset)",
			"Discard all changes made since the last save point.",
			MastodonGitController::reset );

	private static final String NEW_BRANCH_ACTION_KEY = actionDescriptions.addActionDescription(
			"[mastodon git] new branch",
			"Plugins > Collaborative (Git) > Branches > Create New Branch",
			"Create a new branch in the git repository.",
			MastodonGitController::newBranch );

	private static final String SHOW_BRANCH_NAME_ACTION_KEY = actionDescriptions.addActionDescription(
			"[mastodon git] show branch name",
			"Plugins > Collaborative (Git) > Branches > Show Branch Name",
			"Show the name of the current git branch",
			MastodonGitController::showBranchName );

	private static final String SWITCH_ACTION_KEY = actionDescriptions.addActionDescription(
			"[mastodon git] switch branch",
			"Plugins > Collaborative (Git) > Branches > Switch Branch",
			"Switch to a different branch in the git repository.",
			MastodonGitController::switchBranch );

	private static final String MERGE_ACTION_KEY = actionDescriptions.addActionDescription(
			"[mastodon git] merge branch",
			"Plugins > Collaborative (Git) > Branches > Merge Branch",
			"Merge a branch into the current branch.",
			MastodonGitController::mergeBranch );

	private static final List< String > IN_REPOSITORY_ACTIONS = Arrays.asList(
			SYNCHRONIZE_ACTION_KEY,
			COMMIT_ACTION_KEY,
			PUSH_ACTION_KEY,
			PULL_ACTION_KEY,
			RESET_ACTION_KEY,
			NEW_BRANCH_ACTION_KEY,
			SWITCH_ACTION_KEY,
			MERGE_ACTION_KEY );

	private MastodonGitRepository repository;

	public MastodonGitController()
	{
		super( actionDescriptions );
	}

	@Override
	protected void initialize()
	{
		super.initialize();
		repository = new MastodonGitRepository( getProjectModel() );
		updateEnableCommands();
	}

	private void setAuthor()
	{
		settingsService.askForAuthorName();
	}

	private void shareProject()
	{
		if ( !settingsService.ensureAuthorIsSet( "Please set your author name before sharing a project." ) )
			return;

		final MastodonGitCreateRepository.Callback callback = ( final File directory, final String url ) -> {
			this.repository = MastodonGitRepository.shareProject( getProjectModel(), directory, url );
			updateEnableCommands();
		};
		commandService.run( MastodonGitCreateRepository.class, true, "callback", callback );
	}

	private void updateEnableCommands()
	{
		final boolean isRepository = repository.isRepository();
		IN_REPOSITORY_ACTIONS.forEach( action -> setActionEnabled( action, isRepository ) );
	}

	private void cloneGitRepository()
	{
		commandService.run( MastodonGitCloneRepository.class, true );
	}

	private void commit()
	{
		if ( !settingsService.ensureAuthorIsSet( "Please set your author name before adding a save point (commit)." ) )
			return;
		run( "Add Save Point (Commit)", () -> {
			if ( repository.isClean() )
				NotificationDialog.show( "Add Save Point (Commit)",
						"<html><body><font size=+4 color=green>&#10003</font> No changes to commit." );
			else
			{
				final String commitMessage = CommitMessageDialog.showDialog();
				if ( commitMessage == null )
					return;
				repository.commitWithoutSave( commitMessage );
			}
		} );
	}

	private void push()
	{
		run( "Upload Changes (Push)", () -> {
			repository.push();
			NotificationDialog.show( "Upload Changes (Push)",
					"<html><body><font size=+4 color=green>&#10003</font> Completed successfully." );
		} );
	}

	private void newBranch()
	{
		commandService.run( MastodonGitNewBranch.class, true, "repository", repository );
	}

	private void switchBranch()
	{
		try
		{
			String message = "Select a branch";
			try
			{
				repository.fetchAll();
			}
			catch ( final Exception e )
			{
				message += " \n(There was a failure downloading the latest branch changes.)";
			}
			final List< String > branches = repository.getBranches();
			final String currentBranch = repository.getCurrentBranch();
			// show JOptionPane that allows to select a branch
			final String selectedBranch = ( String ) JOptionPane.showInputDialog( null, message, "Switch Git Branch", JOptionPane.PLAIN_MESSAGE, null, branches.toArray(), currentBranch );
			if ( selectedBranch == null )
				return;
			// switch to selected branch
			run( "Switch To Branch", () -> repository.switchBranch( selectedBranch ) );
		}
		catch ( final Exception e )
		{
			ErrorDialog.showErrorMessage( "Select Branch", e );
		}
	}

	private void mergeBranch()
	{
		if ( !settingsService.ensureAuthorIsSet( "You need to set your author name before you can merge branches." ) )
			return;

		try
		{
			final List< String > branches = repository.getBranches();
			final String currentBranch = repository.getCurrentBranch();
			branches.remove( currentBranch );
			// show JOptionPane that allows to select a branch
			final String selectedBranch = ( String ) JOptionPane.showInputDialog( null, "Select a branch", "Switch Git Branch", JOptionPane.PLAIN_MESSAGE, null, branches.toArray(), null );
			if ( selectedBranch == null )
				return;
			repository.mergeBranch( selectedBranch );
		}
		catch ( final Exception e )
		{
			ErrorDialog.showErrorMessage( "Merge Branch", e );
		}
	}

	private void pull()
	{
		if ( !settingsService.ensureAuthorIsSet( "You need to set your author name before you can pull branches." ) )
			return;

		run( "Download Changes (Pull)", () -> {
			try
			{
				repository.pull();
			}
			catch ( final GraphMergeException e )
			{
				if ( !( e instanceof GraphMergeConflictException ) )
					e.printStackTrace();
				SwingUtilities.invokeLater( () -> suggestPullAlternative( e.getMessage() ) );
			}
		} );
	}

	private void suggestPullAlternative( final String errorMessage )
	{
		final String title = "Conflict During Download Of Changes (Pull)";
		final String message = "There was a merge conflict during the pull. Details:\n"
				+ "  " + errorMessage + "\n\n"
				+ "You made changes on your computer that could not be automatically\n"
				+ "merged with the changes on the server.\n\n"
				+ "You can either:\n"
				+ "  1. Throw away your local changes & local save points.\n"
				+ "  2. Or cancel (And maybe save your local changes to a new branch,\n"
				+ "             which you can then be merged into the remote branch.)\n";

		final String[] options = { "Discard Local Changes", "Cancel" };
		final int result = JOptionPane.showOptionDialog( null, message, title, JOptionPane.YES_NO_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, options, options[ 0 ] );
		if ( result == JOptionPane.YES_OPTION )
			resetToRemoteBranch();
	}

	private void resetToRemoteBranch()
	{
		run( "Throw Away All Local Changes (Reset To Remote)", () -> repository.resetToRemoteBranch() );
	}

	private void reset()
	{
		run( "Go Back To Last Save Point (Reset)", () -> repository.reset() );
	}

	private void showBranchName()
	{
		run( "Show Branch Name", () -> {
			final String longBranchName = repository.getCurrentBranch();
			final String shortBranchName = longBranchName.replaceAll( "^refs/heads/", "" );
			final String title = "Current Branch Name";
			final String message = "<html><body>The current branch is:<br><b>" + shortBranchName;
			SwingUtilities.invokeLater( () ->
					JOptionPane.showMessageDialog( null, message, title, JOptionPane.PLAIN_MESSAGE ) );
		} );
	}

	private void synchronize()
	{
		if ( !settingsService.ensureAuthorIsSet( "Please set your author name before syncing with the remote changes." ) )
			return;

		run( "Synchronize Changes", () -> {
			final boolean clean = repository.isClean();
			if ( !clean )
			{
				final String commitMessage = CommitMessageDialog.showDialog();
				if ( commitMessage == null )
					return;
				repository.commitWithoutSave( commitMessage );
			}
			try
			{
				repository.pull();
			}
			catch ( final GraphMergeException e )
			{
				if ( !( e instanceof GraphMergeConflictException ) )
					e.printStackTrace();
				SwingUtilities.invokeLater( () -> suggestPullAlternative( e.getMessage() ) );
				return;
			}
			repository.push();
			NotificationDialog.show( "Synchronize Changes (Commit, Pull, Push)",
					"<html><body><font size=+4 color=green>&#10003</font> Completed successfully." );
		} );
	}

	private void run( final String title, final RunnableWithException action )
	{
		new Thread( () -> {
			try
			{
				action.run();
			}
			catch ( final CancellationException e )
			{
				// ignore
			}
			catch ( final Exception e )
			{
				ErrorDialog.showErrorMessage( title, e );
			}
		} ).start();
	}

	interface RunnableWithException
	{
		void run() throws Exception;
	}

	@Plugin( type = CommandDescriptionProvider.class )
	public static class DescriptionProvider extends BasicDescriptionProvider
	{
		public DescriptionProvider()
		{
			super( actionDescriptions, KeyConfigScopes.MAMUT, KeyConfigContexts.MASTODON, KeyConfigContexts.TRACKSCHEME );
		}
	}
}
