package Tran;
public class TextManager {

    private String text = "";
    private int position; //tracks the position of a string


    public TextManager(String input) {
        this.position = 0;
        this.text = input;
    }

    /**
     * Returns true if position number is higher than the valid number
     * of indicies in the string
     * @return
     */
    public boolean isAtEnd() {
        if (position >= text.length()) {
            return true;
        } else {
            return false;
        }
    }

    public char peekCharacter() {
        if (isAtEnd()){
            return '\0';
        }
        return text.charAt(position);
    }

    public char peekCharacter(int dist) {
        if (isAtEnd()){
            return '\0';
        }
        return text.charAt(position+dist);
    }

    public char getCharacter() {
        if (isAtEnd()){
            return '\0';
        }
        return text.charAt(position++);
    }
}
