package org.mastodon.mamut.collaboration.io;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.imglib2.util.StopWatch;

import org.junit.Test;
import org.mastodon.collection.IntRefMap;
import org.mastodon.collection.ObjectRefMap;
import org.mastodon.collection.ref.IntRefHashMap;
import org.mastodon.collection.ref.ObjectRefHashMap;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;

public class UUIDPreformanceExperiment
{

	private static class UUID
	{
		private long mostSigBits;

		private long leastSigBits;

		private int hashCode;

		public UUID( final long mostSigBits, final long leastSigBits )
		{
			set( mostSigBits, leastSigBits );
		}

		public UUID()
		{
			set( 0, 0 );
		}

		public void set( final long mostSigBits, final long leastSigBits )
		{
			this.mostSigBits = mostSigBits;
			this.leastSigBits = leastSigBits;
			this.hashCode = ( int ) ( leastSigBits >>> 32 );
		}

		private static final Random random = new Random( 2 );

		public static UUID randomUUID()
		{
			return new UUID( random.nextLong(), random.nextLong() );
		}

		public long getMostSignificantBits()
		{
			return mostSigBits;
		}

		public long getLeastSignificantBits()
		{
			return leastSigBits;
		}

		@Override
		public int hashCode()
		{
			return hashCode;
		}

		@Override
		public boolean equals( final Object obj )
		{
			if ( obj == this )
				return true;
			if ( obj == null || obj.getClass() != this.getClass() )
				return false;
			final UUID that = ( UUID ) obj;
			return this.mostSigBits == that.mostSigBits && this.leastSigBits == that.leastSigBits;
		}
	}

	@Test
	public void testUUIDs() throws IOException
	{
		// have a list of 1000 UUIDs
		// Map between UUIDs and Spots
		// Read a table with the same 1000 UUIDs and values
		// Create a map spot -> value.

		// initialize n spots with random positions and radii, add them to a model graph
		final ModelGraph graph = initializeRandomGraph();
		final ObjectRefMap< UUID, Spot > uuidToSpot = initializeRandomUUIDs( graph );
		writeRandomUUIDs( uuidToSpot, graph.vertices().size() * 4 );

		final Spot ref1 = graph.vertexRef();
		final Spot ref2 = graph.vertexRef();
		final UUID sourceUUID = new UUID();
		final UUID targetUUID = new UUID();
		// read UUIDs and add links between corresponding spots
		final StopWatch watch = StopWatch.createAndStart();
		try (final DataInputStream in = new DataInputStream( new BufferedInputStream( new FileInputStream( "uuids.raw" ) ) ))
		{
			while ( in.available() > 0 )
			{
				sourceUUID.set( in.readLong(), in.readLong() );
				targetUUID.set( in.readLong(), in.readLong() );
				final Spot source = uuidToSpot.get( sourceUUID, ref1 );
				final Spot target = uuidToSpot.get( targetUUID, ref2 );
				graph.addEdge( source, target );
			}
		}
		System.out.println( watch );
	}

	@Test
	public void testIDs() throws IOException
	{
		// have a list of 1000 UUIDs
		// Map between UUIDs and Spots
		// Read a table with the same 1000 UUIDs and values
		// Create a map spot -> value.

		// initialize n spots with random positions and radii, add them to a model graph
		final ModelGraph graph = initializeRandomGraph();
		final IntRefMap< Spot > idToSpot = initializeRandomIds( graph );
		writeRandomIds( idToSpot.keySet(), graph.vertices().size() * 4 );

		final Spot ref1 = graph.vertexRef();
		final Spot ref2 = graph.vertexRef();
		// read UUIDs and add links between corresponding spots
		final StopWatch watch = StopWatch.createAndStart();
		try (final DataInputStream in = new DataInputStream( new BufferedInputStream( new FileInputStream( "ids.raw" ) ) ))
		{
			while ( in.available() > 0 )
			{
				final int sourceId = in.readInt();
				final int targetId = in.readInt();
				final Spot source = idToSpot.get( sourceId, ref1 );
				final Spot target = idToSpot.get( targetId, ref2 );
				graph.addEdge( source, target );
			}
		}
		System.out.println( watch );
	}

	private void writeRandomIds( final TIntSet ids, final int n ) throws IOException
	{
		final Random random = new Random( 42 );
		final TIntList list = new TIntArrayList( ids );
		// write pairs of randomly selected UUIDs to a file "links.raw"
		try (final DataOutputStream out = new DataOutputStream( new FileOutputStream( "ids.raw" ) ))
		{
			for ( int i = 0; i < n; i++ )
			{
				final int id = list.get( random.nextInt( list.size() ) );
				out.writeInt( id );
			}
		}
	}

	private static IntRefMap< Spot > initializeRandomIds( final ModelGraph graph )
	{
		final IntRefMap< Spot > idToSpot = new IntRefHashMap<>( graph.vertices().getRefPool(), -1 );
		final Random random = new Random( 1 );
		final int bound = Integer.MAX_VALUE;
		for ( final Spot spot : graph.vertices() )
		{
			int id = random.nextInt( bound );
			while ( idToSpot.containsKey( id ) )
				id = random.nextInt( bound );
			idToSpot.put( id, spot );
		}
		return idToSpot;
	}

	private static void writeRandomUUIDs( final ObjectRefMap< UUID, Spot > uuidToSpot, final int n ) throws IOException
	{
		final Random random = new Random( 42 );
		final List< UUID > uuids = new ArrayList<>( uuidToSpot.keySet() );
		// write pairs of randomly selected UUIDs to a file "links.raw"
		try (final DataOutputStream out = new DataOutputStream( new FileOutputStream( "uuids.raw" ) ))
		{
			for ( int i = 0; i < n; i++ )
			{
				final UUID uuid = uuids.get( random.nextInt( uuids.size() ) );
				out.writeLong( uuid.getMostSignificantBits() );
				out.writeLong( uuid.getLeastSignificantBits() );
			}
		}
	}

	private static ObjectRefMap< UUID, Spot > initializeRandomUUIDs( final ModelGraph graph )
	{
		final ObjectRefMap< UUID, Spot > uuidToSpot = new ObjectRefHashMap<>( graph.vertices().getRefPool() );
		for ( final Spot spot : graph.vertices() )
			uuidToSpot.put( UUID.randomUUID(), graph.vertexRef() );
		return uuidToSpot;
	}

	private static ModelGraph initializeRandomGraph()
	{
		final Random random = new Random( 42 );
		final long n = 10_000;
		final ModelGraph graph = new ModelGraph();
		final double[] position = new double[ 3 ];
		for ( int i = 0; i < n; i++ )
		{
			final int t = random.nextInt( 100 );
			position[ 0 ] = random.nextDouble();
			position[ 1 ] = random.nextDouble();
			position[ 2 ] = random.nextDouble();
			final double radius = random.nextDouble();
			graph.addVertex().init( t, position, radius );
		}
		return graph;
	}

}
