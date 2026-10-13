package com.chattynpcs;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.PluginPanel;

/**
 * Debug tools, in the sidebar while debug mode is on: the settings panel has no buttons
 */
class ChattyNpcsPanel extends PluginPanel
{
	ChattyNpcsPanel(Runnable makeSomeoneTalk, Runnable describeCharacter)
	{
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 8));
		buttons.add(new JLabel("Chatty NPCs debug"));
		buttons.add(button("Make someone talk", "The nearest chatty NPC comments on you now, ignoring cooldowns", makeSomeoneTalk));
		buttons.add(button("Describe my character", "List what the locals can see about you in the chatbox", describeCharacter));
		add(buttons, BorderLayout.NORTH);
	}

	private static JButton button(String name, String tooltip, Runnable action)
	{
		JButton button = new JButton(name);
		button.setToolTipText(tooltip);
		button.addActionListener(e -> action.run());
		return button;
	}
}
