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
import java.util.HashSet;
import java.util.Set;
import java.util.function.ToIntFunction;

import org.mastodon.collection.RefIntMap;
import org.mastodon.collection.RefList;
import org.mastodon.collection.ref.RefArrayList;
import org.mastodon.labels.LabelMapping;
import org.mastodon.labels.LabelSets;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.tag.DefaultTagSetModel;
import org.mastodon.model.tag.TagSetStructure;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class TagIo
{

	public static void writeTagSetStructure( final File file, final TagSetStructure tagSetStructure ) throws IOException
	{
		tagSetStructure.saveRaw( file );
	}

	public static TagSetStructure readTagSetStructure( final File file ) throws IOException
	{
		final TagSetStructure tagSetStructure = new TagSetStructure();
		tagSetStructure.loadRaw( file );
		return tagSetStructure;
	}

	public static ToIntFunction< Spot > writeSpotLabelSets( final File file, final Model model ) throws IOException
	{
		return writeSpotLabelSets( file, getSpotTagProperty( model ) );
	}

	public static ToIntFunction< Link > writeLinkLabelSets( final File file, final Model model ) throws IOException
	{
		return writeSpotLabelSets( file, getLinkTagProperty( model ) );
	}

	public static < T > ToIntFunction< T > writeSpotLabelSets( final File file, final LabelSets< T, Integer > propertyMap ) throws IOException
	{
		final LabelMapping< Integer > mapping = propertyMap.getLabelMapping();
		final RefIntMap< T > pmap = propertyMap.getBackingProperty().getMap();

		final TIntList used = new TIntArrayList();
		final TIntIntMap mappingIndexToFileIndex = new TIntIntHashMap();
		used.add( 0 );
		mappingIndexToFileIndex.put( 0, 0 );
		pmap.forEachValue( index -> {
			if ( !used.contains( index ) )
			{
				mappingIndexToFileIndex.put( index, used.size() );
				used.add( index );
			}
			return true;
		} );

		try (final DataOutputStream oos = new DataOutputStream( new BufferedOutputStream( new FileOutputStream( file ) ) ))
		{
			// NUMBER OF LABEL SETS
			oos.writeInt( used.size() );

			// LABEL SETS
			for ( final TIntIterator it = used.iterator(); it.hasNext(); )
			{
				final Set< Integer > labels = mapping.labelsAtIndex( it.next() );
				oos.writeInt( labels.size() );
				for ( final Integer tagId : labels )
					oos.writeInt( tagId );
			}
		}
		return spotOrLink -> {
			final int index = pmap.get( spotOrLink );
			return mappingIndexToFileIndex.get( index );
		};
	}

	public static TagReader< Spot > createSpotsTagReader( final File file, final Model model ) throws IOException
	{
		return new TagReader<>( file, getSpotTagProperty( model ) );
	}

	public static TagReader< Link > createLinksTagReader( final File file, final Model model ) throws IOException
	{
		return new TagReader<>( file, getLinkTagProperty( model ) );
	}

	public static class TagReader< T >
	{

		private final RefIntMap< T > backingMap;

		private final RefList< T > labeled;

		private final LabelSets< T, Integer > propertyMap;

		public TagReader(
				final File file,
				final LabelSets< T, Integer > propertyMap )
				throws IOException
		{
			this.propertyMap = propertyMap;
			this.propertyMap.clear();
			this.backingMap = propertyMap.getBackingProperty().getMap();

			final LabelMapping< Integer > mapping = propertyMap.getLabelMapping();
			new LabelMapping.SerialisationAccess< Integer >( mapping )
			{{
				setLabelSets( readLabelSets( file ) );
			}};

			labeled = new RefArrayList<>( propertyMap.getPool() );
		}

		private static ArrayList< Set< Integer > > readLabelSets( File file ) throws IOException
		{
			final ArrayList< Set< Integer > > labelSets = new ArrayList<>();

			try (final DataInputStream in = new DataInputStream( new BufferedInputStream( new FileInputStream( file ) ) ))
			{
				// NUMBER OF LABEL SETS
				final int numberOfSets = in.readInt();

				// LABEL SETS
				for ( int i = 0; i < numberOfSets; i++ )
				{
					// NUMBER OF LABELS IN SET
					final int numberOfLabels = in.readInt();

					// LABELS
					final Set< Integer > labelSet = new HashSet< Integer >();
					for ( int j = 0; j < numberOfLabels; j++ )
						labelSet.add( in.readInt() );

					labelSets.add( labelSet );
				}
			}
			return labelSets;
		}

		public void assignTagId( final T object, final int tagId )
		{
			if ( tagId > 0 )
			{
				labeled.add( object );
				backingMap.put( object, tagId );
			}
		}

		public void finish()
		{
			propertyMap.recomputeLabelToObjects( labeled );
		}
	}

	private static LabelSets< Spot, Integer > getSpotTagProperty( final Model model )
	{
		return new DefaultTagSetModel.SerialisationAccess< Spot, Link >( ( DefaultTagSetModel< Spot, Link > ) model.getTagSetModel() )
		{
			public LabelSets< Spot, Integer > run()
			{
				return getVertexIdLabelSets();
			}
		}.run();
	}

	private static LabelSets< Link, Integer > getLinkTagProperty( final Model model )
	{
		return new DefaultTagSetModel.SerialisationAccess< Spot, Link >( ( DefaultTagSetModel< Spot, Link > ) model.getTagSetModel() )
		{
			public LabelSets< Link, Integer > run()
			{
				return getEdgeIdLabelSets();
			}
		}.run();
	}
}
