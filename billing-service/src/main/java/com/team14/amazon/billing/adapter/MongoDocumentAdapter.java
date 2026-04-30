package com.team14.amazon.billing.adapter;

import com.team14.amazon.billing.dto.LifecycleEventDTO;
import com.team14.amazon.billing.dto.TransactionDetailsDTO;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class MongoDocumentAdapter {
    //Changed by Mohammad Zahran
    // This type should be changed when S5-F11 is implemented to LifecycleEventDTO, TransactionDetailsDTO is currently used as a placeholder
    public LifecycleEventDTO adapt(Document document) {
        if (document == null) {
            return null;
        }

        LocalDateTime timestamp = null;
        Date date = document.getDate("timestamp");
        if (date != null) {
            timestamp = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        //Changed by Mohammad Zahran
        // This return should be changed when S5-F11 is implemented to LifecycleEventDTO, TransactionDetailsDTO is currently used as a placeholder
        return LifecycleEventDTO.builder().transactionId(document.getLong("transactionId")).orderId(document.getLong("orderId")).userId(document.getLong("userId")).originalAmount(document.getDouble("originalAmount")).totalDiscount(document.getDouble("totalDiscount")).finalAmount(document.getDouble("finalAmount")).build();
    }
}
