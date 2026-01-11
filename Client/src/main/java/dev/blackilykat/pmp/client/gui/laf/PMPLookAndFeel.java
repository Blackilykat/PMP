/*
 * Copyright (C) 2026 Blackilykat and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.blackilykat.pmp.client.gui.laf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.UIDefaults;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class PMPLookAndFeel extends MetalLookAndFeel {
	private static final Logger LOGGER = LogManager.getLogger(PMPLookAndFeel.class);

	@Override
	public String getName() {
		return "PMP LaF";
	}

	@Override
	public String getID() {
		return "pmp";
	}

	@Override
	public String getDescription() {
		return "Look and feel created specifically for PMP";
	}

	protected void initClassDefaults(UIDefaults table) {
		super.initClassDefaults(table);
		String pmpPackageName = "dev.blackilykat.pmp.client.gui.laf.";

		// @formatter:off
		Object[] uiDefaults = {
				"LabelUI", pmpPackageName + "PMPLabelUI",
				/*
				"ButtonUI", pmpPackageName + "PMPButtonUI",
				"CheckBoxUI", pmpPackageName + "PMPCheckBoxUI",
				"DirectoryPaneUI", pmpPackageName + "PMPDirectoryPaneUI",
				"FileChooserUI", pmpPackageName + "PMPFileChooserUI",
				"MenuBarUI", pmpPackageName + "PMPMenuBarUI",
				"MenuUI", pmpPackageName + "PMPMenuUI",
				"MenuItemUI", pmpPackageName + "PMPMenuItemUI",
				"CheckBoxMenuItemUI", pmpPackageName + "PMPCheckBoxMenuItemUI",
				"RadioButtonMenuItemUI", pmpPackageName + "PMPRadioButtonMenuItemUI",
				"RadioButtonUI", pmpPackageName + "PMPRadioButtonUI",
				"ToggleButtonUI", pmpPackageName + "PMPToggleButtonUI",
				"PopupMenuUI", pmpPackageName + "PMPPopupMenuUI",
				"ProgressBarUI", pmpPackageName + "PMPProgressBarUI",
				"ScrollBarUI", pmpPackageName + "PMPScrollBarUI",
				"ScrollPaneUI", pmpPackageName + "PMPScrollPaneUI",
				"SliderUI", pmpPackageName + "PMPSliderUI",
				"SplitPaneUI", pmpPackageName + "PMPSplitPaneUI",
				"TabbedPaneUI", pmpPackageName + "PMPTabbedPaneUI",
				"TextAreaUI", pmpPackageName + "PMPTextAreaUI",
				"TextFieldUI", pmpPackageName + "PMPTextFieldUI",
				"PasswordFieldUI", pmpPackageName + "PMPPasswordFieldUI",
				"TextPaneUI", pmpPackageName + "PMPTextPaneUI",
				"EditorPaneUI", pmpPackageName + "PMPEditorPaneUI",
				"TreeUI", pmpPackageName + "PMPTreeUI",
				"InternalFrameUI", pmpPackageName + "PMPInternalFrameUI",
				"DesktopPaneUI", pmpPackageName + "PMPDesktopPaneUI",
				"SeparatorUI", pmpPackageName + "PMPSeparatorUI",
				"PopupMenuSeparatorUI", pmpPackageName + "PMPPopupMenuSeparatorUI",
				"OptionPaneUI", pmpPackageName + "PMPOptionPaneUI",
				"ComboBoxUI", pmpPackageName + "PMPComboBoxUI",
				"DesktopIconUI", pmpPackageName + "PMPDesktopIconUI"
				 */
		};
		// @formatter:on

		table.putDefaults(uiDefaults);
	}
}
