package org.example.sansrus.sansrusmod.client.chatcoord;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoordParser {

    public static class CoordMatch {
        public final int x, y, z;
        public final int start, end;
        public final String original;
        public final boolean hasY;

        public CoordMatch(int x, int y, int z, boolean hasY, int start, int end, String original) {
            this.x = x; this.y = y; this.z = z;
            this.hasY = hasY;
            this.start = start; this.end = end;
            this.original = original;
        }
    }

    // x:123 y:456 z:789 | x=123 y=456 z=789 | x123 y456 z789
    private static final Pattern LABELED_XYZ = Pattern.compile(
            "[xXхХ][=:]?\\s*(-?\\d+)\\s+[yYуУ][=:]?\\s*(-?\\d+)\\s+[zZзЗ][=:]?\\s*(-?\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    // x:123 z:789 (без Y)
    private static final Pattern LABELED_XZ = Pattern.compile(
            "[xXхХ][=:]?\\s*(-?\\d+)\\s+[zZзЗ][=:]?\\s*(-?\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    // Три числа подряд: -123 456 789
    // Используем word boundary чтобы не захватывать числа внутри слов
    private static final Pattern PLAIN_XYZ = Pattern.compile(
            "(?<![\\w.])-?(\\d+)(?![\\w.])\\s+(?<![\\w.])-?(\\d+)(?![\\w.])\\s+(?<![\\w.])-?(\\d+)(?![\\w.])"
    );

    // Два числа подряд: -123 456
    private static final Pattern PLAIN_XZ = Pattern.compile(
            "(?<![\\w.])-?(\\d+)(?![\\w.])\\s+(?<![\\w.])-?(\\d+)(?![\\w.])"
    );

    public static List<CoordMatch> findAll(String text) {
        List<CoordMatch> results = new ArrayList<>();

        tryPattern(LABELED_XYZ, text, results, true);
        tryPattern(LABELED_XZ,  text, results, false);
        tryPattern(PLAIN_XYZ,   text, results, true);
        tryPattern(PLAIN_XZ,    text, results, false);

        // Убираем пересечения: оставляем только неперекрывающиеся совпадения
        results.removeIf(a -> results.stream().anyMatch(b ->
                b != a && b.start <= a.start && b.end >= a.end && (b.hasY || !a.hasY)
        ));
        results.sort((a, b) -> Integer.compare(a.start, b.start));
        return results;
    }

    private static void tryPattern(Pattern pattern, String text,
                                   List<CoordMatch> results, boolean hasY) {
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            try {
                // Исходная строка чтобы проверить допустимые диапазоны
                int g1 = Integer.parseInt(m.group(1).replaceAll("[^0-9]", ""));
                int g2 = Integer.parseInt(m.group(2).replaceAll("[^0-9]", ""));

                // Фильтр: исключаем явно "нехотябы-на-координаты" числа
                // (например версии 1.21.7, временные метки 23:45, etc.)
                boolean firstIsCoord  = m.group(1).contains("-") || isCoordRange(g1);
                boolean secondIsCoord = m.group(2).contains("-") || isCoordRange(g2);
                if (!firstIsCoord || !secondIsCoord) continue;

                int x, y, z;
                if (hasY && m.groupCount() >= 3) {
                    int g3 = Integer.parseInt(m.group(3).replaceAll("[^0-9]", ""));
                    x = parseGroupSign(m, 1);
                    y = parseGroupSign(m, 2);
                    z = parseGroupSign(m, 3);
                } else {
                    x = parseGroupSign(m, 1);
                    y = 64;
                    z = parseGroupSign(m, 2);
                }

                results.add(new CoordMatch(x, y, z, hasY, m.start(), m.end(), m.group()));
            } catch (NumberFormatException ignored) {}
        }
    }

    private static int parseGroupSign(Matcher m, int group) {
        String full = m.group(0);
        String val  = m.group(group);
        // Найти позицию числа в полной строке и проверить минус перед ним
        int pos = full.indexOf(val);
        boolean negative = pos > 0 && full.charAt(pos - 1) == '-';
        int num = Integer.parseInt(val.replaceAll("[^0-9]", ""));
        return negative ? -num : num;
    }

    // Координаты Minecraft обычно в диапазоне [-30000000; 30000000]
    private static boolean isCoordRange(int n) {
        return n > 0 && n <= 60_000_000;
    }
}
