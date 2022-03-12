package run.runnable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
    /**
     * 通过正则获取字符串
     * @param str 匹配的字符串
     * @param reg 正则表达式
     * @return 提取正则表达式匹配的字符
     */
    public static List<String> getRegexString(String str, String reg){
        Matcher matcher = Pattern.compile(reg).matcher(str);
        List<String> matchStrs = new ArrayList<>();
        while (matcher.find()) {
            matchStrs.add(matcher.group());
        }
        return matchStrs;
    }
}
