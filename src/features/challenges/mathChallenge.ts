export type Difficulty = 'easy' | 'medium' | 'hard';

export type Question = {
  prompt: string;
  answer: number;
  /** Four distinct choices, one of which is [answer], in random order. */
  options: number[];
};

export type Rng = () => number;

const int = (rng: Rng, min: number, max: number) => Math.floor(rng() * (max - min + 1)) + min;

function makePrompt(difficulty: Difficulty, rng: Rng): {prompt: string; answer: number} {
  if (difficulty === 'easy') {
    const a = int(rng, 2, 20);
    const b = int(rng, 2, 20);
    if (rng() < 0.5) return {prompt: `${a} + ${b}`, answer: a + b};
    const [hi, lo] = a >= b ? [a, b] : [b, a];
    return {prompt: `${hi} − ${lo}`, answer: hi - lo};
  }
  if (difficulty === 'medium') {
    const kind = int(rng, 0, 2);
    if (kind === 0) {
      const a = int(rng, 12, 89);
      const b = int(rng, 12, 89);
      return {prompt: `${a} + ${b}`, answer: a + b};
    }
    if (kind === 1) {
      const a = int(rng, 40, 99);
      const b = int(rng, 11, a - 1);
      return {prompt: `${a} − ${b}`, answer: a - b};
    }
    const a = int(rng, 3, 12);
    const b = int(rng, 3, 9);
    return {prompt: `${a} × ${b}`, answer: a * b};
  }
  const a = int(rng, 12, 29);
  const b = int(rng, 3, 9);
  const c = int(rng, 5, 60);
  if (rng() < 0.5) return {prompt: `${a} × ${b} + ${c}`, answer: a * b + c};
  return {prompt: `${a} × ${b} − ${c}`, answer: a * b - c};
}

/** Plausible wrong answers near the correct one (off-by-one, off-by-ten, sign slips). */
function distractors(answer: number, rng: Rng): number[] {
  const offsets = [1, 2, 3, 10, 9, 11, 5, 4];
  const result = new Set<number>();
  let guard = 0;
  while (result.size < 3 && guard++ < 100) {
    const offset = offsets[int(rng, 0, offsets.length - 1)] * (rng() < 0.5 ? -1 : 1);
    const candidate = answer + offset;
    if (candidate >= 0 && candidate !== answer) result.add(candidate);
  }
  let filler = answer + 1;
  while (result.size < 3) {
    if (filler !== answer) result.add(filler);
    filler++;
  }
  return [...result];
}

function shuffle<T>(items: T[], rng: Rng): T[] {
  const a = [...items];
  for (let i = a.length - 1; i > 0; i--) {
    const j = int(rng, 0, i);
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

export function generateQuestion(difficulty: Difficulty, rng: Rng = Math.random): Question {
  const {prompt, answer} = makePrompt(difficulty, rng);
  return {prompt, answer, options: shuffle([answer, ...distractors(answer, rng)], rng)};
}

export function toDifficulty(value: string): Difficulty {
  return value === 'medium' || value === 'hard' ? value : 'easy';
}

/** Challenge progress persisted natively so it survives the app being killed mid-challenge. */
export type ChallengeProgress = {
  sessionStartedAt: number;
  solved: number;
  current: Question;
};

export function parseProgress(raw: string | null, sessionStartedAt: number): ChallengeProgress | null {
  if (!raw) return null;
  try {
    const p = JSON.parse(raw) as ChallengeProgress;
    if (p.sessionStartedAt !== sessionStartedAt || !Array.isArray(p.current?.options)) return null;
    return p;
  } catch {
    return null;
  }
}
