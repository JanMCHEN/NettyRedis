package core;

import java.util.Random;

/**

 * */
public class SkipList <T> {
    private static final int MAX_LEVEL = 62;  // long类型总共64位，除去先验1/4判断用掉两位，最多能表示62位的概率
    static final class Node<T> {
        final int key;
        T val;
        Node<T> next;
        Node(int key, T value, Node<T> next) {
            this.key = key;
            this.val = value;
            this.next = next;
        }
    }
    static final class Index<T> {
        Node<T> node;  // currently, never detached
        final Index<T> down;
        Index<T> right;
        Index(Node<T> node, Index<T> down, Index<T> right) {
            this.node = node;
            this.down = down;
            this.right = right;
        }
    }
    private static final Random RANDOM = new Random();

    private Index<T> head;
    private long length;

    public SkipList() {
    }
    public boolean add(int key, T value) {
        Node<T> node = new Node<>(key, value, null);
        length ++;  // always success
        if(this.head==null) {
            // first add
            Node<T> baseNode = new Node<>(Integer.MAX_VALUE, null, node);
            head = new Index<>(baseNode, null, null);
            return true;
        }
        int level = 0;      // 当前层为0层
        Index<T> cur = this.head;
        for (Index<T> next;;) {
            while ((next = cur.right) != null) {
                if (key >= next.node.key)
                    cur = next;
                else
                    break;
            }
            if(cur.down!=null) { // 下边还有层
                cur = cur.down;
                level ++;
            }
            else {
                break;
            }
        }
        insert(cur, node);
        newIndex(node, level);
        return true;
    }
    public boolean remove(int key, T value) {
        Index<T> cur = this.head;
        for (Index<T> next;;) {
            while ((next = cur.right) != null) {
                if(key < next.node.key)
                    break;
                if (key > next.node.key)
                    cur = next;
                else if(next.node.val.equals(value)) {  // key&&val 相等
                    doRemove(cur);
                    return true;
                }
                else                                       // 值不相等只能比较下一级
                    break;
            }
            if(cur.down!=null) { // 下边还有层
                cur = cur.down;
            }
            else
                break;
        }
        // 没有找到，需要从cur的node节点开始找,只需删除node
        for(Node<T> c=cur.node; c.next!=null; c=c.next) {
            if(c.next.key > key)
                break;
            if(c.next.key==key && c.next.val.equals(value)) {
                c.next = c.next.next;
                if(cur.node==head.node) { // 删除的是第一个节点
                    cur = this.head;
                    while(cur != null) {       // 删除头节点时直接修改next指针有可能right有重复值
                        if(cur.right!=null && cur.node==cur.right.node) {
                            cur.right = cur.right.right;
                            break;
                        }
                        if(cur.right==null) {
                            cur = cur.down;
                        }
                        else {
                            cur = cur.right;
                        }
                    }
                    tryReduce();
                }
                length --;
                return true;
            }
        }

        return false;
    }
    private void doRemove(Index<T> pre) {
        Index<T> del = pre.right;
        Index<T> cur=pre;
        Node<T> delNode = del.node;
        for(Index<T> next=pre; next != null; next=next.down,del=del.down) {   // 向下 删除
            while (next.right !=del) {
                cur = next;
                next = next.right;
            }
            next.right = del.right;

        }
        // unlink node
        Node<T> n=cur.node;
        while (n.next != delNode) {
            n = n.next;
        }
        n.next = n.next.next;
        length--;
        tryReduce();

    }
    private void tryReduce() {
        Index<T> head = this.head;
        while (head != null && head.right==null && head.down != null) {      // 降级
            head = head.down;
        }
        this.head = head;
    }
    private Index<T> findPredecessor(int key) {
        Index<T> cur = this.head;
        for (Index<T> next;;) {
            while ((next = cur.right) != null) {
                if (key >= next.node.key)
                    cur = next;
                else
                    break;
            }
            if(cur.down!=null) { // 下边还有层
                cur = cur.down;
            }
            else {
                break;
            }
        }
        return cur;
    }

    private void insert(Index<T> index, Node<T> node) {
        Node<T> cur = index.node;
        while(cur.next !=null) {
            if(cur.next.key > node.key){
                break;
            }
            cur = cur.next;
        }
        node.next = cur.next;
        cur.next = node;

    }
    private void newIndex(Node<T> z, int level) {
        int lr = RANDOM.nextInt();
        Index<T> x = null;
        if((lr & 0x03) == 0) {   // 1/4 prob
            int hr = RANDOM.nextInt();
            long rnd = ((long)hr << 32) | ((long)lr & 0xffffffffL);  // 两个int拼接成long
            for (;;) {               // create at most 62 indices
                x = new Index<T>(z, x, null);
                if (rnd >= 0L || --level < 0)       // 1/2 prob，每次左移相当于判断最高位是否为1，1则是负数
                    break;
                else                                // 抽中1，同时判断是否到最顶端，往下走，左移
                    rnd <<= 1;
            }
        }
        if(x == null) return;
        addLevel(x, level);

    }
    private void addLevel(Index<T> newIndex, int skip) {
        Index<T> cur = head;
        int key = newIndex.node.key;
        if(skip == -1) { // 更新head
            head = new Index<>(cur.node, cur, null);
        }
        for (Index<T> next;;) {
            while ((next = cur.right) != null) {
                if (key >= next.node.key)
                    cur = next;
                else
                    break;
            }
            if (skip > 0) {
                skip--;
            } else { // 更新right指针
                Index<T> right = cur.right;
                cur.right = newIndex;
                newIndex.right = right;
                newIndex = newIndex.down;
            }
            if((cur = cur.down) == null) {
                assert newIndex == null;   // 确保操作正确
                break;
            }
        }
    }
    public int getLevel() {
        return getLevel(this.head);
    }
    public int getLevel(Index<T> head) {
        int level = 0;
        for (Index<T> cur=head;cur!=null;cur=cur.down) {
            level++;
        }
        return level;
    }
    public void usageLevel() {
        if(head==null || head.node.next==null){
            System.out.println("Empty");
            return;
        }
        int level = getLevel();
        StringBuilder ans = new StringBuilder();
        for (Index<T> cur=head;cur!=null;cur=cur.down) {
            ans.append("level=").append(level--).append(":[");
            int count = 0;
            for (Index<T> cur1=cur;cur1!=null;cur1=cur1.right) {
                if(count++>10) {
                    continue;
                }
                Node<T> node = cur1.node;
                if(count==1) {
                    node = node.next;
                }
                ans.append(node.val).append(",");
            }
            ans.append("]").append(count).append("\n");
        }
        ans.append("level=").append(level).append(":[");
        int count = 0;
        assert head != null;
        for(Node<T> node = head.node.next; node !=null; node=node.next) {
            if(count++>10) {
                continue;
            }
            ans.append(node.val).append(",");
        }
        ans.append("]").append(count).append("\n");
        System.out.println(ans);
    }
    public long length() {
        return length;
    }
    public boolean isEmpty() {
        return length == 0;
    }
    public int countBetween(int start, int stop) {
        if(isEmpty()) return 0;
        Node<T> node = findPredecessor(start).node;
        int count = 0;
        while(node != null && node.key < start) {
            node = node.next;
        }
        while (node != null && node.key <= stop) {
            node = node.next;
            count++;
        }
        return count;
    }


    @Override
    public String toString() {
        if(head==null){
            return "";
        }
        StringBuilder ans = new StringBuilder();
        Node<T> node = head.node;
        while(node != null) {
            ans.append(node.key).append(":").append(node.val);
            node = node.next;
            if(node==null) break;
            ans.append(", ");
        }
        return ans.toString();
    }

    public static void main(String[] args) {
        SkipList<Integer> skipList = new SkipList<>();
        for(int i=0;i<1000;++i) {
            skipList.add(RANDOM.nextInt(), i);
        }
    }

}
