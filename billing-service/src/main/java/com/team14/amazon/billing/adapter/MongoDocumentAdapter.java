package com.team14.amazon.billing.adapter;

import com.team14.amazon.billing.dto.LifecycleEventDTO;
import com.team14.amazon.billing.model.Source;
import com.team14.amazon.billing.model.mongodb.TransactionAuditEvent;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;


public class MongoDocumentAdapter {

     //This type should be changed when S5-F11 is implemented to LifecycleEventDTO, TransactionDetailsDTO is currently used as a placeholder
    public LifecycleEventDTO adapt(Document document, Source source) {
        if (document == null) {
            return null;
        }

        LocalDateTime timestamp = null;
        Date date = document.getDate("timestamp");
        if (date != null) {
            timestamp = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }

        Map<String, Object> details = null;
        Document detailsDoc = document.get("details", Document.class);
        if(detailsDoc != null){
            details = new HashMap<>(detailsDoc);
        }
        // This return should be changed when S5-F11 is implemented to LifecycleEventDTO, TransactionDetailsDTO is currently used as a placeholder
        return LifecycleEventDTO.builder()
                .timeStamp(timestamp)
                .source(source)
                .action(document.getString("action"))
                .details(details)
                .build();
    }

    public LifecycleEventDTO adapt(TransactionAuditEvent event, Source source) {
        return LifecycleEventDTO.builder()
                .timeStamp(event.getTimestamp())
                .source(source)
                .action(event.getAction())
                .details(event.getDetails())
                .build();
    }
}
