INSERT INTO subscription_plans (
    plan_id,
    billing_cycle,
    commission_percentage,
    created_at,
    deposit_type,
    description,
    fee_type,
    includes_analytics,
    includes_delivery,
    includes_marketing,
    max_containers,
    min_containers,
    partner_type,
    plan_name,
    plan_status,
    plan_type,
    total_containers,
    updated_at,
    user_type
)
VALUES
-- Restaurant Plans
(1, 'MONTHLY', 5.00, '2026-04-26 05:28:25.952666', 100.00, 'Entry level plan', 199.99, true, false, false, 20, 10, 'RESTAURANT', 'Basic Plan', 'ACTIVE', 'SUBSCRIPTION', 200, '2026-04-26 05:28:25.952666', 'RESTAURANT'),

(2, 'QUARTERLY', 8.50, '2026-04-26 05:28:25.952666', 250.00, 'Growing business plan', 499.99, true, true, false, 60, 30, 'RESTAURANT', 'Standard Plan', 'ACTIVE', 'SUBSCRIPTION', 600, '2026-04-26 05:28:25.952666', 'RESTAURANT'),

(3, 'ANNUALLY', 10.50, '2026-04-26 05:28:25.952666', 500.00, 'Full featured plan', 999.99, true, true, true, 100, 50, 'RESTAURANT', 'Premium Plan', 'ACTIVE', 'SUBSCRIPTION', 1000, '2026-04-26 05:28:25.952666', 'RESTAURANT'),

-- Customer Plans
(4, 'MONTHLY', 0.00, '2026-04-26 06:01:27.347525', 0.00, 'Basic plan for individual users with limited benefits', 49.99, false, false, false, 5, 1, 'CUSTOMER', 'Basic User Plan', 'ACTIVE', 'SUBSCRIPTION', 50, '2026-04-26 06:01:27.347525', 'CUSTOMER'),

(5, 'QUARTERLY', 0.00, '2026-04-26 06:01:27.347525', 0.00, 'Standard plan with moderate benefits and delivery support', 99.99, true, true, false, 15, 5, 'CUSTOMER', 'Standard User Plan', 'ACTIVE', 'SUBSCRIPTION', 150, '2026-04-26 06:01:27.347525', 'CUSTOMER'),

(6, 'ANNUALLY', 0.00, '2026-04-26 06:01:27.347525', 0.00, 'Premium plan with full benefits including delivery and analytics', 199.99, true, true, true, 30, 10, 'CUSTOMER', 'Premium User Plan', 'ACTIVE', 'SUBSCRIPTION', 300, '2026-04-26 06:01:27.347525', 'CUSTOMER');
