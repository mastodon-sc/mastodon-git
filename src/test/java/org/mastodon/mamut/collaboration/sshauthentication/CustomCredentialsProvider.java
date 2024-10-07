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
package org.mastodon.mamut.collaboration.sshauthentication;

import java.util.Scanner;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

/**
 * A {@link CredentialsProvider} that allows to answer yes/no questions
 * interactively on the command line.
 */
public class CustomCredentialsProvider extends CredentialsProvider
{

	@Override
	public boolean isInteractive()
	{
		return true;
	}

	@Override
	public boolean supports( CredentialItem... items )
	{
		return true;
	}

	@Override
	public boolean get( URIish uri, CredentialItem... items ) throws UnsupportedCredentialItem
	{
		boolean ok = true;
		for ( CredentialItem item : items )
			ok &= processItem( item );
		if ( !ok )
			throw new UnsupportedOperationException();
		return ok;
	}

	private boolean processItem( CredentialItem item )
	{
		if ( item instanceof CredentialItem.InformationalMessage )
			return processInformalMessage( ( CredentialItem.InformationalMessage ) item );
		if ( item instanceof CredentialItem.YesNoType )
			return processYesNo( ( CredentialItem.YesNoType ) item );
		return false;
	}

	private boolean processInformalMessage( CredentialItem.InformationalMessage item )
	{
		System.out.println( item.getPromptText() );
		return true;
	}

	private boolean processYesNo( CredentialItem.YesNoType item )
	{
		System.out.println( item.getPromptText() + " (yes/no)" );
		String line = new Scanner( System.in ).nextLine();
		item.setValue( "yes".equals( line ) );
		return true;
	}
}

