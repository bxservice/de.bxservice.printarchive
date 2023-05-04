/***********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 * http://www.idempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 *                                                                     *
 * Contributors:                                                       *
 * - Diego Ruiz - BX Service                                           *
 **********************************************************************/
package de.bxservice.printarchive.validator;

import org.adempiere.util.Callback;
import org.adempiere.webui.adwindow.ADWindow;
import org.adempiere.webui.adwindow.ADWindowContent;
import org.adempiere.webui.adwindow.IADTabbox;
import org.adempiere.webui.adwindow.validator.WindowValidator;
import org.adempiere.webui.adwindow.validator.WindowValidatorEvent;
import org.adempiere.webui.adwindow.validator.WindowValidatorEventType;
import org.adempiere.webui.window.Dialog;
import org.osgi.service.component.annotations.Component;

import de.bxservice.printarchive.utils.PrintArchiveUtils;

@Component(
		service = WindowValidator.class, 
		property = {"AD_Window_UU:String=*"}
		)
public class PrintArchiveValidator implements WindowValidator {
	
	private WindowValidatorEvent windowEvent;

	@Override
	public void onWindowEvent(WindowValidatorEvent event, Callback<Boolean> callback) {
		windowEvent = event;
		if (!isValidEvent()) {
			callback.onCallback(Boolean.TRUE);
			return;
		}

		PrintArchiveUtils printArchiveUtils = new PrintArchiveUtils(getAD_Table_ID(), getRecordID());
		if (isBeforePrintEvent() && printArchiveUtils.isPrintFromArchive()) {

			if (printArchiveUtils.hasArchivedDocuments()) {
				Dialog.ask(event.getWindow().getADWindowContent().getWindowNo(), "BXS_ArchivedDocument", callback);
			} else
				callback.onCallback(Boolean.TRUE);
			
		} else if (isAfterPrintEvent() && printArchiveUtils.isArchivePrintout() &&
				!printArchiveUtils.hasArchivedDocuments()) {
			printArchiveUtils.archivePrintout(getTabProcessID());
		} else {
			callback.onCallback(Boolean.TRUE);
		}
	}
	
	private int getTabProcessID() {
		ADWindow window = windowEvent.getWindow();
		if (window != null) {
			ADWindowContent windowContent = window.getADWindowContent();
			if (windowContent != null) {
				IADTabbox tabbox = windowContent.getADTab();
				return tabbox.getSelectedGridTab().getAD_Process_ID();
			}
		}
		
		return 0;
	}
	
	private boolean isValidEvent() {
		return windowEvent != null && windowEvent.getWindow() != null && 
				windowEvent.getWindow().getADWindowContent() != null &&
				windowEvent.getWindow().getADWindowContent().getADTab() != null && 
				windowEvent.getWindow().getADWindowContent().getADTab().getSelectedGridTab() != null && 
				windowEvent.getWindow().getComponent() != null && 
				(WindowValidatorEventType.BEFORE_PRINT.getName().equals(windowEvent.getName()) ||
						WindowValidatorEventType.AFTER_PRINT.getName().equals(windowEvent.getName()));
	}
	
	private boolean isBeforePrintEvent() {
		return WindowValidatorEventType.BEFORE_PRINT.getName().equals(windowEvent.getName());
	}
	
	private boolean isAfterPrintEvent() {
		return WindowValidatorEventType.AFTER_PRINT.getName().equals(windowEvent.getName());
	}
	
	private int getRecordID() {
		return windowEvent.getWindow().getADWindowContent().getADTab().getSelectedGridTab().getRecord_ID();
	}
	
	private int getAD_Table_ID() {
		return windowEvent.getWindow().getADWindowContent().getADTab().getSelectedGridTab().getAD_Table_ID();
	}	
}