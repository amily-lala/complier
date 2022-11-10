package cn.edu.hitsz.compiler.ir;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.NonTerminal;
import cn.edu.hitsz.compiler.parser.table.Symbol;

public class IRSymbol {
    private Token token ;
    private IRValue irV = null;

    public IRSymbol (Token token) {
        this.token = token;
    }

    public void setIrV(IRValue irV) {
        this.irV = irV;
    }

    public IRValue getIrV() {
        return irV;
    }

    public Token getToken() {return token;}
}
