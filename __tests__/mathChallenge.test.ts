import {
  generateQuestion,
  parseProgress,
  toDifficulty,
} from '../src/features/challenges/mathChallenge';

/** Deterministic pseudo-random generator for repeatable tests. */
function seeded(seed: number) {
  let s = seed;
  return () => {
    s = (s * 1664525 + 1013904223) % 4294967296;
    return s / 4294967296;
  };
}

function evaluate(prompt: string): number {
  // Prompts use typographic operators; evaluate them safely without eval.
  const tokens = prompt.split(' ');
  let value = Number(tokens[0]);
  let i = 1;
  while (i < tokens.length) {
    const op = tokens[i];
    let next = Number(tokens[i + 1]);
    i += 2;
    if (op === '×') {
      value *= next;
      continue;
    }
    // Apply any following multiplication before adding/subtracting.
    while (tokens[i] === '×') {
      next *= Number(tokens[i + 1]);
      i += 2;
    }
    value = op === '+' ? value + next : value - next;
  }
  return value;
}

describe.each(['easy', 'medium', 'hard'] as const)(
  '%s questions',
  difficulty => {
    const rng = seeded(42);
    const questions = Array.from({ length: 300 }, () =>
      generateQuestion(difficulty, rng),
    );

    it('have a correct answer that matches the prompt', () => {
      for (const q of questions) expect(evaluate(q.prompt)).toBe(q.answer);
    });

    it('offer four distinct non-negative options including the answer', () => {
      for (const q of questions) {
        expect(q.options).toHaveLength(4);
        expect(new Set(q.options).size).toBe(4);
        expect(q.options).toContain(q.answer);
        for (const o of q.options) expect(o).toBeGreaterThanOrEqual(0);
      }
    });

    it('place the answer in different positions', () => {
      const positions = new Set(
        questions.map(q => q.options.indexOf(q.answer)),
      );
      expect(positions.size).toBe(4);
    });
  },
);

describe('challenge progress', () => {
  const progress = {
    sessionStartedAt: 123,
    solved: 1,
    current: generateQuestion('easy', seeded(1)),
  };

  it('restores progress for the same session', () => {
    expect(parseProgress(JSON.stringify(progress), 123)).toEqual(progress);
  });
  it('ignores progress from another session or corrupt data', () => {
    expect(parseProgress(JSON.stringify(progress), 999)).toBeNull();
    expect(parseProgress('{oops', 123)).toBeNull();
    expect(parseProgress(null, 123)).toBeNull();
  });
  it('defaults unknown difficulty to easy', () => {
    expect(toDifficulty('extreme')).toBe('easy');
    expect(toDifficulty('hard')).toBe('hard');
  });
});
