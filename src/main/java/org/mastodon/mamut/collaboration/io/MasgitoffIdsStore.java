package org.mastodon.mamut.collaboration.io;

import java.util.WeakHashMap;

import org.mastodon.mamut.model.Model;

public class MasgitoffIdsStore
{
	private static final WeakHashMap< Model, MasgitoffIds > store = new WeakHashMap<>();

	private MasgitoffIdsStore() {}

	public static MasgitoffIds get( final Model model )
	{
		MasgitoffIds ids = store.get( model );
		if ( null == ids )
		{
			ids = new MasgitoffIds( model.getGraph() );
			store.put( model, ids );
		}
		return ids;
	}

	public static void put( final Model model, final MasgitoffIds ids )
	{
		store.put( model, ids );
	}
}
