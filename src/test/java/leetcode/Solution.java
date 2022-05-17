package leetcode;

import java.util.*;

public class Solution {
    final static Map<Character,Integer> keys = new HashMap<>();
    static {
        keys.put('A', 0);
        keys.put('C', 1);
        keys.put('G', 2);
        keys.put('T', 3);

    }

    public static int minMutation(String start, String end, String[] bank) {
        Set<Integer> set = new HashSet<>();
        for (String b: bank) {
            set.add(hash(b));
        }
        int stop = hash(end), st = hash(start);
        if (stop == st) return 0;
        if (!set.contains(stop)) return -1;

        Deque<Integer> deq = new ArrayDeque<>();
        deq.add(st);
        int ans = 0;
        set.remove(st);

        while (!deq.isEmpty()) {
            int sz = deq.size();
            for (int i=0;i<sz;++i) {
                int cur = deq.poll();
                System.out.println("cur="+decode(cur)+" stop="+decode(stop));
                if (cur == stop) {
                    return ans;
                }
                for (int j=0;j<16;j+=2) {
                    int bit = cur >> j & 3;
                    int zero = ~(3 << j);
                    System.out.println("zero="+decode(zero));
                    for (int k=0;k<4;++k) {
                        if (k==bit) continue;
                        int r = cur & zero | (k<<j);
                        System.out.println("r="+decode(r));
                        if (set.contains(r)) {
                            deq.add(r);
                            set.remove(r);
                        }
                    }
                }
            }
            ans++;
        }
        return -1;


    }

    static int hash(String s) {
        int code = 0;
        for (int i=0;i<s.length();++i) {
            code = code * 4 + keys.get(s.charAt(i));
        }
        return code;
    }

    static String decode(int code) {
        char[] k = "ACGT".toCharArray();
        StringBuilder ans = new StringBuilder();
        for (int i=0;i<8;++i) {
            ans.append(k[code & 3]);
            code >>= 2;
        }
        return ans.reverse().toString();
    }

    static void combinations(int[] points, int r) {
        int n = points.length;
        int[] indices = new int[r];

        for (int i=0;i<r;++i) {
            indices[i] = i;
        }
        indices[r-1]--;

        while (true) {
            int at = r-1;
            boolean found = false;
            for (;at>=0;--at) {
                if (r-at < n - indices[at]) {
                    found = true;
                    break;
                }
            }
            if (!found) break;
            indices[at]++;

            for (int i=at+1;i<r;++i) {
                indices[i] = indices[i-1] +1;
            }

            int[] comb = new int[r];
            for (int i=0;i<r;++i) {
                comb[i] = points[indices[i]];
            }

            System.out.println(Arrays.toString(comb));

        }
    }

    public static void main(String[] args) {
//        String start = "AACCGGTT", end = "AAACGGTA";
//        String[] bank = {"AACCGGTA","AACCGCTA","AAACGGTA"};
//
//        System.out.println(minMutation(start, end, bank));

        combinations(new int[]{0,1,2,3,4}, 2);
    }
}
