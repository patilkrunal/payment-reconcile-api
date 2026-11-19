package project;

import project.dto.Invoice;
import project.dto.Payment;
import project.utils.InvoiceUtils;
import project.utils.PaymentUtils;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;

public class ReconciliationService {

    private final Object lock = new Object();
    private final Map<String, Invoice> ledger = new LinkedHashMap<>();

    private record Cached(String canonical, BigInteger forgiveness, Map<String, Object> body){}
    private final Map<String, Cached> idem = new HashMap<>();

    public ReconciliationService() {}

    public Response registerInvoices(Object invoices) {
        List<Invoice> batch = InvoiceUtils.parseInvoiceBatch(invoices);

        synchronized (lock) {
            for (Invoice invoice : batch) {
                if(ledger.containsKey(invoice.invoiceId())){
                    throw new ServiceError(409, "duplicate_invoice");
                }
            }
            for(Invoice invoice : batch){
                ledger.put(invoice.invoiceId(), invoice);
            }
        }
        return new Response(201, Values.row("registered_count", batch.size()));
    }

    public Response reconcilePayment(Object payment) {
        return reconcilePayment(payment, 0, null);
    }

    public Response reconcilePayment(Object payment, Object forgiveness, Object key) {
        Payment p = PaymentUtils.parsePayment(payment, forgiveness, key);
        String canonical = UnicodeStrings.strip((String) payment);
        synchronized (lock) {
            return apply(p, canonical, null);
        }
    }

    public Response listInvoices() {
        List<Invoice> snapshot;
        List<Object> rows;

        synchronized (lock) {
            snapshot = new ArrayList<>(ledger.values());
            snapshot.sort(Comparator.comparing((Invoice invoice) -> LocalDate.parse(invoice.dueDate()))
                        .thenComparing(Invoice::invoiceId, UnicodeStrings::compare));

            rows = new ArrayList<>(snapshot.size());
            for (Invoice invoice : snapshot) {
                rows.add(
                        Values.row(
                                "invoice_id", invoice.invoiceId(),
                                "due_date", invoice.dueDate(),
                                "amount_cents", invoice.amountCents(),
                                "status", invoice.status(),
                                "settled_by_payment_id", invoice.settledByPaymentId()));
            }
        }

        return new Response(200, rows);
    }

    public Response reconcileBatch(Object payments) {
        if(!(payments instanceof List<?> rows) || rows.isEmpty()) {
            throw new ServiceError(400, "invalid_input");
        }

        List<?> snapshot = new ArrayList<>(rows);
        synchronized (lock) {
            List<Runnable> undo =  new ArrayList<>();
            List<Map<String, Object>> results =  new ArrayList<>();

            try {
                for(Object row : snapshot) {
                    if(!(row instanceof Map<?, ?> m) ||
                            !m.containsKey("payment") ||
                            !new HashSet<Object>(Set.of("payment", "forgiveness", "key")).containsAll(m.keySet()))
                    {
                        throw new ServiceError(400, "invalid_input");
                    }

                    Object f = m.containsKey("forgiveness") ? m.get("forgiveness") : 0;
                    Payment p = PaymentUtils.parsePayment(m.get("payment"), f, m.get("key"));
                    Response r = apply(p, UnicodeStrings.strip((String) m.get("payment")), undo);
                    results.add(Values.row("status", r.status(), "body", r.body()));
                }
            } catch (RuntimeException e){
                for(int i = undo.size()-1; i >= 0; i--) {
                    undo.get(i).run();
                }
                throw e;
            }
            return new Response(200, Values.row("results", results));
        }
    }

    private Map<String, Object> settle(Payment p, List<Runnable> undo) {
        Invoice hit = null; String tier = null;

        for (String id : p.memo()) {
            Invoice inv = ledger.get(id);
            if (inv != null && inv.status().equals("UNPAID")) { hit = inv; tier = "id"; break; }
        }

        if (hit == null) { hit = best(p, BigInteger.ZERO); if (hit != null) tier = "exact"; }

        if (hit == null && p.forgiveness().signum() > 0) { hit = best(p, p.forgiveness()); if (hit != null) tier = "fuzzy"; }

        if (hit == null) return Values.row("status","UNMATCHED","payment_id",p.paymentId(),
                "settled_invoice_id",null,"tier_used",null,
                "message","Payment "+p.paymentId()+" could not be matched to any invoice");

        hit.setStatus("PAID");
        hit.setSettledByPaymentId(p.paymentId());

        if(undo != null) {
            Invoice h = hit;
            undo.add(() -> {
                h.setStatus("UNPAID");
                h.setSettledByPaymentId(null);
            });
        }

        return Values.row("status","MATCHED","payment_id",p.paymentId(),
                "settled_invoice_id",hit.invoiceId(),"tier_used",tier,
                "message","Payment "+p.paymentId()+" paid "+p.amountCents()+" for invoice "
                        +hit.invoiceId()+" due on "+hit.dueDate());
    }

    private Response apply(Payment p, String canonical, List<Runnable> undo) {
        if(p.key() != null){
            Cached c = idem.get(p.key());
            if(c != null){
                if(!c.canonical().equals(canonical) || c.forgiveness().compareTo(p.forgiveness()) != 0){
                    throw new ServiceError(409, "idempotency_conflict");
                }

                return new Response(200, Values.copy(c.body()));
            }
        }
        Map<String, Object> body = settle(p, undo);
        int status = "MATCHED".equals(body.get("status")) ? 201 : 200;
        if(p.key() != null){
            String k = p.key();
            idem.put(k, new Cached(canonical, p.forgiveness(), Values.copy(body)));
            if(undo != null) {
                undo.add(() -> idem.remove(k));
            }
        }
        return new Response(status, Values.copy(body));
    }

    private Invoice best(Payment p, BigInteger tol) {
        return ledger.values().stream()
                .filter(i -> i.status().equals("UNPAID"))
                .filter(i -> i.amountCents().subtract(p.amountCents()).abs().compareTo(tol) <= 0)
                .min(Comparator.comparing((Invoice i) -> i.dueDate())                 // YYYY-MM-DD sorts as text
                        .thenComparing(i -> i.amountCents().subtract(p.amountCents()).abs())
                        .thenComparing(Invoice::invoiceId, UnicodeStrings::compare))
                .orElse(null);
    }
}
