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

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.sshd.JGitKeyCache;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;

/**
 * Example of how to use JGit with a custom SSH key.
 */
public class JGitSshAuthenticationExample
{
	public static void main( String... args ) throws Exception
	{
		String projectPath = "/home/arzt/devel/mastodon/mastodon/src/test/resources/org/mastodon/mamut/examples/tiny/tiny-project.mastodon";
		String repositoryName = "mgit-test";
		String repositoryURL = "git@github.com:masgitoff/mastodon-test-dataset.git";
		File parentDirectory = new File( "/home/arzt/tmp/" );

		SshSessionFactory sshSessionFactory = new SshdSessionFactoryBuilder()
				.setPreferredAuthentications( "publickey" )
				.setHomeDirectory( new File( "/home/arzt/" ) )
				.setSshDirectory( new File( "/home/arzt/ssh-experiment" ) )
				.build( new JGitKeyCache() );
		try (Git git = Git.cloneRepository()
				.setURI( repositoryURL )
				.setDirectory( new File( parentDirectory, "xyz" ) )
				.setCredentialsProvider( new CustomCredentialsProvider() )
				.setTransportConfigCallback( transport -> ( ( SshTransport ) transport ).setSshSessionFactory( sshSessionFactory ) )
				.call())
		{
			git.push()
					.setTransportConfigCallback( transport -> ( ( SshTransport ) transport ).setSshSessionFactory( sshSessionFactory ) )
					.setCredentialsProvider( new CustomCredentialsProvider() )
					.call();
		}
	}
}
