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
package org.mastodon.mamut.collaboration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.collaboration.settings.MastodonGitSettingsService;
import org.mastodon.mamut.collaboration.utils.ModelAsserts;
import org.mastodon.mamut.collaboration.utils.ModelIO;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.io.ProjectSaver;
import org.mastodon.mamut.io.project.MamutProject;
import org.mastodon.mamut.io.project.MamutProjectIO;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.scijava.Context;

/**
 * Tests for {@link MastodonGitRepository}.
 */
public class MastodonGitRepositoryTest
{
	@BeforeClass
	public static void init()
	{
		try (final Context context = new Context())
		{
			final MastodonGitSettingsService settings = context.getService( MastodonGitSettingsService.class );
			settings.setAuthorName( "Mastodon Git Test" );
			settings.setAuthorEmail( "noreply@example.com" );
		}
	}

	@Test
	public void testShareAndCloneProject() throws Exception
	{
		final Path tempDirectory = Files.createTempDirectory( "mastodon-git-share-test" );
		final Path centralRepo = tempDirectory.resolve( "central-repo.git" );
		final Path localRepo = tempDirectory.resolve( "local-repo" );
		final Path clonedRepo = tempDirectory.resolve( "local-repo2" );
		try (
				final Context context1 = new Context();
				final Context context2 = new Context();
		)
		{
			// Open small mastodon project.
			final String path = MastodonGitRepositoryTest.class.getResource( "tiny/tiny-project.mastodon" ).getPath();
			final ProjectModel projectModel = ProjectLoader.open( path, context1 );
			// Create empty git repository. This will serve as the remote for testing purposes.
			Git.init().setDirectory( centralRepo.toFile() ).setBare( true ).call().close();
			// "Share" the project by "uploading" it to the empty repository.
			Files.createDirectory( localRepo );
			final MastodonGitRepository mgr = MastodonGitRepository.shareProject( projectModel, localRepo.toFile(), centralRepo.toString() );
			assertEquals( localRepo.resolve( "mastodon.project" ).toFile(), mgr.getProjectRoot() );
			// Clone the repository to a new location.
			MastodonGitRepository.cloneRepository( centralRepo.toString(), clonedRepo.toFile() );
			final ProjectModel clonedProjectModel = ProjectLoader.open( clonedRepo.resolve( "mastodon.project" ).toString(), context2 );
			assertEquals( localRepo.resolve( "mastodon.project" ).toFile(), projectModel.getProject().getProjectRoot() );
			assertEquals( clonedRepo.resolve( "mastodon.project" ).toFile(), clonedProjectModel.getProject().getProjectRoot() );
			ModelAsserts.assertModelEquals( projectModel.getModel(), clonedProjectModel.getModel() );
			projectModel.close();
			clonedProjectModel.close();
		}
	}

	@Test
	public void testCommitPullPush() throws Exception
	{
		try (final TwoReposOneRemote example = new TwoReposOneRemote())
		{
			addSpot( example.projectModel2, "Hello World!" );
			example.repo2.commit( "Add spot" );
			example.repo2.push();
			example.repo1.pull();
			ModelAsserts.assertModelEquals( example.projectModel1.getModel(), example.projectModel2.getModel() );
		}
	}

	private static class TwoReposOneRemote implements AutoCloseable
	{

		public final Context context1;

		public final Context context2;

		public final ProjectModel projectModel1;

		public final ProjectModel projectModel2;

		public final MastodonGitRepository repo1;

		public final MastodonGitRepository repo2;

		private final Path tempDirectory;

		public TwoReposOneRemote() throws Exception
		{
			tempDirectory = Files.createTempDirectory( "mastodon-git-share-test" );
			final Path centralRepo = tempDirectory.resolve( "central-repo.git" );
			final Path localRepo = tempDirectory.resolve( "local-repo" );
			context1 = new Context();
			context2 = new Context();
			// Open small mastodon project.
			final String path = MastodonGitRepositoryTest.class.getResource( "tiny/tiny-project.mastodon" ).getPath();
			projectModel1 = ProjectLoader.open( path, context1 );
			// Create empty git repository. This will serve as the remote for testing purposes.
			Git.init().setDirectory( centralRepo.toFile() ).setBare( true ).call().close();
			// "Share" the project by "uploading" it to the empty repository.
			Files.createDirectory( localRepo );
			repo1 = MastodonGitRepository.shareProject( projectModel1, localRepo.toFile(), centralRepo.toString() );
			// Clone the repository to a new location.
			final Path clone = tempDirectory.resolve( "local-repo2" );
			MastodonGitRepository.cloneRepository( centralRepo.toString(), clone.toFile() );
			projectModel2 = ProjectLoader.open( clone.resolve( "mastodon.project" ).toString(), context2 );
			repo2 = new MastodonGitRepository( projectModel2 );
		}

		@Override
		public void close() throws Exception
		{
			projectModel1.close();
			projectModel2.close();
			context1.dispose();
			context2.dispose();
			FileUtils.deleteDirectory( tempDirectory.toFile() );
		}
	}

	/**
	 * Test {@link MastodonGitRepository#isClean()}
	 */
	@Test
	public void testIsClean() throws Exception
	{
		try (final TwoReposOneRemote example = new TwoReposOneRemote())
		{
			final MastodonGitRepository repo = example.repo1;
			assertTrue( repo.isClean() );
			addSpot( example.projectModel1, "Hello World!" );
			assertFalse( repo.isClean() );
			repo.commit( "Add spot" );
			assertTrue( repo.isClean() );
		}
	}

	@Test
	public void testSwitchBranchAndGetCurrentBranch() throws Exception
	{
		try (final TwoReposOneRemote example = new TwoReposOneRemote())
		{
			final String label = "some spot";
			final MastodonGitRepository repo = example.repo1;
			assertTrue( repo.isClean() );

			// create new branch "test" and add a spot
			repo.createNewBranch( "test" );
			assertEquals( "refs/heads/test", repo.getCurrentBranch() );
			addSpot( example.projectModel1, label );
			repo.commit( "Add spot" );

			// switch branch to "master", and run some tests
			repo.switchBranch( "master" );
			assertEquals( "refs/heads/master", repo.getCurrentBranch() );
			assertFalse( hasSpot( example.projectModel1, label ) );

			// switch branch to "test", and run some tests
			repo.switchBranch( "test" );
			assertTrue( hasSpot( example.projectModel1, label ) );
		}
	}

	private boolean hasSpot( final ProjectModel projectModel, final String label )
	{
		final ModelGraph graph = projectModel.getModel().getGraph();
		for ( final Spot spot : graph.vertices() )
			if ( spot.getLabel().equals( label ) )
				return true;
		return false;
	}

	@Test
	public void testShareBranch() throws Exception
	{
		try (final TwoReposOneRemote example = new TwoReposOneRemote())
		{
			example.repo1.createNewBranch( "branchA" );
			final String spotLabel = "new spot in branch A";
			addSpot( example.projectModel1, spotLabel );
			example.repo1.commit( "add spot" );
			example.repo1.push();
			example.repo2.fetchAll();
			example.repo2.switchBranch( "refs/remotes/origin/branchA" );
			example.repo2.pull();
			ModelAsserts.assertModelEquals( example.projectModel1.getModel(), example.projectModel2.getModel() );
			assertTrue( hasSpot( example.projectModel2, spotLabel ) );
			example.repo2.switchBranch( "master" );
			assertFalse( hasSpot( example.projectModel2, spotLabel ) );

			final HashSet< Object > expectedBranches = new HashSet<>();
			expectedBranches.add( "refs/heads/master" );
			expectedBranches.add( "refs/heads/branchA" );
			expectedBranches.add( "refs/remotes/origin/master" );
			expectedBranches.add( "refs/remotes/origin/branchA" );
			assertEquals( expectedBranches, new HashSet<>( example.repo2.getBranches() ) );
		}
	}

	private static void addSpot( final ProjectModel projectModel, final String spotLabel )
	{
		projectModel.getModel().getGraph().addVertex().init( 1, new double[ 3 ], 1 ).setLabel( spotLabel );
	}

	@Test
	public void testReset() throws Exception
	{
		try (final TwoReposOneRemote example = new TwoReposOneRemote())
		{
			addSpot( example.projectModel1, "spotA" );
			example.repo1.commit( "commit A" );
			addSpot( example.projectModel1, "spotB" );
			ProjectSaver.saveProject( example.projectModel1, null );

			assertTrue( hasSpot( example.projectModel1, "spotB" ) );
			assertTrue( hasSpot( example.projectModel1, "spotA" ) );

			example.repo1.reset();

			assertFalse( hasSpot( example.projectModel1, "spotB" ) );
			assertTrue( hasSpot( example.projectModel1, "spotA" ) );
		}
	}

	@Test
	public void testResetToRemoteBranch() throws Exception
	{
		try (final TwoReposOneRemote example = new TwoReposOneRemote())
		{
			addSpot( example.projectModel1, "spotA" );
			example.repo1.commit( "commit A" );
			example.repo1.push();
			addSpot( example.projectModel1, "spotB" );
			example.repo1.commit( "commit B" );

			assertTrue( hasSpot( example.projectModel1, "spotB" ) );
			assertTrue( hasSpot( example.projectModel1, "spotA" ) );

			example.repo1.resetToRemoteBranch();

			assertFalse( hasSpot( example.projectModel1, "spotB" ) );
			assertTrue( hasSpot( example.projectModel1, "spotA" ) );
		}
	}

	@Test
	public void testPullWithAutomaticMerge() throws Exception
	{
		try (final TwoReposOneRemote example = new TwoReposOneRemote())
		{
			final Path pathA = TestResources.asPath( "merge/tiny-project_branch-a.mastodon" );
			loadFromDifferentFile( example.projectModel1, pathA );
			example.repo1.commit( "commit A" );
			example.repo1.push();

			final Path pathB = TestResources.asPath( "merge/tiny-project_branch-b.mastodon" );
			loadFromDifferentFile( example.projectModel2, pathB );
			example.repo2.commit( "commit B" );
			example.repo2.pull();

			final Model expected = ModelIO.open( TestResources.asPath( "merge/tiny-project_merged.mastodon" ).toString() );
			ModelAsserts.assertModelEquals( expected, example.projectModel2.getModel() );
		}
	}

	@Test
	public void testMergeBranch() throws Exception
	{
		try (final TwoReposOneRemote example = new TwoReposOneRemote())
		{
			example.repo1.createNewBranch( "branch-a" );
			final Path pathA = TestResources.asPath( "merge/tiny-project_branch-a.mastodon" );
			loadFromDifferentFile( example.projectModel1, pathA );
			example.repo1.commit( "commit A" );

			example.repo1.createNewBranch( "branch-b" );
			final Path pathB = TestResources.asPath( "merge/tiny-project_branch-b.mastodon" );
			loadFromDifferentFile( example.projectModel1, pathB );
			example.repo1.commit( "commit B" );

			example.repo1.switchBranch( "master" );
			example.repo1.mergeBranch( "refs/heads/branch-a" );
			example.repo1.mergeBranch( "refs/heads/branch-b" );

			final Model expected = ModelIO.open( TestResources.asPath( "merge/tiny-project_merged.mastodon" ).toString() );
			ModelAsserts.assertModelEquals( expected, example.projectModel1.getModel() );
		}
	}

	private static void loadFromDifferentFile( final ProjectModel projectModel, final Path pathA ) throws IOException
	{
		try (final MamutProject.ProjectReader reader = MamutProjectIO.load( pathA.toString() ).openForReading())
		{
			projectModel.getModel().loadRaw( reader );
		}
	}

	@Test
	public void testSaveAsBug() throws Exception
	{
		// Running "Save Project As" from the GUI should not change how "Mastodon Git Collaborative" operates.
		// The test checks that Mastodon Git commits to the correct repository after a "Save Project As".
		try (final TwoReposOneRemote example = new TwoReposOneRemote())
		{
			final File oldRoot = example.projectModel1.getProject().getProjectRoot();

			// Simulate a "Save Project As" operation
			// (The new location is also in a git repository to raise no suspicion/exceptions.)
			ProjectSaver.saveProject( example.projectModel2.getProject().getProjectRoot(), example.projectModel1 );

			addSpot( example.projectModel1, "Hello World!" );

			// Perform a commit
			final boolean clean = example.repo1.isClean();
			example.repo1.commitWithoutSave( "Add a spot" );

			// test that the commit was done in the correct repository
			assertFalse( clean );
			assertEquals( oldRoot, example.projectModel1.getProject().getProjectRoot() );
			final ProjectModel reopened = ProjectLoader.open( oldRoot.toString(), example.context1 );
			assertTrue( hasSpot( reopened, "Hello World!" ) );
			reopened.close();
		}
	}

}
