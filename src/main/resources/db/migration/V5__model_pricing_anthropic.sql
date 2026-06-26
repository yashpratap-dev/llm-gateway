-- Anthropic Claude model pricing (per 1k tokens)
-- Source: https://www.anthropic.com/pricing (as of 2024)
INSERT INTO model_pricing (provider, model_name, input_cost_per_1k, output_cost_per_1k)
VALUES
  ('CLAUDE', 'claude-3-5-sonnet-20241022', 0.003000, 0.015000),
  ('CLAUDE', 'claude-3-haiku-20240307',    0.000250, 0.001250)
ON CONFLICT (provider, model_name) DO NOTHING;
