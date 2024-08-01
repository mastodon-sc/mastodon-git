package org.mastodon.mamut.collaboration.io;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.mastodon.Ref;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.collaboration.utils.ModelAsserts;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.util.TagHelper;
import org.mastodon.util.TagSetUtils;
import org.scijava.Context;

import gnu.trove.map.TObjectIntMap;
import mpicbg.spim.data.SpimDataException;

public class MasgitoffIoTest
{

	@Test
	public void test() throws SpimDataException, IOException
	{
		try (final Context context = new Context())
		{
			final Model model = initializeModel( context );
			final File file = new File( "tmp.masgitoff" );
			if ( file.isDirectory() )
				FileUtils.deleteDirectory( file );
			final MasgitoffIds ids = new MasgitoffIds( model.getGraph() );
			MasgitoffIo.writeMasgitoff( model, file, ids );
			final Pair< Model, MasgitoffIds > newModelAndIds = MasgitoffIo.readMasgitoff( file );
			ModelAsserts.assertModelEquals( model, newModelAndIds.getLeft() );
			assertIdsEqual( ids, newModelAndIds.getRight() );
		}
	}

	private static Model initializeModel( final Context context ) throws IOException, SpimDataException
	{
		final ProjectModel projectModel = ProjectLoader.open( "/home/arzt/devel/mastodon/mastodon-git/src/test/resources/org/mastodon/mamut/collaboration/tiny/tiny-project.mastodon", context );
		final Model model = projectModel.getModel();
		final ModelGraph graph = model.getGraph();
		final Spot someSpot = graph.vertices().getRefPool().getObject( 0, graph.vertexRef() );
		final Spot someOtherSpot = graph.vertices().getRefPool().getObject( 1, graph.vertexRef() );
		someSpot.setLabel( "some spot" );
		someOtherSpot.setLabel( "some other spot" );
		TagSetUtils.addNewTagSetToModel( model, "tagset", Arrays.asList( Pair.of( "a", 0xff0000 ), Pair.of( "b", 0x00ff00 ) ) );
		TagSetUtils.addNewTagSetToModel( model, "other tagset", Arrays.asList( Pair.of( "c", 0x0000ff ) ) );
		final TagHelper tagA = new TagHelper( model, "tagset", "a" );
		final TagHelper tagB = new TagHelper( model, "tagset", "b" );
		final TagHelper tagC = new TagHelper( model, "other tagset", "c" );
		tagA.tagSpot( someSpot );
		tagB.tagSpot( someOtherSpot );
		tagC.tagSpot( someOtherSpot );
		final Link aLink = someSpot.outgoingEdges().get( 0 );
		tagA.tagLink( aLink );
		tagB.tagLink( aLink );
		return model;
	}

	private void assertIdsEqual( final MasgitoffIds ids, final MasgitoffIds newIds )
	{
		assertEquals( toString( ids.getSpotUuids() ), toString( newIds.getSpotUuids() ) );
		assertEquals( toString( ids.getSpotIndex(), Spot::toString ), toString( newIds.getSpotIndex(), Spot::toString ) );

		final Spot ref = ids.getSpotIndex().createRef();
		final Function< Link, String > linkToString = link -> {
			final String source = link.getSource( ref ).toString();
			final String target = link.getTarget( ref ).toString();
			return "(" + source + " -> " + target + ")";
		};
		assertEquals( toString( ids.getLinkIndex(), linkToString ), toString( newIds.getLinkIndex(), linkToString ) );
		assertEquals( toString( ids.getLabelIndex() ), toString( newIds.getLabelIndex() ) );
	}

	private < T extends Ref< T > > String toString( final Index< T > spotIndex, final Function< T, String > keyToString )
	{
		final StringBuilder builder = new StringBuilder();
		spotIndex.forEach( ( key, id ) -> builder.append( keyToString.apply( key ) ).append( " -> " ).append( id ).append( "\n" ) );
		return builder.toString();
	}

	private static String toString( final Map< ?, ? > spotUuids )
	{
		final List< String > lines = new ArrayList<>();
		spotUuids.forEach( ( key, value ) -> lines.add( key + " -> " + value ) );
		Collections.sort( lines );
		return String.join( "\n", lines );
	}

	private String toString( TObjectIntMap< String > labelIndex )
	{
		final List< String > lines = new ArrayList<>();
		labelIndex.forEachEntry( ( key, value ) -> lines.add( key + " -> " + value ) );
		Collections.sort( lines );
		return String.join( "\n", lines );
	}
}
