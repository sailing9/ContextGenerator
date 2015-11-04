package prepareDatabase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import util.TimeStat;
import util.Database;

public class ForWordAssociation {

	public static void initialTier2(ArrayList<String> tier2){
		String filename = ".//resource//Tier2.txt";
		File f = new File(filename);
		try{
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			while((line=br.readLine())!=null){
				tier2.add(line.toLowerCase());
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	
	public static void addColumns(String target_word, Database db){
		String queryupdate = "alter table liuliu."+target_word+"_relatedwords add " +
				"new_measure_left_1 float default null after freq_right_4, " +
				"add new_measure_left_2 float default null after new_measure_left_1, "+
				"add new_measure_left_3 float default null after new_measure_left_2, "+
				
				"add new_measure_left_4 float default null after new_measure_left_3," +
				"add new_measure_right_1 float default null after new_measure_left_4," +
				"add new_measure_right_2 float default null after new_measure_right_1,"+
				
				"add new_measure_right_3 float default null after new_measure_right_2," +
				"add new_measure_right_4 float default null after new_measure_right_3," +
				"add mutual_information_left_1 float default null after new_measure_right_4,"+
				
				"add mutual_information_left_2 float default null after mutual_information_left_1," +
				"add mutual_information_left_3 float default null after mutual_information_left_2," +
				"add mutual_information_left_4 float default null after mutual_information_left_3," +
				
				"add mutual_information_right_1 float default null after mutual_information_left_4," +
				"add mutual_information_right_2 float default null after mutual_information_right_1," +
				"add mutual_information_right_3 float default null after mutual_information_right_2,"+
				
				"add mutual_information_right_4 float default null after mutual_information_right_3" ;
		int row = db.executeUpdate(queryupdate);		
	}
	
	
	
	public String calculateMutualInformation(Long fx, Long fy, Long fxy){
		long total = 1024908267229L;
		double mutual = 0.0;
		double temp = 0.0;
		if(fx.compareTo(fy)>=0){
			temp = (double)total/(fx.doubleValue()*6);
			temp = temp*fxy.doubleValue()/fy.doubleValue();
			mutual = Math.log(temp);
			mutual = mutual/Math.log(2);				
		}else{
			temp = (double)total/(fy.doubleValue()*6);
			temp = temp*fxy.doubleValue()/fx.doubleValue();
			mutual = Math.log(temp);
			mutual = mutual/Math.log(2);	
		}
		BigDecimal b = new BigDecimal(Double.toString(mutual));
        return b.setScale(6,BigDecimal.ROUND_HALF_UP).toString();
	}
	
	public String calculateNewMeasure(Long fx, Long fy, Long fxy){
		long total = 1024908267229L;
		double new_measure = 0.0;
		new_measure = ((double)fxy)-(double)fx*(double)fy*6/(double)total;
		BigDecimal b = new BigDecimal(Double.toString(new_measure));
        return b.setScale(6,BigDecimal.ROUND_HALF_UP).toString();
	}
	
	public String calculateMutualInformation_displacement(Long fx, Long fy, Long fxy){
		long total = 1024908267229L;
		double mutual = 0.0;
		double temp = 0.0;
		if(fx.compareTo(fy)>=0){
			temp = (double)total/fx.doubleValue();
			temp = temp*fxy.doubleValue()/fy.doubleValue();
			mutual = Math.log(temp);
			mutual = mutual/Math.log(2);				
		}else{
			temp = (double)total/fy.doubleValue();
			temp = temp*fxy.doubleValue()/fx.doubleValue();
			mutual = Math.log(temp);
			mutual = mutual/Math.log(2);	
		}
		BigDecimal b = new BigDecimal(Double.toString(mutual));

        return b.setScale(6,BigDecimal.ROUND_HALF_UP).toString();
		//return mutual;
	}
	
	public String calculateNewMeasure_displacement(Long fx, Long fy, Long fxy){
		long total = 1024908267229L;
		double new_measure = 0.0;
		new_measure = ((double)fxy)-(double)fx*(double)fy/(double)total;
		BigDecimal b = new BigDecimal(Double.toString(new_measure));
        return b.setScale(6,BigDecimal.ROUND_HALF_UP).toString();
	}
	
	public void fillAssociationMeasure(String target_word, Database db){
		//sample
		long total = 1024908267229L;
		
		String query = "select related_word_base, freq_target_word, " +
				"freq_related_word, freq_co_occurrence, freq_left_1, freq_left_2, freq_left_3, " +
				"freq_left_4, freq_right_1, freq_right_2, freq_right_3, freq_right_4 from liuliu."+
				target_word+"_relatedwords";
		
		ResultSet rs = db.execute(query);
		
		try{
			while(rs.next()){
				String related_word_base = rs.getString("related_word_base");
				Long fx = rs.getLong("freq_target_word");
				Long fy = rs.getLong("freq_related_word");
//				Long fxy = rs.getLong("freq_co_occurrence");
//				Long fleft1 = rs.getLong("freq_left_1");
//				Long fleft2 = rs.getLong( "freq_left_2");
//				Long fleft3 = rs.getLong("freq_left_3");
//				Long fleft4 = rs.getLong("freq_left_4");
				Long fright1 = rs.getLong("freq_right_1");
//				Long fright2 = rs.getLong( "freq_right_2");
//				Long fright3 = rs.getLong("freq_right_3");
//				Long fright4 = rs.getLong("freq_right_4");
				
				
				
				String queryupdate = "update liuliu."+target_word+"_relatedwords set ";
//				if(fleft1!=0){
//					String mutualleft1 = this.calculateMutualInformation_displacement(fx,fy, fleft1);
//					String newmeasureleft1 = this.calculateNewMeasure_displacement(fx,fy, fleft1);
//					queryupdate+="mutual_information_left_1 = "+mutualleft1+", new_measure_left_1 = "+newmeasureleft1+",";
//				}
//				if(fleft2!=0){
//					String mutualleft2 = this.calculateMutualInformation_displacement(fx,fy, fleft2);
//					String newmeasureleft2 = this.calculateNewMeasure_displacement(fx,fy, fleft2);
//					queryupdate+="mutual_information_left_2 = "+mutualleft2+",new_measure_left_2 = "+newmeasureleft2+",";
//				}
//
//				if(fleft3!=0){
//					String mutualleft3 = this.calculateMutualInformation_displacement(fx,fy, fleft3);
//					String newmeasureleft3 = this.calculateNewMeasure_displacement(fx,fy, fleft3);
//					queryupdate+="mutual_information_left_3 = "+mutualleft3+",new_measure_left_3 = "+newmeasureleft3+",";
//				}
//				
//				if(fleft4!=0){
//					String mutualleft4 = this.calculateMutualInformation_displacement(fx,fy, fleft4);
//					String newmeasureleft4 = this.calculateNewMeasure_displacement(fx,fy, fleft4);
//					queryupdate+="mutual_information_left_4 = "+mutualleft4+",new_measure_left_4 = "+newmeasureleft4+",";
//				}
				
				if(fright1!=0){
					String mutualright1 = this.calculateMutualInformation_displacement(fx,fy, fright1);
					String newmeasureright1 = this.calculateNewMeasure_displacement(fx,fy, fright1);
					queryupdate +="mutual_information_right_1 = "+mutualright1+",new_measure_right_1 = "+newmeasureright1+",";
					queryupdate = queryupdate.substring(0, queryupdate.length()-2); // remove the ","
					queryupdate+=" where related_word_base = '"+related_word_base+"'";
					//System.out.println(queryupdate);
					int row = db.executeUpdate(queryupdate);
				
				}
//				if(fright2!=0){
//					String mutualright2 = this.calculateMutualInformation_displacement(fx,fy, fright2);
//					String newmeasureright2 = this.calculateNewMeasure_displacement(fx,fy, fright2);
//					queryupdate +="mutual_information_right_2 = "+mutualright2+",new_measure_right_2 = "+newmeasureright2+",";
//				}
//				if(fright3!=0){
//					String mutualright3 = this.calculateMutualInformation_displacement(fx,fy, fright3);
//					String newmeasureright3 = this.calculateNewMeasure_displacement(fx,fy, fright3);
//					queryupdate +="mutual_information_right_3 = "+mutualright3+",new_measure_right_3 = "+newmeasureright3+",";
//				}
//				if(fright4!=0){
//					String mutualright4 = this.calculateMutualInformation_displacement(fx,fy, fright4);
//					String newmeasureright4 = this.calculateNewMeasure_displacement(fx,fy, fright4);
//					queryupdate +="mutual_information_right_4 = "+mutualright4+",new_measure_right_4 = "+newmeasureright4+",";
//				}
//				
//				if(fxy!=0){
//					String mutual_information = this.calculateMutualInformation(fx, fy, fxy);
//					String new_measure = this.calculateNewMeasure(fx, fy, fxy);
//					queryupdate+="mutual_information = "+mutual_information+", new_measure = "+new_measure+",";
//				}
//				queryupdate = queryupdate.substring(0, queryupdate.length()-2); // remove the ","
//				queryupdate+=" where related_word_base = '"+related_word_base+"'";
//				System.out.println(queryupdate);
//				int row = db.executeUpdate(queryupdate);					
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Database db = new Database();
		TimeStat ts = new TimeStat();
		ForWordAssociation fwa = new ForWordAssociation();
		ArrayList<String> tier2list = new ArrayList<String>();
		ForWordAssociation.initialTier2(tier2list);
		for(int i = 0;i<tier2list.size();i++){
			ts.start();
			//ForWordAssociation.addColumns(tier2list.get(i), db);
			fwa.fillAssociationMeasure(tier2list.get(i), db);
			System.out.println(tier2list.get(i)+"\t"+ts.getElapsedTimeSeconds());
		}
		
//		BigDecimal b = new BigDecimal(Double.toString(0.000234));
//		System.out.println(b.setScale(6,BigDecimal.ROUND_HALF_UP).toString());
//        double result = Double.valueOf(b.setScale(6,BigDecimal.ROUND_HALF_UP).toString());
//        System.out.println(result);
//        
//        double   v1=0.00000001;     
//        BigDecimal   b1   =   new   BigDecimal(Double.toString(v1));   
//        System.out.println(b1);

	}

}
