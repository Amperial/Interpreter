package core;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {

    private final List<Token> tokens;
    private final Token whitespace;

    private StringBuilder input;
    private int token;
    private String value;

    private int tokenNumber;

    public Tokenizer() {
        tokens = new LinkedList<>();
        whitespace = new Token(0, "\\s");
    }

    public void add(Token token) {
        tokens.add(token);
    }

    public void setInput(StringBuilder input) {
        this.input = input;
        this.token = 0;
        this.tokenNumber = 0;
        this.value = "";
        // Remove any initial whitespace
        whitespace.match(input);
    }

    public int getToken() {
        return token;
    }

    public String getValue() {
        return value;
    }

    public boolean skipToken() throws Exception {
        if (input.length() == 0) {
            // EOF token
            token = 33;
            value = "";
            return false;
        }
        for (Token token : tokens) {
            // Check for next match
            Optional<String> match = token.match(input);
            if (match.isPresent()) {
                // Found a match, set token and value
                this.token = token.getId();
                this.value = match.get();
                // Remove any whitespace after the match
                whitespace.match(input);
                // Increment token number
                tokenNumber++;
                return true;
            }
        }
        // Illegal token
        throw new Exception("Encountered illegal token (after token #" + tokenNumber + "): " + input);
    }

    public int getTokenNumber() {
        return tokenNumber;
    }

    public void tokenize(StringBuilder input) {
        try {
            // Set tokenizer input
            setInput(input);
            // Step through and print all token numbers
            boolean hasNext;
            do {
                hasNext = skipToken();
                System.out.println(getToken());
            } while (hasNext);
        } catch (Exception e) {
            // Illegal token or some other issue
            System.out.println(e.getMessage());
        }
    }

    public static class Token  {

        private final int id;
        private final Pattern pattern;
        private final int maxLength;

        public Token(int id, String regex, int maxLength) {
            this.id = id;
            this.pattern = Pattern.compile(regex);
            this.maxLength = maxLength;
        }

        public Token(int id, String regex) {
            this(id, regex, -1);
        }

        public int getId() {
            return id;
        }

        public Optional<String> match(StringBuilder input) {
            // Create a matcher from the token's pattern and given input
            Matcher matcher = pattern.matcher(input);
            // Attempt to find a match at the start of the input
            if (matcher.find() && matcher.start() == 0) {
                String match = matcher.group();
                // Ensure match meets length requirements
                if (maxLength < 0 || match.length() <= maxLength) {
                    // Remove match from input
                    input.delete(0, match.length());
                    // Return match value
                    return Optional.of(match);
                }
            }
            // No match found
            return Optional.empty();
        }

    }

}
