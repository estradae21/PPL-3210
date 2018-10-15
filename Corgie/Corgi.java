import java.util.*;
import java.io.*;
public class Lexer {

   public static String margin = "";

   // holds any number of tokens that have been put back
   private Stack<Token> stack;

   // the source of physical symbols
   // (use BufferedReader instead of Scanner because it can
   //  read a single physical symbol)
   private BufferedReader input;

   // one lookahead physical symbol
   private int lookahead;
 
   // construct a Lexer ready to produce tokens from a file
   public Lexer( String fileName ) {
     try {
       input = new BufferedReader( new FileReader( fileName ) );
     }
     catch(Exception e) {
       error("Problem opening file named [" + fileName + "]" );
     }
     stack = new Stack<Token>();
     lookahead = 0;  // indicates no lookahead symbol present
   }// constructor
 
   // produce the next token
   private Token getNext() {
      if( ! stack.empty() ) {
         //  produce the most recently putback token
         Token token = stack.pop();
         return token;
      }
      else {
         // produce a token from the input source

         int state = 1;  // state of FA
         String data = "";  // specific info for the token
         boolean done = false;  
         int sym;  // holds current symbol
 
         do {
            sym = getNextSymbol();

// System.out.println("current symbol: " + sym + " state = " + state );

            if ( state == 1 ) {
               if ( sym == 9 || sym == 10 || sym == 13 ||
                    sym == 32 ) {// whitespace
                  state = 1;
               }
               else if ( 'a'<=sym && sym<='z' ) {// lowercase
                  data += (char) sym;
                  state = 2;
               }
               else if ( digit( sym ) ) {
                  data += (char) sym;
                  state = 3;
               }
               else if ( sym == '.' ) {
                  data += (char) sym;
                  state = 5;
               }
               else if ( sym == '\"' ) {
                 state = 6;
               }
               else if ( sym == '+' || sym == '-' || sym == '*' ||
                         sym == '/' || sym == '(' || sym == ')' ||
                         sym == ',' || sym == '='
                       ) {
                  data += (char) sym;
                  state = 8;
                  done = true;
               }
               else if ( sym == -1 ) {// end of file
                  state = 9;
                  done = true;
               }
               else {
                 error("Error in lexical analysis phase with symbol "
                                      + sym + " in state " + state );
               }
            }
 
            else if ( state == 2 ) {
               if ( letter(sym) || digit(sym) ) {
                  data += (char) sym;
                  state = 2;
               }
               else {// done with variable token
                 putBackSymbol( sym );
                 done = true;
               }
            }

            else if ( state == 3 ) {
               if ( digit(sym) ) {
                  data += (char) sym;
                  state = 3;
               }
               else if ( sym == '.' ) {
                  data += (char) sym;
                  state = 4;
               }
               else {// done with number token
                 putBackSymbol( sym );
                 done = true;
               }

            }

            else if ( state == 4 ) {
               if ( digit(sym) ) {
                  data += (char) sym;
                  state = 4;
               }
               else {// done with number token
                 putBackSymbol( sym );
                 done = true;
               }
            }

            else if ( state == 5 ) {
               if ( digit(sym) ) {
                  data += (char) sym;
                  state = 4;
               }
               else {
                 error("Error in lexical analysis phase with symbol "
                                      + sym + " in state " + state );
               }
            }

            else if ( state == 6 ) {
               if ( (' '<=sym && sym<='~') && sym != '\"' ) {
                  data += (char) sym;
                  state = 6;     
               }
               else if ( sym == '\"' ) {
                  state = 7;
                  done = true;
               }
            }

            // note: states 7, 8, and 9 are accepting states with
            //       no arcs out of them, so they are handled
            //       in the arc going into them
         }while( !done );
       
         // generate token depending on stopping state
         Token token;
 
         if ( state == 2 ) {
            // see if data matches any special words
            if ( data.equals("input") ) {
               return new Token( "bif0", data );
            }
            else if ( data.equals("sqrt") || data.equals("cos") || 
                      data.equals("sin") || data.equals("atan") 
                    ) {
               return new Token( "bif1", data );
            }
            else if ( data.equals("pow") ) {
               return new Token( "bif2", data );
            }
            else if ( data.equals("print") ) {
               return new Token( "print", "" );
            }
            else if ( data.equals("newline") ) {
               return new Token( "newline", "" );
            }
            else {// is just a variable
               return new Token( "var", data );
            }
         }
         else if ( state == 3 || state == 4 ) {
            return new Token( "num", data );
         }
         else if ( state == 7 ) {
            return new Token( "string", data );
         }
         else if ( state == 8 ) {
            return new Token( "single", data );
         }
         else if ( state == 9 ) {
            return new Token( "eof", data );
         }

         else {// Lexer error
           error("somehow Lexer FA halted in bad state " + state );
           return null;
        }

     }// else generate token from input
 
   }// getNext
   
   public Token getNextToken() {
     Token token = getNext();
     System.out.println("                     got token: " + token );
     return token;
   }
 
   public void putBackToken( Token token )
   {
     System.out.println( margin + "put back token " + token.toString() );
     stack.push( token );
   }
 
   // next physical symbol is the lookahead symbol if there is one,
   // otherwise is next symbol from file
   private int getNextSymbol() {
     int result = -1;
 
     if( lookahead == 0 ) {// is no lookahead, use input
       try{  result = input.read();  }
       catch(Exception e){}
     }
     else {// use the lookahead and consume it
       result = lookahead;
       lookahead = 0;
     }
     return result;
   }
 
   private void putBackSymbol( int sym ) {
     if( lookahead == 0 ) {// sensible to put one back
       lookahead = sym;
     }
     else {
       System.out.println("Oops, already have a lookahead " + lookahead +
            " when trying to put back symbol " + sym );
       System.exit(1);
     }
   }// putBackSymbol
 
   private boolean letter( int code ) {  
      return 'a'<=code && code<='z' ||
             'A'<=code && code<='Z';
   }
 
   private boolean digit( int code ) {  
     return '0'<=code && code<='9';
   }
 
   private boolean printable( int code ) {  
     return ' '<=code && code<='~';
   }
 
   private static void error( String message ) {
     System.out.println( message );
     System.exit(1);
   }
 
   public static void main(String[] args) throws Exception {
     System.out.print("Enter file name: ");
     Scanner keys = new Scanner( System.in );
     String name = keys.nextLine();
 
     Lexer lex = new Lexer( name );
     Token token;
 
     do{
       token = lex.getNext();
       System.out.println( token.toString() );
     }while( ! token.getKind().equals( "eof" )  );
     
   }
 
}