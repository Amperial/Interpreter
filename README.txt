CSE 3341 Interpreter Project Part 2: Parser, Printer, Executor
Austin Payne.596

CORE LANGUAGE GRAMMAR:
<prog>     ::= program <decl seq> begin <stmt seq> end
<decl seq> ::= <decl> | <decl> <decl seq>
<stmt seq> ::= <stmt> | <stmt> <stmt seq>
<decl>     ::= int <id list>;
<id list>  ::= <id> | <id>, <id list>
<stmt>     ::= <assign>|<if>|<loop>|<in>|<out>
<assign>   ::= <id> = <exp>;
<if>       ::= if <cond> then <stmt seq> end; | if <cond> then <stmt seq> else <stmt seq> end;
<loop>     ::= while <cond> loop <stmt seq> end;
<in>       ::= read <id list>;
<out>      ::= write <id list>;
<cond>     ::= <comp>|!<cond> | [<cond> && <cond>] | [<cond> or <cond>]
<comp>     ::= (<op> <comp op> <op>)
<exp>      ::= <fac>|<fac>+<exp>|<fac>-<exp>
<fac>      ::= <op> | <op> * <fac>
<op>       ::= <int> | <id> | (<exp>)
<comp op>  ::= != | == | < | > | <= | >=
<id>       ::= <let> | <let><id> | <let><int>
<let>      ::= A | B | C | ... | X | Y | Z
<int>      ::= <digit> | <digit><int>
<digit>    ::= 0 | 1 | 2 | 3 | ... | 9

Files submitted:
Tokenizer.java
- Contains Tokenizer and Token classes
Interpreter.java
- Contains Interpreter class, and all OO core grammar classes
- Contains main() method which uses these classes with the tokenizer to fulfill part 2 requirements

Program compilation:
From main directory (outside of /core directory):
javac -cp . core/Interpreter.java

Program execution (still outside of /core):
java core/Interpreter testfile inputfile
-instructor test cases included as test1, test2, test3 files
    -input files for these cases are test1_input1, etc
-for example "java core/Interpreter test1 test1_input1"