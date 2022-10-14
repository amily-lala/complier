package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.*;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.stream.StreamSupport;

import static cn.edu.hitsz.compiler.utils.FilePathConfig.PARSER_PATH;

//TODO: 实验二: 实现 LR 语法分析驱动程序

/**
 * LR 语法分析驱动程序
 * <br>
 * 该程序接受词法单元串与 LR 分析表 (action 和 goto 表), 按表对词法单元流进行分析, 执行对应动作, 并在执行动作时通知各注册的观察者.
 * <br>
 * 你应当按照被挖空的方法的文档实现对应方法, 你可以随意为该类添加你需要的私有成员对象, 但不应该再为此类添加公有接口, 也不应该改动未被挖空的方法,
 * 除非你已经同助教充分沟通, 并能证明你的修改的合理性, 且令助教确定可能被改动的评测方法. 随意修改该类的其它部分有可能导致自动评测出错而被扣分.
 */
public class SyntaxAnalyzer {
    /**
     * 1.0 符号表 ；
     * 2.0 LexicalAnalyzer得到的tokenlist；
     * 3.0 lrTable：已给
     * 4.0 parser_list;先存储在一个字符串中，最后写入文件PARSER_PATH
     */
    private final SymbolTable symbolTable;
    private List<Token> tokenList = new ArrayList<>();
    private LRTable lrTable = null;
    private final List<ActionObserver> observers = new ArrayList<>();
//    private List<String> parseList = new ArrayList<>();

    public SyntaxAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 注册新的观察者
     *
     * @param observer 观察者
     */
    public void registerObserver(ActionObserver observer) {
        observers.add(observer);
        observer.setSymbolTable(symbolTable);
    }

    /**
     * 在执行 shift 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param currentToken  当前词法单元
     */
    public void callWhenInShift(Status currentStatus, Token currentToken) {
        for (final var listener : observers) {
            listener.whenShift(currentStatus, currentToken);
        }
    }

    /**
     * 在执行 reduce 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param production    待规约的产生式
     */
    public void callWhenInReduce(Status currentStatus, Production production) {
        for (final var listener : observers) {
            listener.whenReduce(currentStatus, production);
        }
    }

    /**
     * 在执行 accept 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     */
    public void callWhenInAccept(Status currentStatus) {
        for (final var listener : observers) {
            listener.whenAccept(currentStatus);
        }
    }

    public void loadTokens(Iterable<Token> tokens) {
        // TODO: 加载词法单元
        // 你可以自行选择要如何存储词法单元, 譬如使用迭代器, 或是栈, 或是干脆使用一个 list 全存起来
        // 需要注意的是, 在实现驱动程序的过程中, 你会需要面对只读取一个 token 而不能消耗它的情况,
        // 在自行设计的时候请加以考虑此种情况
        for (Token token : tokens) {
            tokenList.add(token);
        }
    }

    public void loadLRTable(LRTable table) {
        // TODO: 加载 LR 分析表
        // 你可以自行选择要如何使用该表格:
        // 是直接对 LRTable 调用 getAction/getGoto, 抑或是直接将 initStatus 存起来使用
        this.lrTable = table;
    }

    public void run() {
        // TODO: 实现驱动程序
        // 你需要根据上面的输入来实现 LR 语法分析的驱动程序
        // 请分别在遇到 Shift, Reduce, Accept 的时候调用上面的 callWhenInShift, callWhenInReduce, callWhenInAccept
        // 否则用于为实验二打分的产生式输出可能不会正常工作
        // TODO:查goto表两种方法均可：
        // 1.0 lrTable(status,token)
        // 2.0 status.getGoto(symbols.peek().getNonTerminal)

        /**
         * 符号栈和状态栈 两个栈的动作（长度）要保持一致
         * 初始状态 ： 符号栈 $ ; 状态栈 0
         * 也许可以用一个新的结构，将符号和状态 合并成一个栈
         */

        // 符号栈
        Stack<Symbol> symbols = new Stack<>();
        symbols.add(new Symbol(Token.eof()));
        // 状态栈
        Stack<Status> statuses = new Stack<>();
        statuses.push(lrTable.getInit());


        /**
         * 驱动
         * 将归约时的结果输入到文件中 PARSER_PATH
         */
        for (Token token : tokenList) { // 输入缓冲区
            // 测试用
//            if (token.getKind().toString().equals("$"))
//                System.out.println(token.getKind());
            boolean flag = false; // 判断输入缓冲区指针是否右移
            while (!flag) {
                // method 1
                Status status = statuses.peek(); // 当前符号栈栈顶状态
//                Action action = status.getAction(token);
                // method 2 (TODO：不知道这两种方法的区别在什么地方，还要读读原码~emmm,好像都可以诶！)
                Action action = lrTable.getAction(status,token);

                switch (action.getKind()) {
                    case Shift -> {
                        // shifti ：移进后跳转到新的状态
                        Status shifti = action.getStatus();
                        // 状态 和 符号 入栈
                        symbols.push(new Symbol(token));
                        statuses.push(shifti);
                        callWhenInShift(shifti, token);
                        flag = true;
                        System.out.println("Shift :"+shifti);
                    }

                    case Reduce -> {
                        // 按 ri 进行归约
                        Production production = action.getProduction(); // 归约时用到的第i个产生式

                        // 产生式右部的符号全部弹出栈（状态栈同样操作）
                        for (Term term : production.body()) {
                            symbols.pop();
                            statuses.pop();
                        }

                        // 产生式左部的符号入栈
                        symbols.push(new Symbol(production.head()));

                        // 查 goto 表 找到 将要跳转到的状态 ；状态入栈
                        // method 2
                        Status nstatus = statuses.peek();
                        callWhenInReduce(nstatus, production);
                        Status ri = lrTable.getGoto(nstatus,production.head());
                        statuses.push(ri);

                        // 打印归约式 并 写入文件
                        System.out.println("Reduce:"+production);
//                        parseList.add(production.toString());
                    }

                    case Accept -> {
                        callWhenInAccept(status);
                        System.out.println("Congradulation : Accept!");
                        /**
                         * 将归约过程写入文件中：parse_list.txt
                         */
//                        FileUtils.writeLines(PARSER_PATH,parseList);
                        return;
                    }

                    case Error -> {
                        System.out.println("Error!");
                        /**
                         * 将归约过程写入文件中：parse_list.txt
                         */
//                        FileUtils.writeLines(PARSER_PATH,parseList);
                        return;
                    }

                    default -> {
                        return;
                    }
                }
            }
        }

    }
}
