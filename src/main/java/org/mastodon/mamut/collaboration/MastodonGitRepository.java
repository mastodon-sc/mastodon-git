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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.mastodon.graph.io.RawGraphIO;
import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.collaboration.exceptions.MastodonGitException;
import org.mastodon.mamut.collaboration.settings.MastodonGitSettingsService;
import org.mastodon.mamut.collaboration.utils.ConflictUtils;
import org.mastodon.mamut.collaboration.utils.ReloadFromDiskUtils;
import org.mastodon.mamut.feature.MamutRawFeatureModelIO;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.io.ProjectSaver;
import org.mastodon.mamut.io.project.MamutProject;
import org.mastodon.mamut.io.project.MamutProjectIO;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.collaboration.credentials.PersistentCredentials;
import org.mastodon.mamut.collaboration.exceptions.GraphMergeConflictException;
import org.mastodon.mamut.collaboration.exceptions.GraphMergeException;
import org.mastodon.mamut.tomancak.merging.Dataset;
import org.mastodon.mamut.tomancak.merging.MergeDatasets;
import org.scijava.Context;

// make it one synchronized class per repository
// don't allow to open a repository twice (maybe read only)
public class MastodonGitRepository
{

	private static final PersistentCredentials credentials = new PersistentCredentials();

	private final ProjectModel projectModel;

	private final File projectRoot;

	private final MastodonGitSettingsService settingsService;

	public MastodonGitRepository( final ProjectModel projectModel )
	{
		this.projectModel = projectModel;
		this.projectRoot = projectModel.getProject().getProjectRoot();
		settingsService = projectModel.getContext().service( MastodonGitSettingsService.class );
	}

	/**
	 * This method uploads the given Mastodon project to a remote git repository.
	 * <br>
	 * It does so by cloning an (empty) repository from the given URL into the
	 * given directory, and saving the Mastodon project into a subdirectory,
	 * comitting and pushing the changes.
	 */
	public static MastodonGitRepository shareProject(
			final ProjectModel projectModel,
			final File directory,
			final String repositoryURL )
			throws Exception
	{
		if ( !directory.isDirectory() )
			throw new IllegalArgumentException( "Not a directory: " + directory );
		if ( !isDirectoryEmpty( directory ) )
			throw new IllegalArgumentException( "Directory not empty: " + directory );
		final Git git = Git.cloneRepository()
				.setURI( repositoryURL )
				.setCredentialsProvider( credentials.getSingleUseCredentialsProvider() )
				.setDirectory( directory )
				.call();
		final Path mastodonProjectPath = directory.toPath().resolve( "mastodon.project" );
		if ( Files.exists( mastodonProjectPath ) )
			throw new MastodonGitException( "The repository already contains a shared mastodon project: " + repositoryURL );
		Files.createDirectory( mastodonProjectPath );
		ProjectSaver.saveProject( mastodonProjectPath.toFile(), projectModel );
		Files.copy( mastodonProjectPath.resolve( "gui.xml" ), mastodonProjectPath.resolve( "gui.xml_remote" ) );
		Files.copy( mastodonProjectPath.resolve( "project.xml" ), mastodonProjectPath.resolve( "project.xml_remote" ) );
		Files.copy( mastodonProjectPath.resolve( "dataset.xml.backup" ), mastodonProjectPath.resolve( "dataset.xml.backup_remote" ) );
		final Path gitignore = directory.toPath().resolve( ".gitignore" );
		Files.write( gitignore, "/mastodon.project/gui.xml\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND );
		Files.write( gitignore, "/mastodon.project/project.xml\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND );
		Files.write( gitignore, "/mastodon.project/dataset.xml.backup\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND );
		git.add().addFilepattern( ".gitignore" ).call();
		git.commit().setMessage( "Add .gitignore file" ).call();
		git.add().addFilepattern( "mastodon.project" ).call();
		git.commit().setMessage( "Share mastodon project" ).call();
		git.push().setCredentialsProvider( credentials.getSingleUseCredentialsProvider() ).setRemote( "origin" ).call();
		git.close();
		return new MastodonGitRepository( projectModel );
	}

	public File getProjectRoot()
	{
		return projectRoot;
	}

	private static boolean isDirectoryEmpty( final File directory )
	{
		final String[] containedFiles = directory.list();
		return containedFiles == null || containedFiles.length == 0;
	}

	/**
	 * This method clones a shared Mastodon project from a remote git repository.
	 * <br>
	 * It does so by cloning the repository into the given directory, and opening
	 * the project in Mastodon. Note that the  files gui.xml, project.xml and
	 * dataset.xml.backup are treated specially.
	 */
	public static void cloneRepository( final String repositoryURL, final File directory ) throws Exception
	{
		try (final Git ignored = Git.cloneRepository()
				.setURI( repositoryURL )
				.setCredentialsProvider( credentials.getSingleUseCredentialsProvider() )
				.setDirectory( directory )
				.call())
		{
			final Path mastodonProjectPath = directory.toPath().resolve( "mastodon.project" );
			Files.copy( mastodonProjectPath.resolve( "gui.xml_remote" ), mastodonProjectPath.resolve( "gui.xml" ) );
			Files.copy( mastodonProjectPath.resolve( "project.xml_remote" ), mastodonProjectPath.resolve( "project.xml" ) );
			Files.copy( mastodonProjectPath.resolve( "dataset.xml.backup_remote" ), mastodonProjectPath.resolve( "dataset.xml.backup" ) );
		}
	}

	/**
	 * Simply starts a new Mastodon window with the project in the given repository.
	 */
	public static void openProjectInRepository( final Context context, final File directory ) throws Exception
	{
		final String mastodonFile = directory.toPath().resolve( "mastodon.project" ).toString();
		final boolean restoreGUIState = true;
		final boolean authorizeSubstituteDummyData = true;
		final ProjectModel newProject = ProjectLoader.open( mastodonFile, context, restoreGUIState, authorizeSubstituteDummyData );
		new MainWindow( newProject ).setVisible( true );
	}

	/**
	 * Commits last saved changes to the git repository.
	 */
	public void commitWithoutSave( final String message ) throws Exception
	{
		try (final Git git = initGit())
		{
			git.add().addFilepattern( "mastodon.project" ).call();
			final CommitCommand commit = git.commit();
			commit.setMessage( message );
			commit.setAuthor( settingsService.getPersonIdent() );
			commit.call();
		}
	}

	/**
	 * Save the project and add a commit.
	 * <p>
	 * This method is mostly for testing purposes. As production code would
	 * usually run {@link #isClean()} before asking the user for a commit message
	 * and then call {@link #commitWithoutSave(String)} to avoid saving the
	 * project twice.
	 *
	 * @param message the commit message.
	 */
	public synchronized void commit( final String message ) throws Exception
	{
		ProjectSaver.saveProject( projectRoot, projectModel );
		commitWithoutSave( message );
	}

	/**
	 * This method performs an operation similar to {@code "git push origin --set-upstream <current-branch>"}.
	 *
	 * @throws MastodonGitException if the push fails because the remote server has changes that the local
	 * repository does not have. Or if the push fails for any other reason.
	 */
	public synchronized void push() throws Exception
	{
		try (final Git git = initGit())
		{
			final Iterable< PushResult > results = git.push().setCredentialsProvider( credentials.getSingleUseCredentialsProvider() ).setRemote( "origin" ).call();
			raiseExceptionOnUnsuccessfulPush( results );
			final String branchName = getSimpleName( getCurrentBranch() );
			if ( !upstreamIsConfigured( git, branchName ) )
				setUpstream( git, branchName );
		}
	}

	/**
	 * Sets the upstream for the given branch to "origin".
	 */
	private static void setUpstream( final Git git, final String branchName ) throws IOException
	{
		final StoredConfig config = git.getRepository().getConfig();
		config.setString( ConfigConstants.CONFIG_BRANCH_SECTION, branchName, "remote", "origin" );
		config.setString( ConfigConstants.CONFIG_BRANCH_SECTION, branchName, "merge", "refs/heads/" + branchName );
		config.save();
	}

	/**
	 * Checks if the upstream is configured for the given branch.
	 */
	private static boolean upstreamIsConfigured( final Git git, final String branchName )
	{
		final StoredConfig config = git.getRepository().getConfig();
		final String merge = config.getString( ConfigConstants.CONFIG_BRANCH_SECTION, branchName, "merge" );
		return merge != null;
	}

	private static void raiseExceptionOnUnsuccessfulPush( final Iterable< PushResult > results )
	{
		for ( final PushResult result : results )
		{
			for ( final RemoteRefUpdate update : result.getRemoteUpdates() )
			{
				if ( update.getStatus() == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD )
					throw new MastodonGitException( "The remote server has changes, that you didn't download yet.\n"
							+ "Please download changes first. (pull)\n"
							+ "You can upload your changes afterwards.\n" );
				if ( update.getStatus() != RemoteRefUpdate.Status.OK &&
						update.getStatus() != RemoteRefUpdate.Status.UP_TO_DATE )
					throw new MastodonGitException( "Push failed: " + update.getMessage() + " " + update.getStatus() );
			}
		}
	}

	/**
	 * Create a new git branch with the given name.
	 * Similar to {@code "git checkout -b <branchName>"}.
	 */
	public synchronized void createNewBranch( final String branchName ) throws Exception
	{
		try (final Git git = initGit())
		{
			git.checkout().setCreateBranch( true ).setName( branchName ).call();
		}
	}

	/**
	 * Switches to the given branch.
	 * <br>
	 * Three steps:
	 * <ol>
	 *     <li>Saves, an checks if there are no uncommited changes. (clean)</li>
	 *     <li>Switches to the given branch.</li>
	 *     <li>Reloads the project from disk.</li>
	 * </ol>
	 */
	public synchronized void switchBranch( final String branchName ) throws Exception
	{
		try (final Git git = initGit())
		{
			ensureClean( git, "switching the branch" );
			final boolean isRemoteBranch = branchName.startsWith( "refs/remotes/" );
			if ( isRemoteBranch )
			{
				final String simpleName = getSimpleName( branchName );
				final boolean conflict = git.branchList().call().stream().map( Ref::getName ).anyMatch( localName -> simpleName.equals( getSimpleName( localName ) ) );
				if ( conflict )
					throw new MastodonGitException( "There's already a local branch with the same name." );
				git.checkout()
						.setCreateBranch( true )
						.setName( simpleName )
						.setUpstreamMode( CreateBranchCommand.SetupUpstreamMode.TRACK )
						.setStartPoint( branchName )
						.call();
			}
			else
				git.checkout().setName( branchName ).call();
		}
		reloadFromDisk();
	}

	private synchronized String getSimpleName( final String branchName )
	{
		final String[] parts = branchName.split( "/" );
		return parts[ parts.length - 1 ];
	}

	/**
	 * Returns a list of all branches local and remote branches in the git repository.
	 */
	public synchronized List< String > getBranches() throws Exception
	{
		try (final Git git = initGit())
		{
			return git.branchList().setListMode( ListBranchCommand.ListMode.ALL ).call().stream().map( Ref::getName ).collect( Collectors.toList() );
		}
	}

	/**
	 * Fetches all branches from the remote repository.
	 */
	public synchronized void fetchAll() throws Exception
	{
		try (final Git git = initGit())
		{
			git.fetch().setCredentialsProvider( credentials.getSingleUseCredentialsProvider() ).call();
		}
	}

	/**
	 * Returns the name of the current branch.
	 */
	public synchronized String getCurrentBranch() throws Exception
	{
		try (final Git git = initGit())
		{
			return git.getRepository().getFullBranch();
		}
	}

	/**
	 * Merges the given branch into the current branch. Throws an exception if there are conflicts.
	 * Otherwise, creates a merge comit with the message "Merge commit generated with Mastodon".
	 */
	public synchronized void mergeBranch( final String selectedBranch ) throws Exception
	{
		final Context context = projectModel.getContext();
		try (final Git git = initGit())
		{
			ensureClean( git, "merging" );
			final String currentBranch = getCurrentBranch();
			final Dataset dsA = new Dataset( projectRoot.getAbsolutePath() );
			git.checkout().setName( selectedBranch ).call();
			final Dataset dsB = new Dataset( projectRoot.getAbsolutePath() );
			git.checkout().setName( currentBranch ).call();
			git.merge().setCommit( false ).include( git.getRepository().exactRef( selectedBranch ) ).call(); // TODO selected branch, should not be a string but a ref instead
			final MamutProject project = projectModel.getProject();
			project.setProjectRoot( projectRoot );
			mergeAndCommit( context, project, dsA, dsB, "Merge commit generated with Mastodon" );
			reloadFromDisk();
		}
	}

	/**
	 * Pulls changes from the remote repository.
	 * <br>
	 * If there are conflicts, it tries to resolve them automatically by creating a merge commit.
	 */
	public synchronized void pull() throws Exception
	{
		final Context context = projectModel.getContext();
		try (final Git git = initGit())
		{
			ensureClean( git, "pulling" );
			try
			{
				final boolean conflict = !git.pull()
						.setCredentialsProvider( credentials.getSingleUseCredentialsProvider() )
						.setRemote( "origin" )
						.setRebase( false )
						.call().isSuccessful();
				if ( conflict )
				{
					final MamutProject project = projectModel.getProject();
					project.setProjectRoot( projectRoot );
					automaticMerge( context, project, projectRoot, git );
				}
			}
			finally
			{
				abortMerge( git );
			}
			reloadFromDisk();
		}
	}

	private void automaticMerge( final Context context, final MamutProject project, final File projectRoot, final Git git )
	{
		try
		{
			git.checkout().setAllPaths( true ).setStage( CheckoutCommand.Stage.OURS ).call();
			final Dataset dsA = new Dataset( projectRoot.getAbsolutePath() );
			git.checkout().setAllPaths( true ).setStage( CheckoutCommand.Stage.THEIRS ).call();
			final Dataset dsB = new Dataset( projectRoot.getAbsolutePath() );
			git.checkout().setAllPaths( true ).setStage( CheckoutCommand.Stage.OURS ).call();
			final String commitMessage = "Automatic merge by Mastodon during pull";
			mergeAndCommit( context, project, dsA, dsB, commitMessage );
		}
		catch ( final GraphMergeException e )
		{
			throw e;
		}
		catch ( final Throwable t )
		{
			throw new GraphMergeException( "There was a failure, when merging changes to the Model.", t );
		}
	}

	private void mergeAndCommit( final Context context, final MamutProject project, final Dataset datasetA, final Dataset datasetB, final String commitMessage ) throws Exception
	{
		final Model mergedModel = merge( datasetA, datasetB );
		if ( ConflictUtils.hasConflict( mergedModel ) )
			throw new GraphMergeConflictException();
		ConflictUtils.removeMergeConflictTagSets( mergedModel );
		saveModel( context, mergedModel, project );
		commitWithoutSave( commitMessage );
	}

	private static void saveModel( final Context context, final Model model, final MamutProject project ) throws IOException
	{
		try (final MamutProject.ProjectWriter writer = project.openForWriting())
		{
			MamutProjectIO.save( project, writer );
			final RawGraphIO.GraphToFileIdMap< Spot, Link > idmap = model.saveRaw( writer );
			MamutRawFeatureModelIO.serialize( context, model, idmap, writer );
		}
	}

	private static Model merge( final Dataset dsA, final Dataset dsB )
	{
		final MergeDatasets.OutputDataSet output = new MergeDatasets.OutputDataSet( new Model() );
		final double distCutoff = 1000;
		final double mahalanobisDistCutoff = 1;
		final double ratioThreshold = 2;
		MergeDatasets.merge( dsA, dsB, output, distCutoff, mahalanobisDistCutoff, ratioThreshold );
		return output.getModel();
	}

	private synchronized void reloadFromDisk() throws IOException
	{
		ReloadFromDiskUtils.reloadFromDisk( projectModel );
	}

	/**
	 * Resets the current branch to the last commit. And reloads the project from disk.
	 */
	public synchronized void reset() throws Exception
	{
		try (final Git git = initGit())
		{
			git.reset().setMode( ResetCommand.ResetType.HARD ).call();
			reloadFromDisk();
		}
	}

	private synchronized Git initGit() throws IOException
	{
		final boolean correctFolder = projectRoot.getName().equals( "mastodon.project" );
		if ( !correctFolder )
			throw new MastodonGitException( "The current project does not appear to be in a git repo." );
		final File gitRoot = projectRoot.getParentFile();
		if ( !new File( gitRoot, ".git" ).exists() )
			throw new MastodonGitException( "The current project does not appear to be in a git repo." );
		return Git.open( gitRoot );
	}

	private synchronized boolean isClean( final Git git ) throws GitAPIException
	{
		return git.status().call().isClean();
	}

	public boolean isRepository()
	{
		try (final Git ignored = initGit())
		{
			return true;
		}
		catch ( final Exception e )
		{
			return false;
		}
	}

	private static void abortMerge( final Git git ) throws Exception
	{
		final Repository repository = git.getRepository();
		repository.writeMergeCommitMsg( null );
		repository.writeMergeHeads( null );
		git.reset().setMode( ResetCommand.ResetType.HARD ).call();
	}

	/**
	 * Hard reset of the current branch to the remote branch.
	 */
	public void resetToRemoteBranch() throws Exception
	{
		try (final Git git = initGit())
		{
			final Repository repository = git.getRepository();
			final String remoteTrackingBranch = new BranchConfig( repository.getConfig(), repository.getBranch() ).getRemoteTrackingBranch();
			git.reset().setMode( ResetCommand.ResetType.HARD ).setRef( remoteTrackingBranch ).call();
			reloadFromDisk();
		}
	}

	private void ensureClean( final Git git, final String title ) throws Exception
	{
		ProjectSaver.saveProject( projectRoot, projectModel );
		final boolean clean = isClean( git );
		if ( !clean )
			throw new MastodonGitException( "There are uncommitted changes. Please add a save point before " + title + "." );
	}

	/**
	 * Returns true if the currently opened Mastodon project is the same as the last commit on the current branch.
	 * Side effect: Saves the project.
	 */
	public boolean isClean() throws Exception
	{
		ProjectSaver.saveProject( projectRoot, projectModel );
		try (final Git git = initGit())
		{
			return isClean( git );
		}
	}
}
