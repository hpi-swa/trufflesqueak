package de.hpi.swa.trufflesqueak.instrumentation;

public class SourceStringBuilder {
    private static final String INDENT = "  ";
    private final StringBuilder sb = new StringBuilder();
    private int padding = 0;

    public SourceStringBuilder newline() {
        sb.append('\n');
        for (int i = 0; i < padding; i++) {
            sb.append(INDENT);
        }
        return this;
    }

    public SourceStringBuilder append(String string) {
        sb.append(string);
        return this;
    }

    public SourceStringBuilder append(char c) {
        sb.append(c);
        return this;
    }

    public SourceStringBuilder append(int arraySize) {
        sb.append(arraySize);
        return this;
    }

    public SourceStringBuilder append(Object obj) {
        sb.append(obj.toString());
        return this;
    }

    public SourceStringBuilder indent() {
        padding += 2;
        sb.append(INDENT).append(INDENT);
        return this;
    }

    public SourceStringBuilder dedent() {
        padding -= 2;
        int length = sb.length();
        int dedentLength = INDENT.length() * 2;
        // only delete whitespace
        if (sb.subSequence(length - dedentLength, length).toString().trim().length() == 0) {
            sb.delete(length - dedentLength, length);
        }
        return this;
    }

    @Override
    public String toString() {
        throw new RuntimeException("shouldn't use");
    }

    public String build() {
        return sb.toString();
    }
}
