package com.kritik.POS.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    public static final int MONEY_SCALE = 2;
    public static final int RATE_SCALE = 4;

    private MoneyUtils() {
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return zero();
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal rate(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal add(BigDecimal left, BigDecimal right) {
        return money(money(left).add(money(right)));
    }

    public static BigDecimal subtract(BigDecimal left, BigDecimal right) {
        return money(money(left).subtract(money(right)));
    }

    public static BigDecimal multiply(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return zero();
        }
        return left.multiply(right).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal percentOf(BigDecimal base, BigDecimal rate) {
        if (base == null || rate == null) {
            return zero();
        }
        return base.multiply(rate)
                .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal inclusivePercentTax(BigDecimal gross, BigDecimal rate) {
        if (gross == null || rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return zero();
        }
        return gross.multiply(rate)
                .divide(BigDecimal.valueOf(100).add(rate), MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
