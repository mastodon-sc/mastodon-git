package org.mastodon.mamut.collaboration.io.benchmark;

import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;

public interface GrowingGraphExample
{
	static void removeWrongEdges( final ModelGraph graph )
	{
		final Spot ref1 = graph.vertexRef();
		final Spot ref2 = graph.vertexRef();
		try
		{
			for ( final Link link : graph.edges() )
			{
				final Spot source = link.getSource( ref1 );
				final Spot target = link.getTarget( ref2 );
				if ( source.getTimepoint() >= target.getTimepoint() )
					graph.remove( link );
			}
		}
		finally
		{
			graph.releaseRef( ref1 );
			graph.releaseRef( ref2 );
		}
	}

	int getCompletion();

	boolean hasNext();

	void grow( Model growingProjectModel );

	void assertEqualsOriginal( Model model );
}
