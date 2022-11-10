package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Symbol;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.symtab.SymbolTableEntry;


import java.util.*;

import static cn.edu.hitsz.compiler.symtab.SourceCodeType.None;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {
    // 更新后的符号表
    // 添加属性type from token
    private SymbolTable newSymbolTable = new SymbolTable();

    // 语义分析符号栈
    private Stack<SourceCodeType> semanticAnalyzerStack = new Stack<>();
    private Stack<Token> symbolStack = new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
        // TODO: 该过程在遇到 Accept 时要采取的代码动作
        return;
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
        // 1、当D->int这条产生式归约时，int这个token应该在语义分析栈中，把这个token的tpye类型赋值给D的type;
        // 2、当S->D id这条产生式归约时，取出D的type，这个type就是id的type，更新符号表中相应变量的type信息，压入空记录占位；
        // 3、如果使用其他产生式规约，直接压入空记录占位。

        // 由production序列号判断符号表的更新行为
        int number = production.index();
        switch (number) {
            case 4 -> { // S -> D id
//                System.out.println("REDUCE,我在这里呢？");
                Token id = symbolStack.pop();
                // 在符号表中找到id并将D的type赋值给id
                if (newSymbolTable.has(id.getText())) {
                    var id1 = newSymbolTable.get(id.getText());
                    id1.setType(semanticAnalyzerStack.pop());
                    // 压入空记录占位
                    semanticAnalyzerStack.push(None);
                }
            }

            case 5 ->{ // D -> int
                semanticAnalyzerStack.push(SourceCodeType.Int);
            }

            default -> {
                semanticAnalyzerStack.push(None);
            }
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作 ，将符号的type属性入栈
        // 自底向上的分析过程中Shift时将符号的type属性（token中获得）入栈
        // text
        symbolStack.push(currentToken);
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        newSymbolTable = table;
    }

    public SymbolTable getNewSymbolTable() {
        return this.newSymbolTable;
    }

}


