import React from 'react';
import renderer, { act } from 'react-test-renderer';
import NativeRcbWake from '../src/native/NativeRcbWake';
import { useElapsedNow } from '../src/hooks/useElapsedNow';

const fake = NativeRcbWake as unknown as {
  __advance: (ms: number) => void;
  elapsedRealtime: () => number;
};

jest.useFakeTimers();

describe('useElapsedNow', () => {
  it('never returns a stale clock when a countdown starts', () => {
    const seen: number[] = [];
    function Probe({ active }: { active: boolean }) {
      seen.push(useElapsedNow(active));
      return null;
    }
    let tree: renderer.ReactTestRenderer;
    act(() => {
      tree = renderer.create(<Probe active={false} />);
    });
    // Time passes while the screen sits idle (the old code cached the mount-time value).
    fake.__advance(3_000);
    act(() => tree.update(<Probe active />));
    expect(seen[seen.length - 1]).toBe(fake.elapsedRealtime());
    act(() => tree.unmount()); // stops the interval
  });
});
