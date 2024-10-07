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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.tag.ObjTagMap;
import org.mastodon.model.tag.TagSetModel;
import org.mastodon.model.tag.TagSetStructure;

/**
 * Utility class that provides static methods to assert that two Mastodon
 * {@link Model}s are equal.
 */
public class ModelAsserts
{
	/**
	 * Compares two {@link Model}s. Throws an {@link AssertionError} if they are
	 * different.
	 * <br>
	 * The following properties are compared:
	 * <ul>
	 *    <li>Graph structure</li>
	 *    <li>Spot labels and timepoints</li>
	 *    <li>Spot positions and covariances</li>
	 *    <li>Order of outgoing edges</li>
	 *    <li>Spot and link tags</li>
	 *    <li>Order of tagsets and tags, tag colors</li>
	 * </ul>
	 * The following properties are ignored:
	 * <ul>
	 *     <li>{@link Spot#getInternalPoolIndex()}</li>
	 *     <li>Order of incoming edges is ignored</li>
	 *     <li>The entire {@link org.mastodon.feature.FeatureModel}</li>
	 * </ul>
	 */
	public static void assertModelEquals( Model a, Model b )
	{
		assertGraphEquals( a.getGraph(), b.getGraph() );
		assertTagSetModelEquals( a, b );
	}

	/**
	 * Compares two {@link ModelGraph}s. Throws an {@link AssertionError} if they
	 * are different.
	 * <br>
	 * The following properties are compared:
	 * <ul>
	 *    <li>Graph structure</li>
	 *    <li>Spot labels and timepoints</li>
	 *    <li>Spot positions and covariances</li>
	 *    <li>Order of outgoing edges</li>
	 * </ul>
	 * The following properties are ignored:
	 * <ul>
	 *     <li>{@link Spot#getInternalPoolIndex()}</li>
	 *     <li>Order of incoming edges is ignored</li>
	 * </ul>
	 */
	public static void assertGraphEquals( ModelGraph a, ModelGraph b )
	{
		assertStringEquals( spotsAsString( a ), spotsAsString( b ) );
		assertStringEquals( adjacencyAsString( a ), adjacencyAsString( b ) );
	}

	private static void assertTagSetModelEquals( Model a, Model b )
	{
		assertStringEquals( tagSetModelAsString( a ), tagSetModelAsString( b ) );
	}

	private static String spotsAsString( ModelGraph graph )
	{
		List< String > strings = new ArrayList<>();
		for ( Spot spot : graph.vertices() )
			strings.add( spotToString( spot ) );
		Collections.sort( strings );
		return String.join( "\n", strings );
	}

	private static String adjacencyAsString( ModelGraph graph )
	{
		List< String > strings = new ArrayList<>();
		Spot ref = graph.vertexRef();
		StringBuilder sb = new StringBuilder();
		for ( Spot spot : graph.vertices() )
		{
			sb.setLength( 0 );
			sb.append( spotToString( spot ) ).append( " -> " );
			for ( Link link : spot.outgoingEdges() )
				sb.append( spotToString( link.getTarget( ref ) ) ).append( ", " );
			strings.add( sb.toString() );
		}
		Collections.sort( strings );
		return String.join( "\n", strings );
	}

	private static String spotToString( Spot spot )
	{
		String label = spot.isLabelSet() ? spot.getLabel() : "<null>";
		int timepoint = spot.getTimepoint();
		double[] position = new double[ 3 ];
		spot.localize( position );
		double[][] cov = new double[ 3 ][ 3 ];
		spot.getCovariance( cov );
		StringBuilder sb = new StringBuilder();
		sb.append( "Spot(" );
		sb.append( "label=" ).append( label );
		sb.append( ", timepoint=" ).append( timepoint );
		sb.append( ", position=" ).append( Arrays.toString( position ) );
		sb.append( ", cov=" ).append( Arrays.deepToString( cov ) );
		sb.append( ")" );
		return sb.toString();
	}

	// Package private to allow testing.
	static String tagSetModelAsString( Model model )
	{
		StringBuilder sb = new StringBuilder();
		appendTagSetNamesTagsAndColors( sb, model );
		appendVertexTags( sb, model );
		appendEdgeTags( sb, model );
		return sb.toString();
	}

	private static void appendTagSetNamesTagsAndColors( StringBuilder sb, Model model )
	{
		List< TagSetStructure.TagSet > tagSets = model.getTagSetModel().getTagSetStructure().getTagSets();
		for ( int i = 0; i < tagSets.size(); i++ )
		{
			TagSetStructure.TagSet tagSet = tagSets.get( i );
			sb.append( "== Tag set: index=" ).append( i ).append( " name=" ).append( tagSet.getName() );
			if ( tagSet.getTags().isEmpty() )
				sb.append( " (no tags)" );
			else
			{
				sb.append( " tags: " );
				sb.append( tagSet.getTags().stream().map( tag -> String.format( "%s color=0x%08x", tag.label(), tag.color() ) ).collect( Collectors.joining( ", " ) ) );
			}
			sb.append( "\n" );
		}
	}

	private static void appendVertexTags( StringBuilder sb, Model model )
	{
		TagSetModel< Spot, Link > tagSetModel = model.getTagSetModel();
		List< TagSetStructure.TagSet > tagSets = tagSetModel.getTagSetStructure().getTagSets();
		List< ObjTagMap< Spot, TagSetStructure.Tag > > vertexTags = tagSets.stream().map( ts -> tagSetModel.getVertexTags().tags( ts ) ).collect( Collectors.toList() );

		ArrayList< String > lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for ( Spot spot : model.getGraph().vertices() )
		{
			line.setLength( 0 );
			line.append( spotToString( spot ) );
			line.append( ":" );
			for ( int i = 0; i < vertexTags.size(); i++ )
			{
				ObjTagMap< Spot, TagSetStructure.Tag > spotTagObjTagMap = vertexTags.get( i );
				TagSetStructure.Tag tag = spotTagObjTagMap.get( spot );
				if ( tag != null )
					line.append( " " ).append( i ).append( "=" ).append( tag.label() );
			}
			line.append( "\n" );
			lines.add( line.toString() );
		}
		Collections.sort( lines );
		lines.forEach( sb::append );
	}

	private static void appendEdgeTags( StringBuilder sb, Model model )
	{
		TagSetModel< Spot, Link > tagSetModel = model.getTagSetModel();
		List< TagSetStructure.TagSet > tagSets = tagSetModel.getTagSetStructure().getTagSets();
		List< ObjTagMap< Link, TagSetStructure.Tag > > edgeTags = tagSets.stream()
				.map( ts -> tagSetModel.getEdgeTags().tags( ts ) )
				.collect( Collectors.toList() );

		Spot ref1 = model.getGraph().vertexRef();
		Spot ref2 = model.getGraph().vertexRef();
		try
		{
			ArrayList< String > lines = new ArrayList<>();
			StringBuilder line = new StringBuilder();
			for ( Link link : model.getGraph().edges() )
			{
				Spot source = link.getSource( ref1 );
				Spot target = link.getTarget( ref2 );
				line.setLength( 0 );
				line.append( spotToString( source ) );
				line.append( "->" );
				line.append( spotToString( target ) );
				line.append( ":" );
				for ( int i = 0; i < edgeTags.size(); i++ )
				{
					TagSetStructure.Tag tag = edgeTags.get( i ).get( link );
					if ( tag != null )
						line.append( " " ).append( i ).append( "=" ).append( tag.label() );
				}
				line.append( "\n" );
				lines.add( line.toString() );
			}
			Collections.sort( lines );
			lines.forEach( sb::append );
		}
		finally
		{
			model.getGraph().releaseRef( ref1 );
			model.getGraph().releaseRef( ref2 );
		}
	}

	private static void assertStringEquals( String expected, String actual )
	{
		int length = Math.max( expected.length(), actual.length() );
		if ( length <= 200_000 )
			assertEquals( expected, actual );
		else
		{
			if ( Objects.equals( expected, actual ) )
				return;
			try
			{
				Path directory = Files.createTempDirectory( "assertion_error_strings_are_different" );
				FileUtils.writeStringToFile( directory.resolve( "expected.txt" ).toFile(), expected );
				FileUtils.writeStringToFile( directory.resolve( "actual.txt" ).toFile(), actual );
				throw new AssertionError( "Two strings that where expected to be equal but are different. "
						+ "The strings are very long so they where written into text files in: " + directory );
			}
			catch ( IOException e )
			{
				assertEquals( expected, actual );
			}
			// write expected and actual strings into the files
		}
	}
}
