package Tran;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IndentTest {

    @Test
    public void testIndentation() throws Exception {
        String input =
                """
                class Test:
                    if condition:
                        print(Hello)
                    print(Done)
                """;


        Lexer lexer = new Lexer(input);
        List<Token> tokens = lexer.Lex();

        assertNotNull(tokens);
        assertTrue(tokens.size() > 0);

        // Find the indices of the INDENT and DEDENT tokens
        int indentCount = 0;
        int dedentCount = 0;

        for (Token token : tokens) {
            if (token.getType() == Token.TokenTypes.INDENT) {
                indentCount++;
            }
            if (token.getType() == Token.TokenTypes.DEDENT) {
                dedentCount++;
            }
            System.out.println(token.getType() + " -> " + token.getValue());
        }

        // Assertions
        assertEquals(2, indentCount, "Expected 2 INDENT tokens");
        assertEquals(2, dedentCount, "Expected 2 DEDENT tokens");
    }

    @Test
    public void IndentTest() {
        var l = new Lexer(
                "loop keepGoing\n" +
                        "    if n >= 15\n" +
                        "    \tkeepGoing = false\n" +
                        "    n++\n" +
                        "console.write(n)\n" +
                        "loop keepGoing\n"  +
                        "    if n >= 15\n" +
                        "        keepGoing = false\n" +
                        "    n++\n"
        );

        try {
            var res = l.Lex();

            // Initialize counters for INDENT and DEDENT tokens
            int indentCount = 0;
            int dedentCount = 0;

            // Print all tokens and count INDENT and DEDENT tokens
            System.out.println("Generated tokens:");
            for (Token token : res) {
                System.out.println(token.getType() + " -> " + token.getValue());

                // Count INDENT and DEDENT tokens
                if (token.getType() == Token.TokenTypes.INDENT) {
                    indentCount++;
                } else if (token.getType() == Token.TokenTypes.DEDENT) {
                    dedentCount++;
                }
            }

            // Print the counts for INDENT and DEDENT tokens
            System.out.println("Total INDENT tokens: " + indentCount);
            System.out.println("Total DEDENT tokens: " + dedentCount);
            System.out.println("Total tokens: " + res.size());

            // Assertions to check token counts (adjust if necessary)
            Assertions.assertEquals(47, res.size());
            // You can also assert the specific counts for INDENT and DEDENT if needed
            // For example, based on expected indent and dedent counts
            // Assertions.assertEquals(expectedIndentCount, indentCount);
            // Assertions.assertEquals(expectedDedentCount, dedentCount);

        } catch (Exception e) {
            Assertions.fail("Exception occurred: " + e.getMessage());
        }
    }


}
