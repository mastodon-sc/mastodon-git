package org.mastodon.mamut.collaboration.io.benchmark;

import java.io.IOException;

import org.apache.commons.lang3.tuple.Pair;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.Model;

public interface Saver< T >
{
	Pair< Model, T > open() throws IOException;

	void save( Model model, T details ) throws IOException;

	Pair< Model, T > createEmpty() throws IOException;
}
