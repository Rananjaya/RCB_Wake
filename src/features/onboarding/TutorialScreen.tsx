import React, { useEffect, useRef, useState } from 'react';
import {
  BackHandler,
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Icon, type IconName } from '../../components/Icon';
import { Button } from '../../components/ui';
import { useT, type StringKey } from '../../i18n';
import { useTheme } from '../../theme/useTheme';
import { radius, spacing } from '../../theme/themes';

export const TUTORIAL_PAGES: {
  icon: IconName;
  title: StringKey;
  body: StringKey;
}[] = [
  { icon: 'alarm', title: 'tut1Title', body: 'tut1Body' },
  { icon: 'plus', title: 'tut2Title', body: 'tut2Body' },
  { icon: 'timer', title: 'tut3Title', body: 'tut3Body' },
  { icon: 'globe', title: 'tut4Title', body: 'tut4Body' },
  { icon: 'settings', title: 'tut5Title', body: 'tut5Body' },
];

type Props = {
  /** Replay from Settings: Close on every page, Done at the end; completion state is unchanged. */
  replay?: boolean;
  onFinish: () => void;
};

/** Five swipeable pages: Skip (pages 1–4), Back, Next, Get started. */
export function TutorialScreen({ replay = false, onFinish }: Props) {
  const theme = useTheme();
  const t = useT();
  const { width } = useWindowDimensions();
  const list = useRef<FlatList>(null);
  const [page, setPage] = useState(0);
  const last = page === TUTORIAL_PAGES.length - 1;

  const go = (p: number) => {
    list.current?.scrollToIndex({ index: p, animated: true });
    setPage(p);
  };

  useEffect(() => {
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      if (page > 0) go(page - 1);
      else if (replay) onFinish();
      else BackHandler.exitApp();
      return true;
    });
    return () => sub.remove();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, replay]);

  const onScroll = (e: NativeSyntheticEvent<NativeScrollEvent>) =>
    setPage(Math.round(e.nativeEvent.contentOffset.x / width));

  return (
    <SafeAreaView
      testID="tutorial"
      style={[styles.screen, { backgroundColor: theme.colors.background }]}
    >
      <View style={styles.header}>
        <Text style={[styles.counter, { color: theme.colors.textSecondary }]}>
          {t('tutorialCounter', { n: page + 1, total: TUTORIAL_PAGES.length })}
        </Text>
        {(replay || !last) && (
          <Pressable
            onPress={onFinish}
            accessibilityRole="button"
            hitSlop={12}
            testID="tutorial-skip"
          >
            <Text style={[styles.skip, { color: theme.colors.primary }]}>
              {replay ? t('tutorialClose') : t('tutorialSkip')}
            </Text>
          </Pressable>
        )}
      </View>
      <FlatList
        ref={list}
        data={TUTORIAL_PAGES}
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        keyExtractor={p => p.title}
        onMomentumScrollEnd={onScroll}
        getItemLayout={(_, i) => ({
          length: width,
          offset: width * i,
          index: i,
        })}
        renderItem={({ item }) => (
          <View style={[styles.page, { width }]}>
            <View
              style={[styles.art, { backgroundColor: theme.colors.elevated }]}
            >
              <View
                style={[
                  styles.artInner,
                  { backgroundColor: theme.colors.selected },
                ]}
              >
                <Icon
                  name={item.icon}
                  color={theme.colors.onSelected}
                  size={64}
                  strokeWidth={1.8}
                />
              </View>
            </View>
            <Text
              style={[styles.title, { color: theme.colors.text }]}
              accessibilityRole="header"
            >
              {t(item.title)}
            </Text>
            {t(item.body)
              .split('\n')
              .map(line => (
                <Text
                  key={line}
                  style={[styles.body, { color: theme.colors.textSecondary }]}
                >
                  {line}
                </Text>
              ))}
          </View>
        )}
      />
      <View style={styles.footer}>
        <View style={styles.side}>
          {page > 0 && (
            <Pressable
              onPress={() => go(page - 1)}
              accessibilityRole="button"
              hitSlop={12}
            >
              <Text style={[styles.skip, { color: theme.colors.primary }]}>
                {t('tutorialBack')}
              </Text>
            </Pressable>
          )}
        </View>
        <View style={styles.dots}>
          {TUTORIAL_PAGES.map((p, i) => (
            <View
              key={p.title}
              style={[
                styles.dot,
                {
                  backgroundColor:
                    i === page ? theme.colors.primary : theme.colors.divider,
                },
              ]}
            />
          ))}
        </View>
        <View style={[styles.side, styles.right]}>
          <Button
            testID={last ? 'tutorial-finish' : 'tutorial-next'}
            label={
              last
                ? replay
                  ? t('tutorialDone')
                  : t('tutorialGetStarted')
                : t('tutorialNext')
            }
            onPress={() => (last ? onFinish() : go(page + 1))}
          />
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: spacing.xl,
    height: 56,
  },
  counter: { fontSize: 14, fontWeight: '600' },
  skip: { fontSize: 15, fontWeight: '700' },
  page: {
    paddingHorizontal: spacing.xxl,
    justifyContent: 'center',
    gap: spacing.md,
  },
  art: {
    alignSelf: 'center',
    width: 200,
    height: 200,
    borderRadius: radius.lg * 4,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.xl,
  },
  artInner: {
    width: 128,
    height: 128,
    borderRadius: 64,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: { fontSize: 26, fontWeight: '700' },
  body: { fontSize: 16, lineHeight: 24 },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.xl,
    paddingVertical: spacing.lg,
  },
  side: { flex: 1 },
  right: { alignItems: 'flex-end' },
  dots: { flexDirection: 'row', gap: 6 },
  dot: { width: 8, height: 8, borderRadius: 4 },
});
