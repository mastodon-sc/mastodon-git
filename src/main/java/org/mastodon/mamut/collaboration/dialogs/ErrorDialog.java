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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ItemEvent;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * A dialog that shows an exception message and stack trace.
 * The stack trace is hidden by default and can be shown by
 * clicking on "details".
 */
public class ErrorDialog
{

	public static void showErrorMessage( String title, Exception exception )
	{
		SwingUtilities.invokeLater( () -> showDialog( null, title + " (Error)", exception ) );
	}

	private static void showDialog( Frame parent, String title, Exception exception )
	{
		String message = "\nThere was a problem:\n\n  " + exception.getMessage() + "\n\n";
		final JScrollPane scrollPane = initScrollPane( exception );
		final JCheckBox checkBox = new JCheckBox( "show details" );
		checkBox.setForeground( Color.BLUE );
		Object[] objects = { message, checkBox, scrollPane };
		JOptionPane pane = new JOptionPane( objects, JOptionPane.ERROR_MESSAGE );
		JDialog dialog = pane.createDialog( parent, title );
		dialog.setResizable( true );
		checkBox.addItemListener( event -> {
			boolean visible = event.getStateChange() == ItemEvent.SELECTED;
			scrollPane.setVisible( visible );
			scrollPane.setPreferredSize( visible ? null : new Dimension( 0, 0 ) );
			dialog.pack();
		} );
		dialog.setModal( true );
		dialog.pack();
		dialog.setVisible( true );
		dialog.dispose();
	}

	private static JScrollPane initScrollPane( Exception exception )
	{
		String stackTrace = ExceptionUtils.getStackTrace( exception );
		int lines = Math.min( 20, countLines( stackTrace ) );
		JTextArea textArea = new JTextArea( stackTrace, lines, 70 );
		textArea.setForeground( new Color( 0x880000 ) );
		textArea.setEditable( false );
		textArea.setFont( new Font( Font.MONOSPACED, Font.PLAIN, textArea.getFont().getSize() ) );
		final JScrollPane scrollPane = new JScrollPane( textArea );
		scrollPane.setVisible( false );
		return scrollPane;
	}

	private static int countLines( String str )
	{
		String[] lines = str.split( "\r\n|\r|\n" );
		return lines.length;
	}
}
