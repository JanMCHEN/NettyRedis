package leetcode;

import java.util.*;

class TreeNode {
    // 数据简单定义成int，不用泛型即Object避免繁琐
    int val;
    TreeNode left;
    TreeNode right;
    TreeNode(int val) {
        this.val = val;
    }
    TreeNode(int val, TreeNode left, TreeNode right){
        this.val = val;
        this.left = left;
        this.right = right;
    }
}

public class BinaryTree {
    private final TreeNode root;
    private int count;  // 节点数量
    public BinaryTree(TreeNode root) {
        this.root = root;
        if(root != null){
            count = 1;
        }
    }
    public boolean isEmpty() {
        return count == 0;
    }

    public int[] layerOrder() {
        if(isEmpty()) return new int[0];
        int []ans = new int[count];
        Deque<TreeNode> deque = new ArrayDeque<>();
        TreeNode node = root;
        deque.addLast(node);
        int i = 0;
        while(!deque.isEmpty()) {
            node = deque.pop();
            ans[i++] = node.val;
            if(node.left!=null) deque.addLast(node.left);
            if(node.right!=null) deque.addLast(node.right);
        }
        return ans;

    }

    public List<Integer> inOrder (TreeNode root) {
        if(root==null){
            return null;
        }
        Deque<TreeNode> stack = new ArrayDeque<>();
        List<Integer> res = new LinkedList<>();
        while(root!=null || !stack.isEmpty()){
            if(root != null){
                stack.push(root);
                root = root.left;
                continue;
            }
            root = stack.pop();
            res.add(root.val);
            root = root.right;
        }
        return res;
    }
    public List<Integer> preOrder (TreeNode root) {
        if(root==null){
            return null;
        }
        Deque<TreeNode> stack = new ArrayDeque<>();
        List<Integer> res = new LinkedList<>();
        while(root!=null || !stack.isEmpty()){
            if(root != null){
                res.add(root.val);
                stack.push(root);
                root = root.left;
                continue;
            }
            root = stack.pop();
            root = root.right;
        }
        return res;
    }
    public List<Integer> postOrder (TreeNode root) {
        if(root==null){
            return null;
        }
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode lastVisited = null;
        List<Integer> res = new LinkedList<>();
        while(root!=null || !stack.isEmpty()){
            if(root != null){
                stack.push(root);
                root = root.left;
                continue;
            }
            root = stack.peek();
            if(root.right==null || root.right==lastVisited){
                res.add(root.val);
                lastVisited = root;
                stack.pop();
                root = null;
            }
            else{
                root = root.right;
            }
        }
        return res;
    }



    @Override
    public String toString() {
        return Arrays.toString(layerOrder());
    }

    public static BinaryTree createTree(int[] array) {
        int len = array.length;
        TreeNode root = null;
        BinaryTree bt;
        if(len>0){
            root = new TreeNode(array[0]);
        }
        bt = new BinaryTree(root);
        if(root == null) return bt;
        ArrayDeque<TreeNode> treeNodes = new ArrayDeque<>();
        treeNodes.addLast(root);
        for(int i=1;i<len;++i) {
            if(treeNodes.isEmpty()) break;
            root = treeNodes.pop();
            root.left = new TreeNode(array[i]);
            bt.count++;
            treeNodes.addLast(root.left);
            if(i+1<len) {
                root.right = new TreeNode(array[++i]);
                bt.count ++;
                treeNodes.addLast(root.right);
            }
        }
        return bt;
    }

    public static void main(String[] args) {
        int[] arr = {0,1,2,3,4,5,6};
        BinaryTree tree = createTree(arr);
        System.out.println(tree);

    }
}
