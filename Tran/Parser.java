package Tran;
import AST.*;

import java.util.*;

public class Parser {

    private final TokenManager tokenManager;
    private TranNode tranNode; //the node for the language, top of the AST

    /**
     * Constructor method for parser
     * @param top of AST
     * @param tokens for list of tokens from lexer
     */
    public Parser(TranNode top, List<Token> tokens) {
        this.tranNode = top;
        this.tokenManager = new TokenManager(tokens);
    }

    /**
     * require a new line token, allow for multiple
     */
    public void requireNewLine() throws SyntaxErrorException{
        boolean foundNewline = false;

        while (tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {
            foundNewline = true;
        }

        if (foundNewline || tokenManager.peek(0).orElseThrow().getType() == Token.TokenTypes.DEDENT) { //dedent "issue" fix
            return;
        }

        throw new SyntaxErrorException("Expected a newline: ", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
    }

    public void requireIndent() throws SyntaxErrorException{
        if ((tokenManager.matchAndRemove(Token.TokenTypes.INDENT)).isEmpty()) {
            throw new SyntaxErrorException("Expected an indentation: ", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
    }

    public void requireDedent() throws SyntaxErrorException{
        if ((tokenManager.matchAndRemove(Token.TokenTypes.DEDENT)).isEmpty()) {
            throw new SyntaxErrorException("Expected a dedentation: ", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

    }

    /**
     * @return interface node
     * @throws SyntaxErrorException
     */
    //identifier is WORD
    //Interface = "interface" IDENTIFIER NEWLINE INDENT MethodHeader* DEDENT
    private Optional<InterfaceNode> parseInterface() throws SyntaxErrorException {


        if ((tokenManager.matchAndRemove(Token.TokenTypes.INTERFACE)).isEmpty()){
            return Optional.empty(); //not an interface, quit and change nothing
        }

        //store name token
        Optional<Token> interfaceNameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);

        if (interfaceNameToken.isEmpty()){
            throw new SyntaxErrorException("Expected identifier \"word\"", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        String interfaceName = interfaceNameToken.get().getValue(); //store it as a string and then add it to the interface node as a name

        requireNewLine();

        //indent
        requireIndent();

        InterfaceNode interfaceNode = new InterfaceNode(); //create interface node
        interfaceNode.name = interfaceName; //assign the name to the interface

        while (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() != Token.TokenTypes.DEDENT) {
            Optional<MethodHeaderNode> methodHeader = parseMethodHeader();
            if (methodHeader.isPresent()) {
                interfaceNode.methods.add(methodHeader.get());
            } else {
                throw new SyntaxErrorException("Expected method header in interface", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
        }



        //dedent
        requireDedent();
        //requireNewLine(); //for some reason allows for more than one interface to be parsed

        return Optional.of(interfaceNode);
    }
    //optional :return type
    //MethodHeader = IDENTIFIER "(" ParameterVariableDeclarations ")" (":" ParameterVariableDeclarations)? NEWLINE
//    public String name;
//    public List<VariableDeclarationNode> parameters = new ArrayList<>();
//    public List<VariableDeclarationNode> returns = new ArrayList<>();

    //implement separate lists for parameters and returns

    private Optional<MethodHeaderNode> parseMethodHeader() throws SyntaxErrorException {

        //store method name in token
        Optional<Token> methodNameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);

        if (methodNameToken.isEmpty()){
            throw new SyntaxErrorException("Expected identifier \"word\"", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //assign method name token to a string
        String methodName = methodNameToken.get().getValue();

        System.out.println("Next token: " + tokenManager.peek(0));
        if ((tokenManager.matchAndRemove(Token.TokenTypes.LPAREN)).isEmpty()){
            throw new SyntaxErrorException("Expected: \"(\"", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //create a methodheaderNode with the attributes: name and parameters
        MethodHeaderNode methodHeaderNode = new MethodHeaderNode();
        methodHeaderNode.name = methodName;
        methodHeaderNode.parameters = parseParameters();

        if ((tokenManager.matchAndRemove(Token.TokenTypes.RPAREN)).isEmpty()){
            throw new SyntaxErrorException("Expected: \")\"", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //colon is used for return values, if colon present, treat them as return
        System.out.println("Checking for return values -> Next token: " + tokenManager.peek(0));
        if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.COLON){
            tokenManager.matchAndRemove(Token.TokenTypes.COLON);
            System.out.println("Next token: " + tokenManager.peek(0));
            List<VariableDeclarationNode> returns = new ArrayList<>();

            Optional<VariableDeclarationNode> returnVariable = parseVariableDeclaration();
            if (returnVariable.isPresent()){
                returns.add(returnVariable.get());
            } else {
                throw new SyntaxErrorException("Expected a variable declaration after colon ", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            //not in ebnf but important for additional returns
            while (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.COMMA) {
                tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
                returnVariable = parseVariableDeclaration();
                if (returnVariable.isPresent()){
                    returns.add(returnVariable.get());
                } else {
                    throw new SyntaxErrorException("Expected variable declaration for return value after comma", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
            }
            methodHeaderNode.returns = returns;
        }

        System.out.println("Checked for newline in method header -> Next token: " + tokenManager.peek(0));
        requireNewLine();

        return Optional.of(methodHeaderNode);
    }

    /**
     * For declaring multiple parameters
     * @return
     * @throws SyntaxErrorException
     */
    //ParameterVariableDeclarations =  VariableDeclaration  ("," VariableDeclaration)*
    private List<VariableDeclarationNode> parseParameters() throws SyntaxErrorException {
        List<VariableDeclarationNode> params = new ArrayList<>(); //store params in a list

        //if the next character is a right parenthesis, then that means there are no parameters, so quit method
        if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.RPAREN){
            return params;
        }

        //parse the first parameter.
        Optional<VariableDeclarationNode> firstParam = parseVariableDeclaration();
        if (firstParam.isPresent()) {
            params.add(firstParam.get());
        } else {
            throw new SyntaxErrorException("Expected parameter declaration",
                    tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }


        //check for more if the token list is not empty and there's a comma
        while (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.COMMA) {
            tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
            Optional<VariableDeclarationNode> nextParam = parseVariableDeclaration();


            if (nextParam.isPresent()) {
                params.add(nextParam.get());
            } else {
                throw new SyntaxErrorException("Expected parameter declaration after comma",
                        tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
        }


        return params; //returns the list of parameters
    }

    /**
     * For defining what a parameter is
     * @return
     * @throws SyntaxErrorException
     */
    //ParameterVariableDeclaration = IDENTIFIER IDENTIFIER
    private Optional<VariableDeclarationNode> parseVariableDeclaration() throws SyntaxErrorException {
        System.out.println("Checking for variable declaration -> current token: " + tokenManager.peek(0));
        System.out.println("Next token -> " + tokenManager.peek(1));

        if(tokenManager.peek(0).isPresent() && tokenManager.peek(1).isPresent()
        && !(tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD && tokenManager.peek(1).get().getType() == Token.TokenTypes.WORD)){
            System.out.println("Not a variable declaration");
            return Optional.empty();
            //fixes member declaration member bug
        }
        //store token for the type
        Optional<Token> typeToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);

        if (typeToken.isEmpty()){
            throw new SyntaxErrorException("Expected type identifier", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //get value from type token
        String typeIdentifier = typeToken.get().getValue();



        //store token for the name
        Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);

        if ((nameToken).isEmpty()){
            throw new SyntaxErrorException("Expected name identifier", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //get name from token
        String nameIdentifier = nameToken.get().getValue();

        //add both attributes to parameter declaration node
        VariableDeclarationNode parameterDeclarationNode = new VariableDeclarationNode();
        parameterDeclarationNode.name = nameIdentifier;
        parameterDeclarationNode.type = typeIdentifier;

        System.out.println("Variable declared: " + parameterDeclarationNode );
        return Optional.of(parameterDeclarationNode); //want to return type and name
    }

    //implement class, constructor, member, methodDeclaration, and statements

    //Class = "class" Identifier [ "implements" Identifier { "," Identifier } ] NEWLINE INDENT { Constructor NEWLINE | MethodDeclaration NEWLINE | Member NEWLINE } DEDENT

//    public String name;  className
//    public List<String> interfaces = new ArrayList<>(); implementsList
//
//    public List<ConstructorNode> constructors = new ArrayList<>();
//    public List<MethodDeclarationNode> methods = new ArrayList<>();
//    public List<MemberNode> members = new ArrayList<>();
    public Optional<ClassNode> parseClass() throws SyntaxErrorException {

        if ((tokenManager.matchAndRemove(Token.TokenTypes.CLASS)).isEmpty()){
            return Optional.empty(); //not a class, quit and change nothing
        }

        Optional<Token> classNameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (classNameToken.isEmpty()){
            throw new SyntaxErrorException("Expected class identifier", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        String className = classNameToken.get().getValue();

        ClassNode classNode = new ClassNode();
        classNode.name = className;

        //parse implements if present
        if((tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.IMPLEMENTS)){
            tokenManager.matchAndRemove(Token.TokenTypes.IMPLEMENTS);
            Optional<Token> implementsToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);


            if (implementsToken.isEmpty()) {
                throw new SyntaxErrorException("Expected implements identifier", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }

            List<String> implementsList = new ArrayList<>();
            implementsList.add(implementsToken.get().getValue());

            while(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.COMMA){
                tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
                Optional<Token> nextImplements = tokenManager.matchAndRemove(Token.TokenTypes.WORD);

                if (nextImplements.isEmpty()){
                    throw new SyntaxErrorException("Expected implements identifier", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
                implementsList.add(nextImplements.get().getValue());

            }

            classNode.interfaces = implementsList;
        }

        requireNewLine();
        requireIndent();

        //body
        //{ Constructor | MethodDeclaration | Member } DEDENT
        //method declaration ends in a dedent... maybe change it
        //REQUIRING NEWLINES CAUSES ISSUES
        while (tokenManager.peek(0).isPresent() && (tokenManager.peek(0).get().getType() != Token.TokenTypes.DEDENT)) { //third condition doesnt work
            System.out.println("Entered method body in parse class");

            while(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE){
                System.out.println("removed newline");
                tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            }
            System.out.println(tokenManager.peek(0).get().getType());

            Optional<ConstructorNode> constructor = parseConstructor();
            if (constructor.isPresent()){
                classNode.constructors.add(constructor.get());
                for (int i = 0; i < 6; i++) {
                    System.out.println("Token at +" + i + ": " + tokenManager.peek(i).map(Token::getType));
                }

                if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE) {
                    tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
                }
                continue; //run loop again
            }

            Optional<MethodDeclarationNode> methodDeclaration = parseMethodDeclaration();
            if (methodDeclaration.isPresent()){
                System.out.println("Method Declaration present");
                int counter = 0;
                classNode.methods.add(methodDeclaration.get());
                counter++;
                System.out.println(counter + " method(s) added");

                if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE) {
                    tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
                }
                continue;
            }

            //testExample()
            //private shared testExample

            for (int i = 0; i < 6; i++) {
                System.out.println("Token at +" + i + ": " + tokenManager.peek(i).map(Token::getType));
            }
            Optional<MemberNode> member = parseMember();
            if (member.isPresent()){
                classNode.members.add(member.get());

                if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE) {
                    tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
                }
                continue;
            }


            throw new SyntaxErrorException("Unexpected token in class body", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        }

        requireDedent();
        return Optional.of(classNode);
    }

    //Constructor = "construct" "(" VariableDeclarations ")" NEWLINE MethodBody
    //MethodBody = INDENT { VariableDeclaration NEWLINE } {Statement} DEDENT
//    public List<VariableDeclarationNode> parameters = new ArrayList<>(); parse parameters
//    public List<VariableDeclarationNode> locals = new ArrayList<>(); variable declarations
//    public List<StatementNode> statements = new ArrayList<>(); statements such as math computations
    private Optional<ConstructorNode> parseConstructor() throws SyntaxErrorException {
        ConstructorNode constructorNode = new ConstructorNode();
        System.out.println("Called parseConstructor...");
        System.out.println("Checking if constructor exists");
        if (tokenManager.matchAndRemove(Token.TokenTypes.CONSTRUCT).isEmpty()){
            System.out.println("No constructor found");
            return Optional.empty();
        }

        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()){
            throw new SyntaxErrorException("Expected left parenthesis", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //
        if (tokenManager.peek(0).isPresent() && !(tokenManager.peek(0).get().getType() == Token.TokenTypes.RPAREN)){
            System.out.println("Found a parameter");
            constructorNode.parameters = parseParameters();
        }

        if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()){
            throw new SyntaxErrorException("Expected right parenthesis", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        requireNewLine();

        //MethodBody = INDENT { VariableDeclaration NEWLINE } {Statement} DEDENT
        requireIndent();

        //methodBody should have the locals and statements

        //locals variables -> type nam, type = number or string
        while (tokenManager.peek(0).isPresent() && (tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD)
                && (tokenManager.peek(0).get().getValue().equals("string") || tokenManager.peek(0).get().getValue().equals("number"))) {
            Optional<VariableDeclarationNode> local = parseVariableDeclaration();
            if (local.isPresent()) {
                constructorNode.locals.add(local.get());
                requireNewLine();
            } else {
                break;
            }
        }
        //ISSUE FOUND, NOT PARSING STATEMENTS PROPERLY
        //statements
        System.out.println("Attempting to parse statement... Current token: " + tokenManager.peek(0).get().getType());

        Optional<StatementNode> statement = parseStatement();

        //System.out.println("Parsed statement: " + statement.get().toString());
        while (statement.isPresent()) {
            constructorNode.statements.add(statement.get());
            System.out.println("Parsed statement: " + statement.get().toString());
            statement = parseStatement();
            System.out.println("Attempting to parse next statement... Current token: " + tokenManager.peek(0).get().getType());
        }

        while(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE){
            System.out.println("removed newline");
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
        }

        requireDedent();

        System.out.println("Parsed constructorNode: " + constructorNode);
        return Optional.of(constructorNode);
    }

    //MethodBody = INDENT { VariableDeclaration NEWLINE } {Statement} DEDENT
//    public List<StatementNode> parseMethodBody() throws SyntaxErrorException{
//        requireIndent();
//
//        List<StatementNode> methodBody = new ArrayList<>();
//        List<VariableDeclarationNode> locals = new ArrayList<>();
//        List<StatementNode> statements = new ArrayList<>();
//
//        Optional<VariableDeclarationNode> localsVariable = parseVariableDeclaration();
//        while (localsVariable.isPresent()){
//            locals.add(localsVariable.get());
//            requireNewLine();
//            localsVariable = parseVariableDeclaration();
//        }
//
//        Optional<StatementNode> statementsVariable = parseStatement();
//        while (statementsVariable.isPresent()){
//            statements.add(statementsVariable.get());
//            statementsVariable = parseStatement();
//        }
//
//        requireDedent();
//
//
//        methodBody.addAll(statements);
//
//
//
//        return methodBody;
//    }

    //Member = VariableDeclaration ["accessor:" Statements] ["mutator:" Statements]
    //public VariableDeclarationNode declaration;
    public Optional<MemberNode> parseMember() throws SyntaxErrorException {
        System.out.println("Called parseMember...");
        MemberNode memberNode = new MemberNode();

        Optional<VariableDeclarationNode> declaration = parseVariableDeclaration();
        if (declaration.isEmpty()){
            return Optional.empty();
        }
        memberNode.declaration = declaration.get();

//        //accessor:
//        if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getValue().equals("accessor")){
//            tokenManager.matchAndRemove(Token.TokenTypes.WORD);
//            tokenManager.matchAndRemove(Token.TokenTypes.COLON);
//        }

        return Optional.of(memberNode);
    }


    //MethodDeclaration = ["private"] ["shared"] MethodHeader NEWLINE MethodBody
//    public boolean isShared;
//    public boolean isPrivate;
//    public String name;
//    public List<VariableDeclarationNode> parameters = new ArrayList<>();
//    public List<VariableDeclarationNode> returns = new ArrayList<>();
//    public List<VariableDeclarationNode> locals = new ArrayList<>();
//    public List<StatementNode> statements = new ArrayList<>();
    public Optional<MethodDeclarationNode> parseMethodDeclaration() throws SyntaxErrorException {
        System.out.println("Called parseMethodDeclaration...");
        for (int i = 0; i < 6; i++) {
            System.out.println("Token at +" + i + ": " + tokenManager.peek(i).map(Token::getType));
        }

        MethodDeclarationNode methodDeclarationNode = new MethodDeclarationNode();

        if (tokenManager.peek(0).isPresent() && (tokenManager.peek(0).get().getType() == Token.TokenTypes.PRIVATE)){
            tokenManager.matchAndRemove(Token.TokenTypes.PRIVATE);
            methodDeclarationNode.isPrivate = true;
        }

        if (tokenManager.peek(0).isPresent() && (tokenManager.peek(0).get().getType() == Token.TokenTypes.SHARED)){
            tokenManager.matchAndRemove(Token.TokenTypes.SHARED);
            methodDeclarationNode.isShared = true;
        }

        //helloWorld()
        //method header
        if (tokenManager.peek(0).isPresent()
                && tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD
                && tokenManager.peek(1).isPresent()
                && tokenManager.peek(1).get().getType() == Token.TokenTypes.LPAREN) {


            Optional<MethodHeaderNode> methodHeader = parseMethodHeader();
            if (methodHeader.isEmpty()) {
                return Optional.empty(); //not a method
            }

            methodDeclarationNode.name = methodHeader.get().name;
            methodDeclarationNode.parameters = methodHeader.get().parameters;
            methodDeclarationNode.returns = methodHeader.get().returns;


            //requireNewLine(); //dont need it cause method header already does that
            //method body
            requireIndent();

            if (tokenManager.peek(0).isPresent() && (tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE)){
                tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            }
            //locals variables -> type nam, type = number or string
            //check for local declarations
            System.out.println("Now checking for local variables");
            for (int i = 0; i < 6; i++) {
                System.out.println("Token at +" + i + ": " + tokenManager.peek(i).map(Token::getType));
            }
            while (tokenManager.peek(0).isPresent() && (tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD)) {

                System.out.println("Found a local variable");
                Optional<VariableDeclarationNode> local = parseVariableDeclaration();

                if (local.isPresent()) {
                    //now check if it was initialized
                    System.out.println("Local variable is present " + tokenManager.peek(0).get().getType() + " " + tokenManager.peek(1).get().getType() + " " + tokenManager.peek(2).get().getType());
                    if (tokenManager.peek(0).isPresent() && (tokenManager.peek(0).get().getType() == Token.TokenTypes.ASSIGN)) {
                        System.out.println("Found an initialized statement");
                        tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
                        Optional<ExpressionNode> initializationExpression = parseExpression();

                        if (initializationExpression.isPresent()) {

                            local.get().initializer = initializationExpression;

                        } else {
                            throw new SyntaxErrorException("Expected an expression after ASSIGN", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                        }
                    }
                    methodDeclarationNode.locals.add(local.get());
                    System.out.println("Added local variable: " + local.get());
                    System.out.println("Newline removed after local variable");
                    requireNewLine();
                } else {
                    break;
                }
            }
            //ISSUE FOUND, NOT PARSING STATEMENTS PROPERLY
            //statements
            System.out.println("Attempting to parse statement... Current token: " + tokenManager.peek(0).get().getType());
            //check for initialization, then parse statement
            //varDec = number x
            //assignment = variable reference = statement
            //x
            //number x = 5
            //variable declaration
            //consume number, don't consume x,
            //first parse a variable declaration, then parse it as a statement
            //store initialization as a statement



            Optional<StatementNode> statement = parseStatement();

            //System.out.println("Parsed statement: " + statement.get().toString());
                while (statement.isPresent()) {
                    methodDeclarationNode.statements.add(statement.get());
                    //System.out.println("Parsed statement: " + statement.get().toString());
                    statement = parseStatement();
                    System.out.println("Attempting to parse next statement in MD... Current token: " + tokenManager.peek(0).get().getType());
                }

                //useless
//            System.out.println("now checking for dedent");
//            requireDedent();
//            System.out.println("Consumed dedent");
            while(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE){
                tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
                System.out.println("Removed newline in parseMethodDeclaration");
            }

            //check for end of method using dedent
            if (tokenManager.peek(0).isPresent() && (tokenManager.peek(0).get().getType() == Token.TokenTypes.DEDENT)) {
                System.out.println("END OF METHOD BODY FOUND");

                tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
                for (int i = 0; i < 6; i++) {
                    System.out.println("Token at +" + i + ": " + tokenManager.peek(i).map(Token::getType));
                }

                System.out.println("Parsed methodDeclarationNode: " + methodDeclarationNode);
                return Optional.of(methodDeclarationNode);
            }

            System.out.println("Parsed methodDeclarationNode: " + methodDeclarationNode);
            return Optional.of(methodDeclarationNode);
        }

        //Not a method; backtrack
        System.out.println("No method declaration found...");
        return Optional.empty();
    }

//    public boolean isShared;
//    public boolean isPrivate;
//    public String name;
//    public List<VariableDeclarationNode> parameters = new ArrayList<>();
//    public List<VariableDeclarationNode> returns = new ArrayList<>();
//    public List<VariableDeclarationNode> locals = new ArrayList<>();
//    public List<StatementNode> statements = new ArrayList<>();


    //Statements = INDENT {Statement NEWLINE } DEDENT
    public List<StatementNode> statements() throws SyntaxErrorException {
        List<StatementNode> statements = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            System.out.println("Token at +" + i + ": " + tokenManager.peek(i).map(Token::getType));
        }

        requireIndent();

        Optional<StatementNode> statementNode = parseStatement();

        //insert a loop to iterate through parseStatement multiple times

        while (statementNode.isPresent()){
            System.out.println("Found a statement variable");
            statements.add(statementNode.get());
            requireNewLine();

            statementNode = parseStatement();
        }

        for (int i = 0; i < 8; i++) {
            System.out.println("Token at +" + i + " in STATEMENTS: " + tokenManager.peek(i).map(Token::getType));
        }

        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE){ //ADDED 4-14-25
            requireNewLine();
        }

        requireDedent();
        return statements;
    }


    //Statement = If | Loop | MethodCall | Assignment
    //only if and loop
    public Optional<StatementNode> parseStatement() throws SyntaxErrorException {
        System.out.println("Calling parseStatement()...");
        System.out.println("Attempting to parse statement... Current token: " + tokenManager.peek(0).map(Token::getType));
        System.out.println("Next few tokens:");
        for (int i = 0; i < 6; i++) {
            System.out.println("Token at +" + i + ": " + tokenManager.peek(i).map(Token::getType));
        }
        //consumes newlines after statements such as assignment, as there's no check for newlines at the moment
        if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE) {
            System.out.println("Removed newline inside parseStatement()...");
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
        }

        Optional<IfNode> ifNode = parseIf();
        if (ifNode.isPresent()){
            System.out.println("Parsed If statement");
            return Optional.of(ifNode.get());
        }

        Optional<LoopNode> loopNode = parseLoop();
        if (loopNode.isPresent()){ //tighten conditions
            System.out.println("Parsed loop statement");
            return Optional.of(loopNode.get());
        }

        //for standard method call expressions like console.print()
        System.out.println("Before methodCallExpressionNode...");
        for (int i = 0; i < 6; i++) {
            System.out.println("Token at +" + i + ": " + tokenManager.peek(i).map(Token::getType));
        }
        Optional<MethodCallExpressionNode> methodCallExpressionNode = parseMethodCallExpression();
        if (methodCallExpressionNode.isPresent()){
            System.out.println("Parsed MethodCall expression");
            return Optional.of(new MethodCallStatementNode(methodCallExpressionNode.get()));
        }

        Optional<StatementNode> disambiguate = disambiguate();
        if (disambiguate.isPresent()){
            return Optional.of(disambiguate.get());
        }

//        System.out.println("Parsing method call for some reason");
//        Optional<MethodCallStatementNode> methodCallNode = parseMethodCall();
//        if (methodCallNode.isPresent()){ //tighten conditions
//            System.out.println("Parsed method call statement");
//            return Optional.of(methodCallNode.get());
//        }
//
//        Optional <AssignmentNode> assignmentNode = parseAssignment();
//        if (assignmentNode.isPresent()){
//            System.out.println("Parsed assignment statement");
//            return Optional.of(assignmentNode.get());
//        }



        System.out.println("No valid statements found");
        return Optional.empty();
    }

    //statement or nested loops and conditionals
    //statements starts with an indent token
    //If = "if" BoolExp NEWLINE Statements ["else" NEWLINE (Statement | Statements)]
//    public ExpressionNode condition; boolExp
//    public List<StatementNode> statements;
//    public Optional<ElseNode> elseStatement;
    public Optional<IfNode> parseIf() throws SyntaxErrorException {
        System.out.println("Called parseIf()...");
        if (tokenManager.matchAndRemove(Token.TokenTypes.IF).isEmpty()){
            System.out.println("Not an if statement");
            return Optional.empty();
        }

        Optional<ExpressionNode> boolExp = parseBooleanExpTerm();



        if (boolExp.isEmpty()){ //boolean expression needed after if keyword
            throw new SyntaxErrorException("Expected boolean expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }


        requireNewLine();
        System.out.println("Next token before statements is: " + tokenManager.peek(0));
        //statements for if
        List<StatementNode> ifStatements = statements();



        //["else" NEWLINE (Statement | Statements)]
        Optional<ElseNode> optionalElse = Optional.empty(); //else is optional
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.ELSE){ //check else
            System.out.println("Found an else statement");
            tokenManager.matchAndRemove(Token.TokenTypes.ELSE);
            requireNewLine();

            //parse statements
            List<StatementNode> elseStatements = statements();
            if (elseStatements.isEmpty()){
                throw new SyntaxErrorException("Expected statement inside else", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            //

            //
            ElseNode elseNode = new ElseNode();
            elseNode.statements = elseStatements;
            optionalElse = Optional.of(elseNode);
        }

        IfNode ifNode = new IfNode();
        ifNode.condition = boolExp.get();
        ifNode.statements = ifStatements;
        ifNode.elseStatement = optionalElse;


        return Optional.of(ifNode);
    }

    //variable reference is just an identifier
    //loop = [VariableReference "=" ] "loop" ( BoolExpTerm ) NEWLINE Statements
//    public Optional<VariableReferenceNode> assignment; not yet
//    public ExpressionNode expression; not yet
//    public List<StatementNode> statements = new ArrayList<>();
    public Optional<LoopNode> parseLoop() throws SyntaxErrorException {
        System.out.println("Called parseLoop()...");
        //[VariableReference "=" ] optional
        LoopNode loopNode = new LoopNode();
        loopNode.assignment = Optional.empty();

        //issue here with consuming token before actually checking if it is a loop; tighten condition
        /*
            var =
         */

        if (tokenManager.matchAndRemove(Token.TokenTypes.LOOP).isEmpty()){
            System.out.println("Not a loop statement");
            return Optional.empty(); //not a loop
        }

        System.out.println("Checking for a variable reference in loop...");
        if ((tokenManager.peek(0).isPresent() && tokenManager.peek(1).isPresent())
                && (tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD && tokenManager.peek(1).get().getType() == Token.TokenTypes.ASSIGN)
                || tokenManager.peek(0).get().getType() == Token.TokenTypes.ASSIGN) {
            System.out.println("Loop variable reference FOUND");
            Optional<VariableReferenceNode> variableReferenceNode = parseVariableReference();
            if (variableReferenceNode.isPresent()){
                System.out.println("FOUND variable reference");
                if (tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isEmpty()) {
                    throw new SyntaxErrorException("Expected ASSIGN after variable reference", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }

                System.out.println("Loop node assignment is set");
                loopNode.assignment = variableReferenceNode;

            }
        }

        Optional<ExpressionNode> boolExpTerm = parseBooleanExpTerm();
        if (boolExpTerm.isEmpty()){
            throw new SyntaxErrorException("Expected boolean expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }


        requireNewLine();
        for (int i = 0; i < 6; i++) {
            System.out.println("Token at +" + i + " in LOOP: " + tokenManager.peek(i).map(Token::getType));
        }
        List<StatementNode> loopStatements = statements();



        loopNode.expression = boolExpTerm.get();
        loopNode.statements = loopStatements;
        return Optional.of(loopNode);
    }

    //BoolExpTerm = BoolExpFactor {("and"|"or") BoolExpTerm} | "not" BoolExpTerm
    //there are no AND, OR, or NOT tokens
//    public ExpressionNode left;
//    public ExpressionNode right;
//    public enum BooleanOperations { and, or }
//    public BooleanOpNode.BooleanOperations op;
    public Optional<ExpressionNode> parseBooleanExpTerm() throws SyntaxErrorException {

        Optional<ExpressionNode> boolExpFactor = parseBoolExpFactor();
        if (boolExpFactor.isEmpty()){
            throw new SyntaxErrorException("Expected boolean expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
//        while(tokenManager.peek(0).isPresent() && (tokenManager.peek(0).get().getValue().equals("AND") || tokenManager.peek(0).get().getValue().equals("OR"))){
//            String logicalOperation = tokenManager.peek(0).get().getValue();
//            BooleanOpNode.BooleanOperations operations;
//            if (logicalOperation.equals("AND")){
//                operations = BooleanOpNode.BooleanOperations.and;
//            } else if (logicalOperation.equals("OR")){
//                operations = BooleanOpNode.BooleanOperations.or;
//            }
//
//
//        }
//
//        if ((tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getValue().equals("NOT"))) {
//            tokenManager.matchAndRemove(tokenManager.peek(0).get().getType());
//
//        }

        return boolExpFactor; //temp
    }

    //BoolExpFactor = MethodCallExpression | (Expression ( "==" | "!=" | "<=" | ">=" | ">" | "<" ) Expression) | VariableReference
//    public ExpressionNode left;
//    public ExpressionNode right;
//    public enum CompareOperations { lt, le, gt, ge, eq, ne}
//    public CompareNode.CompareOperations op;
//    private String opToString() {
//        switch (op) {
//            case lt -> {return " < ";}
//            case le -> {return " <= ";}
//            case gt -> {return " > ";}
//            case ge -> {return " >= ";}
//            case eq -> {return " == ";}
//            case ne -> {return " != ";}
//        }
    public Optional<ExpressionNode> parseBoolExpFactor() throws SyntaxErrorException {
        //check for method call first
        Optional<MethodCallExpressionNode> methodCallExpressionNode = parseMethodCallExpression();
        if (methodCallExpressionNode.isPresent()){
            return Optional.of(methodCallExpressionNode.get());
        }

        //check for expression, then boolean operation, then another expression
        Optional<ExpressionNode> lefExpressionNode = parseExpression();

        if (lefExpressionNode.isPresent()){ //parse first expression
            CompareNode compareNode = new CompareNode();
            compareNode.left = lefExpressionNode.get();
            //check boolean operation
            if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.EQUAL){
                tokenManager.matchAndRemove(Token.TokenTypes.EQUAL);
                compareNode.op = CompareNode.CompareOperations.eq;

            } else if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NOTEQUAL){
                tokenManager.matchAndRemove(Token.TokenTypes.NOTEQUAL);
                compareNode.op = CompareNode.CompareOperations.ne;

            } else if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.LESSTHANEQUAL) {
                tokenManager.matchAndRemove(Token.TokenTypes.LESSTHANEQUAL);
                compareNode.op = CompareNode.CompareOperations.le;

            } else if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.GREATERTHANEQUAL) {
                tokenManager.matchAndRemove(Token.TokenTypes.GREATERTHANEQUAL);
                compareNode.op = CompareNode.CompareOperations.ge;

            } else if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.GREATERTHAN) {
                tokenManager.matchAndRemove(Token.TokenTypes.GREATERTHAN);
                compareNode.op = CompareNode.CompareOperations.gt;

            } else if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.LESSTHAN) {
                tokenManager.matchAndRemove(Token.TokenTypes.LESSTHAN);
                compareNode.op = CompareNode.CompareOperations.lt;

            } else {
                return Optional.of(lefExpressionNode.get());
            }

            //now parse right expression
            Optional<ExpressionNode> rightExpressionNode = parseExpression();
            if (rightExpressionNode.isPresent()){
                compareNode.right = rightExpressionNode.get();
                return Optional.of(compareNode);
            } else {
                return Optional.of(lefExpressionNode.get()); //otherwise just return left expression
            }
        }

        //check for a variable reference
        Optional<VariableReferenceNode> variableReferenceNode = parseVariableReference();
        if (variableReferenceNode.isPresent()){
            return Optional.of(variableReferenceNode.get());
        }

        return Optional.empty(); //return nothing if none of the options are returned
    }

    //Assignment = VariableReference "=" Expression
    public Optional<AssignmentNode> parseAssignment() throws SyntaxErrorException {
        System.out.println("Called parseAssignment()...");
        if (tokenManager.peek(0).isEmpty() || tokenManager.peek(0).get().getType() != Token.TokenTypes.WORD) {
            System.out.println("No variable reference found");
            return Optional.empty();
        }


        if (tokenManager.peek(1).isEmpty() || tokenManager.peek(1).get().getType() != Token.TokenTypes.ASSIGN) {
            System.out.println("No '=' found after variable reference, next token: " + tokenManager.peek(1).map(Token::getType));
            return Optional.empty();
        }
        //peek 0: word
        //peek 1: ASSIGN
        //peek 2: word
        //peek 3: DOT || LPAREN
        if(tokenManager.peek(2).isPresent() && tokenManager.peek(3).isPresent()
                && (tokenManager.peek(2).get().getType() == Token.TokenTypes.WORD)
                && ((tokenManager.peek(3).get().getType() == Token.TokenTypes.DOT) || tokenManager.peek(3).get().getType() == Token.TokenTypes.LPAREN)) {
            System.out.println("NOT a normal assignment, likely a method call");
            return Optional.empty();
        }

        System.out.println("Before parseVariableReference(), current token: " + tokenManager.peek(0).map(Token::getType));
        Optional<VariableReferenceNode> variableReferenceNode = parseVariableReference();
        System.out.println("After parseVariableReference(), current token: " + tokenManager.peek(0).map(Token::getType));

        if (variableReferenceNode.isEmpty()){
            System.out.println("Referenced node is empty");
            return Optional.empty();
        }

        if (tokenManager.peek(0).isPresent()) {
            System.out.println("Next token before checking ASSIGN: " + tokenManager.peek(0).get().getType());
        } else {
            System.out.println("No more tokens available before checking ASSIGN");
        }
        Optional<Token> assignToken = tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
        if (assignToken.isEmpty()){
            System.out.println("No assignment found");
            throw new SyntaxErrorException("Expected ASSIGN in parseAssignment()", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }



        Optional<ExpressionNode> expression = parseExpression();
        if (expression.isEmpty()){
            throw new SyntaxErrorException("Expected an expression after expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        AssignmentNode assignmentNode = new AssignmentNode();
        assignmentNode.target = variableReferenceNode.get();
        assignmentNode.expression = expression.get();

        System.out.println("Successfully parsed assignment: " + assignmentNode);
        for (int i = 0; i < 1; i++) {
            System.out.println("Token at +" + i + " in ASSIGNMENT: " + tokenManager.peek(i).map(Token::getType));
        }
        return Optional.of(assignmentNode);
    }

    //VariableReference = Identifier
    //public String name;
    public Optional<VariableReferenceNode> parseVariableReference() throws SyntaxErrorException {
        System.out.println("Called parseVariableReference()...");

        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() != Token.TokenTypes.WORD){
            System.out.println("No variable reference found");
            return Optional.empty();
        }

        if(tokenManager.peek(1).isPresent() && tokenManager.peek(1).get().getType() == Token.TokenTypes.DOT){
            System.out.println("Detected DOT after WORD - METHOD CALL, skipping parseVariableReference.");
            return Optional.empty();
        }

        if(tokenManager.peek(1).isPresent() && tokenManager.peek(1).get().getType() == Token.TokenTypes.LPAREN){
            System.out.println("Detected LPAREN after WORD - METHOD CALL, skipping parseVariableReference.");
            return Optional.empty();
        }

        VariableReferenceNode variableReferenceNode = new VariableReferenceNode();
        Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);

        if ((nameToken).isEmpty()){
            throw new SyntaxErrorException("Expected name identifier for variable reference", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //get name from token
        variableReferenceNode.name = nameToken.get().getValue();

        return Optional.of(variableReferenceNode);
    }


    /*  Example
        x = method()
        x, y = method(a, b)
     */
//    public Optional<String> objectName;
//    public String methodName;
//    public List<VariableReferenceNode> returnValues = new ArrayList<>();
//    public List<ExpressionNode> parameters = new ArrayList<>();
    //MethodCall = VariableReference { "," VariableReference } "=" MethodCallExpression
    public Optional<MethodCallStatementNode> parseMethodCall() throws SyntaxErrorException { //tighten conditions
        System.out.println("Called parseMethodCall()...");
        //check for a variable reference
        if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD) {


            Optional<VariableReferenceNode> variableReferenceNode = parseVariableReference();
            List<VariableReferenceNode> returnValues = new ArrayList<>();

            if (variableReferenceNode.isEmpty()) {
                return Optional.empty();
                //returnValues.add(variableReferenceNode.get());
            }
            returnValues.add(variableReferenceNode.get());

            while (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.COMMA) {
                tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
                variableReferenceNode = parseVariableReference();

                if (variableReferenceNode.isEmpty()) {
                    throw new SyntaxErrorException("Expected variable reference after comma", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
                returnValues.add(variableReferenceNode.get());
            }

            if (tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isEmpty()) {
                throw new SyntaxErrorException("Expected ASSIGN after variable reference", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }

            Optional<MethodCallExpressionNode> methodCallExpressionNode = parseMethodCallExpression();
            if (methodCallExpressionNode.isEmpty()) {
                throw new SyntaxErrorException("Expected method call after assignment", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }

            MethodCallStatementNode methodCallStatementNode = new MethodCallStatementNode(methodCallExpressionNode.get());
            methodCallStatementNode.returnValues.addAll(returnValues);

            return Optional.of(methodCallStatementNode);
        }
        System.out.println("Not a method call...");
        return Optional.empty(); //not a methodCall
    }

    //MethodCallExpression = [Identifier "."] Identifier "(" [Expression {"," Expression }] ")"
    public Optional<MethodCallExpressionNode> parseMethodCallExpression() throws SyntaxErrorException {
        System.out.println("Called parseMethodCallExpression()...");
        //TIGHTEN CONDITIONS
        MethodCallExpressionNode methodCallExpressionNode = new MethodCallExpressionNode();
        if (tokenManager.peek(0).isPresent()
                && tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD
                && tokenManager.peek(1).get().getType() == Token.TokenTypes.DOT) { //checks for object name first
            System.out.println("Found word and dot in method call expression");
            //System.out.println("Checking method call: " + tokenManager.peek(0).get().getValue());
            methodCallExpressionNode.objectName = Optional.of(tokenManager.peek(0).get().getValue());

            tokenManager.matchAndRemove(Token.TokenTypes.WORD); //identifier
            tokenManager.matchAndRemove(Token.TokenTypes.DOT);
        } else { //if not present, its empty
            methodCallExpressionNode.objectName = Optional.empty();
        }

        System.out.println("Checking method call: " + tokenManager.peek(0).get().getValue());


        if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD
        && tokenManager.peek(1).get().getType() == Token.TokenTypes.LPAREN) {
            System.out.println("Regular method call fdound");
            methodCallExpressionNode.methodName = tokenManager.peek(0).get().getValue();
            tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        } else {
            for (int i = 0; i < 2; i++) {
                System.out.println("Token at +" + i + ": " + tokenManager.peek(i).map(Token::getType));
            }

            if(tokenManager.peek(0).isPresent()  && (tokenManager.peek(0).get().getValue().equals("false") || tokenManager.peek(0).get().getValue().equals("true"))) {
                System.out.println("Special case found... boolean expression");

                String value = tokenManager.peek(0).get().getValue();
                tokenManager.matchAndRemove(Token.TokenTypes.WORD);
                boolean bool = value.equals("true");
                methodCallExpressionNode.methodName = value;  // Store the boolean value as the method name for now.
                // Set up the parameters for the method call if needed, or treat it as a boolean literal directly.
                List<ExpressionNode> listOfExpressions = new ArrayList<>();
                listOfExpressions.add(new BooleanLiteralNode(bool));  // Add the BooleanLiteralNode
                methodCallExpressionNode.parameters = listOfExpressions;
                for (int i = 0; i < 5; i++) {
                    System.out.println("Token at +" + i + " in BOOLEAN: " + tokenManager.peek(i).map(Token::getType));
                }

                return Optional.of(methodCallExpressionNode);



            }

            System.out.println("No more tokens available before parseMethodCallExpression(), NOT A METHOD CALL EXPRESSION");
            return Optional.empty();
        }

        if(tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()){
            throw new SyntaxErrorException("Expected left parenthesis after identifier in method call expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //optionally check for [Expression {"," Expression }]
        if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() != Token.TokenTypes.RPAREN) {
            List<ExpressionNode> listOfExpressions = new ArrayList<>();
            Optional<ExpressionNode> expressionNode = parseExpression();
            if (expressionNode.isEmpty()) {
                return Optional.empty();
            }
            listOfExpressions.add(expressionNode.get());

            while (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.COMMA) {

                    System.out.println("Token at " + tokenManager.peek(0).map(Token::getType));

                tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
                expressionNode = parseExpression();
                if (expressionNode.isEmpty()) {
                    return Optional.empty();
                }
                listOfExpressions.add(expressionNode.get());
            }

            methodCallExpressionNode.parameters = listOfExpressions;

        }
        System.out.println("Before right parenthesis");
        for (int i = 0; i < 6; i++) {
            System.out.println("Token at +" + i + ": " + tokenManager.peek(i).map(Token::getType));
        }
        if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
            throw new SyntaxErrorException("Expected right parenthesis", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        return Optional.of(methodCallExpressionNode);
    }

    /*  Stubbed out body
        Optional<VariableReferenceNode> expressionNode = parseVariableReference();
        return Optional.of(expressionNode.get());
     */
    //Expression = Term { ("+"|"-") Term } //stubbed out using variable reference
    public Optional<ExpressionNode> parseExpression() throws SyntaxErrorException {
        System.out.println("Called parseExpression()...");
        Optional<ExpressionNode> firstTerm = parseTerm();
        if (firstTerm.isEmpty()){
            System.out.println("First term is empty");
            return Optional.empty();
        }

        ExpressionNode firstTermNode = firstTerm.get();

        while(tokenManager.peek(0).isPresent()
                && (tokenManager.peek(0).get().getType() == Token.TokenTypes.PLUS
                || tokenManager.peek(0).get().getType() == Token.TokenTypes.MINUS)){

            MathOpNode mathOpNode = new MathOpNode();
            mathOpNode.left = firstTermNode;

            if (tokenManager.peek(0).get().getType() == Token.TokenTypes.PLUS){
                tokenManager.matchAndRemove(Token.TokenTypes.PLUS);
                mathOpNode.op = MathOpNode.MathOperations.add;
            }
            if (tokenManager.peek(0).get().getType() == Token.TokenTypes.MINUS){
                tokenManager.matchAndRemove(Token.TokenTypes.MINUS);
                mathOpNode.op = MathOpNode.MathOperations.subtract;
            }

            Optional<ExpressionNode> nthTerm = parseTerm();
            if (nthTerm.isEmpty()){
                throw new SyntaxErrorException("Expected expression after term", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            mathOpNode.right = nthTerm.get();
            firstTermNode = mathOpNode;
        }
        if (firstTerm == null){
            throw new NullPointerException();
        }
        return Optional.of(firstTermNode);
    }

    //Term = Factor { ("*"|"/"|"%") Factor }
    public Optional<ExpressionNode> parseTerm() throws SyntaxErrorException {
        System.out.println("Called parseTerm()... -> next term" + tokenManager.peek(0).map(Token::getType));

        Optional<ExpressionNode> firstFactor = parseFactor();

        if (firstFactor.isEmpty()){
            System.out.println("First factor is empty");
            return Optional.empty();
        }

        ExpressionNode firstFactorNode = firstFactor.get();

        while (tokenManager.peek(0).isPresent()
        && (tokenManager.peek(0).get().getType() == Token.TokenTypes.TIMES
        || tokenManager.peek(0).get().getType() == Token.TokenTypes.DIVIDE
        || tokenManager.peek(0).get().getType() == Token.TokenTypes.MODULO)) { //alternate for factored symbols

            MathOpNode mathOpNode = new MathOpNode();
            mathOpNode.left = firstFactorNode;

            if (tokenManager.peek(0).get().getType() == Token.TokenTypes.TIMES) {
                tokenManager.matchAndRemove(Token.TokenTypes.TIMES);
                mathOpNode.op = MathOpNode.MathOperations.multiply;
            }
            if (tokenManager.peek(0).get().getType() == Token.TokenTypes.DIVIDE) {
                tokenManager.matchAndRemove(Token.TokenTypes.DIVIDE);
                mathOpNode.op = MathOpNode.MathOperations.divide;
            }
            if (tokenManager.peek(0).get().getType() == Token.TokenTypes.MODULO) {
                tokenManager.matchAndRemove(Token.TokenTypes.MODULO);
                mathOpNode.op = MathOpNode.MathOperations.modulo;
            }

            Optional<ExpressionNode> nthFactor = parseFactor();
            if (nthFactor.isEmpty()){
                throw new SyntaxErrorException("Expected right factor after expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }

            mathOpNode.right = nthFactor.get();
            firstFactorNode = mathOpNode;
        }

        return Optional.of(firstFactorNode);
    }

    //number is number literal
    //Factor = NUMBER | VariableReference | StringLiteral | CharacterLiteral | MethodCallExpression | "(" Expression ")" | "new" Identifier "(" [Expression {"," Expression }] ")"
    public Optional<ExpressionNode> parseFactor() throws SyntaxErrorException {
        System.out.println("Called parseFactor()...");

        for (int i = 0; i < 6; i++) {
            System.out.println("Token at +" + i + " IN FACTOR: " + tokenManager.peek(i).map(Token::getType));
        }

        if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD) { //for dealing with boolean values
            String value = tokenManager.peek(0).get().getValue();
            if (value.equals("true") || value.equals("false")) {
                tokenManager.matchAndRemove(Token.TokenTypes.WORD);
                return Optional.of(new BooleanLiteralNode(value.equals("true")));
            }
        }

        Optional<StringLiteralNode> stringLiteralNode = parseStringLiteral();
        if (stringLiteralNode.isPresent()){
            return Optional.of(stringLiteralNode.get());
        }

        Optional<NumericLiteralNode> numericLiteralNode = parseNumberLiteral();
        if (numericLiteralNode.isPresent()){
            return Optional.of(numericLiteralNode.get());
        }

        Optional<VariableReferenceNode> variableReferenceNode = parseVariableReference();
        if (variableReferenceNode.isPresent()){
            return Optional.of(variableReferenceNode.get());
        }

        Optional<CharLiteralNode> charLiteralNode = parseCharLiteral();
        if (charLiteralNode.isPresent()){
            return Optional.of(charLiteralNode.get());
        }

        Optional<MethodCallExpressionNode> methodCallExpressionNode = parseMethodCallExpression();
        if (methodCallExpressionNode.isPresent()){
            if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() != Token.TokenTypes.NEW){
                System.out.println("No new keyword found: continuing");
                return Optional.of(methodCallExpressionNode.get());
            }
        }

        //"(" Expression ")"
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.LPAREN){
            tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
            Optional<ExpressionNode> expressionNode = parseExpression();
            if (expressionNode.isPresent()){
                if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.RPAREN){
                    tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);
                    return Optional.of(expressionNode.get());
                }
                throw new SyntaxErrorException("Expected a right parenthesis after expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
        }

        //"new" Identifier "(" [Expression {"," Expression }] ")"

        if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEW) {
            for (int i = 0; i < 2; i++) {
                System.out.println("BEFOREToken at +" + i + ": " + tokenManager.peek(i).map(Token::getType));
            }
            System.out.println("found a new keyword in new section");
            tokenManager.matchAndRemove(Token.TokenTypes.NEW);
            for (int i = 0; i < 6; i++) {
                System.out.println("AFTERToken at +" + i + ": " + tokenManager.peek(i).map(Token::getType));
            }
            NewNode newNode = new NewNode();

            Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
            if (nameToken.isEmpty()) {
                throw new SyntaxErrorException("Expected an identifier after NEW", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }

            if(tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()){
                throw new SyntaxErrorException("Expected a left parenthesis", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }

            //now check for an optional expression
            if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() != Token.TokenTypes.RPAREN) {
                List<ExpressionNode> listOfExpressions = new ArrayList<>();
                System.out.println("Calling parseExpression after NEW");
                Optional<ExpressionNode> expressionNode = parseExpression();
                if (expressionNode.isEmpty()) {
                    return Optional.empty();
                }
                listOfExpressions.add(expressionNode.get());

                while (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.COMMA) {
                    tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
                    expressionNode = parseExpression();
                    if (expressionNode.isEmpty()) {
                        return Optional.empty();
                    }
                    listOfExpressions.add(expressionNode.get());
                }

                newNode.parameters = listOfExpressions;

            }
            if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
                throw new SyntaxErrorException("Expected right parenthesis", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }

            newNode.className = nameToken.get().getValue();
            System.out.println("returned a new node " + newNode.className);

            return Optional.of(newNode);
        }

        System.out.println("No factor found");
        return Optional.empty();
    }


    //NUMBER
    //public float value
    public Optional<NumericLiteralNode> parseNumberLiteral() throws SyntaxErrorException {
        NumericLiteralNode numberLiteralNode = new NumericLiteralNode();
        Optional<Token> numberToken = tokenManager.matchAndRemove(Token.TokenTypes.NUMBER);
        if (numberToken.isPresent()) {
            numberLiteralNode.value = Float.parseFloat(numberToken.get().getValue());
            return Optional.of(numberLiteralNode);
        }

        return Optional.empty();
    }

    //StringLiteral = " { any non-" } " //reading quoted strings
    public Optional<StringLiteralNode> parseStringLiteral() throws SyntaxErrorException {
        StringLiteralNode stringLiteralNode = new StringLiteralNode();
        Optional<Token> stringLiteralToken = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDSTRING);

        if (stringLiteralToken.isPresent()) {
            stringLiteralNode.value = stringLiteralToken.get().getValue();
            return Optional.of(stringLiteralNode);
        }

        return Optional.empty();
    }

    //CharacterLiteral = ' (one character not a ') ' //reading quoted characters
    public Optional<CharLiteralNode> parseCharLiteral() throws SyntaxErrorException {
        CharLiteralNode charLiteralNode = new CharLiteralNode();
        Optional<Token> charLiteralToken = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDCHARACTER);

        if (charLiteralToken.isPresent()) {
            if(charLiteralToken.get().getValue().length() == 1){
                charLiteralNode.value = charLiteralToken.get().getValue().charAt(0);
                return Optional.of(charLiteralNode);
            }
            throw new SyntaxErrorException("Char literal expected ONE character.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        return Optional.empty();
    }

    /*
        Create a method: Optional<StatementNode> disambiguate(). Call it in statement() if if() and loop() return Empty.

        Disambiguate should first call MethodCallExpression(); if it returns a value, then create a MethodCallStatementNode from it and return it.
        That will handle the case of a method call without any return values (note in the EBNF that this is valid).

        With that case handled, we call VariableReference(). If that fails, we don’t have a valid statement, so return Empty.
        Now, notice the difference between MethodCall and Assignment – after the VariableReference, is there a comma?
        If so, this is a method call.
        But if not, we still have two possibilities – it could be method call OR an assignment.
        But – the good news is that either of these are handled by assignment.

        Note that at this point, we can’t successfully parse method call (because methodCallExpression is stubbed out) and the only expressions we can have are VariableReferences

        Helps distinguish the difference between a method call or an ordinary assignment statement
     */
    public Optional<StatementNode> disambiguate() throws SyntaxErrorException {
        System.out.println("Called disambiguate()...");

        //methodCall case
        Optional<MethodCallExpressionNode> methodCallExpressionNode = parseMethodCallExpression();

        if (methodCallExpressionNode.isPresent()) {
            Optional<MethodCallStatementNode> methodCallStatementNode = parseMethodCall();
            if (methodCallStatementNode.isPresent()) {
                MethodCallStatementNode methodCall = new MethodCallStatementNode(methodCallExpressionNode.get());
                return Optional.of(methodCall);
            }
        }

        //variable reference case
        //after the variable reference, check for a comma.
        //if there is a comma, then it is a method call
        //if there is no comma, then it could be a method call OR an assignment which is handled by assignment

        if(tokenManager.peek(0).isEmpty() || tokenManager.peek(0).get().getType() != Token.TokenTypes.WORD || tokenManager.peek(1).isEmpty()) {
            return Optional.empty();
        }

        if(tokenManager.peek(1).get().getType() == Token.TokenTypes.COMMA) { //for method calls
            System.out.println("Method call after comma found");
            List<VariableReferenceNode> returnValues = new ArrayList<>();
            Optional<VariableReferenceNode> variableReferenceNode = parseVariableReference();

            if (variableReferenceNode.isEmpty()) {
                return Optional.empty();
            }
            returnValues.add(variableReferenceNode.get());

            while(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.COMMA) {
                tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
                variableReferenceNode = parseVariableReference();
                if (variableReferenceNode.isEmpty()) {
                    throw new SyntaxErrorException("Expected variable reference after comma for method call", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
                returnValues.add(variableReferenceNode.get());
            }

            if (tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isEmpty()) {
                throw new SyntaxErrorException("Expected ASSIGN after method call", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }

            methodCallExpressionNode = parseMethodCallExpression();
            if (methodCallExpressionNode.isEmpty()) {
                throw new SyntaxErrorException("Expected method call after assignment", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            MethodCallStatementNode methodCalls = new MethodCallStatementNode(methodCallExpressionNode.get());
            methodCalls.returnValues.addAll(returnValues);

            System.out.println("Left-hand side variables: " + returnValues);
            System.out.println("Method call expression: " + methodCallExpressionNode);

            return Optional.of(methodCalls);

        } else if (tokenManager.peek(1).get().getType() == Token.TokenTypes.ASSIGN) { //second case for assignments OR method calls
            System.out.println("Disambiguate: ASSIGNMENT FOUND");
            for (int i = 0; i < 6; i++) {
                System.out.println("Token at +" + i + ": " + tokenManager.peek(i).map(Token::getType));
            }



            Optional<AssignmentNode> assignmentNode = parseAssignment();

            if (assignmentNode.isPresent()) {
                System.out.println("Returning an assignment node in disambiguate");
                return Optional.of(assignmentNode.get());
            }

            if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE) {
                System.out.println("Consuming newline after assignment in disambiguate");
                tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            }

            Optional<MethodCallStatementNode> methodCallStatementNode = parseMethodCall();
            if (methodCallStatementNode.isPresent()) {
                System.out.println("Returning a method call statement");
                return Optional.of(methodCallStatementNode.get());
            }

            return Optional.empty();
        }

        return Optional.empty();
    }

    //Tran = ( Class | Interface )*
    public void Tran() throws SyntaxErrorException {

        List<InterfaceNode> interfaces = new LinkedList<>(); //creates a list of interfaces
        List<ClassNode> classes = new LinkedList<>(); //lists of classes
        System.out.println("Parser Started");
        System.out.println("==================================================");

        while (!tokenManager.done()){ //run while list is not empty

            /*
                added this because shouldn't there be a newline to distinguish the next interface or class?
                unit test will fail without it because it won't be able to parse more than one interface or class
             */
            while (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE) {
                tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            }
                //create interface node
                System.out.println("checking interface");
                Optional<InterfaceNode> interfaceNode = parseInterface();


                if (interfaceNode.isPresent()){ //if the node is present and valid add the interface to the list
                    System.out.println("Found interface");
                    interfaces.add(interfaceNode.get());
                    continue;
                }

                //parse class node if found
                System.out.println("checking class");
                Optional<ClassNode> classNode = parseClass();

                if(classNode.isPresent()) {
                    System.out.println("Found class");
                    classes.add(classNode.get());
                    continue;
                }

                break;
        }

        tranNode.Interfaces = interfaces; //add list of interfaces to interface property of tran node
        tranNode.Classes = classes;
        System.out.println("Classes parsed: " + tranNode.Classes);

    }
}
