package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    /**
     * 符号表
     * token list
     */
    private final SymbolTable symbolTable;
    private final ArrayList<Token> tokenList = new ArrayList<>();
    /**
     * input_code
      */
    private String inputCode;

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) throws IOException {
        // TODO: 词法分析前的缓冲区实现
        File file = new File(path);
        BufferedReader br
                = new BufferedReader(new FileReader(file));
        // 处理字符串
        StringBuilder sb
                = new StringBuilder();
        String st;
        while ((st = br.readLine()) != null) {
            sb.append(st).append('\n');
        }
        br.close();
        inputCode = sb.toString();
//        System.out.println(inputCode);
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        // 用switch语句实现自动机
        int state = 0;
        String symbol = "";
        int len = inputCode.length();
        for (int i = 0; i < len; i++) {
            char ch = inputCode.charAt(i);
            System.out.println(ch+" "+state);
            switch (state) {
                // 初始状态
                case 0 :
                    if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_') {
                        symbol += ch;
                        state = 1;
                    } else if (ch >= '0' && ch <= '9') {
                        symbol += ch;
                        state = 2;
                    } else if (ch == ';') {
                    tokenList.add(Token.simple("Semicolon"));
                    } else if (ch == '+') {
                        tokenList.add(Token.simple("+"));
                    } else if (ch == '-') {
                        tokenList.add(Token.simple("-"));
                    } else if (ch == '*') {
                        tokenList.add(Token.simple("*"));
                    } else if (ch == '/') {
                        tokenList.add(Token.simple("/"));
                    } else if (ch == ')') {
                        tokenList.add(Token.simple(")"));
                    } else if (ch == '(') {
                        tokenList.add(Token.simple("("));
                    } else if (ch == '=') {
                        tokenList.add(Token.simple("="));
                    } else if (ch == ',') {
                        tokenList.add(Token.simple(","));
                    } else {
                        state = 0;
                    }
                    break;
                // 标识符
                case 1 :
                    if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_' || (ch >= '0' && ch <= '9')) {
                        symbol += ch;
                        state = state;
                    // 加入tokenlist，并重置状态state=0,symbol=“”
                    } else {
                        if (symbol.equals("int")) {
                            tokenList.add(Token.simple("int"));
                        } else if (symbol.equals("return")) {
                            tokenList.add(Token.simple("return"));
                        } else {
                            tokenList.add(Token.normal("id",symbol));
                            // 检查符号表中是否存在，否则添加
                            if (!symbolTable.has(symbol)) {
                                symbolTable.add(symbol);
                            }
                        }
                        // 重置
                        state = 0;
                        symbol = "";

                        if (ch == ';') {
                            tokenList.add(Token.simple("Semicolon"));
                        } else if (ch == '+') {
                            tokenList.add(Token.simple("+"));
                        } else if (ch == '-') {
                            tokenList.add(Token.simple("-"));
                        } else if (ch == '*') {
                            tokenList.add(Token.simple("*"));
                        } else if (ch == '/') {
                            tokenList.add(Token.simple("/"));
                        } else if (ch == ')') {
                            tokenList.add(Token.simple(")"));
                        } else if (ch == '(') {
                            tokenList.add(Token.simple("("));
                        } else if (ch == '=') {
                            tokenList.add(Token.simple("="));
                        } else if (ch == ',') {
                            tokenList.add(Token.simple(","));
                        }
                    }
                    break;
                // 数字
                case 2 :
                    if (ch >= '0' && ch <= '9') {
                        symbol += ch;
                        state = state;
                    } else {
                        tokenList.add(Token.normal("IntConst",symbol));

                        state = 0;
                        symbol = "";
                        if (ch == ';') {
                            tokenList.add(Token.simple("Semicolon"));
                        } else if (ch == '+') {
                            tokenList.add(Token.simple("+"));
                        } else if (ch == '-') {
                            tokenList.add(Token.simple("-"));
                        } else if (ch == '*') {
                            tokenList.add(Token.simple("*"));
                        } else if (ch == '/') {
                            tokenList.add(Token.simple("/"));
                        } else if (ch == ')') {
                            tokenList.add(Token.simple(")"));
                        } else if (ch == '(') {
                            tokenList.add(Token.simple("("));
                        } else if (ch == '=') {
                            tokenList.add(Token.simple("="));
                        } else if (ch == ',') {
                            tokenList.add(Token.simple(","));
                        }
                    }
                    break;
                default:
                    state = state;
            }
        }
        tokenList.add(Token.eof());
//        System.out.println(tokenList);
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokenList;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
