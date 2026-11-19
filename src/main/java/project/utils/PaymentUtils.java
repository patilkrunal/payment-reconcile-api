package project.utils;

import project.ServiceError;
import project.UnicodeStrings;
import project.dto.Payment;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaymentUtils {

    public static Payment parsePayment(Object payment, Object forgiveness, Object key) {
        if (!(payment instanceof String text)) {
            throw new ServiceError(400, "invalid_input");
        }
        String[] parts = text.split(", ", 3);
        if (parts.length < 2) {
            throw new ServiceError(400, "invalid_input");
        }
        String paymentId = UnicodeStrings.strip(parts[0]);
        String amount = UnicodeStrings.strip(parts[1]);
        List<String> memo = parts.length == 3 ? markerIds(parts[2]) :  Collections.emptyList();

        int idLength = paymentId.codePointCount(0, paymentId.length());
        if (idLength < 1 || idLength > 64) {
            throw new ServiceError(400, "invalid_input");
        }
        if (!amount.matches("[0-9]+")) {
            throw new ServiceError(400, "invalid_input");
        }
        BigInteger amountCents = new BigInteger(amount);
        if (amountCents.signum() <= 0) {
            throw new ServiceError(400, "invalid_input");
        }
        BigInteger limit = parseForgiveness(forgiveness);
        if (key != null) {
            if (!(key instanceof String keyText)) {
                throw new ServiceError(400, "invalid_input");
            }
            int keyLength = keyText.codePointCount(0, keyText.length());
            if (keyLength < 1 || keyLength > 64) {
                throw new ServiceError(400, "invalid_input");
            }
        }
        return new Payment(paymentId, amountCents, memo, limit, (String) key);
    }

    private static BigInteger parseForgiveness(Object forgiveness) {
        if (forgiveness == null) {
            return BigInteger.ZERO;
        }
        if (forgiveness instanceof BigInteger big) {
            if (big.signum() < 0) {
                throw new ServiceError(400, "invalid_input");
            }
            return big;
        }
        if (forgiveness instanceof Byte
                || forgiveness instanceof Short
                || forgiveness instanceof Integer
                || forgiveness instanceof Long) {
            long value = ((Number) forgiveness).longValue();
            if (value < 0) {
                throw new ServiceError(400, "invalid_input");
            }
            return BigInteger.valueOf(value);
        }
        throw new ServiceError(400, "invalid_input");
    }

    private static boolean markerAt(String memo, int at, String marker) {
        if (at + marker.length() > memo.length()) return false;
        for (int j = 0; j < marker.length(); j++) {
            char m = marker.charAt(j), c = memo.charAt(at + j);
            if (m == 'i') { if ("iIİı".indexOf(c) < 0) return false; }
            else if (Character.toLowerCase(c) != m) return false;
        }
        return true;
    }

    public static List<String> markerIds(String memo) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < memo.length(); i++) {
            for (String mk : List.of("paying for:", "paying off:")) {
                if (markerAt(memo, i, mk)) {
                    int start = i + mk.length();
                    int end = memo.indexOf(',', start);
                    ids.add(UnicodeStrings.strip(memo.substring(start, end < 0 ? memo.length() : end)));
                }
            }
        }
        return ids;
    }
}
