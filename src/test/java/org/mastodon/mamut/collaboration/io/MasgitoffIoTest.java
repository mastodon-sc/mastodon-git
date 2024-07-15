package org.mastodon.mamut.collaboration.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mastodon.collection.RefList;
import org.mastodon.collection.ref.RefArrayList;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.collaboration.utils.ModelAsserts;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.ModelSerializer;
import org.mastodon.mamut.model.Spot;
import org.mastodon.pool.PoolObject;
import org.mastodon.pool.PoolObjectAttributeSerializer;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

public class MasgitoffIoTest
{

	@Test
	public void test() throws SpimDataException, IOException
	{
		try (final Context context = new Context())
		{
			final ProjectModel projectModel = ProjectLoader.open( "/home/arzt/devel/mastodon/mastodon-git/src/test/resources/org/mastodon/mamut/collaboration/tiny/tiny-project.mastodon", context );
			final ModelGraph graph = projectModel.getModel().getGraph();
			final File file = new File( "tmp.masgitoff" );
			if ( file.isDirectory() )
				FileUtils.deleteDirectory( file );
			writeMasgitoff( graph, file );
			final ModelGraph newGraph = readMasgitoff( file );
			ModelAsserts.assertGraphEquals( graph, newGraph );
		}
	}

	private void writeMasgitoff( final ModelGraph graph, final File file ) throws IOException
	{
		if ( !file.mkdir() )
			throw new RuntimeException( "Could not create directory " + file.getAbsolutePath() );

		final RefList< Spot > spots = new RefArrayList<>( graph.vertices().getRefPool() );
		spots.addAll( graph.vertices() );

		// Have a row index too
		final RefList< Link > links = new RefArrayList<>( graph.edges().getRefPool() );
		links.addAll( graph.edges() );

		final ModelSerializer modelSerializer = ModelSerializer.getInstance();

		writeRawTable( createSubDirectory( file, "spots" ), spots, ( spot, out ) -> {}, modelSerializer.getVertexSerializer() );
		writeSpotLabels( new File( file, "spots_labels" ), spots );
		final Spot ref = graph.vertices().createRef();
		writeRawTable( createSubDirectory( file, "links" ), links, ( link, out ) -> {
			out.writeInt( link.getSource( ref ).getInternalPoolIndex() );
			out.writeInt( link.getTarget( ref ).getInternalPoolIndex() );
			out.writeInt( link.getSourceOutIndex() );
			out.writeInt( link.getTargetInIndex() );
		}, modelSerializer.getEdgeSerializer() );
	}

	private void writeSpotLabels( File spotsLabels, RefList< Spot > spots ) throws IOException
	{
		try (final DataOutputStream out = new DataOutputStream( new BufferedOutputStream( new FileOutputStream( spotsLabels ) ) ))
		{
			for ( Spot spot : spots )
			{
				out.writeInt( spot.getInternalPoolIndex() );
				out.writeUTF( spot.getLabel() );
			}
		}
	}

	private interface Deserializer< T >
	{
		T deserialize( DataInputStream in ) throws IOException;
	}

	private ModelGraph readMasgitoff( final File file ) throws IOException
	{
		final ModelGraph graph = new ModelGraph();
		final Spot ref = graph.vertices().createRef();
		final ModelSerializer instance = ModelSerializer.getInstance();
		read( new File( file, "spots" ), instance.getVertexSerializer(), ( in ) -> graph.addVertex( ref ) );
		final Spot ref1 = graph.vertexRef();
		final Spot ref2 = graph.vertexRef();
		final Link ref3 = graph.edgeRef();
		read( new File( file, "links" ), instance.getEdgeSerializer(), ( in ) -> {
			final Spot source = graph.vertices().getRefPool().getObject( in.readInt(), ref1 );
			final Spot target = graph.vertices().getRefPool().getObject( in.readInt(), ref2 );
			final int sourceOutIndex = in.readInt();
			final int targetInIndex = in.readInt();
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

	private static File createSubDirectory( final File file, final String title )
	{
		final File spotsDirectory = new File( file, title );
		if ( !spotsDirectory.mkdir() )
			throw new RuntimeException( "Could not create directory " + spotsDirectory.getAbsolutePath() );
		return spotsDirectory;
	}

	private interface Serializer< T >
	{
		void serialize( T object, DataOutputStream out ) throws IOException;
	}

	private static < T extends PoolObject< T, ?, ? > > void writeRawTable( final File directory, final RefList< T > spotOrLinks,
			final Serializer< T > preserializer, final PoolObjectAttributeSerializer< T > serializer ) throws IOException
	{
		final T ref = spotOrLinks.createRef();
		final byte[] bytes = new byte[ serializer.getNumBytes() ];
		for ( int j = 0; j < spotOrLinks.size(); j += 1000 )
		{
			final File spots_raw = new File( directory, j + ".raw" );
			try (final DataOutputStream out = new DataOutputStream( new BufferedOutputStream( new FileOutputStream( spots_raw ) ) ))
			{
				final int limit = Math.min( j + 1000, spotOrLinks.size() );
				for ( int k = j; k < limit; k++ )
				{
					final T spotOrLink = spotOrLinks.get( k, ref );
					if ( spotOrLink.getInternalPoolIndex() == -1 )
						continue;
					preserializer.serialize( spotOrLink, out );
					serializer.getBytes( spotOrLink, bytes );
					out.write( bytes );
				}
			}
		}
	}
}
