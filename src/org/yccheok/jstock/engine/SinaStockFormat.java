/*
 * JStock - Free Stock Market Software
 * Copyright (C) 2011 Henry Huang  <henry.huang@cirrus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Shenzhen, Aug 02110-1301 China.
 */

package org.yccheok.jstock.engine;

import au.com.bytecode.opencsv.CSVParser;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author yccheok
 */
public class SinaStockFormat implements StockFormat {

    private SinaStockFormat() {}

    // This function is used to resolve, random corrupted data returned from
    // Yahoo! server. Once a while, we will receive complain from users as in
    // http://sourceforge.net/projects/jstock/forums/forum/723855/topic/4611584
    // 1048576 is just a random picked number. I assume in this world, there
    // should be no stock's last price larger than 1048576.
    // Note that, this is a very hacking way, and not reliable at all!
    private boolean isCorruptedData(double price) {
        return price > 1048576 || price < 0;
    }

    // This function is used to resolve, random corrupted data returned from
    // Yahoo! server. Once a while, we will receive complain from users as in
    // http://sourceforge.net/projects/jstock/forums/forum/723855/topic/4647070
    // 13 days is just a random picked number. I assume a stock should not be
    // older than 13 days. If not, it is just too old.
    private static long now = 0;
    private boolean isTooOldTimestamp(long timestamp) {
        if (timestamp == 0) {
            return false;
        }
        
        // Ensure we have a correct "now" value.
        if (now == 0) {
            long localNow = org.yccheok.jstock.gui.Utils.getGoogleServerTimestamp();
            if (localNow != 0) {
                now = localNow;
            } else {
                now = System.currentTimeMillis();
            }
        }

        // If more than 13 days old stock, we consider it as corrupted stock.
        return (Utils.getDifferenceInDays(timestamp, now) > 13);
    }
    @Override
    public boolean stockFmtCheck(String code) {
        if(code.contains(shanghaiYahoo))
            return true;
        else if (code.contains(shenzhenYahoo))
            return true;
        else
            return false;
    }
    private String parseToYahooFmt(String respond) {
        //we can get the stock infomation form SINA like this: 
        //HttpMethod method=new GetMethod("http://hq.sinajs.cn/list=sh601016,sz000005,sh600477,sz000005,")
        //=========================================================================================
        //var hq_str_sh601016="节能风电,31.28,30.91,31.84,32.45,30.70,31.72,31.74,13154727,413638393,900,31.72,500,31.71,4800,31.70,500,31.66,2000,31.65,1700,31.74,600,31.75,5200,31.76,74300,31.77,6300,31.80,2015-08-14,15:04:10,00";
        //var hq_str_sz000005="世纪星源,9.13,8.80,8.81,9.15,8.71,8.81,8.82,76419762,679014076.19,406729,8.81,641878,8.80,74800,8.79,97800,8.78,54800,8.77,140599,8.82,47409,8.83,137500,8.84,349200,8.85,286383,8.86,2015-08-14,15:05:33,00";
        //var hq_str_sh600477="杭萧钢构,12.20,11.84,12.71,13.02,12.15,12.71,12.73,108146801,1376185882,25873,12.71,26380,12.70,31700,12.69,41100,12.68,3000,12.67,10872,12.73,86600,12.74,92646,12.75,54118,12.76,14256,12.77,2015-08-14,15:04:10,00";
        //var hq_str_sz000005="世纪星源,9.13,8.80,8.81,9.15,8.71,8.81,8.82,76419762,679014076.19,406729,8.81,641878,8.80,74800,8.79,97800,8.78,54800,8.77,140599,8.82,47409,8.83,137500,8.84,349200,8.85,286383,8.86,2015-08-14,15:05:33,00";
        
        //now , we need to change strings to the one that show below:
        //601016.SS,节能风电,31.28,30.91,31.84,32.45,30.70,31.72,31.74,13154727,413638393,900,31.72,500,31.71,4800,31.70,500,31.66,2000,31.65,1700,31.74,600,31.75,5200,31.76,74300,31.77,6300,31.80,2015-08-14,15:04:10,00;
        //000005.SZ,世纪星源,9.13,8.80,8.81,9.15,8.71,8.81,8.82,76419762,679014076.19,406729,8.81,641878,8.80,74800,8.79,97800,8.78,54800,8.77,140599,8.82,47409,8.83,137500,8.84,349200,8.85,286383,8.86,2015-08-14,15:05:33,00;
        //600477.SS,杭萧钢构,12.20,11.84,12.71,13.02,12.15,12.71,12.73,108146801,1376185882,25873,12.71,26380,12.70,31700,12.69,41100,12.68,3000,12.67,10872,12.73,86600,12.74,92646,12.75,54118,12.76,14256,12.77,2015-08-14,15:04:10,00;
        //000005.SZ,世纪星源,9.13,8.80,8.81,9.15,8.71,8.81,8.82,76419762,679014076.19,406729,8.81,641878,8.80,74800,8.79,97800,8.78,54800,8.77,140599,8.82,47409,8.83,137500,8.84,349200,8.85,286383,8.86,2015-08-14,15:05:33,00;
       
        final StringBuilder respondBuilder = new StringBuilder(respond.replace("\"", ""));
        String str = respondBuilder.toString();
        int indexPrefix1, indexPrefix2; 
        do {
            indexPrefix1 = respondBuilder.indexOf(sinaFmtPrefix + shenzhenSina);
            if(indexPrefix1 != -1) {
                int index = respondBuilder.indexOf("=", indexPrefix1 );
                respondBuilder.insert(index, shenzhenYahoo);
                respondBuilder.replace(indexPrefix1, indexPrefix1 + (sinaFmtPrefix + shenzhenSina).length(), "");
            }
            
            indexPrefix2 = respondBuilder.indexOf(sinaFmtPrefix + shanghaiSina);
            if(indexPrefix2 != -1) {
                int index = respondBuilder.indexOf("=", indexPrefix2 );
                respondBuilder.insert(index, shanghaiYahoo);
                respondBuilder.replace(indexPrefix2, indexPrefix2 + (sinaFmtPrefix + shanghaiSina).length(), "");
            }
                
        } while(indexPrefix2 != -1 || indexPrefix1 != -1);
        return respondBuilder.toString().replace("=", ",").replace("\"", "");    
    }
    @Override
    public String changeStockFmt(String source) {
            String codes = null;
            final CSVParser csvParser = new CSVParser();
            String[] fields = null;
            try {
                fields = csvParser.parseLine(source);
            } catch (IOException ex) {
                log.error(null, ex);
               // continue;
            }
            final int length = fields.length;
            
            final StringBuilder codeBuilder = new StringBuilder();
            for(int i = 0; i < length; i ++) {
                String tcodes = null;
                if(fields[i].contains(shanghaiYahoo)) {
                    tcodes = shanghaiSina + fields[i].replace(shanghaiYahoo, "");
                } else if (fields[i].contains(shenzhenYahoo)) {
                    tcodes = shenzhenSina + fields[i].replace(shenzhenYahoo, "");
                } else{
                    System.out.println("SinaStockFormat::parseAsSinaStockFmt(): Unkown Stock Format!!");
                }
                StringBuilder append = codeBuilder.append(tcodes).append(",");
                // codes = codes + tcodes;
            }
            System.out.println("SinaStockFormat::parseAsSinaStockFmt(): Sian Format: " + codeBuilder.toString());
        return codeBuilder.toString();
    }
    @Override
    public String changeStockFmt(String code, int from, int to) {
        //300227.SZ to sz300227
        String tcode = null;
        if(code.contains(shenzhenYahoo)) {
            tcode = code.replace(shenzhenYahoo, "");
            tcode = shenzhenSina + tcode;
        } else if (code.contains(shanghaiYahoo)) {
            tcode = code.replace(shanghaiYahoo, "");
            tcode = shanghaiSina + tcode;
        } else {
            System.out.println("SinaStockFormat::changeStockFmt(): No Support Format:  " + code);
            tcode = "ss000001";
        }          
        return tcode;
    }    
    // Update on 19 March 2009 : We cannot assume certain parameters will always
    // be float. They may become integer too. For example, in the case of Korea
    // Stock Market, Previous Close is in integer. We shall apply string quote
    // protection method too on them.
    //
    // Here are the index since 19 March 2009 :
    // (0) Symbol
    // (1) Name
    // (2) Stock Exchange
    // (3) Symbol
    // (4) Previous Close
    // (5) Symbol
    // (6) Open
    // (7) Symbol
    // (8) Last Trade
    // (9) Symbol
    // (10) Day's high
    // (11) Symbol
    // (12) Day's low
    // (13) Symbol
    // (14) Volume
    // (15) Symbol
    // (16) Change
    // (17) Symbol
    // (18) Change Percent
    // (19) Symbol
    // (20) Last Trade Size
    // (21) Symbol
    // (22) Bid
    // (23) Symbol
    // (24) Bid Size
    // (25) Symbol
    // (26) Ask
    // (27) Symbol
    // (28) Ask Size
    // (29) Symbol
    // (30) Last Trade Date
    // (31) Last Trade Time.
    //
    // s = Symbol
    // n = Name
    // x = Stock Exchange
    // o = Open             <-- Although we will keep this value in our stock data structure, we will not show
    //                          it to clients. As some stock servers unable to retrieve open price.
    // p = Previous Close
    // l1 = Last Trade (Price Only)
    // h = Day's high
    // g = Day's low
    // v = Volume           <-- We need to take special care on this, it may give us 1,234. This will
    //                          make us difficult to parse csv file. The only workaround is to make integer
    //                          in between two string literal (which will always contains "). By using regular
    //                          expression, we will manually remove the comma.
    // c1 = Change
    // p2 = Change Percent
    // k3 = Last Trade Size <-- We need to take special care on this, it may give us 1,234...
    // b = Bid
    // b6 = Bid Size        <-- We need to take special care on this, it may give us 1,234...
    // a = Ask
    // a5 = Ask Size        <-- We need to take special care on this, it may give us 1,234...
    // d1 = Last Trade Date
    // t1 = Last Trade Time
    //
    // c6k2c1p2c -> Change (Real-time), Change Percent (Real-time), Change, Change in Percent, Change & Percent Change
    // "+1400.00","N/A - +4.31%",+1400.00,"+4.31%","+1400.00 - +4.31%"
    //
    // "MAERSKB.CO","AP MOELLER-MAERS-","Copenhagen",32500.00,33700.00,34200.00,33400.00,660,"+1200.00","N/A - +3.69%",33,33500.00,54,33700.00,96,"11/10/2008","10:53am"    
    @Override
    public List<Stock> parse(String source) {
        List<Stock> stocks = new ArrayList<Stock>();
        
        if (source == null) {
            return stocks;
        }                         
        //System.out.println("SinaStockFormat: parse(): source : " + source);
        //final String[] strings = source.split("\r\n|\r|\n");
        //String tsrc = "\"600016.SS\",\"Microsoft Corporation\",\"NMS\",\"600016.SS\",46.73,\"600016.SS\",46.54,\"600016.SS\",47.00,\"600016.SS\",47.10,\"600016.SS\",46.52,\"600016.SS\",21473402,\"600016.SS\",+0.27,\"600016.SS\",\"+0.58%\",\"600016.SS\",N/A,\"600016.SS\",N/A,\"600016.SS\",1200,\"600016.SS\",N/A,\"600016.SS\",900,\"600016.SS\",\"8/14/2015\",\"4:00pm\"";
        //final String[] strings = tsrc.split("\r\n|\r|\n");
        /*
        String tsource = source.replace("var hq_str_", "");
        tsource = tsource.replace("=", ",");
        tsource = tsource.replace("\"", "");
                */
        String tsource = parseToYahooFmt(source);
        System.out.println("SinaStockFormat: parse(): tsource : " + tsource);
        final String[] strings = tsource.split("\r\n|\r|\n"); 
      
        for (String string : strings) {
            //System.out.println("SinaStockFormat: parse(): string: " +  string) ;
            // ",123,456,"   -> ",123456,"
            // ","abc,def"," -> ","abcdef","
            // Please refer http://stackoverflow.com/questions/15692458/different-regular-expression-result-in-java-se-and-android-platform for more details.
            //
            // The idea is : If a comma doesn't have double quote on its left AND on its right, replace it with empty string.
            // http://www.regular-expressions.info/lookaround.html
           // final String stringDigitWithoutComma = commaNotBetweenQuotes.matcher(string).replaceAll("");

            // Do not use String.split although it might be faster.
            // This is because after stringDigitWithoutComma regular expression, we have an edge case
            //
            // ","abcdef,","   -> ","abcdef","  <-- This is our expectation
            // ","abcdef,","   -> ","abcdef,"," <-- This is what we get
            //
            // I think it is difficult to solve this through regular expression.
            // We will use CSVParser to handle this.
            final CSVParser csvParser = new CSVParser();
            String[] fields;
            try {
                //fields = csvParser.parseLine(stringDigitWithoutComma);
                fields = csvParser.parseLine(string);
            } catch (IOException ex) {
                log.error(null, ex);
                continue;
            }
            final int length = fields.length;
           /* 
            for(int i = 0; i < length; i ++) {
                System.out.println("fields[" + i + "]" + fields[i]);
            }
            */
            Code code = null;
            Symbol symbol = null;
            String name = null;
            Stock.Board board = null;
            Stock.Industry industry = null;
            double prevPrice = 0.0;
            double openPrice = 0.0;
            double lastPrice = 0.0;    
            double highPrice = 0.0;  
            double lowPrice = 0.0;
            // TODO: CRITICAL LONG BUG REVISED NEEDED.
            long volume = 0;
            double changePrice = 0.0;
            double changePricePercentage = 0.0;
            int lastVolume = 0;    
            double buyPrice = 0.0;
            int buyQuantity = 0;    
            double sellPrice = 0.0;
            int sellQuantity = 0; 
            double secondBuyPrice = 0.0;
            int secondBuyQuantity = 0;
            double secondSellPrice = 0.0;
            int secondSellQuantity = 0;
            double thirdBuyPrice = 0.0;
            int thirdBuyQuantity = 0;
            double thirdSellPrice = 0.0;
            int thirdSellQuantity = 0;
            long timestamp = 0;
            
            do {
                /*
                if(fields[0].contains(shanghaiSina)) {
                    fields[0] = fields[0].replace(shanghaiSina, "") + shanghaiYahoo;
                } else if (fields[0].contains(shenzhenSina)){
                    fields[0] = fields[0].replace(shenzhenSina, "") + shenzhenYahoo;
                } else {
                    break;
                }
                */
                code = Code.newInstance(quotePattern.matcher(fields[0]).replaceAll("").trim());
                name = quotePattern.matcher(fields[1]).replaceAll("").trim();
                
                // We use name as symbol, to make it more readable.
                symbol = Symbol.newInstance(name.toString());       
                try {
                    board = Stock.Board.valueOf(quotePattern.matcher(fields[1]).replaceAll("").trim());
                }
                catch (java.lang.IllegalArgumentException exp) {
                    board = Stock.Board.Unknown;
                }
                
                industry = Stock.Industry.Unknown;
                
                try { prevPrice = Double.parseDouble(fields[3]); } catch (NumberFormatException exp) {}
                try { openPrice = Double.parseDouble(fields[2]); } catch (NumberFormatException exp) {}
                try { lastPrice = Double.parseDouble(fields[4]); } catch (NumberFormatException exp) {}
                try { highPrice = Double.parseDouble(fields[5]); } catch (NumberFormatException exp) {}
                try { lowPrice = Double.parseDouble(fields[6]); } catch (NumberFormatException exp) {}
                // TODO: CRITICAL LONG BUG REVISED NEEDED.
                try { volume = Long.parseLong(fields[9]); } catch (NumberFormatException exp) {}
                changePrice = lastPrice - openPrice;              
                changePricePercentage = (changePrice * 100)/openPrice;
                try { lastVolume = Integer.parseInt(fields[20]); } catch (NumberFormatException exp) {}
                
                if (length < 23) break;
                try { buyPrice = Double.parseDouble(fields[22]); } catch (NumberFormatException exp) {}
                
                if (length < 25) break;
                try { buyQuantity = Integer.parseInt(fields[24]); } catch (NumberFormatException exp) {}
                
                if (length < 27) break;
                try { sellPrice = Double.parseDouble(fields[26]); } catch (NumberFormatException exp) {}
                
                if (length < 29) break;
                try { sellQuantity = Integer.parseInt(fields[28]); } catch (NumberFormatException exp) {}
                
                if (length < 32) break;
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MM/dd/yyyy hh:mmaa");
                String date_and_time = quotePattern.matcher(fields[30]).replaceAll("").trim() + " " + quotePattern.matcher(fields[31]).replaceAll("").trim();
                java.util.Date serverDate;
                try {
                    serverDate = dateFormat.parse(date_and_time);
                    timestamp = serverDate.getTime();
                } catch (ParseException exp) {
                    // Most of the time, we just obtain "N/A"
                    // log.error(fields[23] + ", " + fields[24] + ", " + data_and_time, exp);
                }
                
                break;
            } while(true);
            System.out.println("SinaStockFormat: parse(): while breaked ");
            if (code == null || symbol == null || name == null || board == null || industry == null) {
                continue;
            }

            // This function is used to resolve, random corrupted data returned from
            // Yahoo! server. Once a while, we will receive complain from users as in
            // http://sourceforge.net/projects/jstock/forums/forum/723855/topic/4611584
            // http://sourceforge.net/projects/jstock/forums/forum/723855/topic/4647070
            // Note that, this is a very hacking way, and not reliable at all!
            if (isCorruptedData(lastPrice) || isTooOldTimestamp(timestamp)) {
                continue;
            }

            if (length > 28) {
                if (
                    fields[28].equalsIgnoreCase("N/A") &&
                    fields[26].equalsIgnoreCase("N/A") &&
                    org.yccheok.jstock.portfolio.Utils.essentiallyEqual(lastPrice, 0.0) &&
                    fields[24].equalsIgnoreCase("N/A") &&
                    fields[22].equalsIgnoreCase("N/A") &&
                    fields[20].equalsIgnoreCase("N/A") &&
                    fields[18].equalsIgnoreCase("N/A") &&
                    fields[16].equalsIgnoreCase("N/A") &&
                    fields[14].equalsIgnoreCase("N/A") &&
                    fields[12].equalsIgnoreCase("N/A") &&
                    fields[10].equalsIgnoreCase("N/A") 
                ) {
                    continue;
                }
            }

            if (timestamp == 0) timestamp = System.currentTimeMillis();
            
            Stock stock = new Stock(
                    code,
                    symbol,
                    name,
                    board,
                    industry,
                    prevPrice,
                    openPrice,
                    lastPrice,
                    highPrice,
                    lowPrice,
                    volume,
                    changePrice,
                    changePricePercentage,
                    lastVolume,
                    buyPrice,
                    buyQuantity,
                    sellPrice,
                    sellQuantity,
                    secondBuyPrice,
                    secondBuyQuantity,
                    secondSellPrice,
                    secondSellQuantity,
                    thirdBuyPrice,
                    thirdBuyQuantity,
                    thirdSellPrice,
                    thirdSellQuantity,
                    timestamp                                        
                    );

            stocks.add(stock);            
        }
        System.out.println("SinaStockFormat: parse End");
        return stocks;
    }

    public static StockFormat getInstance() {
        return stockFormat;
    }
    
    private static final StockFormat stockFormat = new SinaStockFormat();
    
    // ",123,456,"   -> ",123456,"
    // ","abc,def"," -> ","abcdef","
    // Please refer http://stackoverflow.com/questions/15692458/different-regular-expression-result-in-java-se-and-android-platform for more details.
    //
    // The idea is : If a comma doesn't have double quote on its left AND on its right, replace it with empty string.
    // http://www.regular-expressions.info/lookaround.html    
    private static final Pattern commaNotBetweenQuotes = Pattern.compile("(?<!\"),(?!\")");

    private static final Pattern quotePattern = Pattern.compile("\"");
    private static final Pattern percentagePattern = Pattern.compile("%");
    private static final String shanghaiYahoo = ".SS";
    private static final String shenzhenYahoo = ".SZ";
    private static final String shanghaiSina = "sh";
    private static final String shenzhenSina = "sz";
    private static final String sinaFmtPrefix = "var hq_str_";
    private static final Log log = LogFactory.getLog(SinaStockFormat.class);


}
