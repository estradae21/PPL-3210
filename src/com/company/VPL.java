/*
    Ernesto Estrada
    Dan Zapfel
    Wyatt Hyatt
    Megan Jordal
    Alex Tusa
*/

package com.company;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;


public class VPL
{
    static String fileName;
    static Scanner keys;

    static int max;
    static int[] mem;
    static int ip;
    static int bp;
    static int sp;
    static int rv;
    static int hp;
    static int numPassed;
    static int gp;
    static int step;

    public static void main(String[] args) throws Exception {

        keys = new Scanner( System.in );

        if( args.length != 2 ) {
            System.err.println("Usage: java VPL <vpl program> <memory size>" );
            System.exit(1);
        }

        fileName = args[0];

        max = Integer.parseInt( args[1] );
        mem = new int[max];

        // load the program into the front part of
        // memory
        ArrayList<IntPair> labels;
        ArrayList<IntPair> holes;
        int label;
        int k;
        try (Scanner input = new Scanner(new File(System.getProperty("user.dir") + "\\" + fileName))) {
            String line;
            StringTokenizer st;
            int opcode;

            /**List that contains both the label names and the
             * index in memory in which this label resides; Used
             * to fill in the holes later
             */
            labels = new ArrayList<>();

            /**List that keeps track of all of the holes that have
             * been created by how jumps are called or by how calls
             * are performed
             */
            holes = new ArrayList<>();

            /**Holds the integer name of whichever label is currently
             * being processed
             */
            label = 0;

            // load the code

            /** Keeps track of what index we are at in memory during initialization */
            k = 0;
            while (input.hasNextLine()) {
                line = input.nextLine();
                System.out.println("parsing line [" + line + "]");
                if (line != null) {// extract any tokens
                    st = new StringTokenizer(line);
                    if (st.countTokens() > 0) {// have a token, so must be an instruction (as opposed to empty line)

                        opcode = Integer.parseInt(st.nextToken());

                        // load the instruction into memory:

                            if (opcode == labelCode) {// note index that comes where label would go
                                label = Integer.parseInt(st.nextToken());
                                labels.add(new IntPair(label, k));
                            } else if (opcode == noopCode) {
                                assert true;
                            } else {// opcode actually gets stored
                                mem[k] = opcode;
                                k++;

                                // If instruction is a call to a function or label
                                if (opcode == callCode || opcode == jumpCode ||
                                        opcode == condJumpCode) {// note the hole immediately after the opcode to be filled in later
                                    label = Integer.parseInt(st.nextToken());
                                    mem[k] = label; // Store this label in memory at the current location
                                    holes.add(new IntPair(k, label)); // Add this hole to list to be filled in later
                                    ++k; // Create the hole
                                }

                                // load correct number of arguments (following label, if any):
                                for (int j = 0; j < numArgs(opcode); ++j) {
                                    mem[k] = Integer.parseInt(st.nextToken());
                                    ++k;
                                }

                            }// not a label

                    }// have a token, so must be an instruction
                }// have a line
            }// loop to load code
        }

        //System.out.println("after first scan:");
        //showMem( 0, k-1 );

        // fill in all the holes:
        /**Iterate through holes list.
         * For each hole in the list, find it's corresponding
         * label in the labels list. Once that value is found,
         * recall that in the labels list, the second integer
         * in the pair is the index in memory from which the
         * label is created. This is where we assign the index
         * of memory that created this label to our index variable.
         * Finally, store the index of where the label begins into
         * memory where this hole occurs (which is the first integer
         * in the integer pair object).
         */
        int index;
        for (IntPair hole : holes) {
            label = hole.second;
            index = -1;
            for (IntPair label1 : labels)
                if (label1.first == label)
                    index = label1.second;
            mem[hole.first] = index;
        }

        System.out.println("after replacing labels:");
        showMem( 0, k-1 );

        // initialize registers:
        bp = k;
        sp = k+2; // Make room for our return pointer and return base pointer
        ip = 0;
        rv = -1;
        hp = max - 1; // Start the heap pointer at the end of memory (adjusted to allow for direct use in arrays)
        numPassed = 0;

        int codeEnd = bp-1;

        System.out.println("Code is " );
        showMem( 0, codeEnd );

        int gp = codeEnd + 1;

        // start execution:
        boolean done = false;
        int op;
        int actualNumArgs;

        int step = 0;

        int oldIp = 0;

        // repeatedly execute a single operation
        // *****************************************************************

        do {

/*    // show details of current step
      System.out.println("--------------------------");
      System.out.println("Step of execution with IP = " + ip + " opcode: " +
          mem[ip] +
         " bp = " + bp + " sp = " + sp + " hp = " + hp + " rv = " + rv );
      System.out.println(" chunk of code: " +  mem[ip] + " " +
                            mem[ip+1] + " " + mem[ip+2] + " " + mem[ip+3] );
      System.out.println("--------------------------");
      System.out.println( " memory from " + (codeEnd+1) + " up: " );
      showMem( codeEnd+1, sp+3 );
      System.out.println("hit <enter> to go on" );
      keys.nextLine();
*/

            oldIp = ip;

            op = mem[ip];
            ip++;
            // extract the args into a, b, c for convenience:
            int a = -1;
            int b = -2;
            int c = -3;

            // numArgs is wrong for these guys, need one more!
            if( op == callCode || op == jumpCode ||
                    op == condJumpCode )
            {
                actualNumArgs = numArgs( op ) + 1;
            }
            else
                actualNumArgs = numArgs( op );

            if( actualNumArgs == 1 ) {
                a = mem[ip];
                ip++;
            }
            else if( actualNumArgs == 2 ) {
                a = mem[ip];
                ip++;
                b = mem[ip];
                ip++;
            }
            else if( actualNumArgs == 3 ) {
                a = mem[ip];
                ip++;
                b = mem[ip];
                ip++;
                c = mem[ip];
                ip++;
            }

            // implement all operations here:
            // ********************************************

            // put your work right here!


            if ( op == noopCode ) {
               assert true;
            }
            else if ( op == labelCode) {
                // TODO
            }
            else if ( op == callCode) {
                // TODO
            }
            else if ( op == passCode) {
                int i = sp;
                while (sp < mem.length - 1) {
                    if (mem[i] == 0) {
                        mem[bp + 2 + a] = mem[i];
                    }
                    else {
                        i++;
                    }
                }
            }
            //????????????????????????????????????????
            else if ( op == allocCode) {
                sp += a;
            }
            else if ( op == returnCode) {
                //TODO
            }
            else if ( op == getRetvalCode) {
                setmem(a, rv);
            }
            else if ( op == jumpCode) {
                ip = label;
            }
            else if ( op == condJumpCode) {
                if (ip > 0) {
                    ip = label;
                }
                else {
                    ip++;
                }
            }
            else if ( op == addCode) {
                setmem(a,getmem(b) + getmem(c));
            }
            else if ( op == subCode) {
                setmem(a, getmem(b) - getmem(c));
            }
            else if ( op == multCode) {
                setmem(a, getmem(b) * getmem(c));
            }
            else if ( op == divCode) {
                setmem(a, getmem(b) / getmem(c));
            }
            else if ( op == remCode) {
                setmem(a, getmem(b) % getmem(c));
            }
            else if ( op == equalCode) {
                setmem(a, (getmem(b)== getmem(c))?1:0);
            }
            else if ( op == notEqualCode) {
                setmem(a, (getmem(b)!= getmem(c))?1:0);
            }
            else if ( op == lessCode) {
                setmem(a, (getmem(b) < getmem(c))?1:0);
            }
            else if ( op == lessEqualCode) {
                setmem(a, (getmem(b) <= getmem(c))?1:0);
            }
            else if ( op == andCode) {
                setmem(a, (getmem(b) & getmem(c)));
            }
            else if ( op == orCode) {
                setmem(a, (getmem(b) | getmem(c)));
            }
            else if ( op == notCode) {
                setmem(a, (getmem(b) == 0)?1:0);
            }
            else if ( op == oppCode) {
                setmem(a, -getmem(b));
            }
            else if ( op == litCode) {
                setmem(a, b);
            }
            else if ( op == copyCode) {
                setmem(a, getmem(b));
            }
            else if ( op == getCode) {
                setmem(a, mem[hp + getmem(b) + getmem(c)]);
            }
            else if ( op == putCode) {
                mem[hp + getmem(a) + getmem(b)] = getmem(c);
            }
            else if ( op == haltCode) {
                done = true;
            }
            else if ( op == inputCode) {
                try (Scanner read = new Scanner(System.in)) {
                    System.out.println("? ");
                    setmem(a, Integer.parseInt(read.nextLine()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if ( op == outputCode) {
                System.out.print(getmem(a));
            }
            else if ( op == newlineCode) {
                System.out.print("\r\n");
            }
            else if ( op == symbolCode) {
                if (getmem(a) > 32 && getmem(a) < 126) {
                    System.out.print((char) getmem(a));
                }
            }
            else if ( op == newCode) {
                hp -= getmem(b);
                setmem(a, hp);
            }
            else if ( op == allocGlobalCode) {
                gp = codeEnd + 1;
                bp += a;
                sp += a;
            }
            else if ( op == toGlobalCode) {
                mem[gp + a] = getmem(b);
            }
            else if ( op == fromGlobalCode) {
                setmem(a, mem[gp + b]);
            }
            else if ( op == debugCode) {
                //TODO
            }



            else
            {
                System.err.println("Fatal error: unknown opcode [" + op + "]" );
                System.exit(1);
            }

            step++;

        }while( !done );


    }// main

    public static void setmem (int a, int n) {

        mem[bp + 2 + a] = n;
    }

    public static  int getmem (int a) {

        return mem[bp + 2 + a];
    }

    // use symbolic names for all opcodes:

    // op to produce comment
    private static final int noopCode = 0;

    // ops involved with registers
    private static final int labelCode = 1;
    private static final int callCode = 2;
    private static final int passCode = 3;
    private static final int allocCode = 4;
    private static final int returnCode = 5;  // return a means "return and put
    // copy of value stored in cell a in register rv
    private static final int getRetvalCode = 6;//op a means "copy rv into cell a"
    private static final int jumpCode = 7;
    private static final int condJumpCode = 8;

    // arithmetic ops
    private static final int addCode = 9;
    private static final int subCode = 10;
    private static final int multCode = 11;
    private static final int divCode = 12;
    private static final int remCode = 13;
    private static final int equalCode = 14;
    private static final int notEqualCode = 15;
    private static final int lessCode = 16;
    private static final int lessEqualCode = 17;
    private static final int andCode = 18;
    private static final int orCode = 19;
    private static final int notCode = 20;
    private static final int oppCode = 21;

    // ops involving transfer of data
    private static final int litCode = 22;  // litCode a b means "cell a gets b"
    private static final int copyCode = 23;// copy a b means "cell a gets cell b"
    private static final int getCode = 24; // op a b means "cell a gets
    // contents of cell whose
    // index is stored in b"
    private static final int putCode = 25;  // op a b means "put contents
    // of cell b in cell whose offset is stored in cell a"

    // system-level ops:
    private static final int haltCode = 26;
    private static final int inputCode = 27;
    private static final int outputCode = 28;
    private static final int newlineCode = 29;
    private static final int symbolCode = 30;
    private static final int newCode = 31;

    // global variable ops:
    private static final int allocGlobalCode = 32;
    private static final int toGlobalCode = 33;
    private static final int fromGlobalCode = 34;

    // debug ops:
    private static final int debugCode = 35;

    // return the number of arguments after the opcode,
    // except ops that have a label return number of arguments
    // after the label, which always comes immediately after
    // the opcode
    private static int numArgs( int opcode )
    {
        // highlight specially behaving operations
        if( opcode == labelCode ) return 1;  // not used
        else if( opcode == jumpCode ) return 0;  // jump label
        else if( opcode == condJumpCode ) return 1;  // condJump label expr
        else if( opcode == callCode ) return 0;  // call label

            // for all other ops, lump by count:

        else if( opcode==noopCode ||
                opcode==haltCode ||
                opcode==newlineCode ||
                opcode==debugCode
        )
            return 0;  // op

        else if( opcode==passCode || opcode==allocCode ||
                opcode==returnCode || opcode==getRetvalCode ||
                opcode==inputCode ||
                opcode==outputCode || opcode==symbolCode ||
                opcode==allocGlobalCode
        )
            return 1;  // op arg1

        else if( opcode==notCode || opcode==oppCode ||
                opcode==litCode || opcode==copyCode || opcode==newCode ||
                opcode==toGlobalCode || opcode==fromGlobalCode

        )
            return 2;  // op arg1 arg2

        else if( opcode==addCode ||  opcode==subCode || opcode==multCode ||
                opcode==divCode ||  opcode==remCode || opcode==equalCode ||
                opcode==notEqualCode ||  opcode==lessCode ||
                opcode==lessEqualCode || opcode==andCode ||
                opcode==orCode || opcode==getCode || opcode==putCode
        )
            return 3;

        else
        {
            System.err.println("Fatal error: unknown opcode [" + opcode + "]" );
            System.exit(1);
            return -1;
        }

    }// numArgs

    private static void showMem( int a, int b )
    {
        for( int k=a; k<=b; ++k )
        {
            System.out.println( k + ": " + mem[k] );
        }
    }// showMem

}// VPL
