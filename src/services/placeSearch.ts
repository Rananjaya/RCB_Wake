/**
 * Online place search for the widget-weather location, so villages and suburbs that are not in the
 * bundled city list (for example Kosgama) can be chosen. Uses Open-Meteo's free geocoding API
 * (GeoNames data, no key). Only the typed search text is sent, and only while the user searches.
 */
export type Place = {
  name: string;
  /** Region and country, e.g. "Western Province, Sri Lanka". */
  detail: string;
  lat: number;
  lon: number;
};

type ApiResult = {
  name?: string;
  latitude?: number;
  longitude?: number;
  country?: string;
  admin1?: string;
  admin2?: string;
};

export function parsePlaces(body: { results?: ApiResult[] } | null): Place[] {
  return (body?.results ?? [])
    .filter(
      r =>
        typeof r.name === 'string' &&
        typeof r.latitude === 'number' &&
        typeof r.longitude === 'number',
    )
    .map(r => ({
      name: r.name as string,
      detail: [r.admin2, r.admin1, r.country].filter(Boolean).join(', '),
      lat: Math.round((r.latitude as number) * 100) / 100,
      lon: Math.round((r.longitude as number) * 100) / 100,
    }));
}

export async function searchPlaces(
  query: string,
  language: string,
  signal?: AbortSignal,
): Promise<Place[]> {
  const q = query.trim();
  if (q.length < 2) return [];
  const url =
    'https://geocoding-api.open-meteo.com/v1/search?count=15&format=json' +
    `&language=${encodeURIComponent(language)}&name=${encodeURIComponent(q)}`;
  const response = await fetch(url, { signal });
  if (!response.ok) return [];
  return parsePlaces(await response.json());
}
