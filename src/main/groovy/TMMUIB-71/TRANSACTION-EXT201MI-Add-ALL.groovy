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

 import groovy.lang.Closure
 
 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;
 import java.time.ZoneId;



/*
 *Modification area - M3
 *Name        EXT201MI.Add
 *Type        Transaction
 *Description Workflow action Audit Trail - Add
 *
 *Nbr         Date      User        Description
 *TMMUIB-71   20250903  Wyllie Lam  Initial     
 *
 */

/**
* - Add Audit Trail extension table row
*/
public class Add extends ExtendM3Transaction {
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

  private boolean found;
  

  private int XXCONO;
 
 /*
  * Add Workflow Action Audit Trail extension table row
 */
  public Add(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
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
      mi.error("Transaction Type must be entered");
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

  	if (cono.isEmpty()) {
  	  XXCONO = (Integer)program.LDAZD.CONO;
  	} else {
  	  XXCONO = cono.toInteger();
  	}

    // Validate input fields  	
    
    // Validate input fields for transaction type
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

  	acta = mi.inData.get("ACTA") == null ? '' : mi.inData.get("ACTA").trim();
		if (acta.isEmpty()) {
      mi.error("Workflow Action must be entered");
      return;
    }
  	resp = mi.inData.get("RESP") == null ? '' : mi.inData.get("RESP").trim();
  	if (resp == "?") {
  	  resp = "";
  	}


    // - validate planned PO 
    if (ttyp.equals("PRL")) {
      DBAction queryMPOPLP = database.table("MPOPLP").index("00").selection("POCONO", "POPLPN", "POPLPS", "POPLP2").build();
      DBContainer dbMPOPLP = queryMPOPLP.getContainer();
      dbMPOPLP.set("POCONO", XXCONO);
      dbMPOPLP.set("POPLPN", plpn.toInteger());
      dbMPOPLP.set("POPLPS", plps.toInteger());
      dbMPOPLP.set("POPLP2", plp2.toInteger());
      if (!queryMPOPLP.read(dbMPOPLP)) {
        mi.error("PO Requisition number invalid");
        return;
      }    
    }

    // - validate puno
    if (ttyp.equals("PO")) {
       DBAction queryMPHEAD = database.table("MPHEAD").index("00").selection("IAPUNO").build();
       DBContainer dbMPHEAD = queryMPHEAD.getContainer();
       dbMPHEAD.set("IACONO", XXCONO);
       dbMPHEAD.set("IAPUNO", puno);
       if (!queryMPHEAD.read(dbMPHEAD)) {    
        mi.error("PO number is invalid.");
        return;
       }
    }
    
    
    // - validate responsible
    if (!resp.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").selection("JUUSID").build();
      DBContainer dbCMNUSR = queryCMNUSR.getContainer();
      dbCMNUSR.set("JUCONO", 0);
      dbCMNUSR.set("JUDIVI", "");
      dbCMNUSR.set("JUUSID", resp);
      if (!queryCMNUSR.read(dbCMNUSR)) {
        mi.error("User Responsible is invalid.");
        return;
      }
    }
  	

    writeEXT201();
    
  }
  /**
  * writeEXT201 - Write Purchase Audit Trail extension table EXT201
  *
  */
  private void writeEXT201() {
  	ZoneId zid = ZoneId.of("Australia/Sydney"); 
    LocalDateTime currentDateTimeNow = LocalDateTime.now(zid);
    int currentDate = currentDateTimeNow.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
    int currentTime = Integer.valueOf(currentDateTimeNow.format(DateTimeFormatter.ofPattern("HHmmss")));
    Date systemDate = new Date();
    long timestamp  = systemDate.getTime();

	  DBAction actionEXT201 = database.table("EXT201").build();
  	DBContainer dbEXT201 = actionEXT201.getContainer();
  	dbEXT201.set("EXCONO", XXCONO);
	  dbEXT201.set("EXTTYP", ttyp);
  	if (!puno.isEmpty()) {
  	  dbEXT201.set("EXPUNO", puno);
  	}
  	if (!plpn.isEmpty()) {
  	  dbEXT201.set("EXPLPN", plpn.toInteger());
  	}
  	if (!plps.isEmpty()) {
  	  dbEXT201.set("EXPLPS", plps.toInteger());
  	}
  	if (!plp2.isEmpty()) {
  	  dbEXT201.set("EXPLP2", plp2.toInteger());
  	}
  	dbEXT201.set("EXACTA", acta);
  	dbEXT201.set("EXTMST", timestamp);
  	dbEXT201.set("EXRGDT", currentDate);
  	dbEXT201.set("EXRGTM", currentTime);
  	dbEXT201.set("EXLMDT", currentDate);
  	dbEXT201.set("EXCHNO", 0);
  	dbEXT201.set("EXCHID", program.getUser());
  	if (!resp.isEmpty()) {
  	  dbEXT201.set("EXRESP", resp);
  	}  	
    actionEXT201.insert(dbEXT201, recordExists);
	}
	
  /**
   * recordExists - return record already exists error message to the MI
   *
  */
  Closure recordExists = {
	  mi.error("Record already exists in EXT201");
  }
  
}