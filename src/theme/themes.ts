export type ThemeName = 'midnight' | 'pink';

export type Theme = {
  name: ThemeName;
  dark: boolean;
  colors: {
    background: string;
    surface: string;
    elevated: string;
    primary: string;
    secondary: string;
    text: string;
    textSecondary: string;
    divider: string;
    selected: string;
    onPrimary: string;
    onSelected: string;
    danger: string;
    success: string;
    warning: string;
  };
  /** Decorative floral accents are only drawn in Pink Dream. */
  floral: boolean;
};

export const midnightPurple: Theme = {
  name: 'midnight',
  dark: true,
  colors: {
    background: '#100D1C',
    surface: '#1D1830',
    elevated: '#28213B',
    primary: '#A855F7',
    secondary: '#C4A7FF',
    text: '#F8F5FF',
    textSecondary: '#AAA1BB',
    divider: '#393149',
    selected: '#8538E8',
    onPrimary: '#FFFFFF',
    onSelected: '#FFFFFF',
    danger: '#FF6B8B',
    success: '#5FD3A5',
    warning: '#FF9F7A',
  },
  floral: false,
};

export const pinkDream: Theme = {
  name: 'pink',
  dark: false,
  colors: {
    background: '#FFF6F1',
    surface: '#FFFFFF',
    elevated: '#FBE3EA',
    primary: '#C2577F',
    secondary: '#9C83D6',
    text: '#3D2B3F',
    textSecondary: '#7D6479',
    divider: '#F0D9E1',
    selected: '#E7DAF8',
    onPrimary: '#FFFFFF',
    onSelected: '#3D2B3F',
    danger: '#C23B5A',
    success: '#2E8A66',
    warning: '#B5562F',
  },
  floral: true,
};

export const themes: Record<ThemeName, Theme> = {
  midnight: midnightPurple,
  pink: pinkDream,
};

export const spacing = { xs: 4, sm: 8, md: 12, lg: 16, xl: 24, xxl: 32 };
export const radius = { sm: 10, md: 16, lg: 24, pill: 999 };

export type AccentName =
  | 'default'
  | 'purple'
  | 'blue'
  | 'teal'
  | 'green'
  | 'orange'
  | 'rose';

type AccentColors = Pick<
  Theme['colors'],
  'primary' | 'secondary' | 'selected' | 'onPrimary' | 'onSelected'
>;

/**
 * Built-in accent colours. Each has a variant tuned for the dark (Midnight Purple) and light
 * (Pink Dream) themes so text on accent surfaces stays readable. Yellow and gold are excluded
 * by the design rules.
 */
export const accents: Record<
  Exclude<AccentName, 'default'>,
  { swatch: string; dark: AccentColors; light: AccentColors }
> = {
  purple: {
    swatch: '#A855F7',
    dark: {
      primary: '#A855F7',
      secondary: '#C4A7FF',
      selected: '#8538E8',
      onPrimary: '#FFFFFF',
      onSelected: '#FFFFFF',
    },
    light: {
      primary: '#8E44D6',
      secondary: '#9C83D6',
      selected: '#EBDDF9',
      onPrimary: '#FFFFFF',
      onSelected: '#3D2B3F',
    },
  },
  blue: {
    swatch: '#3B82F6',
    dark: {
      primary: '#60A5FA',
      secondary: '#BFDBFE',
      selected: '#2563EB',
      onPrimary: '#0B1530',
      onSelected: '#FFFFFF',
    },
    light: {
      primary: '#2F6FD6',
      secondary: '#6E9BE0',
      selected: '#DCE8FB',
      onPrimary: '#FFFFFF',
      onSelected: '#1E2A44',
    },
  },
  teal: {
    swatch: '#14B8A6',
    dark: {
      primary: '#2DD4BF',
      secondary: '#99F6E4',
      selected: '#0F9F8F',
      onPrimary: '#06201D',
      onSelected: '#FFFFFF',
    },
    light: {
      primary: '#0F8F80',
      secondary: '#4DB6A9',
      selected: '#D2F2EE',
      onPrimary: '#FFFFFF',
      onSelected: '#163A36',
    },
  },
  green: {
    swatch: '#22C55E',
    dark: {
      primary: '#4ADE80',
      secondary: '#BBF7D0',
      selected: '#16A34A',
      onPrimary: '#06210F',
      onSelected: '#FFFFFF',
    },
    light: {
      primary: '#2E8A4E',
      secondary: '#5FB07A',
      selected: '#DAF1E1',
      onPrimary: '#FFFFFF',
      onSelected: '#183A24',
    },
  },
  orange: {
    swatch: '#F97316',
    dark: {
      primary: '#FB923C',
      secondary: '#FED7AA',
      selected: '#EA580C',
      onPrimary: '#2A1405',
      onSelected: '#FFFFFF',
    },
    light: {
      primary: '#C9601F',
      secondary: '#E08B57',
      selected: '#FBE3D3',
      onPrimary: '#FFFFFF',
      onSelected: '#41240F',
    },
  },
  rose: {
    swatch: '#F43F5E',
    dark: {
      primary: '#FB7185',
      secondary: '#FECDD3',
      selected: '#E11D48',
      onPrimary: '#2A0710',
      onSelected: '#FFFFFF',
    },
    light: {
      primary: '#C2577F',
      secondary: '#D98AA8',
      selected: '#F7DCE5',
      onPrimary: '#FFFFFF',
      onSelected: '#3D2B3F',
    },
  },
};

export const accentNames = Object.keys(accents) as Exclude<
  AccentName,
  'default'
>[];

export function withAccent(theme: Theme, accent: string): Theme {
  const a = accents[accent as keyof typeof accents];
  if (!a) return theme;
  return {
    ...theme,
    colors: { ...theme.colors, ...(theme.dark ? a.dark : a.light) },
  };
}
