package org.mastodon.mamut.collaboration;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestResources
{
	public static Path asPath( String name ) throws URISyntaxException
	{
		return Paths.get( TestResources.class.getResource( name ).toURI() );
	}
}
