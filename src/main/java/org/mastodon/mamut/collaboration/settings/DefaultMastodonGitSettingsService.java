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
		Pair< String, String > result = SetAuthorDialog.show( getAuthorName(), getAuthorEmail() );
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
