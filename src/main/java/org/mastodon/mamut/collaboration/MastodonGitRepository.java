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

	private final MastodonGitSettingsService settingsService;

	public MastodonGitRepository( ProjectModel projectModel )
	{
		this.projectModel = projectModel;
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
			ProjectModel projectModel,
			File directory,
			String repositoryURL )
			throws Exception
	{
		if ( !directory.isDirectory() )
			throw new IllegalArgumentException( "Not a directory: " + directory );
		if ( !isDirectoryEmpty( directory ) )
			throw new IllegalArgumentException( "Directory not empty: " + directory );
		Git git = Git.cloneRepository()
				.setURI( repositoryURL )
				.setCredentialsProvider( credentials.getSingleUseCredentialsProvider() )
				.setDirectory( directory )
				.call();
		Path mastodonProjectPath = directory.toPath().resolve( "mastodon.project" );
		if ( Files.exists( mastodonProjectPath ) )
			throw new MastodonGitException( "The repository already contains a shared mastodon project: " + repositoryURL );
		Files.createDirectory( mastodonProjectPath );
		ProjectSaver.saveProject( mastodonProjectPath.toFile(), projectModel );
		Files.copy( mastodonProjectPath.resolve( "gui.xml" ), mastodonProjectPath.resolve( "gui.xml_remote" ) );
		Files.copy( mastodonProjectPath.resolve( "project.xml" ), mastodonProjectPath.resolve( "project.xml_remote" ) );
		Files.copy( mastodonProjectPath.resolve( "dataset.xml.backup" ), mastodonProjectPath.resolve( "dataset.xml.backup_remote" ) );
		Path gitignore = directory.toPath().resolve( ".gitignore" );
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

	private static boolean isDirectoryEmpty( File directory )
	{
		String[] containedFiles = directory.list();
		return containedFiles == null || containedFiles.length == 0;
	}

	/**
	 * This method clones a shared Mastodon project from a remote git repository.
	 * <br>
	 * It does so by cloning the repository into the given directory, and opening
	 * the project in Mastodon. Note that the  files gui.xml, project.xml and
	 * dataset.xml.backup are treated specially.
	 */
	public static void cloneRepository( String repositoryURL, File directory ) throws Exception
	{
		try (Git ignored = Git.cloneRepository()
				.setURI( repositoryURL )
				.setCredentialsProvider( credentials.getSingleUseCredentialsProvider() )
				.setDirectory( directory )
				.call())
		{
			Path mastodonProjectPath = directory.toPath().resolve( "mastodon.project" );
			Files.copy( mastodonProjectPath.resolve( "gui.xml_remote" ), mastodonProjectPath.resolve( "gui.xml" ) );
			Files.copy( mastodonProjectPath.resolve( "project.xml_remote" ), mastodonProjectPath.resolve( "project.xml" ) );
			Files.copy( mastodonProjectPath.resolve( "dataset.xml.backup_remote" ), mastodonProjectPath.resolve( "dataset.xml.backup" ) );
		}
	}

	/**
	 * Simply starts a new Mastodon window with the project in the given repository.
	 */
	public static void openProjectInRepository( Context context, File directory ) throws Exception
	{
		String mastodonFile = directory.toPath().resolve( "mastodon.project" ).toString();
		boolean restoreGUIState = true;
		boolean authorizeSubstituteDummyData = true;
		ProjectModel newProject = ProjectLoader.open( mastodonFile, context, restoreGUIState, authorizeSubstituteDummyData );
		new MainWindow( newProject ).setVisible( true );
	}

	/**
	 * Commits last saved changes to the git repository.
	 */
	public void commitWithoutSave( String message ) throws Exception
	{
		try (Git git = initGit())
		{
			git.add().addFilepattern( "mastodon.project" ).call();
			CommitCommand commit = git.commit();
			commit.setMessage( message );
			commit.setAuthor( settingsService.getPersonIdent() );
			commit.call();
		}
	}

	/**
	 * This method performs an operation similar to {@code "git push origin --set-upstream <current-branch>"}.
	 *
	 * @throws MastodonGitException if the push fails because the remote server has changes that the local
	 * repository does not have. Or if the push fails for any other reason.
	 */
	public synchronized void push() throws Exception
	{
		try (Git git = initGit())
		{
			Iterable< PushResult > results = git.push().setCredentialsProvider( credentials.getSingleUseCredentialsProvider() ).setRemote( "origin" ).call();
			raiseExceptionOnUnsuccessfulPush( results );
			String branchName = getSimpleName( getCurrentBranch() );
			if ( !upstreamIsConfigured( git, branchName ) )
				setUpstream( git, branchName );
		}
	}

	/**
	 * Sets the upstream for the given branch to "origin".
	 */
	private static void setUpstream( Git git, String branchName ) throws IOException
	{
		StoredConfig config = git.getRepository().getConfig();
		config.setString( ConfigConstants.CONFIG_BRANCH_SECTION, branchName, "remote", "origin" );
		config.setString( ConfigConstants.CONFIG_BRANCH_SECTION, branchName, "merge", "refs/heads/" + branchName );
		config.save();
	}

	/**
	 * Checks if the upstream is configured for the given branch.
	 */
	private static boolean upstreamIsConfigured( Git git, String branchName )
	{
		StoredConfig config = git.getRepository().getConfig();
		String merge = config.getString( ConfigConstants.CONFIG_BRANCH_SECTION, branchName, "merge" );
		if ( merge != null )
			return true;
		return false;
	}

	private static void raiseExceptionOnUnsuccessfulPush( Iterable< PushResult > results )
	{
		for ( PushResult result : results )
		{
			for ( RemoteRefUpdate update : result.getRemoteUpdates() )
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
	public synchronized void createNewBranch( String branchName ) throws Exception
	{
		try (Git git = initGit())
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
	public synchronized void switchBranch( String branchName ) throws Exception
	{
		MamutProject project = projectModel.getProject();
		File projectRoot = project.getProjectRoot();
		try (Git git = initGit( projectRoot ))
		{
			ensureClean( git, "switching the branch" );
			boolean isRemoteBranch = branchName.startsWith( "refs/remotes/" );
			if ( isRemoteBranch )
			{
				String simpleName = getSimpleName( branchName );
				boolean conflict = git.branchList().call().stream().map( Ref::getName ).anyMatch( localName -> simpleName.equals( getSimpleName( localName ) ) );
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

	private synchronized String getSimpleName( String branchName )
	{
		String[] parts = branchName.split( "/" );
		return parts[ parts.length - 1 ];
	}

	/**
	 * Returns a list of all branches local and remote branches in the git repository.
	 */
	public synchronized List< String > getBranches() throws Exception
	{
		try (Git git = initGit())
		{
			return git.branchList().setListMode( ListBranchCommand.ListMode.ALL ).call().stream().map( Ref::getName ).collect( Collectors.toList() );
		}
	}

	/**
	 * Fetches all branches from the remote repository.
	 */
	public synchronized void fetchAll() throws Exception
	{
		try (Git git = initGit())
		{
			git.fetch().setCredentialsProvider( credentials.getSingleUseCredentialsProvider() ).call();
		}
	}

	/**
	 * Returns the name of the current branch.
	 */
	public synchronized String getCurrentBranch() throws Exception
	{
		try (Git git = initGit())
		{
			return git.getRepository().getFullBranch();
		}
	}

	/**
	 * Merges the given branch into the current branch. Throws an exception if there are conflicts.
	 * Otherwise, creates a merge comit with the message "Merge commit generated with Mastodon".
	 */
	public synchronized void mergeBranch( String selectedBranch ) throws Exception
	{
		Context context = projectModel.getContext();
		MamutProject project = projectModel.getProject();
		File projectRoot = project.getProjectRoot();
		try (Git git = initGit())
		{
			ensureClean( git, "merging" );
			String currentBranch = getCurrentBranch();
			Dataset dsA = new Dataset( projectRoot.getAbsolutePath() );
			git.checkout().setName( selectedBranch ).call();
			Dataset dsB = new Dataset( projectRoot.getAbsolutePath() );
			git.checkout().setName( currentBranch ).call();
			git.merge().setCommit( false ).include( git.getRepository().exactRef( selectedBranch ) ).call(); // TODO selected branch, should not be a string but a ref instead
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
		Context context = projectModel.getContext();
		MamutProject project = projectModel.getProject();
		File projectRoot = project.getProjectRoot();
		try (Git git = initGit())
		{
			ensureClean( git, "pulling" );
			try
			{
				boolean conflict = !git.pull()
						.setCredentialsProvider( credentials.getSingleUseCredentialsProvider() )
						.setRemote( "origin" )
						.setRebase( false )
						.call().isSuccessful();
				if ( conflict )
					automaticMerge( context, project, projectRoot, git );
			}
			finally
			{
				abortMerge( git );
			}
			reloadFromDisk();
		}
	}

	private void automaticMerge( Context context, MamutProject project, File projectRoot, Git git )
	{
		try
		{
			git.checkout().setAllPaths( true ).setStage( CheckoutCommand.Stage.OURS ).call();
			Dataset dsA = new Dataset( projectRoot.getAbsolutePath() );
			git.checkout().setAllPaths( true ).setStage( CheckoutCommand.Stage.THEIRS ).call();
			Dataset dsB = new Dataset( projectRoot.getAbsolutePath() );
			git.checkout().setAllPaths( true ).setStage( CheckoutCommand.Stage.OURS ).call();
			String commitMessage = "Automatic merge by Mastodon during pull";
			mergeAndCommit( context, project, dsA, dsB, commitMessage );
		}
		catch ( GraphMergeException e )
		{
			throw e;
		}
		catch ( Throwable t )
		{
			throw new GraphMergeException( "There was a failure, when merging changes to the Model.", t );
		}
	}

	private void mergeAndCommit( Context context, MamutProject project, Dataset datasetA, Dataset datasetB, String commitMessage ) throws Exception
	{
		Model mergedModel = merge( datasetA, datasetB );
		if ( ConflictUtils.hasConflict( mergedModel ) )
			throw new GraphMergeConflictException();
		ConflictUtils.removeMergeConflictTagSets( mergedModel );
		saveModel( context, mergedModel, project );
		commitWithoutSave( commitMessage );
	}

	private static void saveModel( Context context, Model model, MamutProject project ) throws IOException
	{
		project.setProjectRoot( project.getProjectRoot() );
		try (final MamutProject.ProjectWriter writer = project.openForWriting())
		{
			MamutProjectIO.save( project, writer );
			final RawGraphIO.GraphToFileIdMap< Spot, Link > idmap = model.saveRaw( writer );
			MamutRawFeatureModelIO.serialize( context, model, idmap, writer );
		}
	}

	private static Model merge( Dataset dsA, Dataset dsB )
	{
		final MergeDatasets.OutputDataSet output = new MergeDatasets.OutputDataSet( new Model() );
		double distCutoff = 1000;
		double mahalanobisDistCutoff = 1;
		double ratioThreshold = 2;
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
		try (Git git = initGit())
		{
			git.reset().setMode( ResetCommand.ResetType.HARD ).call();
			reloadFromDisk();
		}
	}

	private synchronized Git initGit() throws IOException
	{
		File projectRoot = projectModel.getProject().getProjectRoot();
		return initGit( projectRoot );
	}

	private synchronized Git initGit( File projectRoot ) throws IOException
	{
		boolean correctFolder = projectRoot.getName().equals( "mastodon.project" );
		if ( !correctFolder )
			throw new MastodonGitException( "The current project does not appear to be in a git repo." );
		File gitRoot = projectRoot.getParentFile();
		if ( !new File( gitRoot, ".git" ).exists() )
			throw new MastodonGitException( "The current project does not appear to be in a git repo." );
		return Git.open( gitRoot );
	}

	private synchronized boolean isClean( Git git ) throws GitAPIException
	{
		return git.status().call().isClean();
	}

	public boolean isRepository()
	{
		try (Git ignored = initGit())
		{
			return true;
		}
		catch ( Exception e )
		{
			return false;
		}
	}

	private static void abortMerge( Git git ) throws Exception
	{
		Repository repository = git.getRepository();
		repository.writeMergeCommitMsg( null );
		repository.writeMergeHeads( null );
		git.reset().setMode( ResetCommand.ResetType.HARD ).call();
	}

	/**
	 * Hard reset of the current branch to the remote branch.
	 */
	public void resetToRemoteBranch() throws Exception
	{
		try (Git git = initGit())
		{
			Repository repository = git.getRepository();
			String remoteTrackingBranch = new BranchConfig( repository.getConfig(), repository.getBranch() ).getRemoteTrackingBranch();
			git.reset().setMode( ResetCommand.ResetType.HARD ).setRef( remoteTrackingBranch ).call();
			reloadFromDisk();
		}
	}

	private void ensureClean( Git git, String title ) throws GitAPIException
	{
		ProjectSaver.saveProject( projectModel, null );
		boolean clean = isClean( git );
		if ( !clean )
			throw new MastodonGitException( "There are uncommitted changes. Please add a save point before " + title + "." );
	}

	/**
	 * Returns true if the currently opened Mastodon project is the same as the last commit on the current branch.
	 * Side effect: Saves the project.
	 */
	public boolean isClean() throws Exception
	{
		ProjectSaver.saveProject( projectModel, null );
		try (Git git = initGit())
		{
			return isClean( git );
		}
	}
}
