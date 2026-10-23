TRUNCATE TABLE orders, customers RESTART IDENTITY CASCADE;

INSERT INTO customers (name, email, phone) VALUES
    ('Rossi Forniture S.r.l.', 'acquisti@rossi.example', '+39 02 1234567'),
    ('Franchino S.r.l.', 'contatti@franchino.example', NULL),
    ('Bianchino S.r.l.', 'contatti@bianchino.example', '+39 0123456789'),
    ('Giovannino S.p.a.', 'contatti@giovanninospa.example', '+39 9876543210'),
    ('TomMem S.p.a.', 'contatti@tommasogiusto.example', '+39 9876543210');
	
INSERT INTO orders (customer_id, status, order_date, promised_delivery_date, delivered_at, total_amount)
SELECT c.id, o.status, o.order_date, o.promised_delivery_date, o.delivered_at, o.total_amount
FROM (VALUES
        -- email cliente,                  stato,       data ordine,       consegna promessa,  consegnato il,                       importo
    -- LATE: aperti, consegna promessa nel passato
    ('acquisti@rossi.example',         'SHIPPED',   CURRENT_DATE - 20, CURRENT_DATE - 6,  NULL::timestamptz,                   1250.00),
    ('acquisti@rossi.example',         'CONFIRMED', CURRENT_DATE - 15, CURRENT_DATE - 2,  NULL,                                 310.50),
    ('contatti@franchino.example',     'PENDING',   CURRENT_DATE - 12, CURRENT_DATE - 1,  NULL,                                4890.00),
    ('contatti@giovanninospa.example', 'SHIPPED',   CURRENT_DATE - 30, CURRENT_DATE - 10, NULL,                                2100.00),
    -- DUE TODAY: non in ritardo (lo sarà da domani)
    ('contatti@bianchino.example',     'SHIPPED',   CURRENT_DATE - 7,  CURRENT_DATE,      NULL,                                 799.99),
    -- FUTURE: aperti, consegna promessa nel futuro
    ('contatti@tommasogiusto.example', 'CONFIRMED', CURRENT_DATE - 2,  CURRENT_DATE + 5,  NULL,                                 560.00),
    ('contatti@franchino.example',     'PENDING',   CURRENT_DATE,      CURRENT_DATE + 14, NULL,                               12000.00),
    -- DELIVERED: chiusi, anche se la data promessa è passata
    ('contatti@bianchino.example',     'DELIVERED', CURRENT_DATE - 25, CURRENT_DATE - 15, (CURRENT_DATE - 16) + TIME '10:30',   450.00),
    ('contatti@tommasogiusto.example', 'DELIVERED', CURRENT_DATE - 20, CURRENT_DATE - 10, (CURRENT_DATE - 7)  + TIME '16:00',  1730.25),
    -- CANCELLED: chiuso, anche se la data promessa è passata
    ('contatti@giovanninospa.example', 'CANCELLED', CURRENT_DATE - 18, CURRENT_DATE - 8,  NULL,                                 300.00)
) AS o(customer_email, status, order_date, promised_delivery_date, delivered_at, total_amount)
JOIN customers c ON c.email = o.customer_email;	