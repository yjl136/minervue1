package com.ramy.minervue.util;
import java.util.ArrayList;
import java.util.List;
import com.ramy.minervue.bean.GasData;
import com.ramy.minervue.bean.ReplyData;
import android.location.Address;
import android.util.Log;
public class DataProcessUtils {
	public static final String FIX_PART="fe6801241e";
	public static final String EFLAG="0d";
	public static final String TAG="DataProcess";
	private ReplyData requestsData;
	public DataProcessUtils(ReplyData data){
		this.requestsData=data;
	}
	public String getStrFromArray(String[] from ){
		StringBuffer buffer=new StringBuffer();
		for(String str:from){
			buffer.append(str);
		}
		return new String(buffer);
	}

	public String getFixPart(){
		return getStrFromArray(requestsData.getfPart());
	}

	public String[] getDate(){
		return requestsData.getDate();
	}

	public String getCS(){
		return getStrFromArray(requestsData.getCs());
	}

	public String getEFlag(){
		return getStrFromArray(requestsData.geteFlag());
	}
	public List<GasData> getGasData(){
		List<GasData> gasDatas = new ArrayList<GasData>();
		String[] content=requestsData.getGasDatas();
		for(int i=0;i<24;i+=4){
			char gasType=content[i].toLowerCase().charAt(0);
			Log.i(TAG,gasType+"" );
			switch (gasType) {
			case '0':
				//甲烷传感器
				GasData ch4=new GasData();
				ch4.setGasName("CH4");
				String ch4Value=content[i+2].charAt(1)+"."+content[i+3];
				ch4.setGasValue(ch4Value);
				gasDatas.add(ch4);
				break;
			case '1':
				//氧气传感器
				GasData o2=new GasData();
				o2.setGasName("O 2");
				String o2Value=content[i+2]+"."+content[i+3].charAt(0);
				o2.setGasValue(o2Value);
				gasDatas.add(o2);
				break;
			case '2':
				//一氧化碳传感器
				GasData co=new GasData();
				co.setGasName("C O");
				String coValue=content[i+2]+""+content[i+3];
				co.setGasValue(coValue);
				gasDatas.add(co);
				break;
			case '4':
				//温度传感器
				GasData tem=new GasData();
				tem.setGasName("Tem");
				String temValue=content[i+2]+""+content[i+3].charAt(0)+"."+content[i+3].charAt(1);
				tem.setGasValue(Double.parseDouble(temValue)-40+"");
				gasDatas.add(tem);
				break;
			case '7':
				//硫化氢传感器
				GasData h2s=new GasData();
				h2s.setGasName("H2S");
				String h2sValue=content[i+2]+""+content[i+3];
				h2s.setGasValue(h2sValue);
				gasDatas.add(h2s);
				break;
			case 'a':
				//湿度传感器
				GasData rh=new GasData();
				rh.setGasName("R H");
				String rhValue=content[i+2].charAt(0)+""+content[i+3];
				rh.setGasValue(rhValue);
				gasDatas.add(rh);
				break;
			}
		}
		
		return gasDatas;
		
	}
	public String getYMD(){
		String[] dates=getDate();
		String ymd=dateProcess(dates, 0, 3);
		return ymd;
	}
	public String dateProcess(String[] data,int start,int end ){
		StringBuffer buffer=new StringBuffer();
		for(int i=start;i<end;i++){
			if(start==0){
				buffer.append(data[i]+"/");
			}else{;
				buffer.append(data[i]+":");
			}
		}
		buffer.deleteCharAt(buffer.length()-1);
		return new String(buffer);
	}
	public String getHMS(){
		String[] dates=getDate();
		String hms=dateProcess(dates, 3, 6);
		return hms;
	}
	public boolean isDataRight(){
		if(!isFixPartRight()||!isCSRight()||!isEFlagRight()){
			return false;
		}
		return true;
	}
	private boolean isCSRight() {
		String cs=calculateCs();
		return cs.equals(getCS().toLowerCase());
	}
	private String calculateCs() {
		int sum=0;
		String[] result=requestsData.getResult();
		for(int i=1;i<35;i++){
			sum+=Integer.parseInt(result[i], 16);
		}
		String cs=Integer.toHexString(sum);
		return cs.substring(cs.length()-2);
	}
	private boolean isEFlagRight() {
		return EFLAG.equals(getEFlag().toLowerCase());
	}
	private boolean isFixPartRight() {
		return FIX_PART.equals(getFixPart().toLowerCase());
	}
	

}
