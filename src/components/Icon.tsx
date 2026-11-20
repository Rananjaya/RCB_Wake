import React from 'react';
import Svg, { Circle, Path } from 'react-native-svg';

export type IconName =
  | 'alarm'
  | 'timer'
  | 'stopwatch'
  | 'globe'
  | 'settings'
  | 'plus'
  | 'trash'
  | 'play'
  | 'pause'
  | 'reset'
  | 'flag'
  | 'close'
  | 'check'
  | 'chevronRight'
  | 'back'
  | 'sun'
  | 'moon'
  | 'music'
  | 'search'
  | 'warning'
  | 'backspace';

type Props = {
  name: IconName;
  size?: number;
  color: string;
  strokeWidth?: number;
};

/** Line icons drawn for RCB Wake on a 24×24 grid. */
export function Icon({ name, size = 24, color, strokeWidth = 2 }: Props) {
  const common = {
    stroke: color,
    strokeWidth,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    fill: 'none',
  };
  const content = (() => {
    switch (name) {
      case 'alarm':
        return (
          <>
            <Circle cx={12} cy={13} r={7.5} {...common} />
            <Path
              d="M12 9.5V13l2.5 1.8M4 5.5 6.5 3M20 5.5 17.5 3"
              {...common}
            />
          </>
        );
      case 'timer':
        return (
          <Path
            d="M7 3h10M7 21h10M8 3v3.5L12 11l4-4.5V3M8 21v-3.5L12 13l4 4.5V21"
            {...common}
          />
        );
      case 'stopwatch':
        return (
          <>
            <Circle cx={12} cy={14} r={7} {...common} />
            <Path d="M10 2.5h4M12 2.5V7M12 14l3-3M18.5 7.5 20 6" {...common} />
          </>
        );
      case 'globe':
        return (
          <>
            <Circle cx={12} cy={12} r={9} {...common} />
            <Path
              d="M3 12h18M12 3c2.5 2.6 3.7 5.6 3.7 9s-1.2 6.4-3.7 9c-2.5-2.6-3.7-5.6-3.7-9S9.5 5.6 12 3Z"
              {...common}
            />
          </>
        );
      case 'settings':
        return (
          <>
            <Circle cx={12} cy={12} r={3} {...common} />
            <Path
              d="M10.3 3h3.4l.5 2.4 1.7.9 2.3-.9 1.7 2.9-1.8 1.6v2.2l1.8 1.6-1.7 2.9-2.3-.9-1.7.9-.5 2.4h-3.4l-.5-2.4-1.7-.9-2.3.9-1.7-2.9 1.8-1.6v-2.2L4.1 8.3l1.7-2.9 2.3.9 1.7-.9.5-2.4Z"
              {...common}
            />
          </>
        );
      case 'plus':
        return <Path d="M12 5v14M5 12h14" {...common} />;
      case 'trash':
        return (
          <Path
            d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"
            {...common}
          />
        );
      case 'play':
        return <Path d="M8 5.5v13l10.5-6.5L8 5.5Z" {...common} fill={color} />;
      case 'pause':
        return (
          <Path
            d="M8 5v14M16 5v14"
            {...common}
            strokeWidth={strokeWidth + 1.2}
          />
        );
      case 'reset':
        return <Path d="M4 12a8 8 0 1 0 2.3-5.7M4 4v4h4" {...common} />;
      case 'flag':
        return <Path d="M5 21V4M5 4h11l-2 4 2 4H5" {...common} />;
      case 'close':
        return <Path d="M6 6l12 12M18 6 6 18" {...common} />;
      case 'check':
        return <Path d="M5 12.5 10 17.5 19 7" {...common} />;
      case 'chevronRight':
        return <Path d="M9 5l7 7-7 7" {...common} />;
      case 'back':
        return <Path d="M15 5l-7 7 7 7" {...common} />;
      case 'sun':
        return (
          <>
            <Circle cx={12} cy={12} r={4} {...common} />
            <Path
              d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4"
              {...common}
            />
          </>
        );
      case 'moon':
        return (
          <Path d="M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5Z" {...common} />
        );
      case 'music':
        return (
          <>
            <Circle cx={7} cy={18} r={2.5} {...common} />
            <Circle cx={17} cy={16} r={2.5} {...common} />
            <Path d="M9.5 18V6l10-2v12" {...common} />
          </>
        );
      case 'search':
        return (
          <>
            <Circle cx={11} cy={11} r={6.5} {...common} />
            <Path d="M16 16l4.5 4.5" {...common} />
          </>
        );
      case 'warning':
        return (
          <Path d="M12 4 2.5 20h19L12 4ZM12 10v4.5M12 17.5v.01" {...common} />
        );
      case 'backspace':
        return (
          <Path
            d="M9 5h11v14H9l-6-7 6-7ZM12.5 9.5l5 5M17.5 9.5l-5 5"
            {...common}
          />
        );
    }
  })();
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24">
      {content}
    </Svg>
  );
}
