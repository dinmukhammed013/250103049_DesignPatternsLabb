package Task1;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

interface ReportFormatter {
    String format(String title, String[] headers, String[][] rows);
}

class JsonFormatter implements ReportFormatter {
    private String escape(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public String format(String title, String[] headers, String[][] rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"report\": \"")
                .append(escape(title))
                .append("\",\n  \"records\": [\n");

        for (int i = 0; i < rows.length; i++) {
            sb.append("    {");
            for (int j = 0; j < headers.length; j++) {
                if (j > 0) sb.append(", ");
                sb.append("\"").append(escape(headers[j]))
                        .append("\": \"")
                        .append(escape(rows[i][j]))
                        .append("\"");
            }
            sb.append("}")
                    .append(i + 1 < rows.length ? ",\n" : "\n");
        }

        return sb.append("  ]\n}").toString();
    }
}

class CsvFormatter implements ReportFormatter {
    private String quote(String text) {
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String format(String title, String[] headers, String[][] rows) {
        StringBuilder sb = new StringBuilder("# TITLE: ")
                .append(title).append("\n");

        for (int j = 0; j < headers.length; j++) {
            if (j > 0) sb.append(',');
            sb.append(quote(headers[j]));
        }
        sb.append('\n');

        for (String[] row : rows) {
            for (int j = 0; j < row.length; j++) {
                if (j > 0) sb.append(',');
                sb.append(quote(row[j]));
            }
            sb.append('\n');
        }

        return sb.toString();
    }
}

interface Exporter {
    void export(String destination, String data);
}

class BaseFileExporter implements Exporter {
    @Override
    public void export(String destination, String data) {
        System.out.printf(
                "[DISK WRITE] Writing %d bytes to file://%s%n",
                data.getBytes(StandardCharsets.UTF_8).length,
                destination
        );
        System.out.println("----- FILE PAYLOAD BEGIN -----");
        System.out.println(data);
        System.out.println("----- FILE PAYLOAD END -----");
    }
}

abstract class ExporterDecorator implements Exporter {
    protected final Exporter wrappedExporter;

    public ExporterDecorator(Exporter wrappedExporter) {
        if (wrappedExporter == null) {
            throw new IllegalArgumentException(
                    "Wrapped exporter cannot be null");
        }
        this.wrappedExporter = wrappedExporter;
    }

    @Override
    public void export(String destination, String data) {
        wrappedExporter.export(destination, data);
    }
}

class HeaderFooterDecorator extends ExporterDecorator {
    private final String timestamp;

    public HeaderFooterDecorator(Exporter wrappedExporter,
                                 String timestamp) {
        super(wrappedExporter);
        this.timestamp = timestamp;
    }

    @Override
    public void export(String destination, String data) {
        String enrichedData =
                "=== METADATA HEADER: " + timestamp + " ===\n"
                        + data + "\n"
                        + "=== INTEGRITY CHECK: SHA-256 OK | END RECORD ===";

        super.export(destination, enrichedData);
    }
}

class Base64EncryptDecorator extends ExporterDecorator {
    public Base64EncryptDecorator(Exporter wrappedExporter) {
        super(wrappedExporter);
    }

    @Override
    public void export(String destination, String data) {
        String encoded = Base64.getEncoder().encodeToString(
                data.getBytes(StandardCharsets.UTF_8)
        );

        super.export(destination,
                "[BASE64_CIPHERTEXT:" + encoded + "]");
    }
}

public class Task1Runner {
    public static void main(String[] args) {
        String[] headers = {"ID", "User", "Role"};

        String[][] rows = {
                {"101", "sufyan", "Admin"},
                {"102", "alex", "Dev"}
        };

        ReportFormatter jsonStrategy = new JsonFormatter();

        String rawJson = jsonStrategy.format(
                "UserAuditReport", headers, rows
        );

        System.out.println(
                ">>> TESTING DECORATOR STACK: Base64(HeaderFooter(BaseFileExporter)) <<<"
        );

        Exporter securePipeline = new Base64EncryptDecorator(
                new HeaderFooterDecorator(
                        new BaseFileExporter(),
                        "2026-11-02 10:00:00 UTC"
                )
        );

        securePipeline.export("/var/logs/audit.sec", rawJson);
    }
}
