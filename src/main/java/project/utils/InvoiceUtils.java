package project.utils;

import project.ServiceError;
import project.UnicodeStrings;
import project.dto.Invoice;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InvoiceUtils {

    private static final DateTimeFormatter DUE_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static List<Invoice> parseInvoiceBatch(Object invoices) {
        if (!(invoices instanceof List<?> rows) || rows.isEmpty()) {
            throw new ServiceError(400, "invalid_input");
        }
        List<Invoice> batch = new ArrayList<>(rows.size());
        Set<String> seen = new HashSet<>();

        for (Object row : new ArrayList<>(rows)) {
            Invoice inv = parseInvoice(row);
            if(!seen.add(inv.invoiceId())) {
                throw new ServiceError(409, "duplicate_invoice");
            }
            batch.add(inv);
        }
        return batch;
    }

    private static Invoice parseInvoice(Object row) {
        if (!(row instanceof String text)) {
            throw new ServiceError(400, "invalid_input");
        }
        String[] parts = text.split(", ", -1);
        if (parts.length != 3) {
            throw new ServiceError(400, "invalid_input");
        }
        String invoiceId = UnicodeStrings.strip(parts[0]);
        String dueDate = UnicodeStrings.strip(parts[1]);
        String amount = UnicodeStrings.strip(parts[2]);
        int idLength = invoiceId.codePointCount(0, invoiceId.length());

        if (idLength < 1 || idLength > 64) {
            throw new ServiceError(400, "invalid_input");
        }
        if (!dueDate.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new ServiceError(400, "invalid_input");
        }
        try {
            LocalDate.parse(dueDate, DUE_DATE_FORMAT);
        } catch (DateTimeParseException invalidDate) {
            throw new ServiceError(400, "invalid_input");
        }
        if (!amount.matches("[0-9]+")) {
            throw new ServiceError(400, "invalid_input");
        }
        BigInteger amountCents = new BigInteger(amount);
        if (amountCents.signum() <= 0) {
            throw new ServiceError(400, "invalid_input");
        }
        return new Invoice(invoiceId, dueDate, amountCents);
    }
}
