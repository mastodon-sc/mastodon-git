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
