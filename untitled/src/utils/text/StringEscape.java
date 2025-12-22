package utils.text;

/**
 * Utilitaires d'échappement / déséchappement de chaînes et caractères
 * pour un compilateur (principalement génération de code Java et messages).
 *
 * Objectifs :
 * - produire des littéraux Java sûrs : "...\n..." et 'a'
 * - gérer les caractères de contrôle, les guillemets, les backslashes
 * - offrir un déséchappement pratique pour des chaînes source type "a\nb"
 *
 * Conventions :
 * - Les méthodes "java*" renvoient un contenu prêt à être placé ENTRE guillemets
 *   (elles ne rajoutent pas les guillemets elles-mêmes).
 */
public final class StringEscape {

    private StringEscape() {}

    // -------------------------------------------------------------------------
    // Echappement pour Java
    // -------------------------------------------------------------------------

    /**
     * Échappe une chaîne pour l'inclure dans un littéral Java "..." (sans ajouter les guillemets).
     * Ex :
     *  entrée : Bonjour "toi"\n
     *  sortie : Bonjour \"toi\"\\n
     */
    public static String echapperPourJavaString(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"'  -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (estControleOuNonImprimable(c)) {
                        out.append(escapeUnicode(c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    /**
     * Échappe un caractère pour l'inclure dans un littéral Java 'x' (sans ajouter les quotes).
     * - Gère \\, \', \n, etc.
     * - Les caractères non imprimables sont en \\uXXXX.
     */
    public static String echapperPourJavaChar(char c) {
        return switch (c) {
            case '\\' -> "\\\\";
            case '\'' -> "\\'";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            case '\b' -> "\\b";
            case '\f' -> "\\f";
            default -> estControleOuNonImprimable(c) ? escapeUnicode(c) : String.valueOf(c);
        };
    }

    /**
     * Construit un littéral Java complet avec guillemets : "..."
     */
    public static String literalJavaString(String s) {
        return "\"" + echapperPourJavaString(s) + "\"";
    }

    /**
     * Construit un littéral Java complet avec quotes : 'x'
     */
    public static String literalJavaChar(char c) {
        return "'" + echapperPourJavaChar(c) + "'";
    }

    // -------------------------------------------------------------------------
    // Déséchappement (utile si tu stockes des chaînes avec \n, \t,
    // -------------------------------------------------------------------------

    /**
     * Déséchappe une chaîne contenant des séquences type :
     *  \\n, \\t, \\r, \\\\, \\\", \\b, \\f, \\uXXXX
     *
     * Comportement en cas de séquence invalide :
     * - on garde le backslash et ce qui suit (choix robuste pour un compilateur)
     */
    public static String desechapper(String s) {
        if (s == null || s.isEmpty()) return s == null ? "" : s;

        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c != '\\' || i == s.length() - 1) {
                out.append(c);
                continue;
            }

            // On a un backslash + quelque chose
            char n = s.charAt(++i);
            switch (n) {
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case '\\' -> out.append('\\');
                case '"' -> out.append('"');
                case '\'' -> out.append('\'');
                case 'u' -> {
                    //
                    if (i + 4 <= s.length() - 1) {
                        String hex = s.substring(i + 1, i + 5);
                        Integer code = parseHex4(hex);
                        if (code != null) {
                            out.append((char) (int) code);
                            i += 4; // consommer XXXX
                        } else {
                            // invalide => on garde la séquence brute
                            out.append("\\u");
                        }
                    } else {
                        out.append("\\u");
                    }
                }
                default -> {
                    // Séquence inconnue : on garde au mieux (robuste)
                    out.append('\\').append(n);
                }
            }
        }

        return out.toString();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean estControleOuNonImprimable(char c) {
        // contrôle ASCII, DEL, ou caractères "suspects"
        // (tu peux élargir si besoin; ici on garde simple et sûr)
        return c < 0x20 || c == 0x7F;
    }

    private static String escapeUnicode(char c) {
        // Format Java :
        int v = c;
        String hex = Integer.toHexString(v).toUpperCase();
        return "\\u" + "0".repeat(Math.max(0, 4 - hex.length())) + hex;
    }

    private static Integer parseHex4(String hex4) {
        if (hex4 == null || hex4.length() != 4) return null;
        for (int i = 0; i < 4; i++) {
            char ch = hex4.charAt(i);
            boolean ok = (ch >= '0' && ch <= '9')
                    || (ch >= 'a' && ch <= 'f')
                    || (ch >= 'A' && ch <= 'F');
            if (!ok) return null;
        }
        try {
            return Integer.parseInt(hex4, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
