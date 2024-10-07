/*-
 * #%L
 * mastodon-git
 * %%
 * Copyright (C) 2023 - 2024 Matthias Arzt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.mamut.collaboration.utils;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.mastodon.collection.RefRefMap;
import org.mastodon.collection.ref.RefRefHashMap;
import org.mastodon.mamut.io.importer.ModelImporter;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.tag.ObjTagMap;
import org.mastodon.model.tag.TagSetModel;
import org.mastodon.model.tag.TagSetStructure;

/**
 * Utility method that allows to copy: a {@link Model}, a {@link ModelGraph} or a {@link TagSetModel}.
 */
public class CopyModelUtils
{
	public static void copyModelFromTo( Model source, Model target ) {
		try (Importer importer = new Importer( target ))
		{
			ModelGraph sourceGraph = source.getGraph();
			ModelGraph targetGraph = target.getGraph();
			Pair< RefRefMap< Spot, Spot >, RefRefMap< Link, Link > > mapsAB = copyGraphFromTo( sourceGraph, targetGraph );
			copyTagSets( source, target, mapsAB );
		}
	}

	static Pair< RefRefMap< Spot, Spot >, RefRefMap< Link, Link > > copyGraphFromTo( ModelGraph graphA, ModelGraph graphB )
	{
		Spot refA = graphA.vertexRef();
		Spot refB = graphB.vertexRef();
		Spot refB2 = graphB.vertexRef();
		try
		{
			RefRefMap< Spot, Spot > map = new RefRefHashMap<>( graphA.vertices().getRefPool(), graphB.vertices().getRefPool() );
			RefRefMap< Link, Link > linksAtoB = new RefRefHashMap<>( graphA.edges().getRefPool(), graphB.edges().getRefPool() );
			double[] position = new double[ 3 ];
			double[][] cov = new double[ 3 ][ 3 ];
			for ( Spot spotA : graphA.vertices() )
			{
				int timepoint = spotA.getTimepoint();
				spotA.localize( position );
				spotA.getCovariance( cov );
				Spot spotB = graphB.addVertex( refB );
				spotB.init( timepoint, position, cov );
				spotB.setLabel( spotA.getLabel() );
				map.put( spotA, spotB );
			}
			for ( Spot spotA : graphA.vertices() )
			{
				Spot spotB = map.get( spotA, refB );
				for ( Link linkA : spotA.outgoingEdges() )
				{
					// add edges in the order of the outgoing edges, so that
					// the order is preserved.
					Spot targetA = linkA.getTarget( refA );
					Spot targetB = map.get( targetA, refB2 );
					Link linkB = graphB.addEdge( spotB, targetB ).init();
					linksAtoB.put( linkA, linkB );
				}
			}
			return Pair.of( map, linksAtoB );
		}
		finally
		{
			graphA.releaseRef( refA );
			graphB.releaseRef( refB );
			graphB.releaseRef( refB2 );
		}
	}

	private static void copyTagSets( Model model, Model targetModel, Pair< RefRefMap< Spot, Spot >, RefRefMap< Link, Link > > mapsAB )
	{
		copyTagSetStructure( model, targetModel );
		copyTags( model, targetModel, mapsAB );
	}

	private static void copyTagSetStructure( Model model, Model targetModel )
	{
		TagSetStructure tss = model.getTagSetModel().getTagSetStructure();
		targetModel.getTagSetModel().setTagSetStructure( tss );
	}

	private static void copyTags( Model modelA, Model modelB, Pair< RefRefMap< Spot, Spot >, RefRefMap< Link, Link > > mapsAB )
	{
		TagSetModel< Spot, Link > tagSetModelA = modelA.getTagSetModel();
		TagSetModel< Spot, Link > tagSetModelB = modelB.getTagSetModel();
		// throw assertion error if number of tag sets is different.
		List< TagSetStructure.TagSet > tagSetsA = tagSetModelA.getTagSetStructure().getTagSets();
		List< TagSetStructure.TagSet > tagSetsB = tagSetModelB.getTagSetStructure().getTagSets();
		if ( tagSetsA.size() != tagSetsB.size() )
			throw new AssertionError( "Tag set structures are different." );
		for ( int i = 0; i < tagSetsA.size(); i++ )
		{
			TagSetStructure.TagSet tagSetA = tagSetsA.get( i );
			TagSetStructure.TagSet tagSetB = tagSetsB.get( i );
			copyTagsForOneTagSet( modelA, modelB, tagSetA, tagSetB, mapsAB );
		}

	}

	private static void copyTagsForOneTagSet( Model modelA, Model modelB, TagSetStructure.TagSet tagSetA, TagSetStructure.TagSet tagSetB,
			Pair< RefRefMap< Spot, Spot >, RefRefMap< Link, Link > > mapsAB )
	{
		Spot refB = modelB.getGraph().vertexRef();
		Link eRefB = modelB.getGraph().edgeRef();
		try
		{
			List< TagSetStructure.Tag > tagsA = tagSetA.getTags();
			List< TagSetStructure.Tag > tagsB = tagSetB.getTags();
			ObjTagMap< Spot, TagSetStructure.Tag > vertexTagsA = modelA.getTagSetModel().getVertexTags().tags( tagSetA );
			ObjTagMap< Spot, TagSetStructure.Tag > vertexTagsB = modelB.getTagSetModel().getVertexTags().tags( tagSetB );
			copyObjTagMap( mapsAB.getLeft(), refB, vertexTagsA, vertexTagsB, tagsA, tagsB );
			ObjTagMap< Link, TagSetStructure.Tag > edgeTagsA = modelA.getTagSetModel().getEdgeTags().tags( tagSetA );
			ObjTagMap< Link, TagSetStructure.Tag > edgeTagsB = modelB.getTagSetModel().getEdgeTags().tags( tagSetB );
			copyObjTagMap( mapsAB.getRight(), eRefB, edgeTagsA, edgeTagsB, tagsA, tagsB );
		}
		finally
		{
			modelB.getGraph().releaseRef( refB );
			modelB.getGraph().releaseRef( eRefB );
		}
	}

	private static < T > void copyObjTagMap( RefRefMap< T, T > spotsAtoB, T refB, ObjTagMap< T, TagSetStructure.Tag > mapA, ObjTagMap< T, TagSetStructure.Tag > mapB,
			List< TagSetStructure.Tag > tagsA, List< TagSetStructure.Tag > tagsB )
	{
		if ( tagsA.size() != tagsB.size() )
			throw new AssertionError( "Tag set structures are different." );
		for ( int j = 0; j < tagsA.size(); j++ )
		{
			TagSetStructure.Tag tagA = tagsA.get( j );
			TagSetStructure.Tag tagB = tagsB.get( j );
			if ( !tagA.label().equals( tagB.label() ) )
				throw new AssertionError( "Tag set structures are different." );
			for ( T tA : mapA.getTaggedWith( tagA ) )
				mapB.set( spotsAtoB.get( tA, refB ), tagB );
		}
	}

	private static class Importer extends ModelImporter implements AutoCloseable
	{
		public Importer( Model model )
		{
			super( model );
			super.startImport();
		}

		@Override
		public void close()
		{
			super.finishImport();
		}
	}
}
