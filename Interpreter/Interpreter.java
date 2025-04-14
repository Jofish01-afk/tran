package Interpreter;

import AST.*;

import java.util.*;

public class Interpreter {

    private final TranNode top;

    /** Constructor - get the interpreter ready to run. Set members from parameters and "prepare" the class.
     *
     * Store the tran node.
     * Add any built-in methods to the AST
     * @param top - the head of the AST
     */
    public Interpreter(TranNode top) {
        this.top = top;

        //built in methods: times, console, boolean
        //using a separate AST tree to hold other built in classes
        //built in method declaration has attributes:
        /*
            isVariadic
            List<InterpreterData Type?
         */

        //console.print name and method - for some reason its console.write not print like in the lang doc
        ClassNode console = new ClassNode();
        console.name = "console";
        ConsoleWrite write = new ConsoleWrite();
        write.name = "write";
        write.isShared = true;
        write.isVariadic = true;

        console.methods.add(write);
        top.Classes.add(console);

        //times() built in class?
        //put times in tran node?
        //times is a built-in method for number objects
        ClassNode timesClass = new ClassNode(); //create times class
        timesClass.name = "times";

        //maybe make a built in method here, since no object is needed

        //boolean class to hold
        ClassNode bool = new ClassNode();
        bool.name = "boolean";

        //System.out.println("Interpreter added built in classes: " + top.Classes);



    }

    /**
     * This is the public interface to the interpreter. After parsing, we will create an interpreter and call start to
     * start interpreting the code.
     *
     * Search the classes in Tran for a method that is "isShared", named "start", that is not private and has no parameters
     * Call "InterpretMethodCall" on that method, then return.
     * Throw an exception if no such method exists.
     */
    public void start() {
        // Find the "start" method
        System.out.println("Interpreter Started");
        System.out.println("==================================================");
        //look for start method in classNode, top is the entry point
        //iterate through classes, then iterate through methods in those classes

        for (int i = 0; i < top.Classes.size(); i++) {
            ClassNode classNode = top.Classes.get(i);

            for (int j = 0; j < classNode.methods.size(); j++) {
                MethodDeclarationNode method = classNode.methods.get(j);

                //check if its not private and no parameters
                if (!method.isPrivate && method.name.equals("start") && method.parameters.isEmpty()) {

                    List<InterpreterDataType> values = new ArrayList<InterpreterDataType>();
                    /*
                     * @param object - The object this method is being called on (might be empty for shared)
                     * @param m - Which method is being called
                     * @param values - The values to be passed in
                     * @return the returned values from the method
                     */

                    //no return values
                    System.out.println("Interpreter added built in classes: " + top.Classes.get(1));
                    interpretMethodCall(Optional.empty(), method, values);
                    return;
                }
            }
        }

        throw new RuntimeException("No 'start' method found");
    }

    //              Running Methods

    /**
     * Find the method (local to this class, shared (like Java's system.out.print), or a method on another class)
     * Evaluate the parameters to have a list of values
     * Use interpretMethodCall() to actually run the method.
     *
     * Call GetParameters() to get the parameter value list
     * Find the method. This is tricky - there are several cases:
     * someLocalMethod() - has NO object name. Look in "object"
     * console.write() - the objectName is a CLASS and the method is shared
     * bestStudent.getGPA() - the objectName is a local or a member
     *
     * Once you find the method, call InterpretMethodCall() on it. Return the list that it returns.
     * Throw an exception if we can't find a match.
     * @param object - the object we are inside right now (might be empty)
     * @param locals - the current local variables
     * @param mc - the method call
     * @return - the return values
     */
    private List<InterpreterDataType> findMethodForMethodCallAndRunIt(Optional<ObjectIDT> object, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc) {
        System.out.println("searching for method call...");
        List<InterpreterDataType> result = null;
        List<InterpreterDataType> parameters = getParameters(object, locals, mc);
        //could be an object name thats a class and a shared method, OR object name is a local or member


        //look for a method
        //in the case for someLocalMethod, look for the method in the same class.
        if (object.isPresent()) { // object can be empty, so check if its present
            //look for methods
            List<MethodDeclarationNode> methods = object.get().astNode.methods;
            for (int i = 0; i < methods.size(); i++) {
                System.out.println("Object present in method call search " + object.get().astNode.name);
                MethodDeclarationNode method = methods.get(i);
                if(mc.methodName.equals(method.name)) {
                    result = interpretMethodCall(object, method, parameters);
                }
            }

//            if (object.isPresent() && object.get().members.containsKey(mc.objectName.get())) { //check members
//                System.out.println("Members found in method call search");
//                ObjectIDT members =(ObjectIDT) object.get().members.get(mc.objectName.get());
//                List<MethodDeclarationNode> methodsDeclarations = ((ObjectIDT) object.get().members.get(mc.objectName.get())).astNode.methods;
//                for (int i = 0; i < methodsDeclarations.size(); i++) {
//                    MethodDeclarationNode method = methodsDeclarations.get(i);
//                    if(mc.methodName.equals(method.name)) {
//                        result = interpretMethodCall(Optional.of(members), method, parameters); //bug fix
//                    }
//                }
//            }


            //OBJECT NAME IS A CLASS
        } else if (mc.objectName.isPresent()){
            System.out.println("Object is not present");
            for (int i = 0; i < top.Classes.size(); i++){ //iterate through all the classes
                ClassNode classNode = top.Classes.get(i);
                if (mc.objectName.get().equals(classNode.name)) { //look for a class with the same objectName
                    for (int j = 0; j < classNode.methods.size(); j++) { //now look through the class methods
                        MethodDeclarationNode method = classNode.methods.get(j);
                        if (mc.methodName.equals(method.name)) {
                            result = interpretMethodCall(object, method, parameters);
                        }
                    }
                }
            }

            //OBJECT NAME IS A LOCAL OR A MEMBER, check BOTH
            if (locals.containsKey(mc.objectName.get())) { //check locals
                ObjectIDT objectIDT = ((ReferenceIDT) locals.get(mc.objectName.get())).refersTo.get();
                List<MethodDeclarationNode> methods = objectIDT.astNode.methods;
                System.out.println("Locals found in method call search ");
                for (int i = 0; i < methods.size(); i++) {
                    MethodDeclarationNode method = methods.get(i);
                    if(mc.methodName.equals(method.name)) {
                        result = interpretMethodCall(Optional.of(objectIDT), method, parameters); //bug fix
                    }
                }

            } else if (object.isPresent() && object.get().members.containsKey(mc.objectName.get())) { //check members
                System.out.println("Members found in method call search");
                ObjectIDT members =(ObjectIDT) object.get().members.get(mc.objectName.get());
                List<MethodDeclarationNode> methods = ((ObjectIDT) object.get().members.get(mc.objectName.get())).astNode.methods;
                for (int i = 0; i < methods.size(); i++) {
                    MethodDeclarationNode method = methods.get(i);
                    if(mc.methodName.equals(method.name)) {
                        result = interpretMethodCall(Optional.of(members), method, parameters); //bug fix
                    }
                }
            }

        }
        //in the case of console.print, its in another class and its a shared method
        //in the case of bestStudent.getGPA(), the
        return result;
    }

    /**
     * Run a "prepared" method (found, parameters evaluated)
     * This is split from findMethodForMethodCallAndRunIt() because there are a few cases where we don't need to do the finding:
     * in start() and dealing with loops with iterator objects, for example.
     *
     * Check to see if "m" is a built-in. If so, call Execute() on it and return
     * Make local variables, per "m"
     * If the number of passed in values doesn't match m's "expectations", throw
     * Add the parameters by name to locals.
     * Call InterpretStatementBlock
     * Build the return list - find the names from "m", then get the values for those names and add them to the list.
     * @param object - The object this method is being called on (might be empty for shared)
     * @param m - Which method is being called
     * @param values - The values to be passed in
     * @return the returned values from the method
     */
    private List<InterpreterDataType> interpretMethodCall(Optional<ObjectIDT> object, MethodDeclarationNode m, List<InterpreterDataType> values) {
        var retVal = new LinkedList<InterpreterDataType>();

        //check if m is built in and call execute
        if (m instanceof BuiltInMethodDeclarationNode) {
            System.out.println("Built in MethodDeclarationNode found ");
            return ((BuiltInMethodDeclarationNode) m).Execute(values);
        }

        //make local variables, and check if the number of passed values dont match the method's expectations
        HashMap<String, InterpreterDataType> locals = new HashMap<String, InterpreterDataType>();

        if (m.parameters.size() != values.size()) {
            throw new RuntimeException("Wrong number of parameters");
        }

        //add parameters by name (string and name)
        for (int i = 0; i < m.parameters.size(); i++) {
            locals.put(m.parameters.get(i).name, values.get(i));
            System.out.println("Added parameter: " + m.parameters.get(i).name);
        }


        //dealing with locals
        for (int i = 0; i < m.locals.size(); i++) {
            if(!locals.containsKey(m.locals.get(i).name)) {
                locals.put(m.locals.get(i).name, instantiate(m.locals.get(i).type));
                System.out.println("Added local: " + m.locals.get(i).name);
            }
        }

        //also deals with returns, since they are declared in the method header. instantiates return variables for use within the body
        for (int i = 0; i < m.returns.size(); i++) {
            if (!locals.containsKey(m.returns.get(i).name)) {
                locals.put(m.returns.get(i).name, instantiate(m.returns.get(i).type));
                System.out.println("Added return: " + m.returns.get(i).name);
            }
        }

        interpretStatementBlock(object, m.statements, locals);
        //dealing with return values
        for (int i = 0; i < m.returns.size(); i++) {
            retVal.add(locals.get(m.returns.get(i).name));
            System.out.println("Return: " + m.returns.get(i).name);
        }

        return retVal;
    }

    //              Running Constructors

    /**
     * This is a special case of the code for methods. Just different enough to make it worthwhile to split it out.
     *
     * Call GetParameters() to populate a list of IDT's
     * Call GetClassByName() to find the class for the constructor
     * If we didn't find the class, throw an exception
     * Find a constructor that is a good match - use DoesConstructorMatch()
     * Call InterpretConstructorCall() on the good match
     * @param callerObj - the object that we are inside when we called the constructor
     * @param locals - the current local variables (used to fill parameters)
     * @param mc  - the method call for this construction
     * @param newOne - the object that we just created that we are calling the constructor for
     */
    private void findConstructorAndRunIt(Optional<ObjectIDT> callerObj, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc, ObjectIDT newOne) {
        List<InterpreterDataType> parameters = getParameters(callerObj, locals, mc);
        Optional<ClassNode> classNode;
        if (callerObj.isPresent()) {
            classNode = getClassByName(callerObj.get().astNode.name);
        } else {
            classNode = getClassByName(newOne.astNode.name);
        }


        if (classNode.isPresent()) {
            for (ConstructorNode constructor : classNode.get().constructors) {
                if(doesConstructorMatch(constructor, mc, parameters)) {
                    interpretConstructorCall(newOne, constructor, parameters);
                    return;
                }
            }
        } else {
            throw new RuntimeException("Class name not found");
        }

    }

    /**
     * Similar to interpretMethodCall, but "just different enough" - for example, constructors don't return anything.
     *
     * Creates local variables (as defined by the ConstructorNode), calls Instantiate() to do the creation
     * Checks to ensure that the right number of parameters were passed in, if not throw.
     * Adds the parameters (with the names from the ConstructorNode) to the locals.
     * Calls InterpretStatementBlock
     * @param object - the object that we allocated
     * @param c - which constructor is being called
     * @param values - the parameter values being passed to the constructor
     */
    private void interpretConstructorCall(ObjectIDT object, ConstructorNode c, List<InterpreterDataType> values) {
        //make local variabes and check if parameter count matches
        HashMap<String, InterpreterDataType> locals = new HashMap<String, InterpreterDataType>();
        if (c.parameters.size() != values.size()) {
            throw new RuntimeException("Wrong number of parameters");
        }

        for (int i = 0; i < values.size(); i++) {
            locals.put(c.parameters.get(i).name, values.get(i));
        }

        interpretStatementBlock(Optional.of(object), c.statements, locals);

    }

    //              Running Instructions

    /**
     * Given a block (which could be from a method or an "if" or "loop" block, run each statement.
     * Blocks, by definition, do ever statement, so iterating over the statements makes sense.
     *
     * For each statement in statements:
     * check the type:
     *      For AssignmentNode, FindVariable() to get the target. Evaluate() the expression. Call Assign() on the target with the result of Evaluate()
     *      For MethodCallStatementNode, call doMethodCall(). Loop over the returned values and copy the into our local variables
     *      For LoopNode - there are 2 kinds.
     *          Setup:
     *          If this is a Loop over an iterator (an Object node whose class has "iterator" as an interface)
     *              Find the "getNext()" method; throw an exception if there isn't one
     *          Loop:
     *          While we are not done:
     *              if this is a boolean loop, Evaluate() to get true or false.
     *              if this is an iterator, call "getNext()" - it has 2 return values. The first is a boolean (was there another?), the second is a value
     *              If the loop has an assignment variable, populate it: for boolean loops, the true/false. For iterators, the "second value"
     *              If our answer from above is "true", InterpretStatementBlock() on the body of the loop.
     *       For If - Evaluate() the condition. If true, InterpretStatementBlock() on the if's statements. If not AND there is an else, InterpretStatementBlock on the else body.
     * @param object - the object that this statement block belongs to (used to get member variables and any members without an object)
     * @param statements - the statements to run
     * @param locals - the local variables
     */
    private void interpretStatementBlock(Optional<ObjectIDT> object, List<StatementNode> statements, HashMap<String, InterpreterDataType> locals) {
        for (StatementNode stmt : statements) {

            if (object.isPresent()) {
                System.out.println("Evaluating method body with object members: " + object.get().members.keySet());
            } else {
                System.out.println("Evaluating method body with NO object");
            }

            if (stmt instanceof AssignmentNode) { //target and expression
                AssignmentNode assign = (AssignmentNode) stmt;
                System.out.println("Assignment node found");
                InterpreterDataType target = findVariable(assign.target.name, locals, object);
                InterpreterDataType expression = evaluate(locals, object, assign.expression);

                target.Assign(expression);

            }
            else if (stmt instanceof MethodCallStatementNode) {
                MethodCallStatementNode call = (MethodCallStatementNode) stmt;
                System.out.println("Methodcall statement found: looking for method call");
                List<InterpreterDataType> values = findMethodForMethodCallAndRunIt(object, locals, call);
                for (int i = 0; i < call.returnValues.size(); i++) {
                    locals.put(call.returnValues.get(i).name, values.get(i));
                }

            }
            else if (stmt instanceof LoopNode){ //getNext() is a method in tran
                LoopNode loop = (LoopNode) stmt;
                System.out.println("Loop node found... evaluating expression");
                //loops have expression, assignment and statements
                //two types of loops, iterator or boolean
                InterpreterDataType expression = evaluate(locals, object, loop.expression);

                //iterator (an Object node whose class has "iterator" as an interface)
                Optional<ObjectIDT> iterator = Optional.empty();

                if (expression instanceof ObjectIDT objectIDT) { //check for an iterator object
                    iterator = Optional.of(objectIDT);
                }

                if(iterator.isPresent()) { //iterator loop
                    //iterate through
                    ObjectIDT iterate = iterator.get();

                    MethodCallStatementNode getNext = new MethodCallStatementNode();
                    getNext.methodName = "getNext";
                    getNext.parameters.clear();
                    getNext.returnValues.clear();

                    List<InterpreterDataType> getNextResult = findMethodForMethodCallAndRunIt(Optional.of(iterate), locals, getNext);

                    BooleanIDT bool = (BooleanIDT) getNextResult.get(0); //first return has boolean value
                    if (bool.Value) {
                        InterpreterDataType value = getNextResult.get(1); //second return has value

                        if (loop.assignment.isPresent()) {
                            locals.put(loop.assignment.get().name, value);
                        }

                        interpretStatementBlock(object, loop.statements, locals);
                    }

                } else if (expression instanceof BooleanIDT) { //boolean loop
                    while (((BooleanIDT) evaluate(locals, object, loop.expression)).Value){
                        if (loop.assignment.isPresent()) {
                            InterpreterDataType value = evaluate(locals, object, loop.expression);
                            locals.put(loop.assignment.get().name, value);
                        }

                        interpretStatementBlock(object, loop.statements, locals);
                    }
                } else {
                    throw new RuntimeException("Unknown loop expression");
                }

            }
            //For If - Evaluate() the condition. If true, InterpretStatementBlock() on the if's statements. If not AND there is an else, InterpretStatementBlock on the else body.
            else if (stmt instanceof IfNode) {
                IfNode ifNode = (IfNode) stmt;
                InterpreterDataType condition = evaluate(locals, object, ifNode.condition);

                BooleanIDT cond = (BooleanIDT) condition;
                if (cond.Value){
                    interpretStatementBlock(object, ifNode.statements, locals);
                } else {
                    if (ifNode.elseStatement.isPresent()){
                        interpretStatementBlock(object, ifNode.statements, locals);
                    }
                }
            }
        }
    }

    /**
     *  evaluate() processes everything that is an expression - math, variables, boolean expressions.
     *  There is a good bit of recursion in here, since math and comparisons have left and right sides that need to be evaluated.
     *
     * See the How To Write an Interpreter document for examples
     * For each possible ExpressionNode, do the work to resolve it:
     * BooleanLiteralNode - create a new BooleanLiteralNode with the same value
     *      - Same for all of the basic data types
     * BooleanOpNode - Evaluate() left and right, then perform either and/or on the results.
     * CompareNode - Evaluate() both sides. Do good comparison for each data type
     * MathOpNode - Evaluate() both sides. If they are both numbers, do the math using the built-in operators. Also handle String + String as concatenation (like Java)
     * MethodCallExpression - call doMethodCall() and return the first value
     * VariableReferenceNode - call findVariable()
     * @param locals the local variables
     * @param object - the current object we are running
     * @param expression - some expression to evaluate
     * @return a value
     */
    private InterpreterDataType evaluate(HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object, ExpressionNode expression) {
        //check expresion, compare it
        //create  new ______LiteralNode with same value
        //bool
        if (expression instanceof BooleanLiteralNode) {
            System.out.println("BooleanLiteralNode found..." + expression);
            BooleanLiteralNode bool = (BooleanLiteralNode) expression;
            return new BooleanIDT(bool.value);
        }

        //char
        if (expression instanceof CharLiteralNode) {
            System.out.println("CharLiteralNode found..." + expression);
            CharLiteralNode character = (CharLiteralNode) expression;
            return new CharIDT(character.value);
        }

        //number
        if (expression instanceof NumericLiteralNode) {
            System.out.println("NumericLiteralNode found..." + expression);
            NumericLiteralNode numeric = (NumericLiteralNode) expression;
            return new NumberIDT(numeric.value);
        }

        //string
        if (expression instanceof StringLiteralNode) {
            System.out.println("StringLiteralNode found..." + expression);
            StringLiteralNode string = (StringLiteralNode) expression;
            return new StringIDT(string.value);
        }

        //booleanOp node
//        public ExpressionNode left;
//        public ExpressionNode right;
//        public enum BooleanOperations { and, or }
//        public BooleanOpNode.BooleanOperations op;
        //NO AND | OR


        //compareNode
//        public ExpressionNode left;
//        public ExpressionNode right;
//        public enum CompareOperations { lt, le, gt, ge, eq, ne}
//        public CompareNode.CompareOperations op;
//        private String opToString() {
//            switch (op) {
//                case lt -> {return " < ";}
//                case le -> {return " <= ";}
//                case gt -> {return " > ";}
//                case ge -> {return " >= ";}
//                case eq -> {return " == ";}
//                case ne -> {return " != ";}
        if (expression instanceof CompareNode) { //Evaluate() both sides. Do good comparison for each data type
            System.out.println("CompareNode found..." + expression);
            CompareNode compare = (CompareNode) expression;
            InterpreterDataType left = evaluate(locals, object, compare.left);
            InterpreterDataType right = evaluate(locals, object, compare.right);
            //data types: number, string. object, boolean
            //number -> all comparisons
            //string, == and !=
            //boolean, == and !=
            //object, == and !=


            switch (compare.op){
                case lt: //numbers only <
                    if (left instanceof NumberIDT l && right instanceof NumberIDT r) {
                        return new BooleanIDT(l.Value < r.Value);
                    } throw new RuntimeException("Can only compare numbers with <=");
                case gt: //num only >
                    if (left instanceof NumberIDT l && right instanceof NumberIDT r) {
                        return new BooleanIDT(l.Value > r.Value);
                    } throw new RuntimeException("Can only compare numbers with <=");
                case ge: // num only >=
                    if (left instanceof NumberIDT l && right instanceof NumberIDT r) {
                        return new BooleanIDT(l.Value >= r.Value);
                    } throw new RuntimeException("Can only compare numbers with <=");
                case le://num only <=
                    if (left instanceof NumberIDT l && right instanceof NumberIDT r) {
                        return new BooleanIDT(l.Value <= r.Value);
                    } throw new RuntimeException("Can only compare numbers with <=");
                case eq: //num, string, boolean and object ==
                    if (left instanceof NumberIDT l && right instanceof NumberIDT r) return new BooleanIDT(l.Value == r.Value);
                    else if (left instanceof StringIDT l && right instanceof StringIDT r) return new BooleanIDT(l.Value.equals(r.Value));
                    else if (left instanceof ObjectIDT l && right instanceof ObjectIDT r) return new BooleanIDT(l.astNode == r.astNode);
                    else if (left instanceof BooleanIDT l && right instanceof BooleanIDT r) return new BooleanIDT(l.Value == r.Value);
                    else throw new RuntimeException("Unknown equality comparison");
                case ne: //num, string, bool and obj !=
                    if (left instanceof NumberIDT l && right instanceof NumberIDT r) return new BooleanIDT(l.Value != r.Value);
                    else if (left instanceof ObjectIDT l && right instanceof ObjectIDT r) return new BooleanIDT(l.astNode != r.astNode);
                    else if (left instanceof StringIDT l && right instanceof StringIDT r) return new BooleanIDT(!(l.Value.equals(r.Value)));
                    else if (left instanceof BooleanIDT l && right instanceof BooleanIDT r) return new BooleanIDT(l.Value != r.Value);
                    else throw new RuntimeException("Unknown inequality comparison");
                default:
                    throw new RuntimeException("Unknown comparison");
            }


        }

        //mathOpNode
//        public ExpressionNode left;
//        public ExpressionNode right;
//        public enum MathOperations { add, subtract, multiply, divide, modulo }
//        public MathOpNode.MathOperations op;
//
//        private String opToString() {
//            switch (op) {
//                case add -> {return " + ";}
//                case subtract -> {return " - ";}
//                case multiply -> {return " * ";}
//                case divide -> {return " / ";}
//                case modulo -> {return " % ";}
        if (expression instanceof MathOpNode) {
            System.out.println("MathOpNode found..." + expression);
            MathOpNode math = (MathOpNode) expression;
            InterpreterDataType left = evaluate(locals, object, math.left); //left hand expression
            InterpreterDataType right = evaluate(locals, object, math.right); //right hand expression
            //check if they are both numbers before doing operations, because we can concatenate strings
            if(left instanceof NumberIDT l && right instanceof NumberIDT r) {
                if(math.op == MathOpNode.MathOperations.add) return new NumberIDT(l.Value + r.Value);
                if(math.op == MathOpNode.MathOperations.subtract) return new NumberIDT(l.Value - r.Value);
                if(math.op == MathOpNode.MathOperations.multiply) return new NumberIDT(l.Value * r.Value);
                if(math.op == MathOpNode.MathOperations.divide) return new NumberIDT(l.Value / r.Value);
                if(math.op == MathOpNode.MathOperations.modulo) return new NumberIDT(l.Value % r.Value);
                throw new RuntimeException("Unknown Math operation");
            } else if (left instanceof StringIDT l && right instanceof StringIDT r) { //most likely a string concatenation
                if(math.op == MathOpNode.MathOperations.add) return new StringIDT(l.Value + r.Value);
                throw new RuntimeException("Unknown Math operation for string");
            }
        }


//        public Optional<String> objectName;
//        public String methodName;
//        public List<ExpressionNode> parameters = new ArrayList<>();
        //MethodCallExpression - call doMethodCall() and return the first value
        if(expression instanceof MethodCallExpressionNode) {
            System.out.println("MethodCallExpressionNode found..." + expression);
            MethodCallExpressionNode call = (MethodCallExpressionNode) expression;
            MethodCallStatementNode listIDT = new MethodCallStatementNode(call);
            List<InterpreterDataType> returnValues = findMethodForMethodCallAndRunIt(object, locals, listIDT);

            return returnValues.getFirst();
        }


        //VariableReferenceNode - call findVariable()
        //public string name
        if (expression instanceof VariableReferenceNode) {
            System.out.println("VariableReferenceNode found..." + expression);
            VariableReferenceNode var = (VariableReferenceNode) expression;
            return findVariable(var.name, locals, object);
        }

        //constructor
        if (expression instanceof NewNode) {
            System.out.println("NewNode found, constructor..." + expression);
            NewNode newNode = (NewNode) expression;
            ObjectIDT constructor = new ObjectIDT(null);

            for(int i = 0; i < top.Classes.size(); i++){ //iterate through classes to find constructor
                ClassNode classes = top.Classes.get(i);
                if (classes.name.equals(newNode.className)) {
                    constructor = new ObjectIDT(classes);
                    break;
                }
            }


            //create the constructor
            if (constructor.astNode != null) {

                for (int i = 0; i < constructor.astNode.members.size(); i++) {
                    MemberNode member = constructor.astNode.members.get(i);
                    constructor.members.put(member.declaration.name, instantiate(member.declaration.type));
                }

                MethodCallStatementNode constructorCall = new MethodCallStatementNode();
                constructorCall.objectName = Optional.of(newNode.className);
                constructorCall.methodName = newNode.className;
                constructorCall.parameters = newNode.parameters;

                System.out.println("Searching for a constructor " + constructorCall);
                findConstructorAndRunIt(Optional.empty(), locals, constructorCall, constructor);

                return constructor;
            }
        }


        throw new IllegalArgumentException();
    }

    //              Utility Methods

    /**
     * Used when trying to find a match to a method call. Given a method declaration, does it match this method call?
     * We double-check with the parameters, too, although in theory JUST checking the declaration to the call should be enough.
     *
     * Match names, parameter counts (both declared count vs method call and declared count vs value list), return counts.
     * If all of those match, consider the types (use TypeMatchToIDT).
     * If everything is OK, return true, else return false.
     * Note - if m is a built-in and isVariadic is true, skip all of the parameter validation.
     * @param m - the method declaration we are considering
     * @param mc - the method call we are trying to match
     * @param parameters - the parameter values for this method call
     * @return does this method match the method call?
     */
    private boolean doesMatch(MethodDeclarationNode m, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {

        //if m is built in and isVariadic is true, skip the next lines
        if (m instanceof BuiltInMethodDeclarationNode && ((BuiltInMethodDeclarationNode) m).isVariadic){
            return true;
        }

        //match name, match parameter counts, and match return counts
        if(!m.name.equals(mc.methodName)) { //check method name
            return false;
        }

        if(m.parameters.size() == mc.parameters.size()) { //check if parameter size matches
            for (int i = 0; i < m.parameters.size(); i++) { //double check parameters
                if(!typeMatchToIDT(m.parameters.get(i).type, parameters.get(i))){
                    return false;
                }
            }
        } else {
            return false;
        }

        //now compare counts
        if(m.returns.size() != mc.returnValues.size()) {
            return false;
        }

        //returns true if all of the previous conditions were correct
        return true;
    }

    /**
     * Very similar to DoesMatch() except simpler - there are no return values, the name will always match.
     * @param c - a particular constructor
     * @param mc - the method call
     * @param parameters - the parameter values
     * @return does this constructor match the method call?
     */
    private boolean doesConstructorMatch(ConstructorNode c, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {

        if(c.parameters.size() == mc.parameters.size()) { //same from doesMatch, no returns and no name checking
            for (int i = 0; i < c.parameters.size(); i++) {
                if(!typeMatchToIDT(c.parameters.get(i).type, parameters.get(i))){
                    return false;
                }
            }
        } else {
            return false;
        }

        //return true if previous condition is correct
        return true;
    }

    /**
     * Used when we call a method to get the list of values for the parameters.
     *
     * for each parameter in the method call, call Evaluate() on the parameter to get an IDT and add it to a list
     * @param object - the current object
     * @param locals - the local variables
     * @param mc - a method call
     * @return the list of method values
     */
    private List<InterpreterDataType> getParameters(Optional<ObjectIDT> object, HashMap<String,InterpreterDataType> locals, MethodCallStatementNode mc) {
        List<InterpreterDataType> methodValues = new ArrayList<>();
        for (int i = 0; i < mc.parameters.size(); i++) {
            ExpressionNode param = mc.parameters.get(i); //get parameter
            InterpreterDataType idt = evaluate(locals, object, param); //evaluate each parameter in the list
            System.out.println("Found parameter: " + idt);
            methodValues.add(idt); //then add it
        }
        return methodValues;
    }

    /**
     * Used when we have an IDT and we want to see if it matches a type definition
     * Commonly, when someone is making a function call - do the parameter values match the method declaration?
     *
     * If the IDT is a simple type (boolean, number, etc.) - does the string type match the name of that IDT ("boolean", etc)
     * If the IDT is an object, check to see if the name matches OR the class has an interface that matches
     * If the IDT is a reference, check the inner (refered to) type
     * @param type the name of a data type (parameter to a method)
     * @param idt the IDT someone is trying to pass to this method
     * @return is this OK?
     */
    private boolean typeMatchToIDT(String type, InterpreterDataType idt) {

        //check to see if IDT is an object
        if(idt instanceof ObjectIDT) {
            //compare name or class with interface that matches
            ObjectIDT obj = (ObjectIDT) idt;
            //compare name
            if (obj.astNode.name.equals(type)) {
                return true;
            }

            //compare interfaces if present by looping through the interfaces
            for(int i = 0; i < obj.astNode.interfaces.size(); i++) {
                if (obj.astNode.interfaces.get(i).equals(type)) {
                    return true;
                }
            }

        }

        //check to see if IDT is a reference
        if(idt instanceof ReferenceIDT) { // inner referred to
            idt = ((ReferenceIDT) idt).refersTo.get(); //**fix?? isPresent()
        }

        //simple types
        switch (type){
            case "string":
                return idt instanceof StringIDT;
            case "number":
                return idt instanceof NumberIDT;
            case "boolean":
                return idt instanceof BooleanIDT;
            case "character":
                return idt instanceof CharIDT;
            default:
                throw new RuntimeException("Unable to resolve type " + type);
        }

    }

    /**
     * Find a method in an object that is the right match for a method call (same name, parameters match, etc. Uses doesMatch() to do most of the work)
     *
     * Given a method call, we want to loop over the methods for that class, looking for a method that matches (use DoesMatch) or throw
     * @param object - an object that we want to find a method on
     * @param mc - the method call
     * @param parameters - the parameter value list
     * @return a method or throws an exception
     */
    private MethodDeclarationNode getMethodFromObject(ObjectIDT object, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        for (int i = 0; i < object.astNode.methods.size(); i++) {
            if(doesMatch(object.astNode.methods.get(i), mc, parameters)) {
                return object.astNode.methods.get(i);
            }
        }

        throw new RuntimeException("Unable to resolve method call " + mc);
    }

    /**
     * Find a class, given the name. Just loops over the TranNode's classes member, matching by name.
     *
     * Loop over each class in the top node, comparing names to find a match.
     * @param name Name of the class to find
     * @return either a class node or empty if that class doesn't exist
     */
    private Optional<ClassNode> getClassByName(String name) {
        for (int i = 0; i < top.Classes.size(); i++){
            ClassNode classNode = top.Classes.get(i);
            if (classNode.name.equals(name)){
                System.out.println("Found class: " + classNode);
                return Optional.of(classNode);
            }
        }

        return Optional.empty();
    }

    /**
     * Given an execution environment (the current object, the current local variables), find a variable by name.
     *
     * @param name  - the variable that we are looking for
     * @param locals - the current method's local variables
     * @param object - the current object (so we can find members)
     * @return the IDT that we are looking for or throw an exception
     */
    private InterpreterDataType findVariable(String name, HashMap<String,InterpreterDataType> locals, Optional<ObjectIDT> object) {
        //check locals then members

        if (locals.containsKey(name)){
            return locals.get(name);
        }

        if(object.isPresent()){
            if (object.get().members.containsKey(name)){
                return object.get().members.get(name);
            }
        }

        throw new RuntimeException("Unable to find variable " + name);
    }

    /**
     * Given a string (the type name), make an IDT for it.
     *
     * @param type The name of the type (string, number, boolean, character). Defaults to ReferenceIDT if not one of those.
     * @return an IDT with default values (0 for number, "" for string, false for boolean, ' ' for character)
     */
    private InterpreterDataType instantiate(String type) {
        switch (type) {
            case "string":
                return new StringIDT("");
            case "number":
                return new NumberIDT(0);
            case "boolean":
                    return new BooleanIDT(false);
            case "character":
                return new CharIDT(' ');
            default:
                return new ReferenceIDT();
        }

    }
}
