# Android 通知桥接器

这是一个 Android 手机端软件源码工程。它的作用是：

1. 在 Android 手机本机读取通知栏消息。
2. 根据 App 包名白名单、包含关键词、屏蔽关键词做过滤。
3. 对验证码、手机号、长数字做基础脱敏。
4. 把符合规则的通知直接推送到飞书群机器人或钉钉群机器人。

> 重要边界：这个软件只能监听你自己手机上、你已经授权通知读取权限后能看到的通知。不要用于监听他人设备，也不要默认转发所有 App 通知。

---

## 一、工程结构

```text
mobile_notification_bridge/
├─ settings.gradle
├─ build.gradle
├─ gradle.properties
└─ app/
   ├─ build.gradle
   └─ src/main/
      ├─ AndroidManifest.xml
      ├─ java/com/kaly/notificationbridge/
      │  ├─ MainActivity.java
      │  ├─ NotificationBridgeService.java
      │  ├─ WebhookSender.java
      │  ├─ BridgeConfig.java
      │  ├─ NotificationPayload.java
      │  ├─ FilterUtils.java
      │  └─ TimeUtils.java
      └─ res/
```

---

## 二、怎么打包成 APK

### 方式 1：Android Studio

1. 安装 Android Studio。
2. 打开本文件夹 `mobile_notification_bridge`。
3. 等待 Gradle Sync 完成。
4. 点击 `Build > Build Bundle(s) / APK(s) > Build APK(s)`。
5. 构建完成后，APK 一般在：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 方式 2：命令行

如果本机已有 Gradle 和 Android SDK：

```bash
cd mobile_notification_bridge
./gradlew assembleDebug
```

如果没有 gradle wrapper，可以在 Android Studio 里打开项目后自动同步构建。

---

## 三、手机端怎么配置

安装 APK 后：

1. 打开“通知桥接器”。
2. 点击“打开通知读取权限设置”。
3. 找到“通知桥接器”，允许读取通知。
4. 回到 App，填写飞书或钉钉 Webhook。
5. 勾选对应通道。
6. 填写 App 包名白名单。
7. 填写包含关键词和屏蔽关键词。
8. 点击“保存配置”。
9. 点击“发送测试消息”，确认群里能收到。
10. 打开系统设置，建议允许本应用后台运行，避免被省电策略杀掉。

---

## 四、配置项说明

### 1. App 包名白名单

只转发指定 App 的通知。强烈建议填写，不要留空。

示例：

```text
com.alibaba.android.rimet,com.ss.android.lark
```

常见包名示例：

```text
钉钉：com.alibaba.android.rimet
飞书：com.ss.android.lark
微信：com.tencent.mm
企业微信：com.tencent.wework
```

不同版本或海外版本包名可能不同，建议用 ADB 或第三方应用信息查看工具确认。

### 2. 包含关键词

只有通知标题或内容包含这些关键词时才转发。

示例：

```text
审批,待办,通知,报警,申请,任务
```

留空表示不按关键词过滤。

### 3. 屏蔽关键词

只要通知标题或内容包含这些词，就不转发。

默认值：

```text
验证码,密码,登录,银行卡,支付,转账,动态码,code,otp
```

### 4. 去重时间

同一 App、同一标题、同一内容，在指定秒数内只转发一次。默认 60 秒。

---

## 五、飞书机器人配置

1. 在飞书群中添加“自定义机器人”。
2. 复制 Webhook。
3. 如果开启签名校验，复制签名密钥，填到 App 的“飞书签名密钥”。
4. 如果没开签名校验，密钥留空。

发送格式为文本消息：

```text
【手机通知转发】
应用：xxx
包名：xxx
标题：xxx
内容：xxx
时间：yyyy-MM-dd HH:mm:ss
```

---

## 六、钉钉机器人配置

1. 在钉钉群中添加“自定义机器人”。
2. 复制 Webhook。
3. 如果开启“加签”，复制 SEC 开头的密钥，填到 App 的“钉钉加签密钥”。
4. 如果开启“关键词”安全策略，确保转发内容里包含你的关键词。例如可以把关键词设置为“手机通知转发”。

---

## 七、正式使用建议

1. 必须填写 App 包名白名单。
2. 不要转发验证码、登录、支付、银行卡、账号安全类通知。
3. Webhook 和签名密钥不要截图发给别人。
4. 尽量开启飞书/钉钉的签名校验。
5. 如果手机系统杀后台，需要在系统电池设置里允许本应用后台运行。
6. 部分 App 如果在通知里隐藏正文，本软件也无法读取隐藏后的内容。

---

## 八、已知限制

1. 不能读取 App 内部消息，只能读取通知栏展示出来的信息。
2. 如果目标 App 没有弹通知，本软件不会知道有新消息。
3. 如果通知内容被系统或 App 隐藏，只能读取到隐藏后的文本。
4. 厂商系统省电策略可能导致服务被杀，需要手动允许后台运行。
5. 该工程是源码工程，需要用 Android Studio 构建 APK。
