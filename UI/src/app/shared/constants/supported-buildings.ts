export const SUPPORTED_BUILDINGS = [
  'Greater Infras Jasmine',
  'Greater Infras Iris',
  "Greater Infra's Gardenia",
  'Greater Infra Bluebells',
  'Greater Infras Honesty',
  'Greater Infras Aspen',
  'Greater Aster',
  'Greater Infra Daffodil',
  'Greater Infra Carnation'
];

/** Case-insensitive substring match both ways — tolerates minor typos/abbreviations */
export function isSupportedBuilding(input: string): boolean {
  if (!input?.trim()) return true; // blank = skip validation
  const normalized = input.trim().toLowerCase();
  return SUPPORTED_BUILDINGS.some(
    b => b.toLowerCase().includes(normalized) || normalized.includes(b.toLowerCase())
  );
}
