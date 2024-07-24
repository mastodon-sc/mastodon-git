package org.mastodon.mamut.collaboration.io;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;

import gnu.trove.map.TObjectIntMap;

public class MasgitoffIo
{

	static void writeMasgitoff( final Model model, final File file, final MasgitoffIds masgitoffIds ) throws IOException
	{
		if ( !file.mkdir() )
			throw new RuntimeException( "Could not create directory " + file.getAbsolutePath() );

		final ModelGraph graph = model.getGraph();

		masgitoffIds.fillIds( model );
		final Map< Spot, UUID > uuids = masgitoffIds.getSpotUuids();
		final Index< Spot > spotIndex = masgitoffIds.getSpotIndex();
		final Index< Link > linkIndex = masgitoffIds.getLinkIndex();
		final TObjectIntMap< String > labelIndex = masgitoffIds.getLabelIndex();

		writeSpotTable( file, spotIndex, uuids, labelIndex );
		writeLinkTable( file, linkIndex, spotIndex, graph.vertexRef() );
		LabelIo.writeSpotLabels( createSubDirectory( file, "spots_labels" ), labelIndex );
		// FIXME: write labelsets
	}

	private static void writeSpotTable( final File file, final Index< Spot > spotIndex, final Map< Spot, UUID > uuids, final TObjectIntMap< String > labelIndex ) throws IOException
	{
		TableIo.writeRawTable( createSubDirectory( file, "spots" ), spotIndex, ( id, spot, out ) -> {
			final UUID uuid = uuids.get( spot );
			out.writeLong( uuid.getLeastSignificantBits() );
			out.writeLong( uuid.getMostSignificantBits() );
			out.writeInt( labelIndex.get( spot.getLabel() ) );
			final int tagId = 0; // FIXME: write actual labelset id
			out.writeInt( tagId );
		}, ModelSerializer.getInstance().getVertexSerializer() );
	}

	private static void writeLinkTable( final File file, final Index< Link > linkIndex, final Index< Spot > spotIndex, final Spot ref ) throws IOException
	{
		TableIo.writeRawTable( createSubDirectory( file, "links" ), linkIndex, ( id, link, out ) -> {
			out.writeInt( spotIndex.getId( link.getSource( ref ) ) );
			out.writeInt( spotIndex.getId( link.getTarget( ref ) ) );
			out.writeInt( link.getSourceOutIndex() );
			out.writeInt( link.getTargetInIndex() );
			final int tagId = 0; // FIXME: write actual labelset id
			out.writeInt( tagId );
		}, ModelSerializer.getInstance().getEdgeSerializer() );
	}

	public static ModelGraph readMasgitoff( final File file ) throws IOException
	{
		final ModelGraph graph = new ModelGraph();
		final MasgitoffIds masgitoffIds = new MasgitoffIds( graph );
		final Index< Spot > spotIndex = masgitoffIds.getSpotIndex();
		final List< String > labels = LabelIo.readStringsChunked( new File( file, "spots_labels" ) );
		readSpots( new File( file, "spots" ), graph, labels, spotIndex );
		readLinks( file, graph, spotIndex );
		return graph;
	}

	private static void readSpots( final File spotsFolder, final ModelGraph graph, final List< String > labelIndex, final Index< Spot > spotIndex ) throws IOException
	{
		final Spot ref = graph.vertices().createRef();
		TableIo.read( spotsFolder, ModelSerializer.getInstance().getVertexSerializer(), ( in ) -> {
			final int id = in.readInt();
			final UUID uuid = new UUID( in.readLong(), in.readLong() );
			final int labelId = in.readInt();
			final int tagId = in.readInt();
			final Spot spot = graph.addVertex( ref );
			if ( labelId >= 0 )
				ref.setLabel( labelIndex.get( labelId ) );
			spotIndex.put( spot, id );
			return spot;
		} );
	}

	private static void readLinks( final File file, final ModelGraph graph, final Index< Spot > spotIndex ) throws IOException
	{
		final Spot ref1 = graph.vertexRef();
		final Spot ref2 = graph.vertexRef();
		final Link ref3 = graph.edgeRef();
		TableIo.read( new File( file, "links" ), ModelSerializer.getInstance().getEdgeSerializer(), ( in ) -> {
			final int id = in.readInt();
			final Spot source = spotIndex.getObject( in.readInt(), ref1 );
			final Spot target = spotIndex.getObject( in.readInt(), ref2 );
			final int sourceOutIndex = in.readInt();
			final int targetInIndex = in.readInt();
			final int tagId = in.readInt();
			return graph.insertEdge( source, sourceOutIndex, target, targetInIndex, ref3 );
		} );
	}

	private static File createSubDirectory( final File file, final String title )
	{
		final File spotsDirectory = new File( file, title );
		if ( !spotsDirectory.mkdir() )
			throw new RuntimeException( "Could not create directory " + spotsDirectory.getAbsolutePath() );
		return spotsDirectory;
	}
}
