package xyz.chenjm.redis.utils;

public class CommonUtils {
    private static boolean match0(String str, String pattern, int st, int pt) {
        for(;pt<pattern.length();++pt) {
            if(st == str.length()) return false;
            switch (pattern.charAt(pt)) {
                case '?':
                    ++st;
                    break;
                case '[':
                    boolean flag = false;
                    for(pt=pt+1;pt<pattern.length();++pt){
                        if(pattern.charAt((pt))==']') break;
                        if(pattern.charAt(pt)==str.charAt(st)) {
                            flag = true;
                        }

                    }
                    if (!flag) return false;
                    ++st;
                    break;
                case '*':
                    // 找到下一个不是'*'的pattern位置
                    for(pt++;pt<pattern.length();++pt) {
                        if(pattern.charAt(pt)!='*') break;
                    }

                    // 如果最后一个还是’*‘比定匹配成功
                    if (pt==pattern.length()){
                        return true;
                    }

                    // *是否匹配当前字符向后递归，提前终止原则，尽量匹配最少
                    return match0(str, pattern, st+1, pt-1) || match0(str, pattern, st, pt);
                default:
                    if (str.charAt(st) == pattern.charAt(pt)) {
                        st++;
                    }
                    else return false;

            }

        }
        return str.length()==st && pattern.length()==pt;
    }
    public static boolean match(String str, String pattern) {
        return "*".equals(pattern) || match0(str, pattern, 0, 0);
    }

    public static boolean isNumber(String s) {
        for(int i=0;i<s.length();++i) {
            if (i==0 && s.charAt(i)=='-') continue;
            if(s.charAt(i)<'0' || s.charAt(i) > '9') return false;
        }
        return true;
    }
    public static boolean isNumber(byte[] b) {
        for(int i=0;i<b.length;++i) {
            if (i==0 && b[i]=='-') continue;
            if(b[i]<'0' || b[i] > '9') return false;
        }
        return true;
    }
    public static boolean isExpire(long timeout) {
        return timeout < System.currentTimeMillis();
    }

}
