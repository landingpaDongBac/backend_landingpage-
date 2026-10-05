INSERT INTO roles (name) VALUES
    ('ADMIN'),
    ('EDITOR')
ON CONFLICT (name) DO NOTHING;

INSERT INTO sections (id, section_key, name, display_order, is_enabled, created_at, updated_at) VALUES
    ('10000000-0000-0000-0000-000000000002', '02', 'Product Composition', 2, TRUE, NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000003', '03', 'Product Image Gallery', 3, TRUE, NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000004', '04', 'Product Authenticity', 4, FALSE, NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000005', '05', 'Product Benefits', 5, TRUE, NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000006', '06', 'Promotion / Consultation Content', 6, TRUE, NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000007', '07', 'Crop Applications', 7, TRUE, NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000008', '08', 'Why Choose Us / Commitments', 8, TRUE, NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000009', '09', 'Usage Instructions', 9, FALSE, NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000010', '10', 'Customer Feedback', 10, TRUE, NOW(), NOW())
ON CONFLICT (section_key) DO UPDATE SET
    name = EXCLUDED.name,
    display_order = EXCLUDED.display_order,
    updated_at = NOW();
