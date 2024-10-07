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
