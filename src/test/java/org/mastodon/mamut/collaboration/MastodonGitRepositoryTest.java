package org.mastodon.mamut.collaboration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
			MastodonGitRepository.shareProject( projectModel, localRepo.toFile(), centralRepo.toString() );
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
			ProjectSaver.saveProject( example.projectModel2, null );
			example.repo2.commitWithoutSave( "Add spot" );
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
			ProjectSaver.saveProject( example.projectModel1, null );
			repo.commitWithoutSave( "Add spot" );
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
			ProjectSaver.saveProject( example.projectModel1, null );
			repo.commitWithoutSave( "Add spot" );

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
			final ProjectModel projectModel1 = example.projectModel1;
			addSpot( projectModel1, spotLabel );
			ProjectSaver.saveProject( example.projectModel1, null );
			example.repo1.commitWithoutSave( "add spot" );
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
			ProjectSaver.saveProject( example.projectModel1, null );
			example.repo1.commitWithoutSave( "commit A" );
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
			ProjectSaver.saveProject( example.projectModel1, null );
			example.repo1.commitWithoutSave( "commit A" );
			example.repo1.push();
			addSpot( example.projectModel1, "spotB" );
			ProjectSaver.saveProject( example.projectModel1, null );
			example.repo1.commitWithoutSave( "commit B" );

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
			ProjectSaver.saveProject( example.projectModel1, null );
			example.repo1.commitWithoutSave( "commit A" );
			example.repo1.push();

			final Path pathB = TestResources.asPath( "merge/tiny-project_branch-b.mastodon" );
			loadFromDifferentFile( example.projectModel2, pathB );
			ProjectSaver.saveProject( example.projectModel2, null );
			example.repo2.commitWithoutSave( "commit B" );
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
			ProjectSaver.saveProject( example.projectModel1, null );
			example.repo1.commitWithoutSave( "commit A" );

			example.repo1.createNewBranch( "branch-b" );
			final Path pathB = TestResources.asPath( "merge/tiny-project_branch-b.mastodon" );
			loadFromDifferentFile( example.projectModel1, pathB );
			ProjectSaver.saveProject( example.projectModel1, null );
			example.repo1.commitWithoutSave( "commit B" );

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
}
