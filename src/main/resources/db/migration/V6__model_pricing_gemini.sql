-- Gemini model pricing (per 1 000 tokens, USD)
-- gemini-1.5-flash: $0.000075 input / $0.000300 output
-- gemini-1.5-pro:   $0.001250 input / $0.005000 output
INSERT INTO model_pricing (provider, model_name, input_cost_per_1k, output_cost_per_1k)
VALUES ('GEMINI', 'gemini-1.5-flash', 0.000075, 0.000300),
       ('GEMINI', 'gemini-1.5-pro',   0.001250, 0.005000)
ON CONFLICT (provider, model_name) DO NOTHING;
