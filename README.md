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
