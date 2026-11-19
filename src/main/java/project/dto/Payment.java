package project.dto;

import java.math.BigInteger;
import java.util.List;

public final class Payment {
    private final String paymentId;
    private final BigInteger amountCents;
    private final List<String> memo;
    private final BigInteger forgiveness;
    private final String key;

    public Payment(
            String paymentId,
            BigInteger amountCents,
            List<String> memo,
            BigInteger forgiveness,
            String key) {
        this.paymentId = paymentId;
        this.amountCents = amountCents;
        this.memo = memo;
        this.forgiveness = forgiveness;
        this.key = key;
    }

    public String paymentId() {
        return paymentId;
    }

    public BigInteger amountCents() {
        return amountCents;
    }

    public List<String> memo() {
        return memo;
    }

    public BigInteger forgiveness() {
        return forgiveness;
    }

    public String key() {
        return key;
    }
}
