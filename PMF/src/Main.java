
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.data.DataModel;
import net.librec.data.model.TextDataModel;
import net.librec.data.splitter.KCVDataSplitter;
import net.librec.data.splitter.LOOCVDataSplitter;
import net.librec.eval.RecommenderEvaluator;
import net.librec.eval.ranking.PrecisionEvaluator;
import net.librec.eval.ranking.RecallEvaluator;
import net.librec.eval.rating.MAEEvaluator;
import net.librec.eval.rating.RMSEEvaluator;
import net.librec.filter.GenericRecommendedFilter;
import net.librec.filter.RecommendedFilter;
import net.librec.job.RecommenderJob;
import net.librec.recommender.Recommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.cf.rating.PMFRecommender;
import net.librec.recommender.item.RecommendedItem;
import net.librec.util.DriverClassUtil;
import net.librec.util.FileUtil;

public class Main {
	
	 public static void saveResult(List<RecommendedItem> recommendedList) throws LibrecException, IOException, ClassNotFoundException {
	        if (recommendedList != null && recommendedList.size() > 0) {
	            // make output path
	        	Configuration conf = new Configuration();
	        	
	        	final Log LOG = LogFactory.getLog(RecommenderJob.class);
	        	
	        	
	        	Properties prop = new Properties();
	        	String ConfFilepath = CONFIG_FILE;
	    		prop.load(new FileInputStream(ConfFilepath));
	    		for(String name: prop.stringPropertyNames())
	    		{
	    			//System.out.println(name +" = "+ prop.getProperty(name));
	    			conf.set(name, prop.getProperty(name));
	    		}
	        	DataModel dataModel = new TextDataModel(conf);
	    		dataModel.buildDataModel();
	    		
	    		
	            String algoSimpleName = DriverClassUtil.getDriverName("net.librec.recommender.cf.rating.PMFRecommender");//net.librec.recommender.cf.rating.PMFRecommender
	            																										//net.librec.recommender.cf.rating.SVDPlusPlusRecommender
	            String outputPath = conf.get("dfs.result.dir") + "/" + conf.get("data.input.path") + "-" + algoSimpleName + "-output/" + algoSimpleName;
	            if (null != dataModel && (dataModel.getDataSplitter() instanceof KCVDataSplitter || dataModel.getDataSplitter() instanceof LOOCVDataSplitter) && null != conf.getInt("data.splitter.cv.index")) {
	                outputPath = outputPath + "-" + String.valueOf(conf.getInt("data.splitter.cv.index"));
	            }
	            LOG.info("Result path is " + outputPath);
	            // convert itemList to string
	            StringBuilder sb = new StringBuilder();
	            for (RecommendedItem recItem : recommendedList) {
	                String userId = recItem.getUserId();
	                String itemId = recItem.getItemId();
	                String value = String.valueOf(recItem.getValue());
	                
	                //System.out.print(" 	userId :" + userId+ " itemId: "+itemId+ " value: "+ value);
	                sb.append(userId).append(",").append(itemId).append(",").append(value).append("\n");
	            }
	            String resultData = sb.toString();
	            // save resultData
	            try {
	                FileUtil.writeString(outputPath, resultData);
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	    }

	public static String CONFIG_FILE = //"conf/SVD.properties" 
			"conf/pmf.properties"
			;
	
	public static HashMap<Integer,HashMap<Integer,Double>> Out_Pmf_Svd(List<RecommendedItem> recItem)
	{
		HashMap<Integer,HashMap<Integer,Double>> drt=new HashMap<Integer,HashMap<Integer,Double>>();
		HashMap<Integer,Double> itemR;
		for (RecommendedItem rec : recItem) {
            String userId = rec.getUserId();
           
            String itemId = rec.getItemId();
            //itemR.get(Integer.parseInt(itemId)).keySet();
            String value = String.valueOf(rec.getValue());
           
            if(!drt.containsKey(Integer.parseInt(userId))){ 
            itemR = new HashMap<Integer,Double>();
            itemR.put(Integer.parseInt(itemId),Double.parseDouble(value));           
            drt.put(Integer.parseInt(userId),itemR);
            }else
            {
            	itemR=drt.get(Integer.parseInt(userId));
            	 itemR.put(Integer.parseInt(itemId),Double.parseDouble(value));           
                 drt.put(Integer.parseInt(userId),itemR);
            }
		}
		return drt;
	}
	
	public static void main(String[] args)throws Exception{

		BasicConfigurator.configure();//for configuration error
		
		Configuration conf = new Configuration();
		String ConfFilepath = CONFIG_FILE;
		
		Properties prop = new Properties();//lecture de fichier .properties
		prop.load(new FileInputStream(ConfFilepath));
		for(String name: prop.stringPropertyNames())
		{
			//System.out.println(name +" = "+ prop.getProperty(name));
			conf.set(name, prop.getProperty(name));
		}

		// build data model
		DataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();
		
		// set recommendation context
		RecommenderContext context = new RecommenderContext(conf, dataModel);
		

		// Choose Recommender
		//Recommender recommender = new SVDPlusPlusRecommender();
		Recommender recommender = new PMFRecommender();
		recommender.recommend(context);


		// evaluation
		RecommenderEvaluator evaluator = new MAEEvaluator();
		RecommenderEvaluator evaluator1 = new RMSEEvaluator();
		RecommenderEvaluator evaluator2 = new RecallEvaluator();
		RecommenderEvaluator evaluator3 = new PrecisionEvaluator();
		System.out.println("MAE:" +recommender.evaluate(evaluator));
		System.out.println("RMSE:" +recommender.evaluate(evaluator1));
		System.out.println("Recall:" +recommender.evaluate(evaluator2));
		System.out.println("Precision:" +recommender.evaluate(evaluator3));

		// recommendation results
		List<RecommendedItem> recommendedItemList = recommender.getRecommendedList();
		RecommendedFilter filter = new GenericRecommendedFilter();
		recommendedItemList = filter.filter(recommendedItemList);
		saveResult(recommendedItemList);
		
		
	
	
	}
}
