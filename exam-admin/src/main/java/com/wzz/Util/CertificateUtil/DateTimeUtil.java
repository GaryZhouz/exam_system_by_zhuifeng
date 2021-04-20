package com.wzz.Util.CertificateUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtil {

	  public static Date getCurrentDate(){
		  return new Date(System.currentTimeMillis());
	  }

	  public static String DateToString(Date date) {
		  	if(date == null){
		  		return "";
		  	}
		  	SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月dd日 HH时mm分");
		  	return sdf.format(date);
	 }
}
