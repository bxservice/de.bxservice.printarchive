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
package de.bxservice.printarchive.process;

import java.util.List;

import org.compiere.model.MArchive;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;

@org.adempiere.base.annotation.Process
public class BXSPrintFromArchive extends SvrProcess {

	@Override
	protected void prepare() {
	}

	@Override
	protected String doIt() throws Exception {
		String sql = "Record_ID = ? AND AD_Table_ID = ?";
		List<MArchive> archive = new Query(getCtx(), MArchive.Table_Name, sql, get_TrxName())
				.setParameters(getRecord_ID(), getTable_ID())
				.list();
		System.out.println("Runnning I'm running " + archive);
		
		//Print archive with report = Y, if not exists, first Archive.
		
		
		
		return null;
	}

}
