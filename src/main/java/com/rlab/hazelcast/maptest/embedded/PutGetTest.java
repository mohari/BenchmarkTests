package com.rlab.hazelcast.maptest.embedded;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.RandomUtils;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;
import com.rlab.hazelcast.utils.*;
public class PutGetTest {
    
	HazelcastInstance hz ;
	private static final int KB = 10240;
	IMap<Object, Object> map = null;;
	HashMap<String,Future<Double>> stats;
	CountDownLatch cdl;
	int noOfThreads;
	int count;
	
	public PutGetTest(int noOfThreads, int sizeOfKey_KB, int sizeOfValue_KB,int count){
		hz=launchHazelcastInstance();
		stats = new HashMap<String,Future<Double>>();
		map = hz.getMap("TestMap");
		cdl = new CountDownLatch(count);
		this.count = count;
		this.noOfThreads=noOfThreads;
		
		double currNano = System.nanoTime();
        putTestData();
	    double nowNano=0l;
        try {
			cdl.await();
			nowNano=System.nanoTime();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        double throughputNano= (nowNano-currNano)/count;
        System.out.println("Put Avg time ms = ("+nowNano+"-"+currNano+")/"+count+" = " +throughputNano);
        printStats(" PUT ");
	  
	     
	}
	
	private void putTestData(){
		ExecutorService es = Executors.newFixedThreadPool(noOfThreads);
		for (int i = 0; i < count; i++) {
        	String key=new String(createValue(1));
            Future<Double> f = es.submit(new PutTask(key, createValue(1)));
            stats.put(key, f);
        }
    	
	}
	
	private void getTestData(){
		ExecutorService es = Executors.newFixedThreadPool(noOfThreads);
		for(Entry<String,Future<Double>> e : stats.entrySet()){
        	String key=e.getKey();
            Future<Double> f = es.submit(new GetTask(key));
            stats.put(key, f);
        }
    	
	}
	
	private void printStats(String name){
		double min=0d;
		double max=0d;
		double sum=0d;
		for(Entry<String,Future<Double>> e : stats.entrySet()){
            try {
				double lat=e.getValue().get();
				if(lat < min) min=lat;
				if(lat > max) max=lat;
				sum+=lat;
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ExecutionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}			
		}
	    System.out.println("===== "+name+" Stats =======");
	    System.out.println("Min (ns): "+min);
	    System.out.println("Max (ns): "+max);
	    System.out.println("Count : "+stats.size());
	    System.out.println("Total Time (ns): "+sum);
	    
	    System.out.println("Avg Time (ns): "+(sum/(double)stats.size()));
	    System.out.println("Avg Time (ms): "+(sum/(double)stats.size())/100000.0);
	    System.out.printf("Avg Time (ms): %f\n", (sum/(double)stats.size())/100000.0);
	    System.out.println("===== "+name+" Stats =======");
	    
	}
	
	private static byte[] createValue(int numberOfK) {
	        return RandomUtils.nextBytes(numberOfK * KB);
	}
    
    private HazelcastInstance launchHazelcastInstance(){
    	Config config;
		try {
			 config = new FileSystemXmlConfig("src/main/resource/hazelcast.xml");
			 return Hazelcast.newHazelcastInstance(config);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }
    
    public static void main(String[] args) {
		// TODO Auto-generated method stub
    	new PutGetTest(20,1,1,10000);
	}
    
    class PutTask implements Callable<Double>{
    	 String key;
    	 Object value;
    	 PutTask(String key, Object value){
    		 this.key=key;
    		 this.value=value;
    	 }
    	 public Double call() {
    	  double t0=System.nanoTime();	 
		  map.set(key,value);	
		  double t1=System.nanoTime();
		  cdl.countDown();
		  return (new Double((t1-t0)/100000));
		}
    }
    
    class GetTask implements Callable<Double>{
    	String key;
    	GetTask(String key){
    	   this.key=key;
    	}
   	 public Double call() {
   	      double t0=System.nanoTime();	 
		  Object val = map.get(key);	
		  double t1=System.nanoTime();
		  cdl.countDown();
		  return (new Double((t1-t0)/100000));
		}
    }
    
   /* class PutAllTask implements Runnable{
   	 public void run() {
		 // map.set(createValue(1), createValue(1));	
		}
    }
    
    class GetAllTask implements Runnable{
      	 public void run() {
   		 // map.set(createValue(1), createValue(1));	
   		}
     }*/
}
