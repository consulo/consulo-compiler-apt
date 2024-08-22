package consulo.compiler.apt.shared;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class NameUtil {

    public static String normalizeName(String text) {
        char c = text.charAt(0);
        if (c == '0') {
            return "zero" + text.substring(1, text.length());
        }
        else if (c == '1') {
            return "one" + text.substring(1, text.length());
        }
        else if (c == '2') {
            return "two" + text.substring(1, text.length());
        }
        return text;
    }

    public static String captilizeByDot(String id) {
        String[] split = id.replace(" ", ".").split("\\.");

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            if (i != 0) {
                builder.append(capitalise(split[i]));
            }
            else {
                builder.append(split[i]);
            }
        }

        return builder.toString();
    }

    public static String getPackageName(String fqName) {
        return getPackageName(fqName, '.');
    }

    /**
     * Given a fqName returns the package name for the type or the containing type.
     * <p/>
     * <ul>
     * <li><code>java.lang.String</code> -> <code>java.lang</code></li>
     * <li><code>java.util.Map.Entry</code> -> <code>java.util.Map</code></li>
     * </ul>
     *
     * @param fqName    a fully qualified type name. Not supposed to contain any type arguments
     * @param separator the separator to use. Typically '.'
     * @return the package name of the type or the declarator of the type. The empty string if the given fqName is unqualified
     */
    public static String getPackageName(String fqName, char separator) {
        int lastPointIdx = fqName.lastIndexOf(separator);
        if (lastPointIdx >= 0) {
            return fqName.substring(0, lastPointIdx);
        }
        return "";
    }

    public static String getShortName(Class aClass) {
        return getShortName(aClass.getName());
    }

    public static String getShortName(String fqName) {
        return getShortName(fqName, '.');
    }

    public static String getShortName(String fqName, char separator) {
        int lastPointIdx = fqName.lastIndexOf(separator);
        if (lastPointIdx >= 0) {
            return fqName.substring(lastPointIdx + 1);
        }
        return fqName;
    }

    public static String capitalise(String str) {
        if (str == null) {
            return null;
        }
        else {
            int length = str.length();
            if (length == 0) {
                return "";
            }
            else {
                return new StringBuilder(length)
                    .append(Character.toTitleCase(str.charAt(0)))
                    .append(str, 1, length)
                    .toString();
            }
        }
    }

    public static CharSequence getNameWithoutExtension(CharSequence name) {
        int i = lastIndexOf(name, '.', 0, name.length());
        return i < 0 ? name : name.subSequence(0, i);
    }

    public static String getNameWithoutExtension(String name) {
        return getNameWithoutExtension((CharSequence) name).toString();
    }

    /**
     * Allows to retrieve index of last occurrence of the given symbols at <code>[start; end)</code> sub-sequence of the given text.
     *
     * @param s     target text
     * @param c     target symbol which last occurrence we want to check
     * @param start start offset of the target text (inclusive)
     * @param end   end offset of the target text (exclusive)
     * @return index of the last occurrence of the given symbol at the target sub-sequence of the given text if any;
     * <code>-1</code> otherwise
     */
    public static int lastIndexOf(CharSequence s, char c, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            if (s.charAt(i) == c) {
                return i;
            }
        }
        return -1;
    }
}
