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
package de.bxservice.printarchive.utils;

import java.util.List;

import org.compiere.model.MArchive;
import org.compiere.model.MDocType;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;

public class PrintArchiveUtils {
	
	private static final String DOC_TYPE_COLUMNNAME = "C_DocTypeTarget_ID";
	private static final String ISPRINTFROMARCHIVE_COLUMNNAME = "BXS_IsPrintFromArchive";

	private PO po;
	private int AD_Table_ID;
	private int recordID;
	
	public PrintArchiveUtils(int tableID, int recordID) {
		AD_Table_ID = tableID;
		this.recordID = recordID;
		po = MTable.get(tableID) != null ? MTable.get(tableID).getPO(recordID, null) : null;
	}
		
	public boolean isPrintFromArchive() {
		if (po == null || !po.columnExists(DOC_TYPE_COLUMNNAME))
			return false;
		
		MDocType docType = MDocType.get(po.get_ValueAsInt(DOC_TYPE_COLUMNNAME));
		return docType != null && docType.get_ValueAsBoolean(ISPRINTFROMARCHIVE_COLUMNNAME);

	}
	
	public List<MArchive> getRecordArchivedDocuments() {
		String sql = "Record_ID = ? AND AD_Table_ID = ?";
		
		return new Query(Env.getCtx(), MArchive.Table_Name, sql, null)
				.setParameters(recordID, AD_Table_ID)
				.list();
	}
}
