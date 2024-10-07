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
package org.mastodon.mamut.collaboration.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.Test;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.collaboration.TestResources;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.model.Model;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

/**
 * Test for {@link ReloadFromDiskUtils}.
 */
public class ReloadFromDiskUtilsTest
{

	/**
	 * This test opens a files "project1.mastodon" to "project6.mastodon" by copying
	 * them to a temporary directory and then reloading the model from disk.
	 * It checks that the model is the same, if reloaded from disk, as if it was
	 * freshly loaded from the project file.
	 */
	@Test
	public void testReloadFromDisk() throws IOException, URISyntaxException, SpimDataException
	{
		try (Context context = new Context())
		{
			Path tmp = Files.createTempDirectory( "test" );
			Files.copy( TestResources.asPath( "reload/tiny-dataset.h5" ), tmp.resolve( "tiny-dataset.h5" ) );
			Files.copy( TestResources.asPath( "reload/tiny-dataset.xml" ), tmp.resolve( "tiny-dataset.xml" ) );
			Path projectFile = tmp.resolve( "project.mastodon" );
			Files.copy( TestResources.asPath( "reload/project1.mastodon" ), projectFile );
			ProjectModel open = ProjectLoader.open( projectFile.toString(), context );
			for ( int i = 2; i <= 6; i++ )
			{
				Path resource = TestResources.asPath( "reload/project" + i + ".mastodon" );
				Files.copy( resource, projectFile, StandardCopyOption.REPLACE_EXISTING );
				ReloadFromDiskUtils.reloadFromDisk( open );
				Model expected = ModelIO.open( resource.toString() );
				ModelAsserts.assertModelEquals( expected, open.getModel() );
			}
		}
	}
}
