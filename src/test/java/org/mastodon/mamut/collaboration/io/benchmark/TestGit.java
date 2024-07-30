package org.mastodon.mamut.collaboration.io.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.imglib2.util.StopWatch;

import org.apache.commons.io.FileUtils;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.collaboration.io.MasgitoffIo;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

public class TestGit
{
	private static final Path GIT_REPO_FOLDER = Paths.get( "/home/arzt/tmp/test-git" );

	private static final String MASTODON_PROJECT_FILENAME = "test.mastodon";

	private static final int BYTES_PER_MB = 1024 * 1024;

	public static void main( final String... args ) throws SpimDataException, IOException
	{
		// FIXME: make the test more realistic by also removing spots in between
		// FIXME: make the test more realistic by forcing a reopening of the project
		run();
	}

	private static void run() throws IOException, SpimDataException
	{
		try (final Context context = new Context())
		{
			initGit();

			final GrowingGraphExample example = new AddDeleteGrowingGraphExample( context );
			final ProjectModel growingProjectModel = example.getProject();
			final Saver saver = new MasgitoffSaver( growingProjectModel, GIT_REPO_FOLDER.resolve( MASTODON_PROJECT_FILENAME ) );
			double saveSeconds = 0;
			double gitSeconds = 0;

			while ( example.hasNext() )
			{
				System.out.println( "Completion: " + example.getCompletion() + "%" );
				example.grow();

				final StopWatch saveStopWatch = StopWatch.createAndStart();

				saver.save();

				saveStopWatch.stop();
				System.out.println( "time to save: " + saveStopWatch );
				saveSeconds += saveStopWatch.seconds();

				final StopWatch gitStopWatch = StopWatch.createAndStart();

				commit( "text" + growingProjectModel.getModel().getGraph().vertices().size(), MASTODON_PROJECT_FILENAME );

				gitStopWatch.stop();
				System.out.println( "time to run git: " + gitStopWatch );
				gitSeconds += gitStopWatch.seconds();
			}
			System.out.println( "done" );
			System.out.println( "History size: " + FileUtils.sizeOfDirectory( GIT_REPO_FOLDER.resolve( ".git/" ).toFile() ) / BYTES_PER_MB + " MB" );
			System.out.println( "Mastodon project size: " + FileUtils.sizeOfDirectory( GIT_REPO_FOLDER.resolve( MASTODON_PROJECT_FILENAME ).toFile() ) / BYTES_PER_MB + " MB" );
			System.out.println( "Time save: " + saveSeconds );
			System.out.println( "Time git: " + gitSeconds );
			example.assertEqualsOriginal( MasgitoffIo.readMasgitoff( GIT_REPO_FOLDER.resolve( MASTODON_PROJECT_FILENAME ).toFile() ) );
		}
	}

	private static void initGit() throws IOException
	{
		FileUtils.deleteDirectory( GIT_REPO_FOLDER.toFile() );
		Files.createDirectory( GIT_REPO_FOLDER );
		exec( "git", "init" );
	}

	private static void commit( final String message, final String... files )
	{
		for ( final String file : files )
			exec( "git", "add", file );
		exec( "git", "commit", "-m", message );
	}

	private static void exec( final String... command )
	{
		try
		{
			final ProcessBuilder pb = new ProcessBuilder( command );
			pb.directory( GIT_REPO_FOLDER.toFile() );
			pb.redirectOutput( ProcessBuilder.Redirect.INHERIT );
			pb.redirectError( ProcessBuilder.Redirect.INHERIT );
			pb.start().waitFor();
		}
		catch ( final InterruptedException | IOException e )
		{
			throw new RuntimeException( e );
		}
	}
}
