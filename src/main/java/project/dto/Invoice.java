package project.dto;

import java.math.BigInteger;

public final class Invoice {
    private final String invoiceId;
    private final String dueDate;
    private final BigInteger amountCents;
    private String status = "UNPAID";
    private String settledByPaymentId = null;

    public Invoice(String invoiceId, String dueDate, BigInteger amountCents) {
        this.invoiceId = invoiceId;
        this.dueDate = dueDate;
        this.amountCents = amountCents;
    }

    public String invoiceId() {
        return invoiceId;
    }

    public String dueDate() {
        return dueDate;
    }

    public BigInteger amountCents() {
        return amountCents;
    }

    public String status() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String settledByPaymentId() {
        return settledByPaymentId;
    }

    public void setSettledByPaymentId(String settledByPaymentId) {
        this.settledByPaymentId = settledByPaymentId;
    }
}
