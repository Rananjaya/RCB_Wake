import { parsePlaces } from '../src/services/placeSearch';

describe('parsePlaces', () => {
  it('maps results and rounds coordinates to about 1 km', () => {
    expect(
      parsePlaces({
        results: [
          {
            name: 'Kosgama',
            latitude: 6.93333,
            longitude: 80.15,
            country: 'Sri Lanka',
            admin1: 'Western',
            admin2: 'Colombo District',
          },
          { name: 'Broken' },
        ],
      }),
    ).toEqual([
      {
        name: 'Kosgama',
        detail: 'Colombo District, Western, Sri Lanka',
        lat: 6.93,
        lon: 80.15,
      },
    ]);
  });

  it('handles empty responses', () => {
    expect(parsePlaces(null)).toEqual([]);
    expect(parsePlaces({})).toEqual([]);
  });
});
