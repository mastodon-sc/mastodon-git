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

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;

public class LabelIo
{
	static void writeSpotLabels( final File folder, final TObjectIntMap< String > labelIndex ) throws IOException
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

	static List< String > readStringsChunked( final File folder ) throws IOException
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
}
