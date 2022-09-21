package xyz.chenjm.redis.command;

import xyz.chenjm.redis.core.RedisClient;
import xyz.chenjm.redis.exception.WrongNumberCommandErr;

public class Command2 implements CommandRunner{
    CommandRunner runner;
    int flags;

    String name;
    String desc;

    /* 参数数量， -N means >= N */
    int argNums;

    public void setArgNums(int argNums) {
        this.argNums = argNums;
    }

    public void setRunner(CommandRunner runner) {
        this.runner = runner;
    }

    /**
     * 只读的命令，不会修改数据库
     */
    public boolean readonly() {
        return (flags & 1) == 1;
    }

    public void readonly(boolean readonly) {
        flags |= 1;
    }

    /**
     * 事务开启后，应该返回queued加入命令链表，还是直接执行
     * @return true如果需要加入链表，否则返回false;目前只有multi,exec,watch,discard为false
     */
    public boolean multi() {
        return (flags & 2) > 0;
    }

    public void multi(boolean multi) {
        flags |= 2;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void checkArgs(String... args) {
        int n = args.length - 1;
        if (argNums > 0 && n == argNums || (argNums < 0 && n+argNums>=0)) {
            return;
        }
        throw new WrongNumberCommandErr(args[0]);
    }

    public CommandRunner getRunner() {
        return runner;
    }

    @Override
    public Object invoke(RedisClient client, String... args) {
        return runner.invoke(client, args);
    }
}
