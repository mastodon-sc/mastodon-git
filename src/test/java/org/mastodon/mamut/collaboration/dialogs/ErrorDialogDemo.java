package org.mastodon.mamut.collaboration.dialogs;

public class ErrorDialogDemo
{
	public static void main( String... args )
	{
		try
		{
			throw new IllegalArgumentException( "Test exception" );
		}
		catch ( Exception e )
		{
			ErrorDialog.showErrorMessage( "Title", e );
		}
	}
}
