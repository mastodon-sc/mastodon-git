package org.mastodon.mamut.collaboration.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.tag.TagSetStructure;
import org.mastodon.util.TagSetUtils;

/**
 * Tests for {@link ModelAsserts}.
 **/
public class ModelAssertsTest
{
	@Test
	public void testAssertModelEqualsForSingleSpot()
	{
		Model modelA = singleSpotModel( "A", 1, new double[] { 1, 2, 3 }, new double[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } } );
		Model modelA2 = singleSpotModel( "A", 1, new double[] { 1, 2, 3 }, new double[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } } );
		Model modelB = singleSpotModel( "B", 1, new double[] { 1, 2, 3 }, new double[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } } );
		Model modelC = singleSpotModel( "A", 2, new double[] { 1, 2, 3 }, new double[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } } );
		Model modelD = singleSpotModel( "A", 1, new double[] { 1, 2, 3.1 }, new double[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } } );
		Model modelE = singleSpotModel( "A", 1, new double[] { 1, 2, 3 }, new double[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1.1 } } );

		Model modelTwoSpots = new Model();
		ModelGraph graphTwoSpots = modelTwoSpots.getGraph();
		Spot spot1 = graphTwoSpots.addVertex().init( 1, new double[] { 1, 2, 3 }, new double[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } } );
		Spot spot2 = graphTwoSpots.addVertex().init( 1, new double[] { 1, 2, 3 }, new double[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } } );
		spot1.setLabel( "A" );
		spot2.setLabel( "A" );

		ModelAsserts.assertModelEquals( modelA, modelA );
		ModelAsserts.assertModelEquals( modelA, modelA2 );
		assertThrows( AssertionError.class, () -> ModelAsserts.assertModelEquals( modelA, modelB ) );
		assertThrows( AssertionError.class, () -> ModelAsserts.assertModelEquals( modelA, modelC ) );
		assertThrows( AssertionError.class, () -> ModelAsserts.assertModelEquals( modelA, modelD ) );
		assertThrows( AssertionError.class, () -> ModelAsserts.assertModelEquals( modelA, modelE ) );
		assertThrows( AssertionError.class, () -> ModelAsserts.assertModelEquals( modelA, modelTwoSpots ) );
	}

	private static Model singleSpotModel( String label, int timepoint, double[] position, double[][] cov )
	{
		Model model1 = new Model();
		ModelGraph graph1 = model1.getGraph();
		Spot spot1 = graph1.addVertex().init( timepoint, position, cov );
		spot1.setLabel( label );
		return model1;
	}

	@Test
	public void testAssertModelEquals_outgoingEdgesOrder()
	{
		Model a = initializeThreeSpotModel( false );
		Model b = initializeThreeSpotModel( false );
		Model c = initializeThreeSpotModel( true );
		ModelAsserts.assertModelEquals( a, b );
		assertThrows( AssertionError.class, () -> ModelAsserts.assertModelEquals( a, c ) );
	}

	private Model initializeThreeSpotModel( boolean invertedEdgesOrder )
	{
		Model model = new Model();
		ModelGraph graph = model.getGraph();
		Spot spotA = graph.addVertex().init( 1, new double[ 3 ], new double[ 3 ][ 3 ] );
		Spot spotB = graph.addVertex().init( 2, new double[ 3 ], new double[ 3 ][ 3 ] );
		Spot spotC = graph.addVertex().init( 2, new double[ 3 ], new double[ 3 ][ 3 ] );
		spotA.setLabel( "A" );
		spotB.setLabel( "B" );
		spotC.setLabel( "C" );
		if ( invertedEdgesOrder )
		{
			graph.addEdge( spotA, spotC );
			graph.addEdge( spotA, spotB );
		}
		else
		{
			graph.addEdge( spotA, spotB );
			graph.addEdge( spotA, spotC );
		}
		return model;
	}

	@Test
	public void testAssertModelEquals_tagSets()
	{
		Model model = initializeSmallModelWithTags();
		String text = ModelAsserts.tagSetModelAsString( model );
		String expected = "== Tag set: index=0 name=tagset1 tags: tag1 color=0x01020304, tag2 color=0x05060708\n"
				+ "== Tag set: index=1 name=tabset2 (no tags)\n"
				+ "Spot(label=A, timepoint=0, position=[0.0, 0.0, 0.0], cov=[[1.0, 0.0, 0.0], [0.0, 1.0, 0.0], [0.0, 0.0, 1.0]]): 0=tag1\n"
				+ "Spot(label=B, timepoint=1, position=[0.0, 0.0, 0.0], cov=[[1.0, 0.0, 0.0], [0.0, 1.0, 0.0], [0.0, 0.0, 1.0]]): 0=tag2\n"
				+ "Spot(label=A, timepoint=0, position=[0.0, 0.0, 0.0], cov=[[1.0, 0.0, 0.0], [0.0, 1.0, 0.0], [0.0, 0.0, 1.0]])->Spot(label=B, timepoint=1, position=[0.0, 0.0, 0.0], cov=[[1.0, 0.0, 0.0], [0.0, 1.0, 0.0], [0.0, 0.0, 1.0]]):Spot(label=A, timepoint=0, position=[0.0, 0.0, 0.0], cov=[[1.0, 0.0, 0.0], [0.0, 1.0, 0.0], [0.0, 0.0, 1.0]])->Spot(label=B, timepoint=1, position=[0.0, 0.0, 0.0], cov=[[1.0, 0.0, 0.0], [0.0, 1.0, 0.0], [0.0, 0.0, 1.0]]): 0=tag1";
		assertEquals( expected, text );
	}

	private static Model initializeSmallModelWithTags()
	{
		Model model = new Model();
		TagSetStructure.TagSet tagSet1 = TagSetUtils.addNewTagSetToModel( model, "tagset1", Arrays.asList( Pair.of( "tag1", 0x01020304 ), Pair.of( "tag2", 0x05060708 ) ) );
		TagSetUtils.addNewTagSetToModel( model, "tabset2", Collections.emptyList() );
		Spot spotA = model.getGraph().addVertex().init( 0, new double[ 3 ], 1 );
		spotA.setLabel( "A" );
		Spot spotB = model.getGraph().addVertex().init( 1, new double[ 3 ], 1 );
		spotB.setLabel( "B" );
		Link link = model.getGraph().addEdge( spotA, spotB );
		TagSetUtils.tagSpot( model, tagSet1, tagSet1.getTags().get( 0 ), spotA );
		TagSetUtils.tagSpot( model, tagSet1, tagSet1.getTags().get( 1 ), spotB );
		TagSetUtils.tagLinks( model, tagSet1, tagSet1.getTags().get( 0 ), Collections.singletonList( link ) );
		return model;
	}

}
