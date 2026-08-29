# Android TV UsageStats 検証アプリ 開発指示書

## 目的

SONY BRAVIA **KJ-43X8000E（Android TV）** 上で、Androidの `UsageStatsManager` APIを利用し、他のアプリの利用履歴・利用時間を取得できるか検証する。

最終的にはABEMAなどの動画アプリについて、日別の視聴時間を把握するアプリを作成したい。

今回は本格アプリではなく、まず **UsageStats APIが実機で正常に利用できるか確認するための検証用APK** を作成する。

---

# 開発環境・技術要件

- Language: Kotlin
- Platform: Android TV
- UI: XML + View Binding
- 最小SDKはAndroid TV実機に対応できる値にする
- Jetpack Composeは使用しない
- 外部ライブラリは原則使用しない
- シンプルな単一Activity構成

Android TV向けアプリとして作成すること。

---

# 実装する機能

## 1. UsageStats取得権限の確認

以下の権限をManifestに宣言する。

```xml
<uses-permission
    android:name="android.permission.PACKAGE_USAGE_STATS" />
```

アプリ起動時にUsageStatsへのアクセス権限を確認する。

権限がない場合は、Usage Access設定画面へ遷移するボタンを表示する。

使用するIntent：

```kotlin
Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
```

Android TVでこの設定画面が存在しない、またはIntentを処理できない場合は、クラッシュせずエラーメッセージを表示すること。

---

## 2. UsageStats取得テスト

画面に「利用履歴を取得」ボタンを配置する。

ボタンを押すと、`UsageStatsManager` を利用してアプリ利用状況を取得する。

使用するAPI：

```kotlin
queryUsageStats()
```

取得期間：

- 今日の00:00
- 現在時刻

取得できたUsageStatsを一覧表示する。

表示項目：

- アプリ名
- パッケージ名
- 最終使用時刻
- 合計フォアグラウンド時間

表示例：

```text
ABEMA
tv.abema
最終使用: 2026/08/29 13:42
利用時間: 1時間23分

YouTube
com.google.android.youtube.tv
最終使用: 2026/08/29 12:10
利用時間: 45分
```

---

## 3. 権限状態の表示

画面上部に現在の状態を表示する。

例：

```text
UsageStats 権限: 許可済み
```

または：

```text
UsageStats 権限: 未許可
```

さらに、UsageStats APIの取得結果件数も表示する。

例：

```text
取得結果: 15件
```

権限があるのに結果が0件の場合は、以下を明確に表示する。

```text
UsageStatsデータが取得できませんでした
```

---

## 4. Android TV対応UI

スマホ向けではなくAndroid TVで操作するため、以下を考慮する。

- リモコンの十字キーだけで操作可能
- Buttonにフォーカスが当たる
- 初期フォーカスを設定する
- RecyclerViewの操作がリモコンで可能
- 文字サイズはTV視聴距離を考慮して大きめ
- 横画面（Landscape）前提
- タッチ操作前提にしない

UIは凝ったデザイン不要。

---

# 画面構成

```text
┌─────────────────────────────────────┐
│ UsageStats TV Test                  │
│                                     │
│ 権限状態: UsageStats 未許可          │
│                                     │
│ [ Usage Access設定を開く ]           │
│                                     │
│ [ 利用履歴を取得 ]                   │
│                                     │
│ 取得結果: 12件                       │
│                                     │
├─────────────────────────────────────┤
│ ABEMA                               │
│ tv.abema                            │
│ 最終使用: 13:42                      │
│ 利用時間: 1時間23分                  │
├─────────────────────────────────────┤
│ YouTube                             │
│ com.google.android.youtube.tv       │
│ 最終使用: 12:10                      │
│ 利用時間: 45分                       │
└─────────────────────────────────────┘
```

---

# 重要な検証ポイント

このアプリの目的は機能完成ではなく、以下を確認すること。

## 検証1

Android TV上で以下の設定画面が起動できるか。

```kotlin
Settings.ACTION_USAGE_ACCESS_SETTINGS
```

## 検証2

`PACKAGE_USAGE_STATS` 権限をユーザー操作で許可できるか。

## 検証3

`UsageStatsManager.queryUsageStats()` がデータを返すか。

## 検証4

他アプリのUsageStatsが取得できるか。

特に以下を確認する。

- ABEMA
- YouTube
- Prime Video
- Netflix

## 検証5

`totalTimeInForeground` が正常な値を返すか。

---

# エラー処理

以下のケースを明確に画面表示する。

### Usage Access設定画面が存在しない

```text
このAndroid TVではUsage Access設定画面を開けません
```

### UsageStats権限が未許可

```text
UsageStatsへのアクセス権限が必要です
```

### データが取得できない

```text
UsageStatsデータが取得できませんでした
```

### 例外発生

クラッシュさせず、Logcatに詳細ログを出力する。

画面には簡潔なエラー内容を表示する。

---

# ログ出力

検証のためLogcatへ以下を出力する。

```text
UsageStats permission: true
UsageStats count: 15

package: tv.abema
lastTimeUsed: ...
totalTimeInForeground: ...
```

ログタグは統一する。

```text
UsageStatsTest
```

---

# APKビルド

以下のAPKを生成できるようにする。

```text
app-debug.apk
```

Android TV実機へのADBインストールを想定する。

```bash
adb install app-debug.apk
```

---

# AndroidManifest要件

以下の権限を設定する。

```xml
<uses-permission
    android:name="android.permission.PACKAGE_USAGE_STATS" />
```

Android TVアプリとして認識されるように設定する。

Leanback対応を含める。

```xml
<uses-feature
    android:name="android.software.leanback"
    android:required="false" />

<uses-feature
    android:name="android.hardware.touchscreen"
    android:required="false" />
```

また、Android TVのホーム画面から起動できるよう、`LEANBACK_LAUNCHER` Intent Filterを設定する。

---

# 実装時の注意

重要なのは、UsageStats APIがAndroid TV実機で動作するかの検証である。

アーキテクチャやデザインパターンを過度に複雑にしないこと。

以下は不要。

- Room
- Retrofit
- DI
- Repository Pattern
- MVVMの過剰な分割
- ネットワーク通信
- Firebase
- ログイン機能

単一Activity + 必要最小限のクラス構成で実装すること。

---

# 完成条件

以下をAndroid TV実機で確認できれば成功。

1. APKをインストールできる
2. TVホーム画面にアプリアイコンが表示される
3. リモコン操作だけで利用できる
4. Usage Access設定画面を開ける、または開けない場合にエラー表示される
5. UsageStats権限状態を判定できる
6. 今日のアプリ利用履歴を取得できる
7. 他アプリ（ABEMA等）のパッケージ名が取得できる
8. `totalTimeInForeground` が取得できるか確認できる
9. Logcatに詳細な取得結果が出力される

---

# 次フェーズについて

今回の検証が成功した場合、次フェーズで以下を実装予定。

- ABEMAのみフィルタ
- アプリ別利用時間の日別集計
- 過去7日間の利用時間表示
- カレンダー表示
- 視聴開始・終了時刻の推定
- 一定時間以上利用時の通知
- Web APIへ利用状況送信
- スマホから利用時間確認

ただし今回は実装しないこと。

まずは **SONY BRAVIA KJ-43X8000E上でUsageStatsManagerが他アプリの利用状況を取得できるか検証することだけに集中すること。**
