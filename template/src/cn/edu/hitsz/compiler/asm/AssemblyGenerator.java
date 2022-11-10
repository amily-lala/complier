package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    // 中间代码
    List<Instruction> immeCode = null;
    // 目标代码
    List<String> asmCode = new ArrayList<>();
    // 寄存器占用信息 : 双向映射BiMap实现
    BiMap<IRValue,Reg> regBiMap = new BiMap<>();


    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */

    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        List<Instruction> origin = originInstructions;

        /** 中间代码预处理 */
        // 对于 BinaryOp(两个操作数的指令):
        //  将操作两个立即数的 BinaryOp 直接进行求值得到结果, 然后替换成 MOV 指令
        //  将操作一个立即数的指令 (除了乘法和左立即数减法) 进行调整, 使之满足 a := b op imm 的格式
        //  将操作一个立即数的乘法和左立即数减法调整, 前插一条 MOV a, imm, 用 a 替换原立即数, 将指令调整为无立即数指令.
        //  对于 UnaryOp(一个操作数的指令):
        //  根据语言规定, 当遇到 Ret 指令后直接舍弃后续指令.
        //  考虑实现 add addi sub mul mv li 汇编指令

        int len = origin.size();
        Instruction inst = null;
        IRValue lhs = null;
        IRValue rhs = null;
        List<Instruction> temp = new ArrayList<>();

        for (int i = 0; i < len; i++) {
            inst = origin.get(i);
            // 对于 BinaryOp(两个操作数的指令)
            if (inst.getKind().isBinary()) {
                lhs = inst.getLHS();
                rhs = inst.getRHS();
                // 将操作两个立即数的 BinaryOp 直接进行求值得到结果, 然后替换成 MOV 指令
                if (lhs.isImmediate() && rhs.isImmediate()) {
                    IRValue a = null;
                    switch (inst.getKind()) {
                        case ADD -> {
                             a = IRImmediate.of(Integer.parseInt(lhs.toString()) + Integer.parseInt(rhs.toString()));
                        }
                        case MUL -> {
                             a = IRImmediate.of(Integer.parseInt(lhs.toString()) * Integer.parseInt(rhs.toString()));
                        }
                        case SUB -> {
                             a = IRImmediate.of(Integer.parseInt(lhs.toString()) - Integer.parseInt(rhs.toString()));
                        }
                    }
                    temp.add(Instruction.createMov(inst.getResult(), a));
                } else if (lhs.isImmediate() && rhs.isIRVariable()) {
                    switch (inst.getKind()) {
                        case ADD -> {
                            temp.add(Instruction.createAdd(inst.getResult(), inst.getRHS(), inst.getLHS()));
                        }
                        case MUL -> {
                            IRVariable a = IRVariable.temp();
                            temp.add(Instruction.createMov(a,inst.getLHS()));
                            temp.add(Instruction.createMul(inst.getResult(),a, inst.getRHS()));
                        }
                        case SUB -> {
                            // 将 subi 转化为 sub
                            IRVariable a = IRVariable.temp();
                            temp.add(Instruction.createMov(a,inst.getLHS()));
                            temp.add(Instruction.createSub(inst.getResult(), a, inst.getRHS()));
                        }
                        default -> {
                            temp.add(inst);
                        }
                    }
                } else if (lhs.isIRVariable() && rhs.isImmediate()) {
                    if (inst.getKind() == InstructionKind.MUL) {
                        IRVariable a = IRVariable.temp();
                        temp.add(Instruction.createMov(a, inst.getRHS()));
                        temp.add(Instruction.createMul(inst.getResult(), inst.getLHS(), a));
                    } else {
                        temp.add(inst);
                    }
                } else {
                    temp.add(inst);
                }
            } else if (inst.getKind().isUnary()) {
            // 对于 UnaryOp(一个操作数的指令)
                temp.add(inst);
                // 遇到ret指令，直接舍弃后面的指令
                if (inst.getKind().isReturn()) {
                    break;
                }
            } else {
                temp.add(inst);
            }
        }
        immeCode = temp;
//        // TODO:测试中间代码优化
//        for (Instruction inst1: immeCode) {
//            System.out.println(inst1.toString());
//        }
    }


    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */


    /**
     * 寄存器分配算法
     * @param op        待分配寄存器变量
     * @param instIndex 中间代码序列号
     * @思路 立即数不分配
     */
    public void regSelect(IRValue op,int instIndex) {
        // imm 不分配寄存器
        if (op.isImmediate()) {
            System.out.println("不需要分配寄存器");
            return;
        }

        if (op.isIRVariable()) {
            // 已分配寄存器
            if (regBiMap.containsKey(op)) {
                System.out.println("原来已分配寄存器");
                return;
            }
            // 查找空闲寄存器分配
            for (Reg reg : Reg.values()) {
                // 存在即分配
                if (regBiMap.containsValue(reg)) {
                    continue;
                }
                regBiMap.put(op,reg);
                System.out.println("已分配好寄存器");
                return;
            }
            System.out.println("无空余寄存器,清理不再使用的寄存器ing");
            // 否则清理不再使用的寄存器，再分配
            // TODO:处理空闲寄存器不足

            // must 后续过程中不再使用到的变量
            // 通过查找后续需要使用到的变量，然后在寄存器中删掉他们的值，剩下的就是不再需要使用的寄存器了
            Set<Reg> recycle = new HashSet<>();
            // 初始化，假设所有可回收
            recycle = Arrays.stream(Reg.values()).collect(Collectors.toSet());
            int len = immeCode.size();
            for (int i = instIndex; i < len; i++) {
                Instruction instruction = immeCode.get(i);
                for (IRValue irValue:instruction.getOperands()) {
                    if (regBiMap.containsKey(irValue)) {
                        recycle.remove(regBiMap.getByK(irValue));
                    }
                }
            }
            if(!recycle.isEmpty()) {
                Reg temp = recycle.iterator().next();
                regBiMap.put(op,temp);
                recycle.remove(temp);
                System.out.println("清理完成，找到可用寄存器，已分配好！");
                return;
            }
            System.out.println("抱歉，真的没有可用寄存器了，你再想想办法吧！");
        }
    }

    /**
     * 为运算类型生成字符串
     * @param inst 中间代码
     * @return 中间代码的类型
     * @目的  避免run中大量的重复代码
     */
    public String instType(Instruction inst) {
        Map<InstructionKind,String> instTypeMap = new HashMap<>();
        instTypeMap.put(InstructionKind.ADD,"add");
        instTypeMap.put(InstructionKind.SUB,"sub");
        instTypeMap.put(InstructionKind.MUL,"mul");
        instTypeMap.put(InstructionKind.MOV,"mv");
        instTypeMap.put(InstructionKind.RET,"mv");
        return instTypeMap.get(inst.getKind());
    }

    /**
     * 执行代码生成
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        int len = immeCode.size();

        // RegSelect 寄存器分配算法已写好
        // 目标代码：第一行确定
        asmCode.add(".text");

        Instruction inst = null;
        // 值
        IRValue res = null;
        IRValue lhs = null;
        IRValue rhs = null;
        IRValue from = null;
        // 寄存器分配
        Reg resReg = null;
        Reg lhsReg = null;
        Reg rhsReg = null;
        Reg fromReg = null;
        // 目标代码
        String asmCodeTemp = null;

        for (int i = 0; i < len; i++) {
            inst = immeCode.get(i);
            switch (inst.getKind()) {
                // add & addi
                // sub
                case ADD,SUB -> {
                    // 得到值
                    res = inst.getResult();
                    lhs = inst.getLHS();
                    rhs = inst.getRHS();
                    // 分配寄存器
                    regSelect(res,i);
                    regSelect(lhs,i);
                    regSelect(rhs,i);
                    // 查看寄存器
                    resReg = regBiMap.getByK(res);
                    lhsReg = regBiMap.getByK(lhs);
                    rhsReg = regBiMap.getByK(rhs);
                    // 生成目标代码 ：制表符空格
                    if (rhs.isImmediate()) {
                        // addi 指令
                        asmCodeTemp = "\t%si %s,%s,%s".formatted(instType(inst),resReg.toString(),lhsReg.toString(),rhs.toString());
                    } else {
                        // add 指令
                        asmCodeTemp = "\t%s %s,%s,%s".formatted(instType(inst),resReg.toString(),lhsReg.toString(),rhsReg.toString());
                    }
                }

                // mul 指令
                case MUL -> {
                    // 得到值
                    res = inst.getResult();
                    lhs = inst.getLHS();
                    rhs = inst.getRHS();
                    // 分配寄存器
                    regSelect(res,i);
                    regSelect(lhs,i);
                    regSelect(rhs,i);
                    // 查看寄存器
                    resReg = regBiMap.getByK(res);
                    lhsReg = regBiMap.getByK(lhs);
                    rhsReg = regBiMap.getByK(rhs);

                    asmCodeTemp = "\t%s %s,%s,%s".formatted(instType(inst),resReg.toString(),lhsReg.toString(),rhsReg.toString());
                }

                // mv li
                case MOV ->{
                    // 得到值
                    res = inst.getResult();
                    from = inst.getFrom();
                    // 分配寄存器
                    regSelect(res,i);
                    regSelect(from,i);
                    // 查看寄存器
                    resReg = regBiMap.getByK(res);
                    fromReg = regBiMap.getByK(from);
//                    System.out.println(resReg);
//                    System.out.println(res.isIRVariable());
                    if (from.isImmediate()) {
                        // li
                        asmCodeTemp = "\tli %s,%s".formatted(resReg.toString(),from.toString());
                    } else {
                        // mv
                        asmCodeTemp = "\tmv %s,%s".formatted(resReg.toString(),fromReg.toString());
                    }
                }

                case RET -> {
                    Reg ret = regBiMap.getByK(inst.getReturnValue());
                    asmCodeTemp = "\tmv a0,%s".formatted(ret.toString());
                }
            }
            // 注释
            String annotation = "\t# "+inst.toString();
            asmCodeTemp = asmCodeTemp+annotation;
            // TODO:测试
            System.out.println(asmCodeTemp);
            asmCode.add(asmCodeTemp);
        }
//        for(String i:asmCode) {
//            System.out.println(i);
//        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        FileUtils.writeLines(
                path,
                asmCode.stream().toList()
        );
    }
}

