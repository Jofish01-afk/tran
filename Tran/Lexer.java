package Tran;

import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;

public class Lexer {

    private TextManager text;
    private HashMap<String, Token.TokenTypes> keywords = new HashMap<>(); //hashmap for storing keywords
    private HashMap<String, Token.TokenTypes> punctuation = new HashMap<>();//hashmap for storing punctuation
    private int lineNumber = 1; //tracks line number in a document; increments with '\n'
    private int columnNumber = 0; //character position

    private int indentationLevel = 0;


    /**
     * Constructor class for lexer. Populates hashmap for defined keywords
     * @param input
     */
    public Lexer(String input) {
        this.text = new TextManager(input);
        System.out.println("Lexer Started");
        System.out.println("==================================================");
        System.out.println("Raw Input: [" + input + "]"); //debugging

        //populate keywords
        keywords.put("implements", Token.TokenTypes.IMPLEMENTS);
        keywords.put("class", Token.TokenTypes.CLASS);
        keywords.put("interface", Token.TokenTypes.INTERFACE);
        keywords.put("loop", Token.TokenTypes.LOOP);
        keywords.put("if", Token.TokenTypes.IF);
        keywords.put("else", Token.TokenTypes.ELSE);
        keywords.put("new", Token.TokenTypes.NEW);
        keywords.put("private", Token.TokenTypes.PRIVATE);
        keywords.put("shared", Token.TokenTypes.SHARED);
        keywords.put("construct", Token.TokenTypes.CONSTRUCT);

        //populate punctuation
        punctuation.put("=", Token.TokenTypes.ASSIGN);
        punctuation.put("(", Token.TokenTypes.LPAREN);
        punctuation.put(")", Token.TokenTypes.RPAREN);
        punctuation.put(":", Token.TokenTypes.COLON);
        punctuation.put(".", Token.TokenTypes.DOT);

        punctuation.put("+", Token.TokenTypes.PLUS);
        punctuation.put("-", Token.TokenTypes.MINUS);
        punctuation.put("*", Token.TokenTypes.TIMES);
        punctuation.put("/", Token.TokenTypes.DIVIDE);
        punctuation.put("%", Token.TokenTypes.MODULO);
        punctuation.put(",", Token.TokenTypes.COMMA);

        punctuation.put("==", Token.TokenTypes.EQUAL);
        punctuation.put("!=", Token.TokenTypes.NOTEQUAL);
        punctuation.put("<", Token.TokenTypes.LESSTHAN);
        punctuation.put("<=", Token.TokenTypes.LESSTHANEQUAL);
        punctuation.put(">", Token.TokenTypes.GREATERTHAN);
        punctuation.put(">=", Token.TokenTypes.GREATERTHANEQUAL);

    }

    //"pseudocode"
//    else if (!Character.isLetter(c)) { //check if its not a letter
//        if (!currentWord.isEmpty()) { //and if the current word is not an empty string
//
//            Token token = new Token(Token.TokenTypes.WORD, 0, 0, currentWord); //create token for words
//            listOfTokens.add(token); //add to list
//        }
//        currentWord = ""; //reset word
//    }

    //                if((c == '\t')) { //for checking tab
//                    readIndent();
//                } else if (Character.isWhitespace(c)) { //here we wanna check for four spaces only after making a newline token
//                    boolean isTab = true; //is tab is consists of '   ' (four spaces) assume true
//                    for(int i = 1; i <= 4; i++){ //check if it's true by checking is all consecutive characters are spaces
//                        if (text.getCharacter() != ' '){
//                            isTab = false;
//                            break;
//                        }
//                    }
//                    if (isTab){
//                        readIndent();
//                    }
//                }

    /**
     * A lexical algorithm that goes through a document and
     * tokenizes words through other functions. Populates a linked list of tokens created
     *
     * @return list of tokens
     * @throws Exception syntax error of improperly closed literals
     */
    public List<Token> Lex() throws Exception {
        List<Token> listOfTokens = new LinkedList<>();
        char c; //stores the character being read

        while (!text.isAtEnd()) { //iterates until the end of document
            c = text.peekCharacter();

            if (Character.isLetter(c)) { //if the character is a letter, we call readWord() to tokenize it
                listOfTokens.add(readWord());

            } else if (c == '\n') { //for newlines
                text.getCharacter();
                columnNumber = 0;
                lineNumber++;
                listOfTokens.add(new Token(Token.TokenTypes.NEWLINE, lineNumber, columnNumber, "\\n"));


                //System.out.println("NEWLINE detected. Next character: " + text.peekCharacter());

                //if(text.peekCharacter() == '\t' || text.peekCharacter() == ' ') {
                //System.out.println("Calling readIndent()...");
                readIndent(listOfTokens); //only check for indentation after newlines
                //}

            } else if (Character.isWhitespace(c)) { //for spaces
                text.getCharacter();
                columnNumber++;

            } else if (Character.isDigit(c)) { //parse reading a number
                listOfTokens.add(readNumber());

            } else if (c == '.') { //when we get to a '.' this will determine whether its part of a number, or if its punctuation
                char nextChar = text.peekCharacter(1); //get char after the "."
                if(Character.isDigit(nextChar)){
                    listOfTokens.add(readNumber());
                } else {
                    listOfTokens.add(readPunctuation());
                }

            } else if (c == '\''){ //quoted characters

                if (text.getCharacter() != '\''){
                    throw new SyntaxErrorException("Unclosed character literal", lineNumber, columnNumber);
                }
                listOfTokens.add(new Token(Token.TokenTypes.QUOTEDCHARACTER, lineNumber, columnNumber, String.valueOf(text.getCharacter())));

            } else if (c == '\"'){ //for quoted strings
                listOfTokens.add(readQuotedString());

            } else if(c == '{') { //for comments
                readComments();

            } else { //for punctuation
                listOfTokens.add(readPunctuation());
            }
        }

        System.out.println("Before adding final dedentation:" + indentationLevel);

        //adds any remaining dedentation when the document ends on an indent
        while (indentationLevel > 0){
            listOfTokens.add(new Token(Token.TokenTypes.DEDENT, lineNumber, columnNumber, String.valueOf(indentationLevel)));
            System.out.println("Added DEDENT");
            indentationLevel-=4;
        }


        System.out.println(listOfTokens); //debugging
        return listOfTokens;
    }

    /**
     * Uses the original lex() algorithm described in the "Writing a lexer" document
     * Function is called when a letter is "read", and will continue to process the word.
     * When the word is processed, it will be checked to see if it is a defined keyword
     *
     * @return either WORD token or KEYWORD token
     */
    private Token readWord(){ //original lex() algorithm for processing word tokens
        String currentWord = ""; //stores current word being read (originally in lex())

        while(!text.isAtEnd()) { //iterates through document
            if(Character.isLetter(text.peekCharacter())) {//if the character is a letter then we can continue
                currentWord += text.getCharacter();

            } else if (Character.isDigit(text.peekCharacter())) { //for cases in which a string has a digit like "Example1"
                currentWord += text.getCharacter();
            }else { //otherwise do nothing
                break;
            }
        }
        /*
            Used to check keywords via the hashmap keywords.
            if the current word that is being read is a keyword that was defined it will create a token of the corresponding
            keyword.
         */
        if (keywords.containsKey(currentWord)) {
            return new Token(keywords.get(currentWord), lineNumber, columnNumber, currentWord);

        } else {
            return new Token(Token.TokenTypes.WORD, lineNumber, columnNumber, currentWord); //otherwise it's a WORD token

        }
    }

    /**
     * Method that processes numbers, including decimals and negative numbers
     * @return NUMBER token
     */
    private Token readNumber() {
        String currentNumber = ""; //store current number being read
        boolean hasDecimalPoint = false; //stores info whether the number has a decimal. useful if a number has multiple decimal points
        boolean isNegative = false; //stores info whether the number is negative

        //if the number has a negative sign, it will be marked as a negative number
        if (text.peekCharacter() == '-') {
            isNegative = true;
            currentNumber += text.getCharacter();
            columnNumber++;
        }

        while (!text.isAtEnd()) {
            if (Character.isDigit(text.peekCharacter())) {
                currentNumber += text.getCharacter();
                columnNumber++;
            } else if (text.peekCharacter() == '.' && !hasDecimalPoint) { //a special case (3.4.5 or 3..4), that checks if a decimal point has already been seen in the number before
                hasDecimalPoint = true;
                currentNumber += text.getCharacter();
                columnNumber++;
            } else {
                break;
            }
        }

        //if it was marked as a negative number, it will add it to the number string
        if (isNegative) {
            return new Token(Token.TokenTypes.NUMBER, lineNumber, columnNumber, "-" + currentNumber);
        }
        //otherwise it's just a positive number
        return new Token(Token.TokenTypes.NUMBER, lineNumber, columnNumber, currentNumber);
    }

    /**
     * Processes punctuation, assumes that it's a two character length punctuation by peeking for the next
     * character. If it's a valid double length punctuation token, otherwise assume that it's a single
     * length token and tokenize it. If neither, throw a syntax error as it's not valid.
     * @return punctuation tokens
     * @throws SyntaxErrorException for unrecognized symbols
     */
    private Token readPunctuation() throws SyntaxErrorException {
        char firstChar = text.getCharacter(); // Consume first character
        columnNumber++;

        //first check for a second character (== or <=, etc.)
        if (!text.isAtEnd()) {
            char secondChar = text.peekCharacter(); //check second
            String doublePunctuation = "" + firstChar + secondChar; //combine both characters

            //now check if it's a validly defined punctuation
            if (punctuation.containsKey(doublePunctuation)) { //via hashmap
                text.getCharacter(); //consume 2nd char
                columnNumber++;
                return new Token(punctuation.get(doublePunctuation), lineNumber, columnNumber, doublePunctuation); //if valid then return it
            }
        }

        //if not a valid two character punctuation, then assume it's a single char then tokenize it
        String singlePunctuation = String.valueOf(firstChar);
        if (punctuation.containsKey(singlePunctuation)) {
            return new Token(punctuation.get(singlePunctuation), lineNumber, columnNumber, singlePunctuation);
        }

        //finally, if the punctuation is not recognized, it a syntax error
        throw new SyntaxErrorException("Unrecognized punctuation", lineNumber, columnNumber);
    }

    /**
     * Processes indentation only after newlines are read. Compares previous indentation level.
     * If indentation on previous line is greater than the current line, it's a DEDENT
     * If indentation on the previous line is less that the current line, it's an INDENT
     * @param listOfTokens (uses the same list so it can directly add indent tokens to it)
     */
    private void readIndent(List<Token> listOfTokens) {
        //fixes newline bug and indentation where it would generate unnecessary tokens after a newline such as an empty line
        if (!text.isAtEnd() && text.peekCharacter() == '\n') {
            return;
        }

        boolean blankLine = false; //detects blank lines without changing indentation
        int newIndent = 0; //current indentation level
        while(!text.isAtEnd()) {
            if(text.peekCharacter() == ' '){ //checks for consecutive whitespaces
                newIndent++;
                //System.out.println("Detecting indentation");
            }
            else if (text.peekCharacter() == '\t'){ //treat tabs as four spaces
                //System.out.println("\\t DETECTED FINALLY"); //debugging
                newIndent += 4;
            } else if (text.peekCharacter() == '\n') { //break loop if new line is encountered
                blankLine = true;
                break;
            } else {
                break;
            }

            text.getCharacter(); //consume character
        }
        //System.out.println("New indent: " + newIndent + ", Previous indent: " + indentationLevel);

        //break out of loop if there is a blank line, otherwise continue with generating indentation
        if (blankLine){
            return;
        }


        if (newIndent > indentationLevel){ //for creating indentation tokens
            int indent = (newIndent - indentationLevel)/4;
            for (int i = 0; i < indent; i++){
                listOfTokens.add(new Token(Token.TokenTypes.INDENT, lineNumber, columnNumber, String.valueOf(newIndent)));
            }
             //System.out.println(listOfTokens);
        } else if (newIndent < indentationLevel){ //for creating dedentation tokens
            int dedent = (indentationLevel - newIndent)/4;
            for (int i = 0; i < dedent; i++) {
                listOfTokens.add(new Token(Token.TokenTypes.DEDENT, lineNumber, columnNumber, String.valueOf(newIndent)));
                System.out.println("Added Dedent Token");
                //System.out.println(listOfTokens);
            }
        }
        indentationLevel = newIndent; //set indentation to current indentation level
    }

    /**
     * Algorithm that will handle strings with quotes, with escape sequence support.
     * @return quoted string token
     * @throws SyntaxErrorException for unsupported characters or improper closing of quotes
     */
    private Token readQuotedString() throws SyntaxErrorException {
        String currentString = "";  //storing currently read string
        char currentChar = text.getCharacter(); // Consume the opening quote

        while (true) {
            if (text.isAtEnd()) {
                throw new SyntaxErrorException("Unclosed string literal", lineNumber, columnNumber);
            }

            currentChar = text.getCharacter();
            if (currentChar == '"') {
                break;
            }

            if (currentChar == '\\') { //escape sequences
                if (text.isAtEnd()) {
                    throw new SyntaxErrorException("Unclosed escape sequence", lineNumber, columnNumber);
                }

                char escapeChar = text.getCharacter();
                switch (escapeChar) { //handles different types of escape characters
                    case '\\':
                        currentString += "\\";
                        break;
                    case 'n':
                        currentString += "\n";
                        break;
                    case 't':
                        currentString += "\t";
                        break;
                    case 'r':
                        currentString += "\r";
                        break;
                    case '"':
                        currentString += "\"";
                        break;
                    default:
                        throw new SyntaxErrorException("Unsupported escape sequence: \\" + escapeChar, lineNumber, columnNumber);
                }
            } else {
                currentString += currentChar; //add character to the string
            }
        }
        //then return the string as a token
        return new Token(Token.TokenTypes.QUOTEDSTRING, lineNumber, columnNumber, currentString);
    }

    /**
     * Simple method to process comments. It will just ignore them, meaning no tokenization
     * @throws SyntaxErrorException if comment is closed incorrectly
     */
    private void readComments() throws SyntaxErrorException {
        char currentChar = text.getCharacter();

        while (currentChar != '}') { //closing sequence for comment
            if (text.isAtEnd()) {
                throw new SyntaxErrorException("Unterminated comment", lineNumber, columnNumber); //throw exception
            }
            currentChar = text.getCharacter();
        }


    }
}
