import jnet.PcapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.internal.collections.Pair;
import utils.EvaluationUtil;
import utils.ParseUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Evaluator {

    public static final Logger log = LoggerFactory.getLogger(Evaluator.class);

    //二分类,
    //label 为制定应用的ID
    public static void BC_Result(List<Pair<Integer, LinkedList<Integer>>> predicted, List<Pair<Integer, LinkedList<Integer>>> actual, int label) {
        if (null == predicted || null == actual || predicted.isEmpty() || actual.isEmpty() || predicted.size() != actual.size())    return;
        int appTP = 0;
        int appFP = 0;
        int appTN = 0;
        int appFN = 0;

        int count = 0;

        double behaviorAccuracy = 0;
        double behaviorPrecision = 0;
        double behaviorRecall = 0;
        double behaviorF1 = 0;

        for (int i = 0; i < predicted.size(); i++) {

            Pair<Integer, LinkedList<Integer>> prdPair = predicted.get(i);
            int prdApp = prdPair.first();
            LinkedList<Integer> prdBehaviorSequence = prdPair.second();

            Pair<Integer, LinkedList<Integer>> actPair = actual.get(i);
            int actApp = actPair.first();
            LinkedList<Integer> actBehaviorSequence = actPair.second();

            if (prdApp == actApp) {
                if (prdApp == label) {
                    appTP++;
                } else {
                    appTN++;
                    continue;
                }
            } else {
                if (prdApp == label) {
                    appFP++;
                    continue;
                } else {
                    appFN++;
                    continue;
                }
            }

            int behaviorTP;//表示模型检测到的行为序列与实际行为序列匹配的数量
            int behaviorFP;//表示模型检测到的行为序列与实际行为序列不匹配的数量
            int behaviorFN;// 表示实际行为序列中未被检测到的序列数量

            List<Integer> tpList = new ArrayList<>(prdBehaviorSequence);
            tpList.retainAll(actBehaviorSequence);
            behaviorTP = tpList.size();

            List<Integer> fpList = new ArrayList<>(prdBehaviorSequence);
            fpList.removeAll(actBehaviorSequence);
            behaviorFP = fpList.size();

            List<Integer> fnList = new ArrayList<>(actBehaviorSequence);
            fnList.removeAll(prdBehaviorSequence);
            behaviorFN = fnList.size();

            double[] indicators = EvaluationUtil.BC_Result_3_Evaluation(behaviorTP, behaviorFP, behaviorFN);
            if (indicators.length!=4) {
                log.error("Evaluation indicators count Error. Excepted : " + 4 + " , actual : " + indicators.length);
                return;
            }
            behaviorAccuracy += indicators[0];
            behaviorPrecision += indicators[1];
            behaviorRecall += indicators[2];
            behaviorF1 += indicators[3];
            count ++;
        }

        double[] appIndicators = EvaluationUtil.BC_Result_4_Evaluation(appTP, appFP, appTN, appFN);
        if (appIndicators.length!=4) {
            log.error("App Evaluation indicators count Error. Excepted : " + 4 + " , actual : " + appIndicators.length);
            return;
        }
        log.info("\nFinal Evaluation----------------------------------------------");
        log.info("========App Detection========");
        log.info("Accuracy : " + appIndicators[0]);
        log.info("Precision : " + appIndicators[1]);
        log.info("Recall : " + appIndicators[2]);
        log.info("F1 : " + appIndicators[3]);
        log.info("==================================");
        if (count!=0) {
            log.info("========Behavior Detection========");
            log.info("Accuracy : " + behaviorAccuracy/count);
            log.info("Precision : " + behaviorPrecision/count);
            log.info("Recall : " + behaviorRecall/count);
            log.info("F1 : " + behaviorF1/count);
            log.info("==================================");
        }
    }

    public static void BC_Result1(List<Pair<Integer, LinkedList<Integer>>> predicted, List<Pair<Integer, LinkedList<Integer>>> actual, int label, int fromCount, int toCount) {
        if (null == predicted || null == actual || predicted.isEmpty() || actual.isEmpty() || predicted.size() != actual.size())    return;
        int appTP = 0;
        int appFP = 0;
        int appTN = 0;
        int appFN = 0;

        int count = 0;

        int behaviorTP = 0;//表示模型检测到的行为序列与实际行为序列匹配的数量
        int behaviorFP = 0;//表示模型检测到的行为序列与实际行为序列不匹配的数量
        int behaviorFN = 0;// 表示实际行为序列中未被检测到的序列数量

        for (int i = fromCount-1; i <= toCount-1; i++) {

            Pair<Integer, LinkedList<Integer>> prdPair = predicted.get(i);
            int prdApp = prdPair.first();
            LinkedList<Integer> prdBehaviorSequence = prdPair.second();

            Pair<Integer, LinkedList<Integer>> actPair = actual.get(i);
            int actApp = actPair.first();
            LinkedList<Integer> actBehaviorSequence = actPair.second();

            if (prdApp == actApp) {
                if (prdApp == label) {
                    appTP++;
                } else {
                    appTN++;
                    continue;
                }
            } else {
                if (prdApp == label) {
                    appFP++;
                    continue;
                } else {
                    appFN++;
                    continue;
                }
            }

            List<Integer> tpList = new ArrayList<>(prdBehaviorSequence);
            tpList.retainAll(actBehaviorSequence);
            behaviorTP += tpList.size();

            List<Integer> fpList = new ArrayList<>(prdBehaviorSequence);
            fpList.removeAll(actBehaviorSequence);
            behaviorFP += fpList.size();

            List<Integer> fnList = new ArrayList<>(actBehaviorSequence);
            fnList.removeAll(prdBehaviorSequence);
            behaviorFN += fnList.size();

            count ++;
        }

        double[] appIndicators = EvaluationUtil.BC_Result_4_Evaluation(appTP, appFP, appTN, appFN);
        if (appIndicators.length!=4) {
            log.error("App Evaluation indicators count Error. Excepted : " + 4 + " , actual : " + appIndicators.length);
            return;
        }
        log.info("\nFinal Evaluation----------------------------------------------");
        log.info("========App Detection========");
        log.info("Accuracy : " + appIndicators[0]);
        log.info("Precision : " + appIndicators[1]);
        log.info("Recall : " + appIndicators[2]);
        log.info("F1 : " + appIndicators[3]);
        log.info("==================================");
        if (count!=0) {
            log.info(behaviorTP + " " + behaviorFP + " " + behaviorFN);
            double[] indicators = EvaluationUtil.BC_Result_3_Evaluation(behaviorTP, behaviorFP, behaviorFN);
            if (indicators.length!=4) {
                log.error("Evaluation indicators count Error. Excepted : " + 4 + " , actual : " + indicators.length);
                return;
            }
            log.info("========Behavior Detection========");
            log.info("Accuracy : " + indicators[0]);
            log.info("Precision : " + indicators[1]);
            log.info("Recall : " + indicators[2]);
            log.info("F1 : " + indicators[3]);
            log.info("==================================");
        }
    }

    public static List<Pair<Integer, LinkedList<Integer>>> readData(String dataFileName) {
        File dataFile = new File(dataFileName);
        if (!dataFile.exists()) return null;
        BufferedReader reader = null;
        String temp;
        List<Pair<Integer, LinkedList<Integer>>> data = null;
        try {
            data = new ArrayList<>();
            reader = new BufferedReader(new FileReader(dataFile));
            while ((temp=reader.readLine())!=null) {

                String[] parts = temp.split(" ");
                if (parts.length==1) {
                    int appId = Integer.parseInt(parts[0]);
                    LinkedList<Integer> behaviorSequence = new LinkedList<>();
                    Pair<Integer, LinkedList<Integer>> pair = new Pair<>(appId, behaviorSequence);
                    data.add(pair);
                } else if (parts.length==2) {
                    int appId = Integer.parseInt(parts[0]);
                    LinkedList<Integer> behaviorSequence = new LinkedList<>();
                    String[] behaviors = parts[1].split(",");
                    for (String behavior : behaviors) {
                        behaviorSequence.add(Integer.parseInt(behavior.trim()));
                    }
                    Pair<Integer, LinkedList<Integer>> pair = new Pair<>(appId, behaviorSequence);
                    data.add(pair);
                } else  {
                    log.error("Data File parts error");
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (reader!=null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return data;
    }

    public static List<Pair<Integer, LinkedList<Integer>>> getData(String dirPath, int countFrom, int countTo) {
        List<Pair<Integer, LinkedList<Integer>>> data = new ArrayList<>();
        for(int i = countFrom;i <= countTo;i++) {
            String pcapFile = dirPath + i + ".pcap";
            log.info(pcapFile);
            int appId = Parser.matchApp(pcapFile);
            String outputPath = new File(pcapFile).getParentFile().getParent() + File.separator + "temp";
            String filteredPcap = PcapUtil.filterPcapBySni(pcapFile, appId, outputPath);
            List<String> pcaps = PcapUtil.splitPcapByThreshold(filteredPcap, 3000, 30, outputPath);
            if (null!=pcaps && pcaps.size()>=1) {
                pcaps.remove(0);
            }
            ParseUtil.setSNI(pcapFile);
            LinkedList<Integer> behaviors = Parser.match(1, pcaps, false);

            Pair<Integer, LinkedList<Integer>> pair = new Pair<>(appId, behaviors);

            data.add(pair);
        }
        return data;
    }

    public static void saveData(List<Pair<Integer, LinkedList<Integer>>> data, String fileName) {
        try {
            File dataFile = new File(fileName);
            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }
            FileWriter writer = new FileWriter(dataFile);
            BufferedWriter out = new BufferedWriter(writer);
            for (Pair<Integer, LinkedList<Integer>> pair: data){
                writer.write(pair.first() + " ");
                LinkedList<Integer> behaviors = pair.second();
                if (!behaviors.isEmpty()) {
                    for (int i = 0; i < behaviors.size(); i++) {
                        if (i!=0)   writer.write(",");
                        writer.write(behaviors.get(i)+"");
                    }
                }
                writer.write("\n");
            }
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Fail to save data : " + fileName);
        }
    }

    public static void main(String[] args) {
        List<Pair<Integer, LinkedList<Integer>>> predicted;
        List<Pair<Integer, LinkedList<Integer>>> actual;




    }


}
