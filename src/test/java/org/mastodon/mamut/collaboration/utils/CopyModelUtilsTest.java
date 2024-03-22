package org.mastodon.mamut.collaboration.utils;

import org.junit.Test;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;

/**
 * Tests for {@link CopyModelUtils}.
 */
public class CopyModelUtilsTest
{

	@Test
	public void testOutgoingEdgesOrder()
	{
		ModelGraph graphA = new ModelGraph();
		Spot a = graphA.addVertex().init( 0, new double[ 3 ], 1 );
		a.setLabel( "A" );
		Spot b = graphA.addVertex().init( 1, new double[ 3 ], 1 );
		b.setLabel( "B" );
		Spot c = graphA.addVertex().init( 1, new double[ 3 ], 1 );
		c.setLabel( "C" );
		graphA.addEdge( a, c );
		graphA.addEdge( a, b );
		ModelGraph graphB = new ModelGraph();
		CopyModelUtils.copyGraphFromTo( graphA, graphB );
		ModelAsserts.assertGraphEquals( graphB, graphA );
	}
}
