package org.mastodon.mamut.collaboration.io;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.imglib2.util.Cast;

import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Spot;
import org.mastodon.pool.PoolObjectAttributeSerializer;

/**
 * This class is a wrapper around the package-private "ModelSerializer" class
 * in the Mastodon core. It has basically the same functionality, and API but
 * is public.
 * <br>
 * This class uses reflection to access the methods of the underlying
 * package-private class. It is a workaround to not require a modified Mastodon
 * core package. This means that mastodon git can run with the vanilla
 * Mastodon core and update sites.
 * A better solution would be to make the ModelSerializer class public in the
 * Mastodon core.
 */
public class ModelSerializer
{

	private final PoolObjectAttributeSerializer< Spot > vertexSerializer;

	private final PoolObjectAttributeSerializer< Link > edgeSerializer;

	private ModelSerializer()
	{
		final Class< ? > modelSerializerClass;
		try
		{
			modelSerializerClass = Class.forName( "org.mastodon.mamut.model.ModelSerializer" );
			final Method getInstanceMethod = modelSerializerClass.getDeclaredMethod( "getInstance" );
			final Method getVertexSerializerMethod = modelSerializerClass.getDeclaredMethod( "getVertexSerializer" );
			final Method getEdgeSerializerMethod = modelSerializerClass.getDeclaredMethod( "getEdgeSerializer" );
			getInstanceMethod.setAccessible( true );
			getVertexSerializerMethod.setAccessible( true );
			getEdgeSerializerMethod.setAccessible( true );
			final Object modelSerializer = getInstanceMethod.invoke( null );
			vertexSerializer = Cast.unchecked( getVertexSerializerMethod.invoke( modelSerializer ) );
			edgeSerializer = Cast.unchecked( getEdgeSerializerMethod.invoke( modelSerializer ) );
		}
		catch ( ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e )
		{
			throw new RuntimeException( e );
		}
	}

	public static ModelSerializer getInstance()
	{
		return new ModelSerializer();
	}

	public PoolObjectAttributeSerializer< Spot > getVertexSerializer()
	{
		return vertexSerializer;
	}

	public PoolObjectAttributeSerializer< Link > getEdgeSerializer()
	{
		return edgeSerializer;
	}
}
