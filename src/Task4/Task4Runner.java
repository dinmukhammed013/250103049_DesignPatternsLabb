package Task4;
class LegacyForeignOrder {
    private final String orderId;
    private final double gbpAmount;

    public LegacyForeignOrder(String orderId, double gbpAmount) {
        this.orderId = orderId;
        this.gbpAmount = gbpAmount;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getGbpAmount() {
        return gbpAmount;
    }
}

class LegacyCurrencyConverter {
    public double convert(double amount, String from, String to) {
        if ("GBP".equalsIgnoreCase(from)
                && "USD".equalsIgnoreCase(to)) {
            return amount * 1.25;
        }

        return amount;
    }
}

interface PricingService {
    double calculateTotal();
    String getReceiptBreakdown();
}

class CurrencyOrderAdapter implements PricingService {
    private final LegacyForeignOrder foreignOrder;
    private final LegacyCurrencyConverter converter;
    private final String targetCurrency;

    public CurrencyOrderAdapter(
            LegacyForeignOrder order,
            LegacyCurrencyConverter converter,
            String targetCurrency) {

        if (order == null || converter == null
                || targetCurrency == null) {
            throw new IllegalArgumentException(
                    "Dependencies cannot be null");
        }

        this.foreignOrder = order;
        this.converter = converter;
        this.targetCurrency = targetCurrency;
    }

    @Override
    public double calculateTotal() {
        return converter.convert(
                foreignOrder.getGbpAmount(),
                "GBP",
                targetCurrency
        );
    }

    @Override
    public String getReceiptBreakdown() {
        return String.format(
                java.util.Locale.US,
                "Order [%s] Base Item: $%.2f %s (Converted from %.2f GBP)",
                foreignOrder.getOrderId(),
                calculateTotal(),
                targetCurrency,
                foreignOrder.getGbpAmount()
        );
    }
}

abstract class PricingDecorator implements PricingService {
    protected final PricingService wrappedService;

    public PricingDecorator(PricingService wrappedService) {
        if (wrappedService == null) {
            throw new IllegalArgumentException(
                    "Wrapped service cannot be null");
        }

        this.wrappedService = wrappedService;
    }

    @Override
    public double calculateTotal() {
        return wrappedService.calculateTotal();
    }

    @Override
    public String getReceiptBreakdown() {
        return wrappedService.getReceiptBreakdown();
    }
}

class GiftWrapDecorator extends PricingDecorator {
    public GiftWrapDecorator(PricingService wrapped) {
        super(wrapped);
    }

    @Override
    public double calculateTotal() {
        return super.calculateTotal() + 5.00;
    }

    @Override
    public String getReceiptBreakdown() {
        return super.getReceiptBreakdown()
                + " + [Gift Wrap: $5.00]";
    }
}

class ExpressCourierDecorator extends PricingDecorator {
    public ExpressCourierDecorator(PricingService wrapped) {
        super(wrapped);
    }

    @Override
    public double calculateTotal() {
        return super.calculateTotal() + 15.00;
    }

    @Override
    public String getReceiptBreakdown() {
        return super.getReceiptBreakdown()
                + " + [Express Courier: $15.00]";
    }
}

class CustomsDutyDecorator extends PricingDecorator {
    public CustomsDutyDecorator(PricingService wrapped) {
        super(wrapped);
    }

    @Override
    public double calculateTotal() {
        return Math.round(
                super.calculateTotal() * 1.085 * 100.0
        ) / 100.0;
    }

    @Override
    public String getReceiptBreakdown() {
        return super.getReceiptBreakdown()
                + " + [Customs Duty: 8.5%]";
    }
}

public class Task4Runner {
    public static void main(String[] args) {
        LegacyForeignOrder order =
                new LegacyForeignOrder("ORD-9821", 100.00);

        LegacyCurrencyConverter converter =
                new LegacyCurrencyConverter();

        PricingService baseOrder = new CurrencyOrderAdapter(
                order, converter, "USD"
        );

        System.out.printf(
                "Base Converted: $%.2f%n",
                baseOrder.calculateTotal()
        );

        PricingService completeCheckout = new CustomsDutyDecorator(
                new ExpressCourierDecorator(
                        new GiftWrapDecorator(baseOrder)
                )
        );

        System.out.println("\n>>> INVOICE BREAKDOWN <<<");

        System.out.println(
                completeCheckout.getReceiptBreakdown()
        );

        System.out.printf(
                "FINAL CHARGE TOTAL: $%.2f%n",
                completeCheckout.calculateTotal()
        );
    }
}
