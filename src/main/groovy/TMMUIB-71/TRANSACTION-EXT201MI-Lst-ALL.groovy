/*
 ***************************************************************
 *                                                             *
 *                           NOTICE                            *
 *                                                             *
 *   THIS SOFTWARE IS THE PROPERTY OF AND CONTAINS             *
 *   CONFIDENTIAL INFORMATION OF INFOR AND/OR ITS AFFILIATES   *
 *   OR SUBSIDIARIES AND SHALL NOT BE DISCLOSED WITHOUT PRIOR  *
 *   WRITTEN PERMISSION. LICENSED CUSTOMERS MAY COPY AND       *
 *   ADAPT THIS SOFTWARE FOR THEIR OWN USE IN ACCORDANCE WITH  *
 *   THE TERMS OF THEIR SOFTWARE LICENSE AGREEMENT.            *
 *   ALL OTHER RIGHTS RESERVED.                                *
 *                                                             *
 *   (c) COPYRIGHT 2020 INFOR.  ALL RIGHTS RESERVED.           *
 *   THE WORD AND DESIGN MARKS SET FORTH HEREIN ARE            *
 *   TRADEMARKS AND/OR REGISTERED TRADEMARKS OF INFOR          *
 *   AND/OR ITS AFFILIATES AND SUBSIDIARIES. ALL RIGHTS        *
 *   RESERVED.  ALL OTHER TRADEMARKS LISTED HEREIN ARE         *
 *   THE PROPERTY OF THEIR RESPECTIVE OWNERS.                  *
 *                                                             *
 ***************************************************************
 */

 import groovy.lang.Closure;


/*
 *Modification area - M3
 *Name        EXT201MI.Lst
 *Type        Transaction
 *Description Workflow action Audit Trail - List
 *
 *Nbr         Date      User        Description
 *TMMUIB-71   20250903  Wyllie Lam  Initial     
 *
*/

 /**
  * Get Audit Trail extension table row
 */
 public class Lst extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;
  private final IonAPI ion;
  
  //Input fields
  private String cono;
  private String ttyp;
  private String puno;
  private String plpn;
  private String plps;
  private String plp2;
  private String acta = "";
  private String resp = "";
  
  private int xxCONO;
  
  private String ivqa;

 /*
  * Get Audit Trail extension table row
 */
  public Lst(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
    this.mi = mi;
    this.database = database;
  	this.miCaller = miCaller;
  	this.logger = logger;
  	this.program = program;
	  this.ion = ion;
    
  }
  
  public void main() {
    
    cono = mi.inData.get("CONO") == null ? '' : mi.inData.get("CONO").trim();
  	ttyp = mi.inData.get("TTYP") == null ? '' : mi.inData.get("TTYP").trim();
    if (ttyp.isEmpty()) {
      mi.error("Transaction Type name must be entered");
      return;
    }
  	puno = mi.inData.get("PUNO") == null ? '' : mi.inData.get("PUNO").trim();
  	if (puno == "?") {
  	  puno = " ";
  	} 
  	plpn = mi.inData.get("PLPN") == null ? '' : mi.inData.get("PLPN").trim();
  	if (plpn == "?") {
  	  plpn = " ";
  	} 
  	plps = mi.inData.get("PLPS") == null ? '' : mi.inData.get("PLPS").trim();
  	if (plps == "?") {
  	  plps = " ";
  	} 
  	plp2 = mi.inData.get("PLP2") == null ? '' : mi.inData.get("PLP2").trim();
  	if (plp2 == "?") {
  	  plp2 = " ";
  	} 

   	if (plpn.isEmpty()) { plpn = "0";  }
   	if (plps.isEmpty()) { plps = "0";  }
  	if (plp2.isEmpty()) { plp2 = "0";  } 	
  	
		if (cono.isEmpty()) {
  	  xxCONO = (Integer)program.LDAZD.CONO;
  	} else {
  	  xxCONO = cono.toInteger();
  	}

    // Validate input fields  	
    
    // Validate input fields for Transaction type
		if (ttyp.isEmpty()) {
      mi.error("Transaction type must be entered");
      return;
    }
    if (!ttyp.equals("PRL") && !ttyp.equals("PO")) {
      mi.error("Invalid transaction type, either PO or PRL");
      return;
    }    

    // validate input fields for Purchase Requisition
    if (ttyp.equals("PRL")) {
		   if (plpn.isEmpty()) {
          mi.error("PR no must be entered");
          return;
       }
		   if (plps.isEmpty()) {
          mi.error("PR sub line no must be entered");
          return;
       }
		   if (plp2.isEmpty()) {
          mi.error("PR sub line no 2 must be entered");
          return;
       }
    }
    
    // validate input fields for Purchase Order
    if (ttyp.equals("PO")) {
		   if (puno.isEmpty()) {
          mi.error("PO no must be entered");
          return;
       }
    }


    DBAction queryEXT201 = database.table("EXT201").index("00").selection("EXCONO", "EXTTYP", "EXPUNO", "EXPLPN", "EXPLPS", "EXPLP2", "EXACTA", "EXRESP", "EXRGDT", "EXRGTM", "EXTMST").build();
    DBContainer dbEXT201 = queryEXT201.getContainer();
    dbEXT201.set("EXCONO", xxCONO);
    dbEXT201.set("EXTTYP", ttyp);
    dbEXT201.set("EXPUNO", puno);
    dbEXT201.set("EXPLPN", plpn.toInteger());
    dbEXT201.set("EXPLPS", plps.toInteger());
    dbEXT201.set("EXPLP2", plp2.toInteger());

    int noOfRecords = 0;
    noOfRecords = queryEXT201.readAll(dbEXT201, 6, 999, listEXT201);
    if (noOfRecords == 0) {
      mi.error("Record does not exist in EXT201");
      return;
    }
  }
  
  //listEXT201 - Callback function to return EXT201
  Closure<?> listEXT201 = { DBContainer contEXT201 ->

    mi.outData.put("CONO", contEXT201.get("EXCONO").toString());
    mi.outData.put("TTYP", contEXT201.get("EXTTYP").toString());
    mi.outData.put("PUNO", contEXT201.get("EXPUNO").toString());
    mi.outData.put("PLPN", contEXT201.get("EXPLPN").toString());
    mi.outData.put("PLPS", contEXT201.get("EXPLPS").toString());
    mi.outData.put("PLP2", contEXT201.get("EXPLP2").toString());
    mi.outData.put("ACTA", contEXT201.get("EXACTA").toString());
    mi.outData.put("RESP", contEXT201.get("EXRESP").toString());
    mi.outData.put("RGDT", contEXT201.get("EXRGDT").toString());
    mi.outData.put("RGTM", contEXT201.get("EXRGTM").toString());
    mi.outData.put("TMST", contEXT201.get("EXTMST").toString());

    mi.write();

  }

}