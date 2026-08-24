package hnsMgr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Format {
    private String targetStr;
    public Format(){
        targetStr = null;
    }
    public Format(String str){
        targetStr = str;
    }
    public String[] startFormatting(){
        if(targetStr == null){
            throw new NullPointerException("Target String Cannot be null");
        }
        String[] spl;
        spl=split(targetStr,' ');
        return spl;
    }
    public String[] startFormatting(String str){
        String[] spl;
        spl=split(str,' ');
        return spl;
    }
    public String[] split(String target, char regex){
        boolean shouldSplitTs = true;
        char[] ca = target.toCharArray();
        ArrayList<String> result = new ArrayList<>();
        String[] finalResult = new String[0];
        int index = 0;
        StringBuffer sb = new StringBuffer();
        for (char c : ca){
            if(c == '\"') shouldSplitTs = !shouldSplitTs;
            if(!shouldSplitTs) continue;
            if(c == regex) {
                result.add(sb.toString());
                index += 1;
                sb = new StringBuffer();
            }
            else sb.append(c);
        }
        if (sb.length() > 0) {
            result.add(sb.toString());
        }
        return result.toArray(finalResult);
    }
}
