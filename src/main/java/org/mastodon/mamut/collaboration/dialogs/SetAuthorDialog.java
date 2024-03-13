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

	public static Pair< String, String > show()
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
		panel.add( authorField, "grow, wrap" );
		panel.add( new JLabel( "Author Email" ), "align right" );
		JTextField emailField = new JTextField( "noreply@example.com" );
		panel.add( emailField, "grow, wrap" );
		int result = JOptionPane.showOptionDialog( null, panel, "Set Author Name", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[] { "Set Name", "Cancel" }, null );
		if ( result == 0 )
			return Pair.of( authorField.getText(), emailField.getText() );
		return null;
	}

}
