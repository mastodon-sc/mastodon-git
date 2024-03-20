package org.mastodon.mamut.collaboration;

import java.io.File;

import org.scijava.Context;
import org.scijava.ui.UIService;

public class MastodonGitRepositoryDemo
{
	public static void main( String... args ) throws Exception
	{
		File directory = new File( "/home/arzt/Datasets/DeepLineage/Trackathon/trackathon-lyon-test/" );
		if ( !directory.isDirectory() )
			throw new RuntimeException( "Expected directory: " + directory );
		Context context = new Context();
		context.service( UIService.class ).showUI();
		MastodonGitRepository.openProjectInRepository( context, directory );
	}
}
