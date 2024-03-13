package org.mastodon.mamut.collaboration.settings;

import net.imagej.ImageJService;

import org.eclipse.jgit.lib.PersonIdent;

public interface MastodonGitSettingsService extends ImageJService
{
	boolean isAuthorSpecified();

	/**
	 * May show a dialog to ask the user for the author name.
	 */
	void askForAuthorName();

	/**
	 * Return true if the author is specified. Ask the user for the author
	 * name and email if they are not specified. Return false if the user does not
	 * provide the author name.
	 * <p>
	 * May block until the user provides the author name.
	 */
	boolean ensureAuthorIsSet( String message );

	String getAuthorName();

	String getAuthorEmail();

	PersonIdent getPersonIdent();
}
