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
package org.mastodon.mamut.collaboration;

import org.mastodon.collection.RefCollection;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.collection.RefSet;
import org.mastodon.collection.ref.RefArrayList;
import org.mastodon.collection.ref.RefSetImp;
import org.mastodon.mamut.KeyConfigScopes;
import org.mastodon.mamut.collaboration.utils.ActionDescriptions;
import org.mastodon.mamut.collaboration.utils.BasicDescriptionProvider;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.mamut.collaboration.utils.BasicMamutPlugin;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;

@Plugin( type = MamutPlugin.class )
public class FixGraphInconsistenciesPlugin extends BasicMamutPlugin
{

	private static final ActionDescriptions< FixGraphInconsistenciesPlugin > actionDescriptions = new ActionDescriptions<>( FixGraphInconsistenciesPlugin.class );

	static
	{
		actionDescriptions.addActionDescription(
				"[mastodon-tomancak] fix graph inconsistencies",
				"Plugins > Trees Management > Fix Graph Inconsistencies",
				"Flip reversed edges",
				FixGraphInconsistenciesPlugin::fixGraphInconsistencies );
	}

	public < T > FixGraphInconsistenciesPlugin()
	{
		super( actionDescriptions );
	}

	private void fixGraphInconsistencies()
	{
		Model model = getProjectModel().getModel();
		ModelGraph graph = model.getGraph();
		flipBackwardsEdges( graph );
		removeDoubleEdges( graph );
		removeSameTimepointEdge( graph );
		printWarnings( graph );
	}

	private static void printWarnings( ModelGraph graph )
	{
		for ( Spot spot : graph.vertices() )
		{
			if ( spot.incomingEdges().size() > 1 )
				System.out.println( "More than one parent: " + spot.getLabel() );

			if ( spot.outgoingEdges().size() > 2 )
				System.out.println( "More than two children: " + spot.getLabel() );
		}
	}

	private static void removeSameTimepointEdge( ModelGraph graph )
	{
		RefCollection< Link > backwardEdge = RefCollections.createRefList( graph.edges() );
		Spot sourceRef = graph.vertexRef();
		Spot targetRef = graph.vertexRef();
		for ( Link edge : graph.edges() )
		{
			Spot source = edge.getSource( sourceRef );
			Spot target = edge.getTarget( targetRef );
			if ( source.getTimepoint() == target.getTimepoint() )
				backwardEdge.add( edge );
		}
		for ( Link edge : backwardEdge )
		{
			Spot source = edge.getSource( sourceRef );
			Spot target = edge.getTarget( targetRef );
			System.out.println( "Remove same timepoint edge " + source.getLabel() + " -> " + target.getLabel() );
			graph.remove( edge );
		}
	}

	private static void removeDoubleEdges( ModelGraph graph )
	{
		Spot ref1 = graph.vertexRef();
		Spot ref2 = graph.vertexRef();
		RefSet< Spot > sources = new RefSetImp<>( graph.vertices().getRefPool() );
		RefList< Link > doubleEdge = new RefArrayList<>( graph.edges().getRefPool() );
		for ( Spot spot : graph.vertices() )
		{
			if ( spot.incomingEdges().size() > 1 )
			{
				for ( Link link : spot.incomingEdges() )
				{
					Spot source = link.getSource( ref1 );
					if ( sources.contains( source ) )
						doubleEdge.add( link );
					else
						sources.add( source );
				}
				sources.clear();
			}
		}
		for ( Link link : doubleEdge )
		{
			Spot source = link.getSource( ref1 );
			Spot target = link.getTarget( ref2 );
			System.out.println( "Remove duplicated edge: " + source.getLabel() + " -> " + target.getLabel() );
			graph.remove( link );
		}
	}

	private static void flipBackwardsEdges( ModelGraph graph )
	{
		RefCollection< Link > backwardEdge = RefCollections.createRefList( graph.edges() );
		Spot sourceRef = graph.vertexRef();
		Spot targetRef = graph.vertexRef();
		for ( Link edge : graph.edges() )
		{
			Spot source = edge.getSource( sourceRef );
			Spot target = edge.getTarget( targetRef );
			if ( source.getTimepoint() > target.getTimepoint() )
				backwardEdge.add( edge );
		}
		for ( Link edge : backwardEdge )
		{
			Spot source = edge.getSource( sourceRef );
			Spot target = edge.getTarget( targetRef );
			System.out.println( "Flip backwards edge " + source.getLabel() + " -> " + target.getLabel() );
			graph.remove( edge );
			graph.addEdge( source, target ).init();
		}
	}

	@Plugin( type = CommandDescriptionProvider.class )
	public static class DescriptionProvider extends BasicDescriptionProvider
	{
		public DescriptionProvider()
		{
			super( actionDescriptions, KeyConfigScopes.MAMUT, KeyConfigContexts.MASTODON, KeyConfigContexts.TRACKSCHEME );
		}
	}
}
