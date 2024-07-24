package org.mastodon.mamut.collaboration.io;

import org.mastodon.Ref;
import org.mastodon.RefPool;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

public class Index< T extends Ref< T > >
{
	private final TIntList idToPoolId = new TIntArrayList();

	private final TIntList poolIdToId = new TIntArrayList();

	private final RefPool< T > pool;

	public Index( final RefPool< T > pool )
	{
		this.pool = pool;
	}

	public void put( final T obj, final int id )
	{
		if ( id < 0 )
			throw new IllegalArgumentException( "id must be >= 0" );
		if ( containsId( id ) )
			throw new IllegalArgumentException( "id already in use." );
		final int poolId = obj.getInternalPoolIndex();
		if ( poolId < 0 )
			throw new IllegalArgumentException( "Object is not valid." );
		if ( containsObject( obj ) )
			throw new IllegalArgumentException( "There is already an id for the given object." );
		setAndGrow( idToPoolId, id, poolId );
		setAndGrow( poolIdToId, poolId, id );
	}

	private static void setAndGrow( final TIntList list, final int index, final int value )
	{
		if ( index >= list.size() )
			list.fill( list.size(), index + 1, -1 );
		list.set( index, value );
	}

	public int getId( final T object )
	{
		final int poolId = object.getInternalPoolIndex();
		if ( poolId < 0 || poolId >= poolIdToId.size() )
			return -1;
		return poolIdToId.get( poolId );
	}

	public int getOrCreateId( final T object )
	{
		final int id = getId( object );
		if ( id >= 0 )
			return id;
		final int newId = idToPoolId.size();
		final int poolId = object.getInternalPoolIndex();
		idToPoolId.add( poolId );
		setAndGrow( poolIdToId, poolId, newId );
		return newId;
	}

	public T getObject( final int id, final T ref )
	{
		if ( id < 0 || id >= idToPoolId.size() )
			return null;
		final int poolId = idToPoolId.get( id );
		if ( poolId < 0 )
			return null;
		return pool.getObject( poolId, ref );
	}

	public int getMaxId()
	{
		return idToPoolId.size() - 1;
	}

	public boolean containsId( final int id )
	{
		return id >= 0 && id < idToPoolId.size() && idToPoolId.get( id ) >= 0;
	}

	public boolean containsObject( final T object )
	{
		final int poolId = object.getInternalPoolIndex();
		return poolId >= 0 && poolId < poolIdToId.size() && poolIdToId.get( poolId ) >= 0;
	}

	public T createRef() {
		return pool.createRef();
	}
}
