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
 *   THE PROPERTY OF THEIR APPRECTIVE OWNERS.                  *
 *                                                             *
 ***************************************************************
 */

 import groovy.lang.Closure
 
 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;
 import java.time.ZoneId;


/*
 *Modification area - M3
 *Name        EXT200MI.Add
 *Type        Transaction
 *Description Authorisation status - Add
 *
 *Nbr         Date      User        Description
 *TMMUIB-71   20250903  Wyllie Lam  Initial      
 *
 */

/**
* - Add Purchase Authorisation extension table row
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
  private String asts = "";
  private String appr = "";
  private String ntam;
  private String uchk;

  private boolean found;
  

  private int xxCONO;
 
 /*
  * Add Purchase Authorisation extension table row
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
  	asts = mi.inData.get("ASTS") == null ? '' : mi.inData.get("ASTS").trim();
  	if (asts == "?") {
  	  asts = "";
  	} 
  	appr = mi.inData.get("APPR") == null ? '' : mi.inData.get("APPR").trim();
  	if (appr == "?") {
  	  appr = "";
  	} 
  	ntam = mi.inData.get("NTAM") == null ? '' : mi.inData.get("NTAM").trim();
  	uchk = mi.inData.get("UCHK") == null ? '' : mi.inData.get("UCHK").trim();
  	
  	if (plpn.isEmpty()) { plpn = "0";  }
  	if (plps.isEmpty()) { plps = "0";  }
  	if (plp2.isEmpty()) { plp2 = "0";  }
  	if (ntam.isEmpty()) { ntam = "0";  }
  	if (uchk.isEmpty()) { uchk = "0";  }
  	
  	if (cono.isEmpty()) {
  	  xxCONO = (Integer)program.LDAZD.CONO;
  	} else {
  	  xxCONO = cono.toInteger();
  	}

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

  	asts = mi.inData.get("ASTS") == null ? '' : mi.inData.get("ASTS").trim();
		if (asts.isEmpty()) {
      mi.error("Authorisation status must be entered");
      return;
    }
  	appr = mi.inData.get("APPR") == null ? '' : mi.inData.get("APPR").trim();
  	if (appr == "?") {
  	  appr = "";
  	}
  	ntam = mi.inData.get("NTAM") == null ? '0' : mi.inData.get("NTAM").trim();


    // - validate planned PO 
    if (ttyp.equals("PRL")) {
      DBAction queryMPOPLP = database.table("MPOPLP").index("00").selection("POCONO", "POPLPN", "POPLPS", "POPLP2").build();
      DBContainer dbMPOPLP = queryMPOPLP.getContainer();
      dbMPOPLP.set("POCONO", xxCONO);
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
       dbMPHEAD.set("IACONO", xxCONO);
       dbMPHEAD.set("IAPUNO", puno);
       if (!queryMPHEAD.read(dbMPHEAD)) {    
        mi.error("PO number is invalid.");
        return;
       }
    }

    // - validate authorisation status
    if (!asts.equals("Approved") 
        && !asts.equals("Rejected") 
        && !asts.equals("Sent for approval") 
        && !asts.equals("Awaiting approval")) { 
      mi.error("Invalid authorisation status");
      return;
    }
    // - validate approver
    if (!appr.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").selection("JUUSID").build();
      DBContainer dbCMNUSR = queryCMNUSR.getContainer();
      dbCMNUSR.set("JUCONO", 0);
      dbCMNUSR.set("JUDIVI", "");
      dbCMNUSR.set("JUUSID", appr);
      if (!queryCMNUSR.read(dbCMNUSR)) {
        mi.error("Approver is invalid.");
        return;
      }
    }
  	

    writeEXT200();
    
  }
  /**
  * writeEXT200 - Write Purchase Authorisation extension table EXT200
  *
  */
  private void writeEXT200() {
  	ZoneId zid = ZoneId.of("Australia/Sydney"); 
    LocalDateTime currentDateTimeNow = LocalDateTime.now(zid);
    int currentDate = currentDateTimeNow.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
    int currentTime = Integer.valueOf(currentDateTimeNow.format(DateTimeFormatter.ofPattern("HHmmss")));
    Date systemDate = new Date();
    long timestamp  = systemDate.getTime();

	  DBAction actionEXT200 = database.table("EXT200").build();
  	DBContainer dbEXT200 = actionEXT200.getContainer();
  	dbEXT200.set("EXCONO", xxCONO);
  	dbEXT200.set("EXTTYP", ttyp);
  	if (!puno.isEmpty()) {
  	  dbEXT200.set("EXPUNO", puno);
  	}
  	if (!plpn.isEmpty()) {
  	  dbEXT200.set("EXPLPN", plpn.toInteger());
  	}
  	if (!plps.isEmpty()) {
  	  dbEXT200.set("EXPLPS", plps.toInteger());
  	}
  	if (!plp2.isEmpty()) {
  	  dbEXT200.set("EXPLP2", plp2.toInteger());
  	}
  	dbEXT200.set("EXASTS", asts);
  	if (!appr.isEmpty()) {
  	  dbEXT200.set("EXAPPR", appr);
  	}  	
  	if (!ntam.isEmpty()) {
  	  dbEXT200.set("EXNTAM", ntam.toDouble());
  	}
  	if (!uchk.isEmpty()) {
  	  dbEXT200.set("EXUCHK", uchk.toInteger());
  	}
  	dbEXT200.set("EXLMTS", timestamp);
  	dbEXT200.set("EXRGDT", currentDate);
  	dbEXT200.set("EXRGTM", currentTime);
  	dbEXT200.set("EXLMDT", currentDate);
  	dbEXT200.set("EXCHNO", 0);
  	dbEXT200.set("EXCHID", program.getUser());
    actionEXT200.insert(dbEXT200, recordExists);
	}
	
  /**
   * recordExists - return record already exists error message to the MI
   *
  */
  Closure recordExists = {
	  mi.error("Record already exists in EXT200");
  }
  
}