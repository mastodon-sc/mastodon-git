package org.mastodon.mamut.collaboration.settings;

import org.scijava.Context;

public class ClearTheAuthorNameFromSettingsDemo
{
	public static void main( String... args )
	{
		try (Context context = new Context())
		{
			DefaultMastodonGitSettingsService settings = context.getService( DefaultMastodonGitSettingsService.class );
			settings.clearAuthorName();
		}
	}
}
