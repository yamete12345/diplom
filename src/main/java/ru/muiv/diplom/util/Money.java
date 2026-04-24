package ru.muiv.diplom.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Форматирование денежных сумм для UI: два знака после запятой,
 * неразрывный пробел как разделитель тысяч, точка — десятичный разделитель.
 * Пример: 100000.0000 → "100 000.00".
 */
public final class Money {

    private static final char NBSP = '\u00A0';

    private static final ThreadLocal<DecimalFormat> FORMAT = ThreadLocal.withInitial(() -> {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
        s.setGroupingSeparator(NBSP);
        s.setDecimalSeparator('.');
        DecimalFormat f = new DecimalFormat("#,##0.00", s);
        f.setGroupingSize(3);
        f.setRoundingMode(RoundingMode.HALF_UP);
        return f;
    });

    private Money() {}

    /** "100 000.00" — с неразрывным пробелом между разрядами. */
    public static String format(BigDecimal value) {
        if (value == null) return "";
        return FORMAT.get().format(value);
    }

    /** "100 000.00 RUB". */
    public static String format(BigDecimal value, String currency) {
        if (value == null) return "";
        return format(value) + (currency == null || currency.isBlank() ? "" : NBSP + currency);
    }
}
