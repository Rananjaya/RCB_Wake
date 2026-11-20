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

export const themes: Record<ThemeName, Theme> = {midnight: midnightPurple, pink: pinkDream};

export const spacing = {xs: 4, sm: 8, md: 12, lg: 16, xl: 24, xxl: 32};
export const radius = {sm: 10, md: 16, lg: 24, pill: 999};
