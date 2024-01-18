package org.mastodon.mamut.collaboration.utils;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
	public static void assertModelEquals( Model a, Model b )
	{
		assertGraphEquals( a.getGraph(), b.getGraph() );
		assertTagSetModelEquals( a, b );
	}

	public static void assertGraphEquals( ModelGraph a, ModelGraph b )
	{
		assertEquals( spotsAsStrings( a ), spotsAsStrings( b ) );
		assertEquals( adjacencyAsStrings( a ), adjacencyAsStrings( b ) );
	}

	private static void assertTagSetModelEquals( Model a, Model b )
	{
		assertEquals( tagSetModelAsString( a ), tagSetModelAsString( b ) );
	}

	private static List< String > spotsAsStrings( ModelGraph graph )
	{
		List< String > strings = new ArrayList<>();
		for ( Spot spot : graph.vertices() )
			strings.add( spotToString( spot ) );
		Collections.sort( strings );
		return strings;
	}

	private static List< String > adjacencyAsStrings( ModelGraph graph )
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
		return strings;
	}

	static String spotToString( Spot spot )
	{
		String label = spot.getLabel();
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
				lines.add( line.toString() );
				for ( int i = 0; i < edgeTags.size(); i++ )
				{
					TagSetStructure.Tag tag = edgeTags.get( i ).get( link );
					if ( tag != null )
						line.append( " " ).append( i ).append( "=" ).append( tag.label() );
				}
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
}
