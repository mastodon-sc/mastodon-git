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
package org.mastodon.mamut.collaboration.dialogs;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.tuple.Pair;

public class SetAuthorDialog
{
	private SetAuthorDialog()
	{
		// prevent from instantiation
	}

	/**
	 * Show a dialog that allows the user to set the author name and email.
	 * The method blocks until the dialog is closed.
	 *
	 * @param initialAuthorName the initial author name to display in the dialog.
	 *                          Can be null.
	 * @param initialEmail      the initial email address to display in the dialog.
	 *                          Can be null.
	 * @return a pair of two strings: the author name and the author email address.
	 * Returns null if the dialog was canceled.
	 */
	public static Pair< String, String > show( String initialAuthorName, String initialEmail )
	{
		final String description = "<html><body align=left>"
				+ "The name and email that you specify below<br>"
				+ "are used to identify you as the author of the<br>"
				+ "changes you make to the shared project.<br><br>"
				+ "Name and email are likely to become publicly visible on the internet.<br>"
				+ "You may use a nickname and dummy email address if you wish.<br><br>";
		JPanel panel = new JPanel();
		panel.setLayout( new MigLayout( "insets dialog" ) );
		panel.add( new JLabel( description ), "span, grow, wrap" );
		panel.add( new JLabel( "Author Name" ), "align right" );
		JTextField authorField = new JTextField();
		if ( initialAuthorName != null )
			authorField.setText( initialAuthorName);
		panel.add( authorField, "grow, wrap" );
		panel.add( new JLabel( "Author Email" ), "align right" );
		JTextField emailField = new JTextField( "noreply@example.com" );
		if ( initialEmail != null )
			emailField.setText(initialEmail);
		panel.add( emailField, "grow, wrap" );
		int result = JOptionPane.showOptionDialog( null, panel, "Set Author Name", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[] { "Set Name", "Cancel" }, null );
		if ( result != JOptionPane.OK_OPTION )
			return null;
		return Pair.of( authorField.getText(), emailField.getText() );
	}

}
