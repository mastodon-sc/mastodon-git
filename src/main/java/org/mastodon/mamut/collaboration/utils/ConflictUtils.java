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

import java.util.List;

import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.tag.TagSetModel;
import org.mastodon.model.tag.TagSetStructure;

public class ConflictUtils
{
	private ConflictUtils()
	{
		// prevent from instantiation
	}

	public static boolean hasConflict( Model model )
	{
		TagSetModel< Spot, Link > tagSetModel = model.getTagSetModel();
		return !isTagSetEmpty( tagSetModel, "Merge Conflict", "Conflict" ) ||
				!isTagSetEmpty( tagSetModel, "Merge Conflict (Tags)", "Tag Conflict" ) ||
				!isTagSetEmpty( tagSetModel, "Merge Conflict (Labels)", "Label Conflict" );
	}

	public static void removeMergeConflictTagSets( Model model )
	{
		TagSetModel< Spot, Link > tagSetModel = model.getTagSetModel();
		TagSetStructure original = tagSetModel.getTagSetStructure();
		TagSetStructure replacement = new TagSetStructure();
		replacement.set( original );
		for ( TagSetStructure.TagSet tagSet : original.getTagSets() )
			if ( isConflictTagSetName( tagSet.getName() ) )
				replacement.remove( tagSet );
		tagSetModel.setTagSetStructure( replacement );
	}

	private static boolean isConflictTagSetName( String name )
	{
		return name.equals( "Merge Conflict" ) ||
				name.equals( "Merge Conflict (Tags)" ) ||
				name.equals( "Merge Conflict (Labels)" ) ||
				name.equals( "Merge Source A" ) ||
				name.equals( "Merge Source B" ) ||
				name.startsWith( "((A)) " ) ||
				name.startsWith( "((B)) " );
	}

	/**
	 * Returns true if the given tag set is empty or if it does not exist.
	 */
	private static boolean isTagSetEmpty( TagSetModel< Spot, Link > tagSetModel, String tagSetName, String tagLabel )
	{
		TagSetStructure tagSetStructure = tagSetModel.getTagSetStructure();
		List< TagSetStructure.TagSet > tagSets = tagSetStructure.getTagSets();
		TagSetStructure.TagSet tagSet = tagSets.stream().filter( ts -> tagSetName.equals( ts.getName() ) ).findFirst().orElse( null );
		if ( tagSet == null )
			return true;
		TagSetStructure.Tag tag = tagSet.getTags().stream().filter( t -> tagLabel.equals( t.label() ) ).findFirst().orElse( null );
		if ( tag == null )
			return true;
		return tagSetModel.getVertexTags().getTaggedWith( tag ).isEmpty() && tagSetModel.getEdgeTags().getTaggedWith( tag ).isEmpty();
	}
}
