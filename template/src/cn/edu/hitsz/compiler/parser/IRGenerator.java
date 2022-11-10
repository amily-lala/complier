package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRSymbol;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Symbol;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.symtab.SymbolTableEntry;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    private SymbolTable symbolTable = new SymbolTable();
    // 中间代码
    private List<Instruction> irList = new ArrayList<>();
    private Stack<IRSymbol> symbolStack = new Stack<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        IRSymbol irSymbol = new IRSymbol(currentToken);
        symbolStack.push(irSymbol);
//        System.out.println(symbolStack.size());
//        if (currentToken.getKind().equals("id")) {
//            irSymbol.setIrV(IRVariable.named(currentToken.getText()));
//            System.out.println(IRVariable.named(currentToken.getText()));
//        }
//        if (currentToken.getKind().equals("IntConst")) {
//            irSymbol.setIrV(IRImmediate.of(Integer.parseInt(currentToken.getText())));
//        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO
//        List<IRSymbol> temp = new ArrayList<>();
//        int len = production.body().size();
//        // 产生式右部全部出栈，临时保存
//        System.out.println("REDUCE:LALALAL");
//        System.out.println(symbolStack.size());
//        System.out.println("len:"+len);
//        for (int i = 0; i < len; i++) {
//            temp.add(symbolStack.peek());
//            symbolStack.pop();
//        }
//        System.out.println(symbolStack.size());

        switch (production.index()) {
            case 6 -> { //S -> id = E; Mov
//                System.out.println(symbolStack.peek().getToken().getText());
                IRSymbol E = symbolStack.pop();
//                System.out.println("E:"+E.getToken().getText());
                symbolStack.pop();
                IRSymbol id = symbolStack.pop();
                if (id.getIrV()==null) {
                    id.setIrV(IRVariable.named(id.getToken().getText()));
                }
                Instruction instruction = Instruction.createMov((IRVariable) id.getIrV(),E.getIrV());
                irList.add(instruction);
            }

            case 7 -> { //S -> return E;
                IRSymbol E = symbolStack.pop();
                Instruction instruction = Instruction.createRet(E.getIrV());
                irList.add(instruction);
            }

            case 8 -> { //E -> E + A;
                IRSymbol A = symbolStack.pop();
                symbolStack.pop();
                IRSymbol E = symbolStack.pop();
                IRVariable res = IRVariable.temp();
                Instruction instruction = Instruction.createAdd(res,E.getIrV(),A.getIrV());
                irList.add(instruction);
                E.setIrV(res);
                symbolStack.push(E);
            }

            case 9 -> { //E -> E - A;
                IRSymbol A = symbolStack.pop();
                symbolStack.pop();
                IRSymbol E = symbolStack.pop();
                IRVariable res = IRVariable.temp();
                Instruction instruction = Instruction.createSub(res,E.getIrV(),A.getIrV());
                irList.add(instruction);
                E.setIrV(res);
                symbolStack.push(E);
            }

            case 14 -> { //B -> id;
                IRSymbol A = symbolStack.pop();
                IRSymbol E = new IRSymbol(A.getToken());
                E.setIrV(IRVariable.named(symbolTable.get(A.getToken().getText()).getText()));
                symbolStack.push(E);
            }

            case 15 -> { //E -> A  //B -> id;  //B -> IntConst;
                IRSymbol A = symbolStack.pop();
                IRSymbol E = new IRSymbol(A.getToken());
                E.setIrV(IRImmediate.of(Integer.parseInt(A.getToken().getText())));
                symbolStack.push(E);
            }

            case 10, 12 -> { //E -> A  //B -> id;  //B -> IntConst;
                IRSymbol A = symbolStack.pop();
                IRSymbol E = new IRSymbol(A.getToken());
                E.setIrV(A.getIrV());
                symbolStack.push(E);
            }

            case 11 -> { //A -> A * B
                IRSymbol A = symbolStack.pop();
                symbolStack.pop();
                IRSymbol B = symbolStack.pop();
                IRVariable res = IRVariable.temp();
                Instruction instruction = Instruction.createMul(res,B.getIrV(),A.getIrV());
                irList.add(instruction);
                A.setIrV(res);
                symbolStack.push(A);
            }

            case 13 -> { //B -> ( E );
                symbolStack.pop();
                IRSymbol E = symbolStack.pop();
                symbolStack.pop();
                IRSymbol B = new IRSymbol(E.getToken());
                B.setIrV(E.getIrV());
                symbolStack.push(B);
            }

            default -> {
                symbolStack.pop();
            }

        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO

    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        symbolTable = table;
    }

    public List<Instruction> getIR() {
        // TODO
        return irList;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

