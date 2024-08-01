package org.mastodon.mamut.collaboration.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

import org.apache.commons.lang3.tuple.Pair;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.tag.TagSetStructure;

import gnu.trove.map.TObjectIntMap;

public class MasgitoffIo
{

	public static void writeMasgitoff( final Model model, final File file, final MasgitoffIds masgitoffIds ) throws IOException
	{
		if ( !file.mkdir() )
			throw new RuntimeException( "Could not create directory " + file.getAbsolutePath() );

		final ModelGraph graph = model.getGraph();

		masgitoffIds.fillIds( model );
		final Index< Spot > spotIndex = masgitoffIds.getSpotIndex();
		final Index< Link > linkIndex = masgitoffIds.getLinkIndex();
		final TObjectIntMap< String > labelIndex = masgitoffIds.getLabelIndex();

		TagIo.writeTagSetStructure( new File( file, "tagsetstructure.raw" ), model.getTagSetModel().getTagSetStructure() );
		final ToIntFunction< Spot > spotToTagId = TagIo.writeSpotLabelSets( new File( file, "spots_tag_lookup_table.raw" ), model );
		writeSpotTable( file, masgitoffIds, spotToTagId );
		final ToIntFunction< Link > linkToTagId = TagIo.writeLinkLabelSets( new File( file, "links_tag_lookup_table.raw" ), model );
		writeLinkTable( file, linkIndex, spotIndex, graph.vertexRef(), linkToTagId );
		LabelIo.writeSpotLabels( createSubDirectory( file, "spots_labels" ), labelIndex );
	}

	private static void writeSpotTable( final File file, final MasgitoffIds ids, final ToIntFunction< Spot > spotToTagId ) throws IOException
	{
		final Map< Spot, UUID > uuids = ids.getSpotUuids();
		final Index< Spot > spotIndex = ids.getSpotIndex();
		final TObjectIntMap< String > labelIndex = ids.getLabelIndex();
		TableIo.writeRawTable( createSubDirectory( file, "spots" ), spotIndex, ( id, spot, out ) -> {
			writeUUID( out, uuids.get( spot ) );
			out.writeInt( spot.isLabelSet() ? labelIndex.get( spot.getLabel() ) : -1 );
			out.writeInt( spotToTagId.applyAsInt( spot ) );
		}, ModelSerializer.getInstance().getVertexSerializer() );
	}

	private static void writeLinkTable( final File file, final Index< Link > linkIndex, final Index< Spot > spotIndex, final Spot ref, final ToIntFunction< Link > linkToTagId ) throws IOException
	{
		TableIo.writeRawTable( createSubDirectory( file, "links" ), linkIndex, ( id, link, out ) -> {
			out.writeInt( spotIndex.getId( link.getSource( ref ) ) );
			out.writeInt( spotIndex.getId( link.getTarget( ref ) ) );
			out.writeInt( link.getSourceOutIndex() );
			out.writeInt( link.getTargetInIndex() );
			out.writeInt( linkToTagId.applyAsInt( link ) );
		}, ModelSerializer.getInstance().getEdgeSerializer() );
	}

	public static Pair< Model, MasgitoffIds > readMasgitoff( final File file ) throws IOException
	{
		final Model model = new Model();
		final ModelGraph graph = model.getGraph();
		final MasgitoffIds ids = new MasgitoffIds( graph );
		final List< String > labels = LabelIo.readStringsChunked( new File( file, "spots_labels" ) );
		final TagSetStructure tagSetStructure = TagIo.readTagSetStructure( new File( file, "tagsetstructure.raw" ) );
		model.getTagSetModel().setTagSetStructure( tagSetStructure );
		final TagIo.TagReader< Spot > spotTagReader = TagIo.createSpotsTagReader( new File( file, "spots_tag_lookup_table.raw" ), model );
		readSpots( new File( file, "spots" ), graph, labels, ids, spotTagReader );
		final TagIo.TagReader< Link > linkTagReader = TagIo.createLinksTagReader( new File( file, "links_tag_lookup_table.raw" ), model );
		readLinks( file, graph, ids, linkTagReader );
		return Pair.of( model, ids );
	}

	private static void readSpots( final File spotsFolder, final ModelGraph graph, final List< String > labelIndex, final MasgitoffIds ids, final TagIo.TagReader< Spot > spotTagReader )
			throws IOException
	{
		final Index< Spot > spotIndex = ids.getSpotIndex();
		final Map< Spot, UUID > uuids = ids.getSpotUuids();
		final Spot ref = graph.vertices().createRef();
		TableIo.read( spotsFolder, ModelSerializer.getInstance().getVertexSerializer(), ( in ) -> {
			final int id = in.readInt();
			final UUID uuid = readUUID( in );
			final int labelId = in.readInt();
			final int tagId = in.readInt();
			final Spot spot = graph.addVertex( ref );
			if ( labelId >= 0 )
				spot.setLabel( labelIndex.get( labelId ) );
			spotIndex.put( spot, id );
			uuids.put( spot, uuid );
			spotTagReader.assignTagId( spot, tagId );
			return spot;
		} );
		spotTagReader.finish();
	}

	private static void readLinks( final File file, final ModelGraph graph, final MasgitoffIds ids, final TagIo.TagReader< Link > linkTagReader ) throws IOException
	{
		final Index< Spot > spotIndex = ids.getSpotIndex();
		final Index< Link > linkIndex = ids.getLinkIndex();
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
			final Link link = graph.insertEdge( source, sourceOutIndex, target, targetInIndex, ref3 );
			linkTagReader.assignTagId( link, tagId );
			linkIndex.put( link, id );
			return link;
		} );
	}

	private static File createSubDirectory( final File file, final String title )
	{
		final File spotsDirectory = new File( file, title );
		if ( !spotsDirectory.mkdir() )
			throw new RuntimeException( "Could not create directory " + spotsDirectory.getAbsolutePath() );
		return spotsDirectory;
	}

	private static void writeUUID( final DataOutputStream out, final UUID uuid ) throws IOException
	{
		out.writeLong( uuid.getMostSignificantBits() );
		out.writeLong( uuid.getLeastSignificantBits() );
	}

	private static UUID readUUID( final DataInputStream in ) throws IOException
	{
		final long mostSigBits = in.readLong();
		final long leastSigBits = in.readLong();
		return new UUID( mostSigBits, leastSigBits );
	}

}
