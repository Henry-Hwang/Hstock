/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.yccheok.jstock.engine;

import au.com.bytecode.opencsv.CSVReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Administrator
 */
public class MyUtils {
    public static  List<String[]>  readHistoryFile(String path) throws FileNotFoundException, IOException
    {
            System.out.println("MyUtils::readHistoryFile()");
            File file = new File(path);  
            FileReader fReader = new FileReader(file);  
            CSVReader csvReader = new CSVReader(fReader);  
            String[] strs = csvReader.readNext();   
            List<String[]> list = csvReader.readAll();
            /*
            for(String[] ss : list){ 
                System.out.print(ss.length);
                System.out.println();
                for(String s : ss)  
                    if(null != s && !s.equals(""))  
                        System.out.print(s + " , ");  
                System.out.println();  
            }  
            */       
            csvReader.close();
            return list;
       
    }
    public static void downloadNet(String source, String target) throws MalformedURLException {
        // 下载网络文件
        int bytesum = 0;
        int byteread = 0;

        URL url = new URL(source);

        try {
            URLConnection conn = url.openConnection();
            InputStream inStream = conn.getInputStream();
            FileOutputStream fs = new FileOutputStream(target);

            byte[] buffer = new byte[1204];
            int length;
            while ((byteread = inStream.read(buffer)) != -1) {
                bytesum += byteread;
                System.out.println(bytesum);
                fs.write(buffer, 0, byteread);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String stockCodeYahoo2Sina(String code) {
        if (code.contains(".SS")) {
            return "sh" + code.replace(".SS", "");
        } else if (code.contains(".SZ")) {
            return "sz" + code.replace(".SZ", "");
        }
        return null;
    }
    public static String respondFormat(String respond) {
        String[] stockDatas = respond.split("\r\n|\r|\n");
        StringBuilder respondBuild = new StringBuilder();
        List<String> list = new ArrayList<String>();

        for(String line:stockDatas) {
            list.add(0, line);
        }
        //String hisHeader = "Date,Open,High,Close,Low, Volume";
        //respondBuild.append(hisHeader).append("\r\n");
        for (Iterator<String> it = list.iterator(); it.hasNext();) {
            respondBuild.append(it.next()).append("\r\n");
        }        
        
        return respondBuild.toString();
    }     
}
