package Tran;
import java.util.List;
import java.util.Optional;

public class TokenManager {

    private final List<Token> tokenManager; //list of tokens


    public TokenManager(List<Token> tokens) {
        this.tokenManager = tokens;
    }

    /**
     * used to check if the token list is empty
     * @return empty list of tokens
     */
    public boolean done() {
	    return tokenManager.isEmpty();
    }
    //look at the list and see if the token next in the list, if not, do nothing. (optional.empty)

    /**
     * Looks at the next token in the token list and matches and removes it. If not present, or not next
     * do nothing
     * @param t the token type
     * @return Optional.empty() nothing
     */
    public Optional<Token> matchAndRemove(Token.TokenTypes t) {
        if (!tokenManager.isEmpty()) {
                if (tokenManager.get(0).getType() == t) {
                    return Optional.of(tokenManager.remove(0));
                }
        }

	    return Optional.empty();
    }

    //peeks i characters ahead
    public Optional<Token> peek(int i) {
        if (i >= 0 && i < tokenManager.size()) {
            return Optional.of(tokenManager.get(i));
        }
	    return Optional.empty();
    }


    /**
     * want to check by peeking the first two tokens and see if they match
     * size of list needs to be greater than two for this to work
     * @param first
     * @param second
     * @return
     */
    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second) {
        if (!tokenManager.isEmpty() && (tokenManager.size() >= 2)) {
            if (tokenManager.get(0).getType() == first) {
                return tokenManager.get(1).getType() == second; //return true
            }
        }
	    return false;
    }

    public boolean nextIsEither(Token.TokenTypes first, Token.TokenTypes second) {
	    return false;
    }

    /**
     * @return the current line of a token in the list
     */
    public int getCurrentLine() {
        if (!tokenManager.isEmpty()) {
            return tokenManager.get(0).getLineNumber();
        }
            return -1;
    }

    /**
     * @return the column numnber of a token in the list
     */
    public int getCurrentColumnNumber() {
        if (!tokenManager.isEmpty()) {
            return tokenManager.get(0).getColumnNumber();
        }
            return -1;
    }
}
