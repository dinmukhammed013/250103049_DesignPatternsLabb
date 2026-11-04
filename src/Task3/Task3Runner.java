package Task3;
interface DispatchStrategy {
    boolean shouldDispatch(int severityLevel, int currentHour);
    String getStrategyName();
}

class ImmediateStrategy implements DispatchStrategy {
    @Override
    public boolean shouldDispatch(int severityLevel, int currentHour) {
        return true;
    }

    @Override
    public String getStrategyName() {
        return "IMMEDIATE_DISPATCH";
    }
}

class BusinessHoursStrategy implements DispatchStrategy {
    @Override
    public boolean shouldDispatch(int severityLevel, int currentHour) {
        return severityLevel >= 5
                || (currentHour >= 9 && currentHour <= 17);
    }

    @Override
    public String getStrategyName() {
        return "BUSINESS_HOURS_ONLY (09:00 - 17:00)";
    }
}

interface MessageChannel {
    void transmit(String recipient, String message);
}

class EmailChannel implements MessageChannel {
    @Override
    public void transmit(String recipient, String message) {
        System.out.printf(
                "[EMAIL PROTOCOL -> %s]%n%s%n",
                recipient, message
        );
    }
}

class SmsChannel implements MessageChannel {
    @Override
    public void transmit(String recipient, String message) {
        System.out.printf(
                "[SMS PROTOCOL -> %s] %s%n",
                recipient, message.replace("\n", " -- ")
        );
    }
}

abstract class Alert {
    protected final MessageChannel channel;
    protected final DispatchStrategy strategy;

    public Alert(MessageChannel channel, DispatchStrategy strategy) {
        if (channel == null || strategy == null) {
            throw new IllegalArgumentException(
                    "Dependencies cannot be null");
        }

        this.channel = channel;
        this.strategy = strategy;
    }

    public void processAlert(String recipient,
                             String rawContent,
                             int currentHour) {

        int severity = getSeverityLevel();

        if (strategy.shouldDispatch(severity, currentHour)) {
            channel.transmit(
                    recipient, formatAlertBody(rawContent)
            );
        } else {
            System.out.printf(
                    "[DISPATCH SUPPRESSED] Alert blocked by policy '%s' at hour %02d:00 (Severity: %d)%n",
                    strategy.getStrategyName(),
                    currentHour,
                    severity
            );
        }
    }

    public abstract int getSeverityLevel();
    public abstract String formatAlertBody(String raw);
}

class CriticalSystemAlert extends Alert {
    public CriticalSystemAlert(MessageChannel channel,
                               DispatchStrategy strategy) {
        super(channel, strategy);
    }

    @Override
    public int getSeverityLevel() {
        return 5;
    }

    @Override
    public String formatAlertBody(String raw) {
        return "!!! CRITICAL PRODUCTION ALERT !!!\n"
                + "Incident Report: " + raw + "\n"
                + "ACTION REQUIRED: SRE on-call immediate intervention.";
    }
}

class DailySummaryAlert extends Alert {
    public DailySummaryAlert(MessageChannel channel,
                             DispatchStrategy strategy) {
        super(channel, strategy);
    }

    @Override
    public int getSeverityLevel() {
        return 1;
    }

    @Override
    public String formatAlertBody(String raw) {
        return "Daily Digest:\n" + raw;
    }
}

public class Task3Runner {
    public static void main(String[] args) {
        MessageChannel email = new EmailChannel();
        MessageChannel sms = new SmsChannel();

        DispatchStrategy immediate = new ImmediateStrategy();
        DispatchStrategy businessHours = new BusinessHoursStrategy();

        System.out.println(
                ">>> 1. Critical Alert via SMS (Night time: 02:00, Emergency Bypass) <<<"
        );

        Alert emergency = new CriticalSystemAlert(
                sms, businessHours
        );

        emergency.processAlert(
                "+1555019283",
                "Database primary node kernel panic",
                2
        );

        System.out.println(
                "\n>>> 2. Daily Summary via Email at Night (Hour: 23:00 - Should Suppress) <<<"
        );

        Alert summaryNight = new DailySummaryAlert(
                email, businessHours
        );

        summaryNight.processAlert(
                "dev-team@company.internal",
                "99.98% uptime achieved today.",
                23
        );

        System.out.println(
                "\n>>> 3. Daily Summary via Email during Workday (Hour: 11:00 - Should Deliver) <<<"
        );

        Alert summaryDay = new DailySummaryAlert(
                email, businessHours
        );

        summaryDay.processAlert(
                "dev-team@company.internal",
                "99.98% uptime achieved today.",
                11
        );
    }
}
