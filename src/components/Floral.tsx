import React from 'react';
import type {StyleProp, ViewStyle} from 'react-native';
import {View} from 'react-native';
import Svg, {Circle, G, Path} from 'react-native-svg';

/** A small five-petal blossom used as a subtle accent in the Pink Dream theme. */
export function Floral({size = 44, style}: {size?: number; style?: StyleProp<ViewStyle>}) {
  const petals = [0, 72, 144, 216, 288];
  return (
    <View style={style} pointerEvents="none" importantForAccessibility="no-hide-descendants">
      <Svg width={size} height={size} viewBox="0 0 48 48" opacity={0.55}>
        <G transform="translate(24 24)">
          {petals.map(angle => (
            <Path
              key={angle}
              d="M0 -4 C 6 -10, 6 -18, 0 -20 C -6 -18, -6 -10, 0 -4 Z"
              fill="#F7C6D4"
              transform={`rotate(${angle})`}
            />
          ))}
          <Circle r={4} fill="#D9C8F5" />
        </G>
      </Svg>
    </View>
  );
}
