package org.mastodon.mamut.collaboration.exceptions;

public class GraphMergeConflictException extends GraphMergeException
{
	public GraphMergeConflictException()
	{
		super( "There are conflicting changes in the two versions of the ModelGraph." );
	}
}
