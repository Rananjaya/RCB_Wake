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

type Palette = Theme['colors'];
type AccentPalettes = {
  swatch: string;
  dark: Partial<Palette>;
  light: Partial<Palette>;
};

/**
 * Built-in colour themes. Each accent recolours the whole app — background, surfaces, dividers,
 * text tints and controls — with a variant for dark (Midnight) and light (Pink Dream) modes.
 * Contrast of text on surfaces and on the accent is kept at WCAG AA or better. Yellow and gold are
 * excluded by the design rules.
 */
export const accents: Record<Exclude<AccentName, 'default'>, AccentPalettes> = {
  purple: {
    swatch: '#A855F7',
    dark: { ...midnightPurple.colors },
    light: {
      background: '#F8F3FF',
      surface: '#FFFFFF',
      elevated: '#EBDDF9',
      divider: '#DCCDEE',
      text: '#2F2240',
      textSecondary: '#6E5E86',
      primary: '#8E44D6',
      secondary: '#9C83D6',
      selected: '#EBDDF9',
      onPrimary: '#FFFFFF',
      onSelected: '#2F2240',
    },
  },
  blue: {
    swatch: '#3B82F6',
    dark: {
      background: '#0B1220',
      surface: '#131D33',
      elevated: '#1C2945',
      divider: '#2A3A5C',
      text: '#F2F6FF',
      textSecondary: '#9FB0CC',
      primary: '#60A5FA',
      secondary: '#BFDBFE',
      selected: '#2563EB',
      onPrimary: '#0B1530',
      onSelected: '#FFFFFF',
    },
    light: {
      background: '#F3F7FF',
      surface: '#FFFFFF',
      elevated: '#E2EBFB',
      divider: '#D3DEF2',
      text: '#1E2A44',
      textSecondary: '#5E6F8E',
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
      background: '#081614',
      surface: '#102421',
      elevated: '#18332F',
      divider: '#234541',
      text: '#F0FBF9',
      textSecondary: '#9CBDB8',
      primary: '#2DD4BF',
      secondary: '#99F6E4',
      selected: '#0F9F8F',
      onPrimary: '#06201D',
      onSelected: '#FFFFFF',
    },
    light: {
      background: '#F1FAF8',
      surface: '#FFFFFF',
      elevated: '#D9F0EC',
      divider: '#C9E4DF',
      text: '#163A36',
      textSecondary: '#557A75',
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
      background: '#0A150E',
      surface: '#122219',
      elevated: '#1A3023',
      divider: '#264331',
      text: '#F2FBF4',
      textSecondary: '#A0BEA9',
      primary: '#4ADE80',
      secondary: '#BBF7D0',
      selected: '#16A34A',
      onPrimary: '#06210F',
      onSelected: '#FFFFFF',
    },
    light: {
      background: '#F3FAF4',
      surface: '#FFFFFF',
      elevated: '#DDF0E1',
      divider: '#CDE5D3',
      text: '#183A24',
      textSecondary: '#5A7A63',
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
      background: '#170F0A',
      surface: '#241912',
      elevated: '#33241A',
      divider: '#463325',
      text: '#FFF6F0',
      textSecondary: '#C2AC9C',
      primary: '#FB923C',
      secondary: '#FED7AA',
      selected: '#EA580C',
      onPrimary: '#2A1405',
      onSelected: '#FFFFFF',
    },
    light: {
      background: '#FFF6EF',
      surface: '#FFFFFF',
      elevated: '#FBE4D3',
      divider: '#F0D3BE',
      text: '#41240F',
      textSecondary: '#86634B',
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
      background: '#170A10',
      surface: '#25121B',
      elevated: '#351A26',
      divider: '#4A2535',
      text: '#FFF2F6',
      textSecondary: '#C4A0AF',
      primary: '#FB7185',
      secondary: '#FECDD3',
      selected: '#E11D48',
      onPrimary: '#2A0710',
      onSelected: '#FFFFFF',
    },
    light: { ...pinkDream.colors },
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
