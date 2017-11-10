package core;

import core.Tokenizer.Token;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Interpreter {

    // Indent string used for pretty printing
    private static final String singleIndent = "    ";

    private Tokenizer tokenizer;
    private StringBuilder inputData;
    private Program program;

    private Map<String, Integer> idValues;

    public Interpreter(Tokenizer tokenizer, StringBuilder inputData) {
        this.tokenizer = tokenizer;
        this.inputData = inputData;
        idValues = new HashMap<>();
        program = new Program();

        // Parse program
        try {
            program.parse();
        } catch (Exception e) {
            System.out.println("Error while parsing program (at token #" + tokenizer.getTokenNumber() + "):");
            e.printStackTrace();
            return;
        }

        System.out.println("========== INPUT DATA ==========\n");
        System.out.println(inputData);

        System.out.println("\n========= PRETTY PRINT =========\n");

        // Print program
        program.print("");

        System.out.println("\n========== EXECUTION ===========\n");

        // Execute program
        try {
            program.execute();
        } catch (Exception e) {
            System.out.println("Runtime error in program execution:");
            e.printStackTrace();
            return;
        }
        System.out.println("\nSuccess!");
    }

    // Utility methods to make using tokenizer simpler

    public void skipToken() throws Exception {
        tokenizer.skipToken();
    }

    public boolean checkToken(CoreToken... tokens) {
        return Arrays.asList(tokens).stream().anyMatch(token -> token.token.getId() == tokenizer.getToken());
    }

    public boolean checkAndSkip(CoreToken... tokens) throws Exception {
        if (checkToken(tokens)) {
            skipToken();
            return true;
        }
        return false;
    }

    public void expectToken(CoreToken... tokens) throws Exception {
        if (!checkToken(tokens)) {
            String ids = String.join(",", Arrays.asList(tokens).stream().map(token -> token.token.getId() + "").collect(Collectors.toList()));
            throw new Exception("Expected token of id(s) " + ids + ", got token of id " + tokenizer.getToken());
        }
    }

    public void skipAndExpect(CoreToken... tokens) throws Exception {
        skipToken();
        expectToken(tokens);
    }

    public void expectAndSkip(CoreToken... tokens) throws Exception {
        expectToken(tokens);
        skipToken();
    }

    // Object oriented parse tree classes

    private interface Node {
        void parse() throws Exception;

        void print(String indent);
    }

    private class Program implements Node {
        private DeclarationSequence ds;
        private boolean allowDecl;
        private StatementSequence ss;

        @Override
        public void parse() throws Exception {
            skipAndExpect(CoreToken.PROGRAM);
            skipToken();
            allowDecl = true;
            ds = new DeclarationSequence();
            ds.parse();
            expectAndSkip(CoreToken.BEGIN);
            allowDecl = false;
            ss = new StatementSequence();
            ss.parse();
            expectToken(CoreToken.END);
        }

        @Override
        public void print(String indent) {
            System.out.println("program");
            ds.print(singleIndent);
            System.out.println("begin");
            ss.print(singleIndent);
            System.out.println("end");
        }

        public void execute() throws Exception {
            ss.execute();
        }
    }

    private class DeclarationSequence implements Node {
        private Declaration decl;
        private DeclarationSequence ds;

        @Override
        public void parse() throws Exception {
            decl = new Declaration();
            decl.parse();
            if (checkToken(CoreToken.TYPE_INT)) {
                ds = new DeclarationSequence();
                ds.parse();
            }
        }

        @Override
        public void print(String indent) {
            decl.print(indent);
            if (ds != null) {
                ds.print(indent);
            }
        }
    }

    private class Declaration implements Node {
        private IdList idList;

        @Override
        public void parse() throws Exception {
            expectAndSkip(CoreToken.TYPE_INT);
            idList = new IdList();
            idList.parse();
            idList.declare();
            expectAndSkip(CoreToken.SEMICOLON);
        }

        @Override
        public void print(String indent) {
            System.out.print(indent + "int ");
            idList.print("");
            System.out.println(";");
        }
    }

    private class IdList implements Node {
        private Id id;
        private IdList idList;

        @Override
        public void parse() throws Exception {
            id = new Id();
            id.parse();
            if (checkAndSkip(CoreToken.COMMA)) {
                idList = new IdList();
                idList.parse();
            }
        }

        @Override
        public void print(String indent) {
            id.print("");
            if (idList != null) {
                System.out.print(", ");
                idList.print("");
            }
        }

        public void declare() throws Exception {
            id.declare();
            if (idList != null) {
                idList.declare();
            }
        }

        public void printValues() throws Exception {
            id.printValue();
            if (idList != null) {
                idList.printValues();
            }
        }
    }

    private class Id extends Operand {
        private String id;

        @Override
        public void parse() throws Exception {
            expectToken(CoreToken.IDENTIFIER);
            id = tokenizer.getValue();
            if (!isDeclared() && !program.allowDecl) {
                throw new Exception("Cannot use undeclared ID after program begins: " + id);
            }
            skipToken();
        }

        @Override
        public void print(String indent) {
            System.out.print(indent + id);
        }

        public void declare() throws Exception {
            if (isDeclared()) {
                throw new Exception("ID is already declared: " + id);
            } else {
                // Default value of 0
                idValues.put(id, null);
            }
        }

        public boolean isDeclared() {
            return idValues.containsKey(id);
        }

        @Override
        public int value() throws Exception {
            if (idValues.get(id) == null) {
                throw new Exception("Cannot get value of uninitialized ID: " + id);
            }
            return idValues.get(id);
        }

        public void setValue(int value) {
            idValues.put(id, value);
        }

        public void printValue() throws Exception {
            System.out.println(id + " = " + value());
        }
    }

    private class StatementSequence implements Node {
        private Statement stmt;
        private StatementSequence ss;

        @Override
        public void parse() throws Exception {
            stmt = Statement.construct(Interpreter.this);
            stmt.parse();
            if (checkToken(CoreToken.IDENTIFIER, CoreToken.IF, CoreToken.WHILE, CoreToken.READ, CoreToken.WRITE)) {
                ss = new StatementSequence();
                ss.parse();
            }
        }

        @Override
        public void print(String indent) {
            stmt.print(indent);
            if (ss != null) {
                ss.print(indent);
            }
        }

        public void execute() throws Exception {
            stmt.execute();
            if (ss != null) {
                ss.execute();
            }
        }
    }

    private static abstract class Statement implements Node {
        public static Statement construct(Interpreter interpreter) throws Exception {
            interpreter.expectToken(CoreToken.IDENTIFIER, CoreToken.IF, CoreToken.WHILE, CoreToken.READ, CoreToken.WRITE);
            if (interpreter.checkToken(CoreToken.IDENTIFIER)) {
                return interpreter.new Assignment();
            } else if (interpreter.checkToken(CoreToken.IF)) {
                return interpreter.new If();
            } else if (interpreter.checkToken(CoreToken.WHILE)) {
                return interpreter.new Loop();
            } else if (interpreter.checkToken(CoreToken.READ)) {
                return interpreter.new In();
            } else if (interpreter.checkToken(CoreToken.WRITE)) {
                return interpreter.new Out();
            }
            throw new Exception(); // This would have already been thrown by expectToken but I don't want to return null
        }

        public abstract void execute() throws Exception;
    }

    private class Assignment extends Statement {
        private Id id;
        private Expression exp;

        @Override
        public void parse() throws Exception {
            id = new Id();
            id.parse();
            expectAndSkip(CoreToken.EQUALS);
            exp = new Expression();
            exp.parse();
            expectAndSkip(CoreToken.SEMICOLON);
        }

        @Override
        public void print(String indent) {
            id.print(indent);
            System.out.print(" = ");
            exp.print("");
            System.out.println(";");
        }

        @Override
        public void execute() throws Exception {
            id.setValue(exp.value());
        }
    }

    private class Expression implements Node {
        private Factor fac;
        private boolean plus;
        private Expression exp;

        @Override
        public void parse() throws Exception {
            fac = new Factor();
            fac.parse();
            if (checkToken(CoreToken.PLUS, CoreToken.MINUS)) {
                plus = checkToken(CoreToken.PLUS);
                skipToken();
                exp = new Expression();
                exp.parse();
            }
        }

        @Override
        public void print(String indent) {
            fac.print(indent);
            if (exp != null) {
                System.out.print(plus ? " + " : " - ");
                exp.print("");
            }
        }

        public int value() throws Exception {
            return exp == null ? fac.value() : (plus ? (fac.value() + exp.value()) : (fac.value() - exp.value()));
        }
    }

    private class Factor implements Node {
        private Operand op;
        private Factor fac;

        @Override
        public void parse() throws Exception {
            op = Operand.construct(Interpreter.this);
            op.parse();
            if (checkAndSkip(CoreToken.MULTIPLY)) {
                fac = new Factor();
                fac.parse();
            }
        }

        @Override
        public void print(String indent) {
            op.print(indent);
            if (fac != null) {
                System.out.print(" * ");
                fac.print("");
            }
        }

        public int value() throws Exception {
            return fac == null ? op.value() : (op.value() * fac.value());
        }
    }

    private static abstract class Operand implements Node {
        public static Operand construct(Interpreter interpreter) throws Exception {
            interpreter.expectToken(CoreToken.INTEGER, CoreToken.IDENTIFIER, CoreToken.PAREN_OPEN);
            if (interpreter.checkToken(CoreToken.INTEGER)) {
                return interpreter.new Int();
            } else if (interpreter.checkToken(CoreToken.IDENTIFIER)) {
                return interpreter.new Id();
            } else if (interpreter.checkToken(CoreToken.PAREN_OPEN)) {
                return interpreter.new ParenExpression();
            }
            throw new Exception(); // This would have already been thrown by expectToken but I don't want to return null
        }

        public abstract int value() throws Exception;
    }

    private class Int extends Operand {
        private int value;

        @Override
        public void parse() throws Exception {
            expectToken(CoreToken.INTEGER);
            value = Integer.parseInt(tokenizer.getValue());
            skipToken();
        }

        @Override
        public void print(String indent) {
            System.out.print(indent + value);
        }

        @Override
        public int value() throws Exception {
            return value;
        }
    }

    private class ParenExpression extends Operand {
        private Expression exp;

        @Override
        public void parse() throws Exception {
            expectAndSkip(CoreToken.PAREN_OPEN);
            exp = new Expression();
            exp.parse();
            expectAndSkip(CoreToken.PAREN_CLOSE);
        }

        @Override
        public void print(String indent) {
            System.out.print(indent + "(");
            exp.print("");
            System.out.print(")");
        }

        @Override
        public int value() throws Exception {
            return exp.value();
        }
    }

    private class If extends Statement {
        private Condition cond;
        private StatementSequence ssThen;
        private StatementSequence ssElse;

        @Override
        public void parse() throws Exception {
            expectAndSkip(CoreToken.IF);
            cond = Condition.construct(Interpreter.this);
            cond.parse();
            expectAndSkip(CoreToken.THEN);
            ssThen = new StatementSequence();
            ssThen.parse();
            if (checkAndSkip(CoreToken.ELSE)) {
                ssElse = new StatementSequence();
                ssElse.parse();
            }
            expectAndSkip(CoreToken.END);
            expectAndSkip(CoreToken.SEMICOLON);
        }

        @Override
        public void print(String indent) {
            System.out.print(indent + "if ");
            cond.print("");
            System.out.println();
            ssThen.print(indent + singleIndent);
            if (ssElse != null) {
                System.out.println(indent + "else");
                ssElse.print(indent + singleIndent);
            }
            System.out.println(indent + "end;");
        }

        @Override
        public void execute() throws Exception {
            if (cond.evaluate()) {
                ssThen.execute();
            } else if (ssElse != null) {
                ssElse.execute();
            }
        }
    }

    private static abstract class Condition implements Node {
        public static Condition construct(Interpreter interpreter) throws Exception {
            interpreter.expectToken(CoreToken.PAREN_OPEN, CoreToken.NOT, CoreToken.BRACKET_OPEN);
            if (interpreter.checkToken(CoreToken.PAREN_OPEN)) {
                return interpreter.new Comparison();
            } else if (interpreter.checkToken(CoreToken.NOT)) {
                return interpreter.new Not();
            } else if (interpreter.checkToken(CoreToken.BRACKET_OPEN)) {
                return interpreter.new AndOr();
            }
            throw new Exception(); // This would have already been thrown by expectToken but I don't want to return null
        }

        public abstract boolean evaluate() throws Exception;
    }

    private class Comparison extends Condition {
        private Operand op1;
        private CoreToken compOp;
        private Operand op2;

        @Override
        public void parse() throws Exception {
            expectAndSkip(CoreToken.PAREN_OPEN);
            op1 = Operand.construct(Interpreter.this);
            op1.parse();
            expectToken(CoreToken.EQUAL_TO, CoreToken.NOT_EQUAL_TO, CoreToken.LESS_THAN, CoreToken.GREATER_THAN,
                    CoreToken.LESS_THAN_OR_EQUAL_TO, CoreToken.GREATER_THAN_OR_EQUAL_TO);
            if (checkAndSkip(CoreToken.EQUAL_TO)) {
                compOp = CoreToken.EQUAL_TO;
            } else if (checkAndSkip(CoreToken.NOT_EQUAL_TO)) {
                compOp = CoreToken.NOT_EQUAL_TO;
            } else if (checkAndSkip(CoreToken.LESS_THAN)) {
                compOp = CoreToken.LESS_THAN;
            } else if (checkAndSkip(CoreToken.GREATER_THAN)) {
                compOp = CoreToken.GREATER_THAN;
            } else if (checkAndSkip(CoreToken.LESS_THAN_OR_EQUAL_TO)) {
                compOp = CoreToken.LESS_THAN_OR_EQUAL_TO;
            } else if (checkAndSkip(CoreToken.GREATER_THAN_OR_EQUAL_TO)) {
                compOp = CoreToken.GREATER_THAN_OR_EQUAL_TO;
            }
            op2 = Operand.construct(Interpreter.this);
            op2.parse();
            expectAndSkip(CoreToken.PAREN_CLOSE);
        }

        @Override
        public void print(String indent) {
            System.out.print(indent + "(");
            op1.print("");
            switch (compOp) {
                case EQUAL_TO:
                    System.out.print(" == ");
                    break;
                case NOT_EQUAL_TO:
                    System.out.print(" != ");
                    break;
                case LESS_THAN:
                    System.out.print(" < ");
                    break;
                case GREATER_THAN:
                    System.out.print(" > ");
                    break;
                case LESS_THAN_OR_EQUAL_TO:
                    System.out.print(" <= ");
                    break;
                case GREATER_THAN_OR_EQUAL_TO:
                    System.out.print(" >= ");
                    break;
            }
            op2.print("");
            System.out.print(")");
        }

        @Override
        public boolean evaluate() throws Exception {
            switch (compOp) {
                case EQUAL_TO:
                    return op1.value() == op2.value();
                case NOT_EQUAL_TO:
                    return op1.value() != op2.value();
                case LESS_THAN:
                    return op1.value() < op2.value();
                case GREATER_THAN:
                    return op1.value() > op2.value();
                case LESS_THAN_OR_EQUAL_TO:
                    return op1.value() <= op2.value();
                case GREATER_THAN_OR_EQUAL_TO:
                    return op1.value() >= op2.value();
                default:
                    return false;
            }
        }
    }

    private class Not extends Condition {
        private Condition cond;

        @Override
        public void parse() throws Exception {
            expectAndSkip(CoreToken.NOT);
            cond = Condition.construct(Interpreter.this);
            cond.parse();
        }

        @Override
        public void print(String indent) {
            System.out.print(indent + "!");
            cond.print("");
        }

        @Override
        public boolean evaluate() throws Exception {
            return !cond.evaluate();
        }
    }

    private class AndOr extends Condition {
        private Condition cond1;
        private boolean and;
        private Condition cond2;

        @Override
        public void parse() throws Exception {
            expectAndSkip(CoreToken.BRACKET_OPEN);
            cond1 = Condition.construct(Interpreter.this);
            cond1.parse();
            expectToken(CoreToken.AND, CoreToken.OR);
            and = checkToken(CoreToken.AND);
            skipToken();
            cond2 = Condition.construct(Interpreter.this);
            cond2.parse();
            expectAndSkip(CoreToken.BRACKET_CLOSE);
        }

        @Override
        public void print(String indent) {
            System.out.print(indent + "[");
            cond1.print("");
            System.out.print(and ? " && " : " || ");
            cond2.print("");
            System.out.print("]");
        }

        @Override
        public boolean evaluate() throws Exception {
            return and ? (cond1.evaluate() && cond2.evaluate()) : (cond1.evaluate() || cond2.evaluate());
        }
    }

    private class Loop extends Statement {
        private Condition cond;
        private StatementSequence ss;

        @Override
        public void parse() throws Exception {
            expectAndSkip(CoreToken.WHILE);
            cond = Condition.construct(Interpreter.this);
            cond.parse();
            expectAndSkip(CoreToken.LOOP);
            ss = new StatementSequence();
            ss.parse();
            expectAndSkip(CoreToken.END);
            expectAndSkip(CoreToken.SEMICOLON);
        }

        @Override
        public void print(String indent) {
            System.out.print(indent + "while ");
            cond.print("");
            System.out.println(" loop");
            ss.print(indent + singleIndent);
            System.out.println(indent + "end;");
        }

        @Override
        public void execute() throws Exception {
            while(cond.evaluate()) {
                ss.execute();
            }
        }
    }

    private class In extends Statement {
        private Pattern integer = Pattern.compile("[\\+-]?\\d+");
        private Pattern whitespace = Pattern.compile("\\s");
        private IdList idList;

        @Override
        public void parse() throws Exception {
            expectAndSkip(CoreToken.READ);
            idList = new IdList();
            idList.parse();
            expectAndSkip(CoreToken.SEMICOLON);
        }

        @Override
        public void print(String indent) {
            System.out.print(indent + "read ");
            idList.print("");
            System.out.println(";");
        }

        @Override
        public void execute() throws Exception {
            readValue(idList);
        }

        private void readValue(IdList idList) throws Exception {
            // This could be a lot better for our requirements, but this keeps it open for now
            Matcher matcher = integer.matcher(inputData);
            if (matcher.find()) {
                idList.id.setValue(Integer.parseInt(matcher.group()));
                inputData.delete(matcher.start(), matcher.start() + matcher.end());
                if (idList.idList != null) {
                    readValue(idList.idList);
                }
            } else {
                throw new Exception("Input data does not contain a value to read for id " + idList.id.id);
            }
        }
    }

    private class Out extends Statement {
        private IdList idList;

        @Override
        public void parse() throws Exception {
            expectAndSkip(CoreToken.WRITE);
            idList = new IdList();
            idList.parse();
            expectAndSkip(CoreToken.SEMICOLON);
        }

        @Override
        public void print(String indent) {
            System.out.print(indent + "write ");
            idList.print("");
            System.out.println(";");
        }

        @Override
        public void execute() throws Exception {
            idList.printValues();
        }
    }

    // Tokens that make up the core language. Used in the interpreter and tokenizer.
    public enum CoreToken {
        // Reserved words
        PROGRAM(new Token(1, "program")),
        BEGIN(new Token(2, "begin")),
        END(new Token(3, "end")),
        TYPE_INT(new Token(4, "int")),
        IF(new Token(5, "if")),
        THEN(new Token(6, "then")),
        ELSE(new Token(7, "else")),
        WHILE(new Token(8, "while")),
        LOOP(new Token(9, "loop")),
        READ(new Token(10, "read")),
        WRITE(new Token(11, "write")),

        // Special symbols
        SEMICOLON(new Token(12, ";")),
        COMMA(new Token(13, ",")),
        BRACKET_OPEN(new Token(16, "\\[")),
        BRACKET_CLOSE(new Token(17, "\\]")),
        AND(new Token(18, "&&")),
        OR(new Token(19, "\\|\\|")),
        PAREN_OPEN(new Token(20, "\\(")),
        PAREN_CLOSE(new Token(21, "\\)")),
        PLUS(new Token(22, "\\+")),
        MINUS(new Token(23, "-")),
        MULTIPLY(new Token(24, "\\*")),
        NOT_EQUAL_TO(new Token(25, "!=")),
        EQUAL_TO(new Token(26, "==")),
        LESS_THAN_OR_EQUAL_TO(new Token(29, "<=")),
        GREATER_THAN_OR_EQUAL_TO(new Token(30, ">=")),

        // Special symbols to add last
        EQUALS(new Token(14, "=")),
        NOT(new Token(15, "!")),
        LESS_THAN(new Token(27, "<")),
        GREATER_THAN(new Token(28, ">")),

        // Integers
        INTEGER(new Token(31, "[0-9]+", 8)),

        // Identifiers
        IDENTIFIER(new Token(32, "[A-Z]+[0-9]*", 8));

        private final Tokenizer.Token token;

        CoreToken(Tokenizer.Token token) {
            this.token = token;
        }
    }

    // Main method to satisfy requirements of interpreter project p2

    public static void main(String[] args) {
        // Read program file into input StringBuilder
        if (args.length < 1) {
            System.out.println("Must pass in program file to tokenize");
            return;
        }
        Scanner programIn;
        try {
            programIn = new Scanner(new FileReader(args[0]));
        } catch (FileNotFoundException e) {
            System.out.println("Program file not found");
            return;
        }
        StringBuilder program = new StringBuilder();
        while (programIn.hasNext()) {
            program.append(programIn.next());
            program.append(" ");
        }
        program.deleteCharAt(program.length() - 1);
        programIn.close();

        // Read input data into input StringBuilder
        StringBuilder inputData = new StringBuilder();
        if (args.length > 1) {
            Scanner dataIn;
            try {
                dataIn = new Scanner(new FileReader(args[1]));
            } catch (FileNotFoundException e) {
                System.out.println("Input data file not found");
                return;
            }
            while(dataIn.hasNext()) {
                inputData.append(dataIn.next());
                inputData.append(" ");
            }
            inputData.deleteCharAt(inputData.length() - 1);
            dataIn.close();
        }

        // Instantiate tokenizer and add tokens
        Tokenizer tokenizer = new Tokenizer();
        for (CoreToken token : CoreToken.values()) {
            tokenizer.add(token.token);
        }
        tokenizer.setInput(program);

        // Instantiate core interpreter (this parses, prints, and executes the program)
        Interpreter interpreter = new Interpreter(tokenizer, inputData);
    }

}
