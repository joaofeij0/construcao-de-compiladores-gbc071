package src.visitor;

final class Escapes {
    private Escapes() {}

    static String string(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    static String character(char c) {
        switch (c) {
            case '\'': return "\\'";
            case '\\': return "\\\\";
            case '\n': return "\\n";
            case '\t': return "\\t";
            default:   return String.valueOf(c);
        }
    }
}
