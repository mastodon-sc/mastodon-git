package org.mastodon.mamut.collaboration.io;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

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
			final UUID uuid = uuids.get( spot );
			out.writeLong( uuid.getLeastSignificantBits() );
			out.writeLong( uuid.getMostSignificantBits() );
			out.writeInt( labelIndex.get( spot.getLabel() ) );
			out.writeInt( spotToTagId.applyAsInt( spot ) );
		}, ModelSerializer.getInstance().getVertexSerializer() );
	}

	private static void writeLinkTable( final File file, final Index< Link > linkIndex, final Index< Spot > spotIndex, final Spot ref, ToIntFunction< Link > linkToTagId ) throws IOException
	{
		TableIo.writeRawTable( createSubDirectory( file, "links" ), linkIndex, ( id, link, out ) -> {
			out.writeInt( spotIndex.getId( link.getSource( ref ) ) );
			out.writeInt( spotIndex.getId( link.getTarget( ref ) ) );
			out.writeInt( link.getSourceOutIndex() );
			out.writeInt( link.getTargetInIndex() );
			out.writeInt( linkToTagId.applyAsInt( link ) );
		}, ModelSerializer.getInstance().getEdgeSerializer() );
	}

	public static Model readMasgitoff( final File file ) throws IOException
	{
		final Model model = new Model();
		final ModelGraph graph = model.getGraph();
		final MasgitoffIds masgitoffIds = new MasgitoffIds( graph );
		final Index< Spot > spotIndex = masgitoffIds.getSpotIndex();
		final List< String > labels = LabelIo.readStringsChunked( new File( file, "spots_labels" ) );
		final TagSetStructure tagSetStructure = TagIo.readTagSetStructure( new File( file, "tagsetstructure.raw" ) );
		model.getTagSetModel().setTagSetStructure( tagSetStructure );
		final TagIo.TagReader< Spot > spotTagReader = TagIo.createSpotsTagReader( new File( file, "spots_tag_lookup_table.raw" ), model );
		readSpots( new File( file, "spots" ), graph, labels, spotIndex, spotTagReader );
		final TagIo.TagReader< Link > linkTagReader = TagIo.createLinksTagReader( new File( file, "links_tag_lookup_table.raw" ), model );
		readLinks( file, graph, spotIndex, linkTagReader );
		return model;
	}

	private static void readSpots( final File spotsFolder, final ModelGraph graph, final List< String > labelIndex, final Index< Spot > spotIndex, TagIo.TagReader< Spot > spotTagReader )
			throws IOException
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
			spotTagReader.assignTagId( spot, tagId );
			return spot;
		} );
		spotTagReader.finish();
	}

	private static void readLinks( final File file, final ModelGraph graph, final Index< Spot > spotIndex, final TagIo.TagReader< Link > linkTagReader ) throws IOException
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
			final Link link = graph.insertEdge( source, sourceOutIndex, target, targetInIndex, ref3 );
			linkTagReader.assignTagId( link, tagId );
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
}
