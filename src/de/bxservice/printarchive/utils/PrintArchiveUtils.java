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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MArchive;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.PrintInfo;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ServerProcessCtl;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

public class PrintArchiveUtils {
	
	protected static final CLogger log = CLogger.getCLogger(PrintArchiveUtils.class);

	private static final String DOC_STATUS_COLUMNNAME = "DocStatus";
	private static final String DOC_TYPE_COLUMNNAME = "C_DocTypeTarget_ID";
	private static final String ISPRINTFROMARCHIVE_COLUMNNAME = "BXS_IsPrintFromArchive";
	private static final String ISAUTOARCHIVE_COLUMNNAME = "BXS_IsAutoArchiveFirst";
	private static final String NOTIFICATIONTYPE_COLUMNNAME = "BXS_NotificationTypeArchive";
	
	private static final String NOTIFICATIONTYPE_WARNING = "W";
	private static final String NOTIFICATIONTYPE_FORBID = "F";

	private PO po;
	private int AD_Table_ID;
	private int recordID;
	
	public PrintArchiveUtils(int tableID, int recordID) {
		AD_Table_ID = tableID;
		this.recordID = recordID;
		po = MTable.get(tableID) != null ? MTable.get(tableID).getPO(recordID, null) : null;
	}
		
	public boolean isPrintFromArchive() {
		MDocType docType = getDocType();
		return docType != null && docType.get_ValueAsBoolean(ISPRINTFROMARCHIVE_COLUMNNAME);
	}
	
	private MDocType getDocType() {
		if (po == null || !po.columnExists(DOC_TYPE_COLUMNNAME))
			return null;

		return MDocType.get(po.get_ValueAsInt(DOC_TYPE_COLUMNNAME));
	}
	
	public boolean showWarning() {
		return NOTIFICATIONTYPE_WARNING.equals(getNotificationType());
	}
	
	public boolean showError() {
		return NOTIFICATIONTYPE_FORBID.equals(getNotificationType());
	}
	
	private String getNotificationType() {
		MDocType docType = getDocType();
		return docType != null ? docType.get_ValueAsString(NOTIFICATIONTYPE_COLUMNNAME) : "";
	}
	
	public boolean hasArchivedDocuments() {
		return !getRecordArchivedDocuments().isEmpty();
	}
	
	private List<MArchive> getRecordArchivedDocuments() {
		String sql = "Record_ID = ? AND AD_Table_ID = ?";
		
		return new Query(Env.getCtx(), MArchive.Table_Name, sql, null)
				.setParameters(recordID, AD_Table_ID)
				.list();
	}
	
	public boolean isArchivePrintout() {
		MDocType docType = getDocType();
		
		return docType != null && docType.get_ValueAsBoolean(ISAUTOARCHIVE_COLUMNNAME)
				&& isDocumentComplete();
	}
	
	private boolean isDocumentComplete() {
		if (po == null || !po.columnExists(DOC_STATUS_COLUMNNAME))
			return false;
		
		return "CO".equals(po.get_Value(DOC_STATUS_COLUMNNAME));
	}
	
	public void archivePrintout(int AD_Process_ID) {
		if (AD_Process_ID ==0)
			return;
		
		MProcess reportProcess = MProcess.get(AD_Process_ID); 
		ProcessInfo pi = getProcessInfo(reportProcess);
		MPInstance instance = new MPInstance(po.getCtx(),AD_Process_ID, 0);
		instance.saveEx();
		ServerProcessCtl.process(pi, null);

		if (pi.isError()) 
			throw new AdempiereUserError(pi.getSummary());

		archivePDF(pi.getPDFReport());
	}
	
	private ProcessInfo getProcessInfo(MProcess process) {
		ProcessInfo pi = new ProcessInfo(process.getName(), process.getAD_Process_ID());
		pi.setAD_Client_ID(po.getAD_Client_ID());
		pi.setRecord_ID(po.get_ID());
		pi.setTable_ID(po.get_Table_ID());
		pi.setPrintPreview(true);
		pi.setIsBatch(true);
		pi.setReportType("PDF");
		pi.setTransactionName(Trx.createTrxName());

		setMProcessParameters(pi, process.getAD_Process_ID());
		return pi;
	}
	
	private void setMProcessParameters(ProcessInfo pi, int AD_Process_ID) {
		String language = getBPLanguage();
		if (!Util.isEmpty(language)) {
			MPInstance instance = new MPInstance(po.getCtx(), AD_Process_ID, 0);
			instance.saveEx();
			instance.createParameter(10, "AD_Language", language);
			pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());
		}
	}
	
	private String getBPLanguage() {
		MBPartner bPartner = MBPartner.get(po.getCtx(), getBPartnerID());
		return bPartner !=null ? bPartner.getAD_Language() : Env.getAD_Language(Env.getCtx());
	}
	
	protected int getBPartnerID() {
		return po.get_ValueAsInt("C_BPartner_ID");
	}
	
	private void archivePDF(File pdfFile) {
		PrintInfo printInfo = new PrintInfo(pdfFile.getName(), po.get_Table_ID(), po.get_ID(), getBPartnerID());
		byte[] data = getFileByteData(pdfFile);
		MArchive archive = new MArchive(Env.getCtx(), printInfo, null);
		archive.setBinaryData(data);
		archive.save();
		log.log(Level.WARNING, "File archived automatically for " + po.get_TableName() + ": " + po.get_ID());
	}
	
	/** 
	 * convert File data into Byte Data
	 * @param tempFile
	 * @return file in ByteData 
	 */
	private byte[] getFileByteData(File tempFile) {
		try {
			return Files.readAllBytes(tempFile.toPath());
		} catch (IOException ioe) {
			log.log(Level.SEVERE, "Exception while reading file " + ioe);
			return null;
		}
	} 
}
