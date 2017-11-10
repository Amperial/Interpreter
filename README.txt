CSE 3341 Interpreter Project Part 2: Parser, Printer, Executor
Austin Payne.596

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