package com.bankplatform.reporting.application;

import com.bankplatform.reporting.domain.AccountActivityEntry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CsvStatementRenderer {

    private static final String HEADER = "eventType,amount,currency,occurredAt\n";

    public byte[] render(List<AccountActivityEntry> entries) {
        StringBuilder csv = new StringBuilder(HEADER);
        for (AccountActivityEntry entry : entries) {
            csv.append(escape(entry.eventType()))
                    .append(',')
                    .append(entry.amount())
                    .append(',')
                    .append(escape(entry.currency()))
                    .append(',')
                    .append(entry.occurredAt())
                    .append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.contains(",") ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}
