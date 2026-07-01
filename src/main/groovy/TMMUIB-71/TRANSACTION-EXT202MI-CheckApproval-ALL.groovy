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
 
 import java.time.LocalDate;
 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;
 import java.time.ZoneId;
 import groovy.json.JsonSlurper;
 import java.math.BigDecimal;
 import java.math.RoundingMode;
 import java.text.DecimalFormat;


/*
 *Modification area - M3
 *Name        EXT202MI.CheckApproval
 *Type        Transaction
 *Description Get the approval limit and next level approver
 *
 *Nbr         Date      User        Description
 *TMMUIB-71   20250903  Wyllie Lam  Initial      
 *
 */

/**
* - Get the approval limit and next level approver
*/
public class CheckApproval extends ExtendM3Transaction {
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
  private String appr;
  
  private String itno;
  private String itty;
  private String limt = "0";
  private String mngr;
  private String errorMsg;

  private int xxCONO;
  
  private List <Map<String, String>> lstCUGEX1_PK02;
  private List <Map<String, String>> lstCUGEX1_PK03;
  private String approver_limt = "0";
 
 /*
  * Add Workflow Action Audit Trail extension table row
 */
  public CheckApproval(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
    this.mi = mi;
    this.database = database;
  	this.miCaller = miCaller;
  	this.logger = logger;
  	this.program = program;
	  this.ion = ion;
	  
  }
  
  public void main() {
    
    errorMsg = "";
    lstCUGEX1_PK02 = new ArrayList();
    lstCUGEX1_PK03 = new ArrayList();
    
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
  	appr = mi.inData.get("APPR") == null ? '' : mi.inData.get("APPR").trim();
  	if (appr == "?") {
  	  appr = "";
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
      if (runPps170MiGetPlannedPO() == false) {
        mi.error("PPS170MI/GetPlannedPO fail with error " + errorMsg);
        return;
      }
      if (runMms200MiGet() == false) {
        mi.error("MMS200MI/Get fail with error " + errorMsg);
        return;
      }
      if (runCusExtMiGetFieldValue() == false) {
        itno = '';
        if (runCusExtMiGetFieldValue() == false) {
          itty = '';
          if (runCusExtMiGetFieldValue() == false) {
            limt = 0
          }
        }
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
      // 1 level check for itno
      if (runCusExtMiLstFieldValue() == false) {
        // 2nd level check for itty
        if (lstCUGEX1_PK02.size() > 0) {
          if (runCms100MiLst_Pa_MplineV1() == false) {
            // 3rd level check for approver
            limt = approver_limt;
          }
        } else {
          // 3rd level check for approver
          limt = approver_limt;
        } 
      }
    }
    
    // - validate approver
    if (appr.isEmpty()) {
      mi.error("Approver must be entered");
    } else {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").selection("JUUSID").build();
      DBContainer dbCMNUSR = queryCMNUSR.getContainer();
      dbCMNUSR.set("JUCONO", 0);
      dbCMNUSR.set("JUDIVI", "");
      dbCMNUSR.set("JUUSID", appr);
      if (!queryCMNUSR.read(dbCMNUSR)) {
        mi.error("Approver is invalid and it doesn't exist in MNS150");
        return;
      }
    }
    
    

    if (runPps235MiGetAuthorised() == false) {
      mi.error("PPS235MI/GetAuthorised fail with error " + errorMsg);    
      return;
    }
    
    if (mngr == null) {
      mngr = "";
    }
    mi.outData.put("LIMT", limt);
    mi.outData.put("MNGR", mngr);

    mi.write();

  }

  // Retrieve the Item no from PPS170MI.GetPlannedPO 
  boolean runPps170MiGetPlannedPO(){ 
    Map<String, String> params = ["CONO": xxCONO.toString(), "PLPN": plpn, "PLPS": plps, "PLP2": plp2];
    boolean valid = false;

    def callbackPPS170 = {
      Map<String, String> response ->
      if(response.errorMessage != null){
        errorMsg = response.errorMessage;
      }
      else {
        if(response.ITNO == null) {
            errorMsg = "PPS170MI GetPlannedPO return with ITTY is null";
            return false;
        } else {
            itno = response.ITNO;
            valid = true; 
        }
      } 
    }
    miCaller.call("PPS170MI","GetPlannedPO", params, callbackPPS170);

    if (valid) {
        return true; 
    } else {
      return false;
    }
  } 
  
  // Retrieve the Item type from Mms200MI.Get
  boolean runMms200MiGet(){ 
    logger.debug("Run MMS200MI Get");
    Map<String, String> params = ["CONO": xxCONO.toString(), "ITNO": itno];
    boolean valid = false;

    def callbackMMS200 = {
      Map<String, String> response ->
      if(response.errorMessage != null){
        errorMsg = response.errorMessage;
      }
      else {
        if(response.ITTY == null) {
            errorMsg = "MMS200MI Get return with ITTY is null";
            return false;
        } else {
            itty = response.ITTY;
            valid = true; 
        }
      } 
    }
    miCaller.call("MMS200MI","Get", params, callbackMMS200);

    if (valid) {
        return true; 
    } else {
      return false;
    }
  } 

  // Retrieve the Limit from CUSEXTMI.GetFieldValue field N096 using.
  boolean runCusExtMiGetFieldValue(){ 
    Map<String, String> params = ["FILE": "PPSAUTD", "PK01": appr, "PK02": itty, "PK03": itno];
    boolean valid = false;

    def callbackGetFieldValue = {
      Map<String, String> response ->
      if(response.errorMessage != null){
        errorMsg = response.errorMessage;
      }
      else {
        if(response.N096 == null) {        
            limt = 0;
            return false;
        }
        else { 
            limt = response.N096;
            valid = true; 
        }
      } 
    }
    miCaller.call("CUSEXTMI","GetFieldValue", params, callbackGetFieldValue);

    if (valid) {
        return true; 
    } else {
      return false;
    }
  }
  
  // Retrieve the list from CUSEXTMI.LstFieldValue.
  boolean runCusExtMiLstFieldValue(){ 
    Map<String, String> params = ["FILE": "PPSAUTD", "PK01": appr];
    boolean valid = false;

    def callbackLstFieldValue = {
      Map<String, String> response ->
      if(response.errorMessage != null){
        errorMsg = response.errorMessage;
      }
      else {
        if(response.PK03 == null ) {  
        }
        else { 
          String pk03 = response.PK03.trim();
          String pk02 = response.PK02.trim();
          String n096 = response.N096;
          if (!pk03.isEmpty()) {
            Map<String, String> map = [PK03: pk03, N096: n096];
            lstCUGEX1_PK03.add(map);
          }
          if (!pk02.isEmpty() && pk03.isEmpty()) {
            Map<String, String> map = [PK02: pk02, N096: n096];
            lstCUGEX1_PK02.add(map);
          }
          if (pk02.isEmpty() && pk03.isEmpty()) {
            approver_limt = n096;
            
          }
        }
      } 
    }
    
    miCaller.call("CUSEXTMI","LstFieldValue", params, callbackLstFieldValue);
    if (lstCUGEX1_PK03.size() <= 0) {
      return false;
    }
    if (lstCUGEX1_PK03.size() > 0) {
      valid = runPps200MiSearchLine();
    }

    if (valid) {
        return true; 
    } else {
      return false;
    }
  }
  
  // Retrieve MPLINE info from PPS200MI/SearchLine
  boolean runPps200MiSearchLine(){ 
    boolean valid = false;
    def callbackSearchLine = {
      Map<String, String> response ->
      if(response.errorMessage != null){
        errorMsg = response.errorMessage;
      }
      else {
        if(response.ITNO == null ) {  
        } else {
          String itno = response.ITNO;
          valid = true;
          return true;
        }
      } 
    }
    
    String N096 = "";
    String PK03 = "";
    for (int i=0; i<lstCUGEX1_PK03.size(); i++) {
        Map<String, String> record = (Map<String,String>)lstCUGEX1_PK03[i];
        PK03 = record.PK03.trim();
        limt = record.N096;
        String pps200Sqry = "PUNO:"+ puno + " AND ITNO:" + PK03;;
        Map<String, String> paramsSqry = [ "SQRY": pps200Sqry];
        miCaller.call("PPS200MI","SearchLine", paramsSqry, callbackSearchLine)
    }

    if (valid) {
        return true; 
    } else {
      return false;
    }
  }
  
  
  // Retrieve the manager type from Pps235MI.GetAuthorised
  boolean runPps235MiGetAuthorised(){ 
    logger.debug("Run PPS235MI Get");
    Map<String, String> params = ["CONO": xxCONO.toString(), "AURE": appr];
    boolean valid = false;

    def callbackPPS235 = {
      Map<String, String> response ->
      if(response.errorMessage != null){
        errorMsg = response.errorMessage;
      }
      else {
        if(response.MNGR == null) {
            return false;
        } else {
            mngr = response.MNGR;
            valid = true; 
        }
      } 
    }
    miCaller.call("PPS235MI","GetAuthorized", params, callbackPPS235);

    if (valid) {
        return true; 
    } else {
      return false;
    }
  }  
  
  // Retrueve itno, itty from MPLINE 
  boolean runCms100MiLst_Pa_Mpline(){
    boolean valid = false;

    String endPoint = "M3/m3api-rest/v2/execute/CMS100MI/Lst_PA_MPLINE?F_PUNO=${puno}&T_PUNO=${puno}&dateformat=YMD8&excludeempty=false&righttrim=true&format=PRETTY&extendedresult=false"
    Map<String, String> headers = ["Accept": "application/json"];
    Map<String, String> queryParameters = (Map)null;
    IonResponse response = ion.get(endPoint, headers, queryParameters);
    
    if (response.getError()) {
      valid = false;
      errorMsg = "Failed calling ION API CMS100MI Lst_PA_MPLINE";
    }
    if (response.getStatusCode() != 200) {
      valid = false;
      errorMsg = "Expected status 200 but got ${response.getStatusCode()} instead"
    }
    
    if (response.getStatusCode() == 200) {
      JsonSlurper jsonSlurper = new JsonSlurper();
      Map<String, Object> miResponse = (Map<String, Object>) jsonSlurper.parseText(response.getContent());
      if(miResponse != null) {
        ArrayList<Map<String, Object>> results = (ArrayList<Map<String, Object>>) miResponse.get("results")
        String successTrns = miResponse["nrOfSuccessfullTransactions"]
        
        if (successTrns == "1"){
          itno = results.records.IBITNO.toString();
          itno = itno.replace('[','');
          itno = itno.replace(']','');
          logger.debug(itno);
          itty = results.records.MMITTY.toString();
          itty = itty.replace('[','');
          itty = itty.replace(']','');
          logger.debug(itty);
          if (itno.equals("*")) {
            itno = "";
          }
          if (itty.equals("*")) {
            itty = "";
            itno = "";
          }
          valid = true;
        }
        else{
          errorMsg = results[0]["errorMessage"]
          valid = false;
        }
      }
    }


    
    if (valid) {
        return true; 
    } else {
      return false;
    }
 }
 
  // Retrueve itty from MPLINE 
  boolean runCms100MiLst_Pa_MplineV1(){
    boolean valid = false;

    String endPoint = "M3/m3api-rest/v2/execute/CMS100MI/Lst_PA_MPLINEV1?IBPUNO=${puno}&dateformat=YMD8&excludeempty=false&righttrim=true&format=PRETTY&extendedresult=false"
    Map<String, String> headers = ["Accept": "application/json"];
    Map<String, String> queryParameters = (Map)null;
    IonResponse response = ion.get(endPoint, headers, queryParameters);
    
    if (response.getError()) {
      valid = false;
      errorMsg = "Failed calling ION API CMS100MI Lst_PA_MPLINEV1";
    }
    if (response.getStatusCode() != 200) {
      valid = false;
      errorMsg = "Expected status 200 but got ${response.getStatusCode()} instead"
    }
    
    if (response.getStatusCode() == 200) {
      JsonSlurper jsonSlurper = new JsonSlurper();
      Map<String, Object> miResponse = (Map<String, Object>) jsonSlurper.parseText(response.getContent());
      if(miResponse != null) {
        ArrayList<Map<String, Object>> results = (ArrayList<Map<String, Object>>) miResponse.get("results")
        String successTrns = miResponse["nrOfSuccessfullTransactions"]
        
        if (successTrns == "1"){
          itty = results.records.MMITTY.toString();
          itty = itty.replace('[','');
          itty = itty.replace(']','');
          String PK02;
          String N096;
          String[] lst_itty;
          lst_itty =  itty.split(",");
          
          for (String item_type: lst_itty ) {
            for (int i=0; i<lstCUGEX1_PK02.size(); i++) {
                Map<String, String> record = (Map<String,String>)lstCUGEX1_PK02[i];
                PK02 = record.PK02.trim();
                N096 = record.N096;
                if (PK02.equals(item_type)) {
                  valid = true;
                  limt = N096;
                  return true;
                }
            }
           
          }
        }
        else{
          errorMsg = results[0]["errorMessage"]
          valid = false;
        }
      }
    }


    
    if (valid) {
        return true; 
    } else {
      return false;
    }
 }
   
}