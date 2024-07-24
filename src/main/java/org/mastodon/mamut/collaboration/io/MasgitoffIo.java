package org.mastodon.mamut.collaboration.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.mastodon.collection.ref.RefObjectHashMap;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.pool.PoolCollectionWrapper;
import org.mastodon.pool.PoolObject;
import org.mastodon.pool.PoolObjectAttributeSerializer;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class MasgitoffIo
{
	public static class MasgitoffIds
	{

		private final Map< Spot, UUID > uuids;

		private final Index< Spot > spotIndex;

		private final Index< Link > linkIndex;

		private final TObjectIntMap< String > stringIndex;

		public MasgitoffIds( final ModelGraph graph )
		{
			this.uuids = new RefObjectHashMap<>( graph.vertices().getRefPool() );
			this.spotIndex = new Index<>( graph.vertices().getRefPool() );
			this.linkIndex = new Index<>( graph.edges().getRefPool() );
			this.stringIndex = new TObjectIntHashMap<>( 100, 0.5f, -1 );
		}

		public Map< Spot, UUID > getSpotUuids()
		{
			return uuids;
		}

		public Index< Spot > getSpotIndex()
		{
			return spotIndex;
		}

		public Index< Link > getLinkIndex()
		{
			return linkIndex;
		}

		public TObjectIntMap< String > getLabelIndex()
		{
			return stringIndex;
		}
	}

	static void writeMasgitoff( final Model model, final File file, final MasgitoffIds masgitoffIds ) throws IOException
	{
		if ( !file.mkdir() )
			throw new RuntimeException( "Could not create directory " + file.getAbsolutePath() );

		final ModelGraph graph = model.getGraph();

		fillIds( model, masgitoffIds );
		final Map< Spot, UUID > uuids = masgitoffIds.getSpotUuids();
		final Index< Spot > spotIndex = masgitoffIds.getSpotIndex();
		final Index< Link > linkIndex = masgitoffIds.getLinkIndex();
		final TObjectIntMap< String > labelIndex = masgitoffIds.getLabelIndex();

		writeSpotTable( file, spotIndex, uuids, labelIndex );
		writeLinkTable( file, linkIndex, spotIndex, graph.vertexRef() );
		writeSpotLabels( createSubDirectory( file, "spots_labels" ), labelIndex );
		// FIXME: write labelsets
	}

	private static void writeSpotTable( final File file, final Index< Spot > spotIndex, final Map< Spot, UUID > uuids, final TObjectIntMap< String > labelIndex ) throws IOException
	{
		writeRawTable( createSubDirectory( file, "spots" ), spotIndex, ( id, spot, out ) -> {
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
		writeRawTable( createSubDirectory( file, "links" ), linkIndex, ( id, link, out ) -> {
			out.writeInt( spotIndex.getId( link.getSource( ref ) ) );
			out.writeInt( spotIndex.getId( link.getTarget( ref ) ) );
			out.writeInt( link.getSourceOutIndex() );
			out.writeInt( link.getTargetInIndex() );
			final int tagId = 0; // FIXME: write actual labelset id
			out.writeInt( tagId );
		}, ModelSerializer.getInstance().getEdgeSerializer() );
	}

	private static void fillIds( final Model model, final MasgitoffIds masgitoffIds )
	{
		fillUuids( model, masgitoffIds.getSpotUuids() );
		fillIndex( model.getGraph().vertices(), masgitoffIds.getSpotIndex() );
		fillIndex( model.getGraph().edges(), masgitoffIds.getLinkIndex() );
		fillLabelIndex( model.getGraph().vertices(), masgitoffIds.getLabelIndex() );
	}

	private static void fillUuids( final Model model, final Map< Spot, UUID > spotUuids )
	{
		for ( final Spot spot : model.getGraph().vertices() )
			if ( !spotUuids.containsKey( spot ) )
				spotUuids.put( spot, UUID.randomUUID() );
	}

	private static < T extends PoolObject< T, ?, ? > > void fillIndex( final PoolCollectionWrapper< T > collection, final Index< T > ids )
	{
		for ( final T object : collection )
			ids.getOrCreateId( object );
	}

	private static void fillLabelIndex( final PoolCollectionWrapper< Spot > spots, final TObjectIntMap< String > labelIndex )
	{
		// fixme make sure that label indices are unique
		for ( final Spot spot : spots )
		{
			if ( !spot.isLabelSet() )
				continue;
			final String label = spot.getLabel();
			if ( !labelIndex.containsKey( label ) )
				labelIndex.put( label, labelIndex.size() );
		}
	}

	private static void writeSpotLabels( final File folder, final TObjectIntMap< String > labelIndex ) throws IOException
	{
		final List< String > labels = asList( labelIndex );
		writeStringsChunked( folder, labels );
	}

	private static List< String > asList( final TObjectIntMap< String > labelIndex )
	{
		// FIXME: benchmark and optimize
		final TIntIterator iterator = labelIndex.valueCollection().iterator();
		int maxId = -1;
		while ( iterator.hasNext() )
			maxId = Math.max( maxId, iterator.next() );

		final List< String > labels = new ArrayList<>( Collections.nCopies( maxId + 1, null ) );

		final TObjectIntIterator< String > content = labelIndex.iterator();
		while ( content.hasNext() )
		{
			content.advance();
			labels.set( content.value(), content.key() );
		}

		return labels;
	}

	private static void writeStringsChunked( final File folder, final List< String > labels ) throws IOException
	{
		final int size = labels.size();
		for ( int j = 0; j < size; j += 1000 )
		{
			final File labels_raw = new File( folder, j + ".raw" );
			try (final DataOutputStream out = new DataOutputStream( new BufferedOutputStream( new FileOutputStream( labels_raw ) ) ))
			{
				final int limit = Math.min( j + 1000, size );
				for ( int id = j; id < limit; id++ )
				{
					final String label = labels.get( id );
					if ( label == null )
						continue;
					out.writeInt( id );
					out.writeUTF( label );
				}
			}
		}
	}

	private static List< String > readStringsChunked( final File folder ) throws IOException
	{
		final List< String > labels = new ArrayList<>();
		for ( int i = 0; true; i += 1000 )
		{
			final File labels_raw = new File( folder, i + ".raw" );
			if ( !labels_raw.exists() )
				break;

			try (final DataInputStream in = new DataInputStream( new BufferedInputStream( new FileInputStream( labels_raw ) ) ))
			{
				for ( int j = 0; j < 1000 && in.available() > 0; j++ )
				{
					final int id = in.readInt();
					final String label = in.readUTF();
					setAndGrow( labels, id, label );
				}
			}
		}
		return labels;
	}

	/**
	 * Set the element at the specified index in the list, growing the list if
	 * necessary.
	 */
	private static < T > void setAndGrow( final List< T > list, final int index, final T value )
	{
		if ( index == list.size() )
			list.add( value );
		else if ( index < list.size() )
			list.set( index, value );
		else
		{
			while ( list.size() < index )
				list.add( null );
			list.add( value );
		}
	}


	private static File createSubDirectory( final File file, final String title )
	{
		final File spotsDirectory = new File( file, title );
		if ( !spotsDirectory.mkdir() )
			throw new RuntimeException( "Could not create directory " + spotsDirectory.getAbsolutePath() );
		return spotsDirectory;
	}

	private static < T extends PoolObject< T, ?, ? > > void writeRawTable( final File directory, final Index< T > spotOrLinks,
			final Serializer< T > preserializer, final PoolObjectAttributeSerializer< T > serializer ) throws IOException
	{
		final T ref = spotOrLinks.createRef();
		final byte[] bytes = new byte[ serializer.getNumBytes() ];
		final int size = spotOrLinks.getMaxId() + 1;
		for ( int j = 0; j < size; j += 1000 )
		{
			final File spots_raw = new File( directory, j + ".raw" );
			try (final DataOutputStream out = new DataOutputStream( new BufferedOutputStream( new FileOutputStream( spots_raw ) ) ))
			{
				final int limit = Math.min( j + 1000, size );
				for ( int id = j; id < limit; id++ )
				{
					final T spotOrLink = spotOrLinks.getObject( id, ref );
					if ( spotOrLink == null )
						continue;
					out.writeInt( id );
					preserializer.serialize( id, spotOrLink, out );
					serializer.getBytes( spotOrLink, bytes );
					out.write( bytes );
				}
			}
		}
	}

	public static ModelGraph readMasgitoff( final File file ) throws IOException
	{
		final ModelGraph graph = new ModelGraph();
		final Spot ref = graph.vertices().createRef();
		final ModelSerializer instance = ModelSerializer.getInstance();
		final MasgitoffIds masgitoffIds = new MasgitoffIds( graph );
		final Index< Spot > spotIndex = masgitoffIds.getSpotIndex();
		// FIXME: read labels
		final List< String > labels = readStringsChunked( new File( file, "spots_labels" ) );
		// FIXME: read labelsets
		read( new File( file, "spots" ), instance.getVertexSerializer(), ( in ) -> {
			final int id = in.readInt();
			final UUID uuid = new UUID( in.readLong(), in.readLong() );
			final int labelId = in.readInt();
			final int tagId = in.readInt();
			final Spot spot = graph.addVertex( ref );
			if ( labelId >= 0 )
				ref.setLabel( labels.get( labelId ) );
			spotIndex.put( spot, id );
			return spot;
		} );
		final Spot ref1 = graph.vertexRef();
		final Spot ref2 = graph.vertexRef();
		final Link ref3 = graph.edgeRef();
		read( new File( file, "links" ), instance.getEdgeSerializer(), ( in ) -> {
			final int id = in.readInt();
			final Spot source = spotIndex.getObject( in.readInt(), ref1 );
			final Spot target = spotIndex.getObject( in.readInt(), ref2 );
			final int sourceOutIndex = in.readInt();
			final int targetInIndex = in.readInt();
			final int tagId = in.readInt();
			return graph.insertEdge( source, sourceOutIndex, target, targetInIndex, ref3 );
		} );
		return graph;
	}

	private static < T extends PoolObject< T, ?, ? > > void read(
			final File directory,
			final PoolObjectAttributeSerializer< T > serializer,
			final Deserializer< T > addItem ) throws IOException
	{
		final byte[] bytes = new byte[ serializer.getNumBytes() ];
		int i = 0;
		while ( true )
		{
			final File spots_raw = new File( directory, i + ".raw" );
			if ( !spots_raw.exists() )
				break;
			i += 1000;
			try (final DataInputStream in = new DataInputStream( new BufferedInputStream( new FileInputStream( spots_raw ) ) ))
			{

				for ( int j = 0; j < 1000 && in.available() > 0; j++ )
				{
					final T spot = addItem.deserialize( in );
					if ( bytes.length > 0 )
					{
						final int result = in.read( bytes );
						if ( result == -1 )
							break;
						if ( result != bytes.length )
							throw new IOException( "Unexpected end of file." );
						serializer.setBytes( spot, bytes );
					}
					serializer.notifySet( spot );
				}
			}
		}
	}

	private interface Serializer< T >
	{
		void serialize( int id, T object, DataOutputStream out ) throws IOException;
	}

	private interface Deserializer< T >
	{
		T deserialize( DataInputStream in ) throws IOException;
	}
}
