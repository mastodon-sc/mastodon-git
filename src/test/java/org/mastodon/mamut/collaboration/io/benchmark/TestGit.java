package org.mastodon.mamut.collaboration.io.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.imglib2.util.Cast;
import net.imglib2.util.StopWatch;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mastodon.mamut.collaboration.io.MasgitoffIo;
import org.mastodon.mamut.model.Model;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

public class TestGit
{
	private static final Path GIT_REPO_FOLDER = Paths.get( "/home/arzt/tmp/test-git" );

	private static final String MASTODON_PROJECT_FILENAME = "test.mastodon";

	private static final int BYTES_PER_MB = 1024 * 1024;

	public static void main( final String... args ) throws SpimDataException, IOException
	{
		// FIXME: make sure the graph has the tags as well
		// FIXME: remove unused label sets in the masgitoff file
		run();
	}

	private static < T > void run() throws IOException, SpimDataException
	{
		try (final Context context = new Context())
		{
			initGit();

			final GrowingGraphExample example = new AddDeleteGrowingGraphExample( context );
			final Saver< T > saver = Cast.unchecked( new MasgitoffSaver( GIT_REPO_FOLDER.resolve( MASTODON_PROJECT_FILENAME ) ) );
			double saveSeconds = 0;
			double gitSeconds = 0;
			boolean first = true;

			while ( example.hasNext() )
			{
				System.out.println( "Completion: " + example.getCompletion() + "%" );

				final StopWatch openTime = StopWatch.createAndStart();
				final Pair< Model, T > modelAndDetails = first ? saver.createEmpty() : saver.open();
				final Model model = modelAndDetails.getLeft();
				first = false;
				openTime.stop();
				final StopWatch growTime = measureTime( () -> example.grow( model ) );
				final StopWatch saveTime = measureTime( () -> saver.save( model, modelAndDetails.getRight() ) );
				final StopWatch gitTime = measureTime( () -> commit( "text" + model.getGraph().vertices().size(), MASTODON_PROJECT_FILENAME ) );

				System.out.println( "time to grow the graph: " + growTime );
				System.out.println( "time to save: " + saveTime );
				System.out.println( "time to run git: " + gitTime );
				gitSeconds += gitTime.seconds();
				saveSeconds += saveTime.seconds();
			}
			System.out.println( "done" );
			System.out.println( "History size: " + FileUtils.sizeOfDirectory( GIT_REPO_FOLDER.resolve( ".git/" ).toFile() ) / BYTES_PER_MB + " MB" );
			System.out.println( "Mastodon project size: " + FileUtils.sizeOfDirectory( GIT_REPO_FOLDER.resolve( MASTODON_PROJECT_FILENAME ).toFile() ) / BYTES_PER_MB + " MB" );
			System.out.println( "Time save: " + saveSeconds );
			System.out.println( "Time git: " + gitSeconds );
			example.assertEqualsOriginal( MasgitoffIo.readMasgitoff( GIT_REPO_FOLDER.resolve( MASTODON_PROJECT_FILENAME ).toFile() ).getLeft() );
			System.out.println( "works!" );
		}
	}

	private static StopWatch measureTime( final MyRunnable runnable ) throws IOException
	{
		final StopWatch watch = StopWatch.createAndStart();
		runnable.run();
		watch.stop();
		return watch;
	}

	interface MyRunnable
	{
		void run() throws IOException;
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