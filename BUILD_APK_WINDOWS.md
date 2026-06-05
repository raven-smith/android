# Windows 本地打包 APK

## 推荐方式：Android Studio

1. 安装 Android Studio。
2. 打开 `mobile_notification_bridge` 文件夹。
3. 等待 Gradle Sync 完成。
4. 菜单选择：`Build > Build Bundle(s) / APK(s) > Build APK(s)`。
5. 打包结果一般在：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions 自动打包

如果你不想本地装环境：

1. 新建一个 GitHub 仓库。
2. 上传本工程所有文件。
3. 进入仓库的 `Actions` 页面。
4. 运行 `Build Android APK` 工作流。
5. 构建结束后，在 Artifacts 里下载 `mobile-notification-bridge-debug-apk`。

