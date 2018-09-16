package com.company;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class Jive {

    private static Scanner keys = new Scanner( System.in );
    private static Scanner input;
    private static PrintWriter output;

    // holds the vpl code as it is produced
    private static ArrayList<Integer> vpl = new ArrayList<>();

    // holds the local variables and literals temporarily for each function
    private static ArrayList<String> locsList = new ArrayList<>();

    // holds all the global variables for entire program
    private static ArrayList<String> globsList;

    // holds the locations of the holes and the corresponding label or function name
    private static ArrayList<StringIntPair> holes = new ArrayList<>();

    // holds the label, function name, or #<func name> paired with corresponding index
    private static ArrayList<StringIntPair> info = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage:  java Jive <fileName>");
            System.exit(1);
        }

        String fileName = args[0];

        input = new Scanner( new File( fileName ) );
        output = new PrintWriter( new File( fileName + ".vpl" ) );

        // scan Jive file one word at a time
        // (line breaks mean nothing more than a space)

        // first function has no params and no name
        String currentFuncName = "?";   // name of function currently being scanned
        // starts at "?" for the "main"
        int currentFuncNumParams = 0;  // remember number of parameters for current function

        // need to store info while scanning function call in part 1
        // because might have first occurrence of a literal requiring
        // command 22 before do things for the call
        ArrayList<Integer> callInfo = new ArrayList<>(); // initialize to avoid compiler complaint
        String calledFuncName = "?";  // store name of called function briefly, until args are processed

        // make scratch cell (local cell 0) for scratch space for main
        locsList.add( "-" );
        int scratch = 0;

        int state = 1;  // beginning a part 1

        while ( input.hasNext() ) {

            String word = input.next();

            System.out.println("In state    " + state + "    processing  [" + word + "]" );

            System.out.println("current function is " + currentFuncName );

            // if no globals declared, put in command 4 for anonymous "main" here
            // (skip for all later words)
            if ( ! word.equals("Globs") && vpl.isEmpty() ) {
                vpl.add( 4 );
                vpl.add( -1 ); // leave a hole for # of locals needed for "main"
                holes.add( new StringIntPair( "#?", vpl.size()-1 ) );
            }

            if ( state == 1 ) {
                if ( word.equals("/*") ) {
                    state = 2;
                }
                else if ( word.equals("Halt") ) {
                    vpl.add( 26 );
                    state = 1;
                }
                else if ( word.equals("NL") ) {
                    vpl.add( 29 );
                    state = 1;
                }
                else if ( word.equals("Def") ) {// starting a function definition

                    System.out.println("Local cells for function just finished, " + currentFuncName + ":" );
                    showLocals();

                    // finish off previous definition
                    // add count of locals to info
                    //  (count used for "4" command, so leave out the parameters)
                    info.add( new StringIntPair( "#" + currentFuncName,
                            locsList.size() - currentFuncNumParams ) );
                    // initialize things for this function definition
                    locsList = new ArrayList<>();

                    // move on to see the function name
                    state = 3;
                }
                else if ( isLabel( word ) ) {// process a label
                    // note location of label---will be next spot after whatever have done so far
                    info.add( new StringIntPair( word, vpl.size() ) );
                    state = 1;
                }
                else if ( word.equals("Globs") ) {// global declarations section
                    // global declarations must be first command in program, if there at all
                    if ( vpl.isEmpty() ) {
                        vpl.add( 32 );

                        globsList = new ArrayList<>();

                        state = 6;
                    }
                    else {
                        error( "Globals declaration must occur at very beginning of a Jive program");
                    }
                }
                else if ( word.equals("Jmp") ) {
                    vpl.add( 7 );
                    state = 7;
                }

                else if ( findBIF2( word ) > 0 ) {// is built-in function with 2 args
                    callInfo = new ArrayList<>();
                    callInfo.add( findBIF2(word) + 9 );  // note VPL function code for later
                    state = 8;
                }

                else if ( findBIF1( word ) > 0 ) {// is built-in function with 1 arg
                    callInfo = new ArrayList<>();
                    if ( word.equals( "Not" ) )
                        callInfo.add( 20 );
                    else if ( word.equals( "Opp" ) )
                        callInfo.add( 21 );
                    else if ( word.equals( "New" ) )
                        callInfo.add( 31 );
                    else
                        error("[" + word + "] is not a valid one argument built-in function");

                    state = 11;
                }

                else if ( word.equals("Keys") ) {// is built-in function with 0 args
                    state = 13;
                }

                else if ( isFuncName( word ) ) {
                    calledFuncName = word;
                    callInfo = new ArrayList<>();
                    state = 14;
                }

                else if ( isVar( word ) ) {
                    callInfo = new ArrayList<>();
                    callInfo.add( processVar( word, locsList ) );
                    state = 15;
                }

                else if ( word.equals("Fet") ) {
                    state = 16;
                }

            }// state 1

            else if ( state == 2 ) {
                if ( word.equals("*/") ) {// reached end of comment
                    state = 1;  // start a new command
                }
                else {// part of comment
                    // consume the word and stay in state 2
                }
            }

            else if ( state == 3 ) {
                if ( ! isFuncName( word ) ) {
                    error( "[" + word + "] is not a valid function name");
                }

                currentFuncName = word;
                // note starting location of function definition
                info.add( new StringIntPair( currentFuncName, vpl.size() ) );

                vpl.add( 4 );  // begin function  definition
                // note the hole where argument to command 4 will be put later
                holes.add( new StringIntPair( "#" + currentFuncName, vpl.size() ) );
                vpl.add( -1 );  // add the hole that will receive # of locals later

                currentFuncNumParams = 0;
                state = 5;
            }

            // state 4 lost in change from f ( a b c )  to  f a b c .

            else if ( state == 5 ) {
                if ( word.equals( "." ) ) {

                    // make scratch cell for this function after params cells
                    locsList.add( "-" );
                    scratch = locsList.size()-1;

                    state = 1;  // go on to body of function

                }
                else if ( isParam( word ) ) {
                    // note another parameter
                    currentFuncNumParams++;
                    locsList.add( word );
                    // loop back to stay in state 5
                }
            }

            else if ( state == 6 ) {
                if ( word.equals( "." ) ) {
                    // done creating list of globals, generate VPL code for command 32
                    vpl.add( 32 );
                    vpl.add( globsList.size() );

                    // if have Globs, must slip in command 4 immediately after
                    vpl.add( 4 );
                    vpl.add( -1 ); // leave a hole for # of locals needed for "main"
                    holes.add( new StringIntPair( "#?", vpl.size()-1 ) );

                    state = 1;
                }
                else if ( isParam( word ) ) {
                    // add global
                    globsList.add( word );
                    // stay in state 6
                }
                else {
                    error("[" + word + "] is not a valid global variable name");
                }
            }

            else if ( state == 7 ) {
                if ( isLabel( word ) ) {
                    // add the hole
                    vpl.add( -1 );
                    holes.add( new StringIntPair( word, vpl.size()-1 ) );

                    state = 1;
                }
                else {
                    error( "[" + word + "] is not a valid label");
                }
            }

            else if ( state == 8 ) {
                if ( isVar( word ) ) {
                    callInfo.add( processVar( word, locsList ) );
                    state = 9;
                }
                else {
                    error( "[" + word + "] is not a valid variable or literal");
                }
            }

            else if ( state == 9 ) {
                if ( isVar( word ) ) {
                    callInfo.add( processVar( word, locsList ) );
                    state = 10;
                }
                else {
                    error( "[" + word + "] is not a valid variable or literal");
                }
            }

            else if ( state == 10 ) {
                if ( word.equals("->") ) {
                    // done with bif2 call, send out the vpl code
                    vpl.add( callInfo.get(0) );
                    vpl.add( scratch );
                    vpl.add( callInfo.get(1) );
                    vpl.add( callInfo.get(2) );

                    state = 100;
                }
                else {
                    error("a part 1 must be followed by ->");
                }
            }

            else if ( state == 11 ) {
                if ( isVar( word ) ) {
                    callInfo.add( processVar( word, locsList ) );
                    state = 12;
                }
                else {
                    error( "[" + word + "] is not a valid variable or literal");
                }
            }

            else if ( state == 12 ) {
                if ( word.equals("->") ) {
                    // done with bif2 call, send out the vpl code
                    vpl.add( callInfo.get(0) );
                    vpl.add( scratch );
                    vpl.add( callInfo.get(1) );

                    state = 100;
                }
                else {
                    error("a part 1 must be followed by ->");
                }
            }

            else if ( state == 13 ) {
                if ( word.equals("->") ) {
                    vpl.add( 27 );
                    vpl.add( scratch );

                    state = 100;
                }
                else {
                    error("a part 1 must be followed by ->");
                }
            }

            else if ( state == 14 ) {
                if ( isVar( word ) ) {
                    callInfo.add( processVar( word, locsList ) );
                    // state loops to 14
                }
                else if ( word.equals("->") ) {
                    // done with function call, send out the vpl code

                    // first do command 3's for arguments
                    for (int k=0; k<callInfo.size(); k++) {
                        vpl.add( 3 );
                        vpl.add( callInfo.get(k) );
                    }
                    // then do command 2
                    vpl.add( 2 );
                    vpl.add( -1 );  // leave a hole
                    holes.add( new StringIntPair( calledFuncName, vpl.size()-1 ) );

                    // followed by command 6 for when return from doing called function
                    vpl.add( 6 );
                    vpl.add( scratch );

                    state = 100;
                }
                else {
                    error("a part 1 must be followed by ->");
                }

            }

            else if ( state == 15 ) {
                if ( word.equals("->") ) {
                    vpl.add( 23 );
                    vpl.add( scratch );
                    vpl.add( callInfo.get( 0 ) );

                    state = 100;
                }
                else {
                    error("a part 1 must be followed by ->");
                }
            }

            else if ( state == 16 ) {
                if ( isParam( word ) ) {
                    callInfo = new ArrayList<>();
                    callInfo.add( processVar( word, globsList ) );

                    state = 17;
                }
                else {
                    error("[" + word + "] is not a valid argument for Fet");
                }
            }

            else if ( state == 17 ) {
                if ( word.equals("->") ) {
                    vpl.add( 34 );
                    vpl.add( scratch );
                    vpl.add( callInfo.get( 0 ) );
                }
                else {
                    error("a part 1 must be followed by ->");
                }

            }

            // end of part 1 states

            // begin part 2 states

            else if ( state == 100 ) {// start state for part 2

                if ( word.equals(".") ) {
                    // do nothing with the value in scratch cell from part 1
                    state = 1;
                }

                else if ( word.equals("Prt") ) {
                    // generate code to print the value in the scratch cell
                    vpl.add( 28 );
                    vpl.add( scratch );
                    state = 1;
                }

                else if ( word.equals("Sym") ) {
                    // generate code to print the value in the scratch cell
                    vpl.add( 30 );
                    vpl.add( scratch );
                    state = 1;
                }

                else if ( word.equals("Ret") ) {
                    // generate code to return the value in scratch cell
                    vpl.add( 5 );
                    vpl.add( scratch );
                    state = 1;
                }

                else if ( isParam( word ) ) {
                    vpl.add( 23 );
                    vpl.add( processVar( word, locsList ) );
                    vpl.add( scratch );
                    state = 1;
                }

                else if ( word.equals("Jmp") ) {
                    state = 101;
                }

                else if ( word.equals("Put") ) {
                    state = 102;
                }

                else if ( word.equals("Sto") ) {
                    state = 104;
                }

            }// start state for part 2 (100)

            else if ( state == 101 ) {
                if ( isLabel( word ) ) {
                    vpl.add( 8 );
                    vpl.add( -1 );  // hole for target of jump
                    holes.add( new StringIntPair( word, vpl.size()-1 ) );
                    vpl.add( scratch );

                    state = 1;
                }
                else {
                    error("[" + word + "] is not a valid label to use after conditional Jmp");
                }
            }

            else if ( state == 102 ) {// processing first <var> for Put
                if ( isVar( word ) ) {// have valid first argument
                    callInfo = new ArrayList<>();
                    callInfo.add( processVar( word, locsList ) );

                    state = 103;
                }
                else {
                    error("[" + word + "] is not a valid first argument for Put");
                }
            }

            else if ( state == 103 ) {// processing second <var> for Put
                if ( isVar( word ) ) {// have valid second argument
                    callInfo.add( processVar( word, locsList ) );

                    // generate VPL code 25 a b c
                    vpl.add( 25 );
                    vpl.add( callInfo.get(0) );
                    vpl.add( callInfo.get(1) );
                    vpl.add( scratch );

                    state = 1;
                }
                else {
                    error("[" + word + "] is not a valid first argument for Put");
                }
            }

            else if ( state == 104 ) {
                if ( isParam( word ) ) {// valid argument for Sto
                    // generate VPL code 33 n a
                    vpl.add( 33 );
                    vpl.add( processVar( word, globsList ) );
                    vpl.add( scratch );

                    state = 1;
                }
                else {
                    error("[" + word + "] is not a valid argument for Sto");
                }
            }

        }// loop to scan all words in Jive source file

        // finish off last function definition
        // add count of locals to info
        System.out.println("Local cells for last function, " + currentFuncName + ":" );
        showLocals();

        //  (count used for "4" command, so leave out the parameters)
        info.add( new StringIntPair( "#" + currentFuncName,
                locsList.size() - currentFuncNumParams ) );

        // display vpl before filling holes:
        for ( int k=0; k<vpl.size(); k++) {
            System.out.printf("%4d %6d\n", k, vpl.get(k) );
        }
        System.out.println();

        // display the holes and info:
        System.out.println("Holes:");
        for (StringIntPair hole : holes) {
            System.out.printf("%6d %s\n", hole.x, hole.s);
        }
        System.out.println("Info:");
        for (StringIntPair pair : info) {
            System.out.printf("%6d %s\n", pair.x, pair.s);
        }

        // fill the holes
        for (StringIntPair pair : holes) {
            // scan info until find the value to fill the hole with
            int index = findString(pair.s, info);
            vpl.set(pair.x, index);
        }

        // breaking into lines is just for human readability,
        // JiveVPL reads all ints directly into mem

        // scan vpl to line-oriented output file

        int ip = 0;
        while ( ip < vpl.size() ) {
            int op = vpl.get( ip );
            if ( op==26 || op==29 ) {// operations with 0 arguments
                output.println( op );
                ip++;
            }
            else if ( op==2 || op==3 || op==4 || op==5 || op==6 ||
                    op==7 || op==27 || op==28 || op==30 || op==32 ) {// ops with 1 arg
                output.print( op + " " );  ip++;
                output.println( vpl.get(ip) + " " );  ip++;
            }
            else if ( op==8 || op==20 || op==21 || op==22 || op==23 ||
                    op==31 || op==33 || op==34 ) {// ops with 2 args
                output.print( op + " " );  ip++;
                output.print( vpl.get(ip) + " " );  ip++;
                output.println( vpl.get(ip) + " " );  ip++;
            }
            else if ( (9<=op && op<=19) || op==24 || op==25 ) {
                output.print( op + " " );  ip++;
                output.print( vpl.get(ip) + " " );  ip++;
                output.print( vpl.get(ip) + " " );  ip++;
                output.println( vpl.get(ip) + " " );  ip++;
            }
            else {
                error("[" + op + "] is an invalid operation code");
            }

        }

        output.close();
        input.close();

    }// main

    // return whether w starts with lowercase,
    // followed by 0 or more letters or digits
    private static boolean isParam( String w ) {

        if ( w.length() == 0 ) return false;

        if ( ! ( 'a' <= w.charAt(0) && w.charAt(0) <= 'z' ) ) return false;

        if ( w.length() ==1 ) return true;

        for (int k=1; k<w.length(); k++) {
            char x = w.charAt(k);
            if ( !letter(x) && !digit(x) )  return false;
        }

        return true;

    }

    // return whether w starts with uppercase,
    // followed by 0 or more letters or digits
    private static boolean isFuncName( String w ) {

        if ( w.length() == 0 ) return false;

        if ( ! ( 'A' <= w.charAt(0) && w.charAt(0) <= 'Z' ) ) return false;

        if ( w.length() == 1 ) return true;

        for (int k=1; k<w.length(); k++) {
            char x = w.charAt(k);
            if ( !letter(x) && !digit(x) ) return false;
        }

        return true;

    }

    private static boolean isLabel( String w ) {
        if ( ! w.endsWith( ":" ) ) {
            return false;
        }
        else if ( w.length() < 2 || ! lowercase( w.charAt(0) ) ) {
            return false;
        }
        else {
            for (int k=1; k<w.length()-1; k++) {
                char x = w.charAt(k);
                if ( !letter(x) && !digit(x) ) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean lowercase( char x ) {
        return 'a'<=x && x<='z';
    }

    private static boolean uppercase( char x ) {
        return 'A'<=x && x<='Z';
    }

    private static boolean letter( char x ) {
        return lowercase(x) || uppercase(x);
    }

    private static boolean digit( char x ) {
        return '0'<=x && x<='9';
    }

    private static String[] bifs2 = { "Add", "Sub", "Mult", "Quot", "Rem",
            "Eq", "NotEq", "Less", "LessEq",
            "And", "Or",
            "Get" };

    private static String[] bifs1 = { "Not", "Opp", "New" };

    private static int findBIF2( String word ) {
        int loc = -1;
        for (int k=0; k<bifs2.length && loc < 0; k++) {
            if ( word.equals( bifs2[k] ) ) {
                loc = k;
            }
        }
        return loc;
    }

    private static int findBIF1( String word ) {
        int loc = -1;
        for (int k=0; k<bifs1.length && loc < 0; k++) {
            if ( word.equals( bifs1[k] ) ) {
                loc = k;
            }
        }
        return loc;
    }

    // return whether word is an int literal or
    //  a parameter
    private static boolean isVar( String word ) {
        return isParam(word) || isInt(word);
    }

    // given word which is a variable name or an int
    // literal, search for it in list (which will be
    // either locsList or globsList) and if found, just
    // return its location, otherwise append to end,
    // and if is an int literal, generate the 22 command to
    // create the literal, and return its location
    private static int processVar( String word, ArrayList<String> list ) {

        for (int k=0; k<list.size(); k++) {
            if ( word.equals( list.get(k) ) ) {
                // found word in the list
                return k;
            }
        }

        // if still here, word was not found, process it further
        if ( isInt(word) ) {// is an int literal, not in list
            list.add( word );
            // add code to do command 22
            vpl.add( 22 );
            vpl.add( list.size() - 1 );
            vpl.add( Integer.parseInt( word ) );

            return list.size()-1;
        }
        else {// is a newly discovered variable
            list.add( word );
            return list.size()-1;
        }

    }// processVar

    private static boolean isInt( String s ) {
        boolean result;
        try {
            int x = Integer.parseInt( s );
            result = true;
        }
        catch( Exception e ) {
            result = false;
        }
        return result;
    }

    private static void showLocals() {
        for (int k=0; k<locsList.size(); k++) {
            System.out.printf("%4d %s\n", k, locsList.get(k) );
        }
    }

    // find item in list with string matching w, and return
    // its matching integer,
    // or report error
    private static int findString( String w, ArrayList<StringIntPair> list ) {
        for (StringIntPair aList : list) {
            if (aList.s.equals(w)) {
                return aList.x;
            }
        }
        // if still here, didn't find
        error("could not find info with string [" + w + "]");
        return -1;
    }

    private static void error( String message ) {
        System.out.println( message );
        System.exit(1);
    }

    public static void main2(String[] args) {
        String w = "A37";
        System.out.println("func?" + isFuncName( w ) );
        System.out.println("var?" + isVar( w ) );
        System.out.println("param?" + isParam( w ) );
        System.out.println("label?" + isLabel( w ) );
    }

}
