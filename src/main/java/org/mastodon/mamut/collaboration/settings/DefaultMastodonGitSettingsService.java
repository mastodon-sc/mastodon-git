package org.mastodon.mamut.collaboration.settings;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.lib.PersonIdent;
import org.mastodon.mamut.collaboration.dialogs.SetAuthorDialog;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

@Plugin( type = Service.class )
public class DefaultMastodonGitSettingsService extends AbstractService implements MastodonGitSettingsService
{

	@Parameter
	private PrefService prefService;

	private String authorName;

	private String authorEmail;

	/**
	 * Shows a dialog that asks the user if they want to set the author name.
	 * Returns true if the user agrees to set the author name, false otherwise.
	 */
	@Override
	public void initialize()
	{
		super.initialize();
		authorName = prefService.get( DefaultMastodonGitSettingsService.class, "author.name", null );
		authorEmail = prefService.get( DefaultMastodonGitSettingsService.class, "author.email", null );
	}

	@Override
	public boolean isAuthorSpecified()
	{
		return authorName != null && authorEmail != null;
	}

	@Override
	public String getAuthorName()
	{
		return authorName;
	}

	@Override
	public String getAuthorEmail()
	{
		return authorEmail;
	}

	@Override
	public PersonIdent getPersonIdent()
	{
		return new PersonIdent( authorName, authorEmail );
	}

	@Override
	public void askForAuthorName()
	{
		Pair< String, String > result = SetAuthorDialog.show();
		if ( result == null )
			return;
		setAuthorName( result.getLeft() );
		setAuthorEmail( result.getRight() );
	}

	@Override
	public void setAuthorName( String name )
	{
		this.authorName = name;
		prefService.put( DefaultMastodonGitSettingsService.class, "author.name", name );
	}

	@Override
	public void setAuthorEmail( String email )
	{
		this.authorEmail = email;
		prefService.put( DefaultMastodonGitSettingsService.class, "author.email", email );
	}

	@Override
	public boolean ensureAuthorIsSet( String message )
	{
		if ( isAuthorSpecified() )
			return true;

		if ( askWhetherToSetTheAuthorName( message ) )
			askForAuthorName();

		return isAuthorSpecified();
	}

	private static boolean askWhetherToSetTheAuthorName( String message )
	{
		String title = "Set Author Name";
		String[] options = { "Set Author Name", "Cancel" };
		int result = JOptionPane.showOptionDialog( null, message, title, JOptionPane.YES_NO_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, options, options[ 0 ] );
		return result == JOptionPane.YES_OPTION;
	}

	public void clearAuthorName()
	{
		this.authorName = null;
		this.authorEmail = null;
		prefService.remove( DefaultMastodonGitSettingsService.class, "author.name" );
		prefService.remove( DefaultMastodonGitSettingsService.class, "author.email" );
	}
}
