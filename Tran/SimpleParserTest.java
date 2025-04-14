package Tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.LinkedList;
import AST.*;
import Tran.*;

public class SimpleParserTest {

    @Test
    public void SimpleParserTestTest() throws Exception {
        // Create a list of tokens for a simple class with one method
        var tokens = new LinkedList<Token>();
        tokens.add(new Token(Token.TokenTypes.CLASS, 1, 5));
        tokens.add(new Token(Token.TokenTypes.WORD, 1, 10, "SimpleClass"));
        tokens.add(new Token(Token.TokenTypes.NEWLINE, 2, 0));
        tokens.add(new Token(Token.TokenTypes.INDENT, 2, 4));

        // Method definition: shared add(number a, number b)
        tokens.add(new Token(Token.TokenTypes.SHARED, 2, 10));
        tokens.add(new Token(Token.TokenTypes.WORD, 2, 17, "add"));
        tokens.add(new Token(Token.TokenTypes.LPAREN, 2, 20));
        tokens.add(new Token(Token.TokenTypes.WORD, 2, 21, "number"));
        tokens.add(new Token(Token.TokenTypes.WORD, 2, 28, "a"));
        tokens.add(new Token(Token.TokenTypes.COMMA, 2, 29));
        tokens.add(new Token(Token.TokenTypes.WORD, 2, 31, "number"));
        tokens.add(new Token(Token.TokenTypes.WORD, 2, 38, "b"));
        tokens.add(new Token(Token.TokenTypes.RPAREN, 2, 39));
        tokens.add(new Token(Token.TokenTypes.NEWLINE, 3, 0));

        // Method body: return a + b;
        tokens.add(new Token(Token.TokenTypes.INDENT, 3, 4));
        tokens.add(new Token(Token.TokenTypes.WORD, 3, 9, "return"));
        tokens.add(new Token(Token.TokenTypes.WORD, 3, 16, "a"));
        tokens.add(new Token(Token.TokenTypes.PLUS, 3, 18));
        tokens.add(new Token(Token.TokenTypes.WORD, 3, 20, "b"));
        tokens.add(new Token(Token.TokenTypes.NEWLINE, 4, 0));

        tokens.add(new Token(Token.TokenTypes.DEDENT, 4, 0));
        tokens.add(new Token(Token.TokenTypes.DEDENT, 4, 0));

        // Create a new TranNode and Parser instance
        var tran = new TranNode();
        var p = new Parser(tran, tokens);

        // Parse the tokens
        p.Tran();

        // Assert that the class was parsed correctly
        Assertions.assertEquals(1, tran.Classes.size());
        Assertions.assertEquals("SimpleClass", tran.Classes.get(0).name);
        Assertions.assertEquals(1, tran.Classes.get(0).methods.size());

        // Assert method information
        MethodDeclarationNode method = (MethodDeclarationNode) tran.Classes.get(0).methods.get(0);
        Assertions.assertEquals("add", method.name);
        Assertions.assertEquals(2, method.parameters.size());
        Assertions.assertEquals("a", method.parameters.get(0).name);
        Assertions.assertEquals("number", method.parameters.get(0).type);
        Assertions.assertEquals("b", method.parameters.get(1).name);
        Assertions.assertEquals("number", method.parameters.get(1).type);

        // Assert the method body
        Assertions.assertEquals(1, method.statements.size());


    }
}
