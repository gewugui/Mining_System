import flow.BasicFlow;
import jnet.PcapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.internal.collections.Pair;
import utils.DBUtil;
import utils.ParseUtil;

import java.io.File;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Parser {

    public static final Logger log = LoggerFactory.getLogger(Parser.class);

    public static final int DO_BACK_SEARCH_COUNT = 2;

    public static void parse(String pkgTime) {
        String timestampFile = "D:\\Workspace\\IDEA Projects\\AppFlowCrawler\\output\\" + pkgTime + "\\pcaps\\timestamp.txt";
        String pcapFIle = "D:\\Workspace\\IDEA Projects\\AppFlowCrawler\\output\\" + pkgTime + "\\pcaps\\com.vkontakte.android.pcap";
        String csvPath = "D:\\Workspace\\IDEA Projects\\AppFlowCrawler\\output\\" + pkgTime + "\\csvs";
//        String sniPath = "D:\\Workspace\\IDEA Projects\\AppFlowCrawler\\output\\" + pkgTime + "\\sni.txt";

        ParseUtil.setSNI(pcapFIle);
        ParseUtil.extract(timestampFile, pcapFIle, csvPath);
    }

    public static Pair<Integer, LinkedList<Integer>> match(String pcapFileName) {
        int appId = matchApp(pcapFileName);
        String outputPath = new File(pcapFileName).getParentFile().getParent() + File.separator + "temp";
        String filteredPcap = PcapUtil.filterPcapBySni(pcapFileName, appId, outputPath);
        List<String> pcaps = PcapUtil.splitPcapByThreshold(filteredPcap, 3000, 30, outputPath);
        ParseUtil.setSNI(pcapFileName);
        LinkedList<Integer> behaviorSequence = Parser.match(appId, pcaps, false);
        return new Pair<>(appId, behaviorSequence);
    }

    //目前的简单实现是根据与每个APP库的SNI匹配数量来决定
    //可能存在两个APP库中有相同的SNI
    public static int matchApp(String pcapFilePath) {
        Map<Integer, Integer> appSniMatchMap;
        try {
             appSniMatchMap = ParseUtil.matchApp(pcapFilePath);
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("Fail to match App.");
            return -1;
        }
        if (appSniMatchMap==null || appSniMatchMap.isEmpty()) {
            log.info("No Match App.");
            return -1;
        }
        int matchApp = 0;
        int maxCount = 0;
        for (int appId : appSniMatchMap.keySet()) {
            int count = appSniMatchMap.get(appId);
            log.info("appId : " + appId + " = " + count);
            if (count > maxCount) {
                matchApp = appId;
                maxCount = count;
            }
        }
        return matchApp != 0 ? matchApp : -1;
    }

    public static LinkedList<Integer> match(int appId, List<String> files, boolean flag){
        LinkedList<Integer> behaviorSequence = new LinkedList<>();
        try {
            Set<String> dbSNIs = DBUtil.getSNIFromDB(1);
            if (dbSNIs == null) return behaviorSequence;
            dbSNIs.retainAll(ParseUtil.SNI.values());
            System.out.println(dbSNIs);
            if (dbSNIs.size()==0) {
                log.info("No matching SNI information was found");
                return behaviorSequence;
            }
        } catch (Exception e) {
            log.error("Failed to retrieve SNI information");
        }


        String csvPath = "D:\\Workspace\\IDEA Projects\\AppFlowCrawler\\temp";
        int currentDepth = 0;
        for (String file : files) {
            System.out.println(file);
            String csvFile = ParseUtil.cicFlowMeter(file, csvPath);
            List<BasicFlow> flows = ParseUtil.getFlowsFromCsv(new File(csvFile));
//            flows.forEach(flow->log.info(flow.getServerHost()));
            int toDepth;
            try {
                int count = 0;
                Pair<Integer, Integer> pair = ParseUtil.matchFlow(flows, currentDepth, appId, flag);
                toDepth = pair.first();
                //
                if (toDepth<0) {
                    while(count < DO_BACK_SEARCH_COUNT && currentDepth-count>=0) {
                        count++;
                        log.info("Fail to match in Depth " + currentDepth + " try doBack Search Count " + count);
                        toDepth = ParseUtil.matchFlow(flows, currentDepth-count, appId, flag).first();
                        if (toDepth>=0) {
                            break;
                        }
                    }

                    if (toDepth < 0) {
                        log.info("Fail to match, try next.");
                        continue;
                    }

                }
                //
                currentDepth = toDepth;
                behaviorSequence.add(pair.second());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return behaviorSequence;
    }

    public static void match(List<List<BasicFlow>> input, int appId) {
        //todo 非网络多流发掘 输入一组多流 应用/行为识别
        if (null == input || input.isEmpty()) {
            log.info("Input NULL, Nothing to Match.");
            return;
        }
        int currentDepth = 0;
        try{
            for (List<BasicFlow> flows : input) {
                int temp = currentDepth;
                Pair<Integer, Integer> pair = ParseUtil.matchFlow(flows, currentDepth, appId, false);
                currentDepth = pair.first();
                if (currentDepth < 0) {
                    log.info("No Matching Multi-Flow Found. Stop at " + input.indexOf(flows) + " [appId : " + appId + " , Depth : " + temp + "] :");
                    flows.forEach(flow -> log.info(flow.toString()));
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Fail to match. Please check the error print.");
        }
    }

    public static void main(String[] args) {
//        parse("com.vkontakte.android-2023-04-07_09-58-39");
//        parse("com.vkontakte.android-2023-04-07_10-21-59");
//        parse("com.vkontakte.android-2023-04-08_20-54-59");

        parse("com.vkontakte.android-2023-04-07_14-37-23");
        parse("com.vkontakte.android-2023-04-07_15-00-17");
        parse("com.vkontakte.android-2023-04-07_10-37-19");
        parse("com.vkontakte.android-2023-04-09_13-58-33");
        parse("com.vkontakte.android-2023-04-09_14-25-12");
        parse("com.vkontakte.android-2023-04-09_14-48-46");
        parse("com.vkontakte.android-2023-04-09_16-35-47");
        parse("com.vkontakte.android-2023-04-09_16-56-34");


//        parse("com.vkontakte.android-2023-04-09_10-58-25");
//        parse("com.vkontakte.android-2023-04-09_11-21-38");
//        parse("com.vkontakte.android-2023-04-09_12-02-28");
//        parse("com.vkontakte.android-2023-04-09_12-35-01");
//        parse("com.vkontakte.android-2023-04-09_13-17-53");

//        parse("com.vkontakte.android-2023-04-03_15-17-53");
//        parse("com.vkontakte.android-2023-04-03_15-56-07");
//        parse("com.vkontakte.android-2023-04-03_16-00-40");
//        parse("com.vkontakte.android-2023-04-03_16-24-17");
//        parse("com.vkontakte.android-2023-04-03_19-54-56");

//        parse("com.vkontakte.android-2023-04-06_21-28-08");
//        parse("com.vkontakte.android-2023-04-06_22-05-07");
//
//        parse("com.vkontakte.android-2023-04-07_09-32-20");
//
//        parse("com.vkontakte.android-2023-04-07_13-04-17");
//        parse("com.vkontakte.android-2023-04-07_13-32-50");
//        parse("com.vkontakte.android-2023-04-07_14-11-13");

//
//        parse("com.vkontakte.android-2023-04-07_15-39-49");
//        parse("com.vkontakte.android-2023-04-07_18-25-50");
//        parse("com.vkontakte.android-2023-04-07_18-53-26");
//        parse("com.vkontakte.android-2023-04-07_19-09-22");
//        parse("com.vkontakte.android-2023-04-07_19-39-27");
//
//        parse("com.vkontakte.android-2023-04-07_20-01-27");
//        parse("com.vkontakte.android-2023-04-07_20-39-45");
//        parse("com.vkontakte.android-2023-04-07_21-03-18");//
//        parse("com.vkontakte.android-2023-04-07_21-28-31");
//        parse("com.vkontakte.android-2023-04-07_21-51-04");

//        parse("com.vkontakte.android-2023-04-08_22-13-02");
//        parse("com.vkontakte.android-2023-04-08_22-37-58");
//        parse("com.vkontakte.android-2023-04-08_22-56-13");
//        parse("com.vkontakte.android-2023-04-08_23-19-26");
//


    }

}
