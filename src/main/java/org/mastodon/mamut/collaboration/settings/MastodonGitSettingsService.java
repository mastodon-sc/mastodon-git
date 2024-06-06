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

	void setAuthorName( String name );

	void setAuthorEmail( String email );

	/**
	 * If the author name is not yet set, show a dialog to ask the user
	 * for author name and email.
	 * <p>
	 * May block until the user provides the author name.
	 *
	 * @param message a message that is displayed in the dialog, explaining
	 *                why the author name is needed.
	 *
	 * @return {@code true} if the author is set after this method returns.
	 *       {@code false} if the user canceled the dialog.
	 */
	boolean ensureAuthorIsSet( String message );

	String getAuthorName();

	String getAuthorEmail();

	PersonIdent getPersonIdent();
}
