package leetcode;

import java.util.*;

public class MySort {
    /**
     * 原地排序，不稳定，因为每次把最小值交换到前面时，次序不确定，有可能两个相同的元素交换后顺序变了
     *      * [3a,3b,1]->[1,3b,3a],两个3位置变了
     * @param arr 待排序数组，会修改arr
     * @return arr
     */
    public int[] selectSort(int []arr) {
        for(int i=0;i<arr.length-1;++i) {
            int select = i;
            for (int j=i+1;j<arr.length;++j) {
                if(arr[j]<arr[select]) select = j;
            }
            if(select != i) {
                int tmp=arr[i];arr[i] = arr[select];arr[select]=tmp;
            }
        }
        return arr;
    }

    /**
     * 冒泡排序，原地，稳定，需要频繁的交换，每次把较大值往后移动1位
     * @param arr 变
     * @return arr
     */
    public int[] bubbleSort(int[] arr) {
        for(int i=0;i<arr.length-1;++i) {
            boolean bubble = false;
            for(int j=0;j<arr.length-1-i;++j) {
                if(arr[j]<arr[j+1]){
                    int tmp=arr[j];arr[j] = arr[j+1];arr[j+1]=tmp;
                    bubble = true;
                }
            }
            if(!bubble) break;
        }
        return arr;
    }

    /**
     * 将数据以gap间隔插入排序,注意必须保证gap=1时才能有序
     * @param arr 变
     * @param gap 间隔
     * @return arr
     */
    private int[] insertSort(int[] arr, int gap) {
        for(int i=gap;i<arr.length;i+=gap) {
            int cur = arr[i];
            int preIndex = i - gap;
            while (preIndex >= 0 && arr[preIndex]>cur){
                arr[preIndex+gap] = arr[preIndex];
                preIndex -= gap;
            }
            arr[preIndex] = cur;
        }
        return arr;
    }
    public int[] insertSort(int[]arr) {
        return insertSort(arr, 1);
    }
    public int[] shellSort(int[] arr){
        int gap = arr.length >> 1;
        while (gap > 0) {
            insertSort(arr, gap);
            gap >>= 1;
        }
        return arr;
    }

    private int[] merge(int [] res, int a, int len, int []arr1, int a1, int len1, int[] arr2, int a2, int len2){
        if(len<len1+len2){
            return res;
        }
        int p=a, p1=a1, p2=a2;
        while (p1<len1+a1 && p2<len2+a2) {
            if(arr1[p1]<arr2[p2]) {
                res[p++] = arr1[p1++];
            }
            else {
                res[p++] = arr2[p2++];
            }
        }
        for(;p1<len1+a1;++p1){
            res[p++] = arr1[p1];
        }
        for(;p2<len2+a2;++p2){
            res[p++] = arr2[p2];
        }

        return res;
    }
    public int[] merge(int[] arr1, int[] arr2) {
        int []res = new int[arr1.length+arr2.length];
        return merge(res, 0, res.length, arr1, 0, arr1.length, arr2, 0, arr2.length);
    }

    /**
     * 递归版本归并排序，空间复杂度O(n),需要一个临时数组存储归并后的值
     * @param arr 变
     * @param left 起点
     * @param right 结束位置+1
     * @param tmp 临时数组
     */
    private void mergeSort(int[] arr, int left, int right, int []tmp) {
        if(right-left<=1) {
            return;
        }
        int mid = (left+right) >> 1;
        mergeSort(arr, left, mid, tmp);
        mergeSort(arr, mid, right, tmp);
        merge(tmp, left, right-left, arr, left, mid-left, arr, mid, right-mid);
        System.arraycopy(tmp, left, arr, left, right - left);

    }
    public int[] mergeSort(int[] arr) {
        int []res = new int[arr.length];
        mergeSort(arr, 0, arr.length,res);
        return res;
    }

    public static class HeapSort {
        public static void heapify(int []arr) {
            int len = arr.length;
            for(int i=(len>>1)-1;i>=0;--i) {
                heapDown(arr, len, i);
            }
        }
        public static void heapDown(int[] arr, int length, int pos) {
            int tmp = arr[pos];
            int j = (pos<<1)+1;
            while (j<length) {
                if(j+1<length && arr[j+1]>arr[j]) {
                    // 调整到右节点
                    j++;
                }
                if(arr[j]<=tmp){
                    break;
                }
                arr[pos] = arr[j];
                pos = j;
                j = (j<<1)+1;
            }
            arr[pos] = tmp;
        }
        public static void sort(int []arr) {
            heapify(arr);
            for(int len=arr.length-1; len>0;--len) {
                int tmp = arr[0];arr[0] = arr[len];arr[len]=tmp;
                heapDown(arr, len, 0);
            }
        }

    }

    private void quickSort(int[] arr, int left, int right) {
        if(left>=right) return;
        int pivot = arr[left], low=left, high=right;
        boolean lt = true;

        while (low < high) {
            if(lt && arr[high--]<pivot) {
                arr[low++] = arr[++high];
                lt = false;
            }
            else if(!lt && arr[low++]>pivot){
                arr[high--] = arr[--low];
                lt = true;
            }
        }
        arr[low] = pivot;
        quickSort(arr, left, low-1);
        quickSort(arr, low+1, right);
    }
    public void quickSort(int[] arr) {
        quickSort(arr, 0, arr.length-1);
    }



    public static void main(String[] args) {
        int [] arr = {4,1,2,3,1,5,4,8,6};
//        System.out.println(Arrays.toString(new MySort().mergeSort(arr)));
//        HeapSort.sort(arr);
        System.out.println(Arrays.toString(arr));
        new MySort().quickSort(arr);
        System.out.println(Arrays.toString(arr));
    }
}
