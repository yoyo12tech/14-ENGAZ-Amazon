package com.team14.amazon.billing.cache;

public final class BillingCacheNames {
    public static final String S5_F1 = "billing-service::S5-F1";
    public static final String S5_F3 = "billing-service::S5-F3";
    public static final String S5_F6 = "billing-service::S5-F6";
    public static final String S5_F8 = "billing-service::S5-F8";
    public static final String S5_F9 = "billing-service::S5-F9";

    public static final String TRANSACTION = "billing-service::transaction";
    public static final String VOUCHER = "billing-service::voucher";
    public static final String TRANSACTION_VOUCHER = "billing-service::transaction-voucher";
    public static final String TRANSACTION_LIFECYCLE = "billing-service::transaction_lifecycle";
    public static final String S5_FEATURE_PATTERN = "billing-service::S5-F*";

    private BillingCacheNames() {
    }

    public static String entityKey(Long id) {
        return "id:" + id;
    }
}

