package org.mastodon.mamut.collaboration.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.mastodon.pool.PoolObject;
import org.mastodon.pool.PoolObjectAttributeSerializer;

class TableIo
{
	static < T extends PoolObject< T, ?, ? > > void writeRawTable( final File directory, final Index< T > spotOrLinks,
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

	static < T extends PoolObject< T, ?, ? > > void read(
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

	interface Serializer< T >
	{
		void serialize( int id, T object, DataOutputStream out ) throws IOException;
	}

	interface Deserializer< T >
	{
		T deserialize( DataInputStream in ) throws IOException;
	}
}
