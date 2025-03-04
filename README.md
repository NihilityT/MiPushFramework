# MiPushFramework

[![Test CI](https://github.com/NihilityT/MiPushFramework/actions/workflows/test_ci.yml/badge.svg)](https://github.com/NihilityT/MiPushFramework/actions/workflows/test_ci.yml)
[![License GPL-3.0](https://img.shields.io/badge/license-GPLv3.0-blue.svg)](LICENSE)
![Min Android Version](https://img.shields.io/badge/android-lollipop-%23860597.svg)

在非 MIUI 系统上体验小米系统级推送。

## 什么是小米系统级推送，为什么会有这个项目

小米推送是小米公司提供的推送服务，许多 App 都在使用（如酷安）。

它非常轻量，会在 MIUI 设备上自动启用系统推送，而非 MIUI 设备则在后台保持长连接。



### 系统级推送

类似 GCM，小米推送的系统级推送是在 MIUI 完成的。

接入小米推送的应用在启动时，会根据系统是否为 MIUI ROM 来决定如何向用户推送消息。
- 对于 MIUI ROM，推送工作都由系统完成，应用**无需后台**，更省电。
- 对于非 MIUI ROM，每个应用都会**在后台启动**一个 `XMPushService` 服务，该服务会创建一个用于接收推送消息的连接。相比于 MIUI ROM 由系统实现的推送能力，多个应用启动的多个服务与连接，会更加的耗电、内存和流量。



### 本项目的意义

本项目起源于这样一个想法：让任何非 MIUI ROM 的用户都能用上类似 MIUI ROM 的系统级推送能力，在应用没有常驻后台的情况下也能向用户推送消息。


## 功能

- 基本的推送能力。基本与 MIUI ROM 中的推送服务（`com.xiaomi.xmsf`）一致，不过默认禁止拉起应用
- 对通知的改写。可以通过配置文件，修改消息的标题、内容及样式等，控制收到消息时忽略、亮屏或自动弹出等
- 观测。可以在本项目的应用界面中，查看都有什么应用接入了小米推送服务及其推送的消息内容

### 注意

* 给予 `com.xiaomi.xmsf` 最大的权限，请勿使用 黑域、绿色守护、Xposed 等模块对其做限制，这可能会导致推送不稳定
* 在 MIUI ROM 上，部分依靠推送的功能不可用，如 网络短信，目前明确可用的有 查找手机
* 服务本身不需要 Root、Xposed 支持，但是为了伪装为 MIUI ROM，使应用自动向 `com.xiaomi.xmsf` 注册信息，建议使用伪装增强模块



## 优点

* 简单，安装非常简单
* 使用后，其他应用的 `XMPushService` 会自动禁用，就像在 MIUI，同时还能保证推送
* 完整事件记录，可以监控每个应用的 注册和推送
* 拦截小米推送产生的不必要唤醒，也能阻止它读取您的隐私
* 自定义能力，定制你的消息内容与行为




## 开始使用

安装步骤非常简单 ：

* 前往 [Releases](https://github.com/MiPushFramework/MiPushFramework/releases) 或 [Test CI](https://github.com/NihilityT/MiPushFramework/actions/workflows/test_ci.yml)，下载最新的 APK 并安装。
* 跟着向导进行设置
* 可选：开启高级配置中的 推送服务保活 选项

### 常见问题

- vc105 版本与 normal 版本有什么区别？
    - 最主要的区别在于消息的传递方式不同，推送服务 105 版本使用 startService，108 以上版本使用 bindService
    - 目前建议优先使用 normal 版本，若有问题再切换为 105 版本
    - 对于 MIUI ROM，使用 normal 版本的另一个好处是，应用不会在重启系统后变回官方版本
    - 建议 COS 使用 vc105 版本，目前发现 COS 系统无法使用 Bind 方式传递消息，因无测试机无法排查原因


- 是否支持分身（999）应用？
    - 目前没有这方面的计划，也没有测试过，不接受相关反馈


- 配置文件都有什么作用？我应该使用配置文件吗？
    - 配置文件可以修改消息的标题、内容及样式等，控制收到消息时忽略、亮屏或自动弹出等
    - 目前大部分“官方”配置都可以无脑使用，部分配置是否要使用，参见[仓库说明](https://github.com/NihilityT/MiPushConfigurations)、配置名或配置中的 description 字段


- 是否应该安装为系统应用？
    - 推荐安装为系统应用。某些应用没有查看设备应用列表的权限，在获取应用列表时，**只能获取到系统应用列表**
    - 将推送服务安装为系统应用，可以让这些应用发现推送服务，从而进行服务注册



## 反馈问题

遇到任何问题，请先看看 Issues 里面有没有人提过。（常见问题：无法收到推送）
如果没有找到答案，请为每个问题提交一份 Issue，并务必带上如下内容，以便开发者解决：

* 你的 ROM 是什么，Android 版本是什么
* 有没有使用框架等工具

同时，请使用 设置-获取日志 获取你的日志文件，写进 Issue。

## 日志

框架会自动记录日志，保存到私有目录。您可以前往 设置-高级配置 中清理。



## 参与项目

请参考 [Contribution Guideline](CONTRIBUTION.md)

## 已知问题

* 对于部分小众的ROM （如 360OS）导致无法正常工作的情况，我们只会竭尽全力保证推送的运行，其它不妨碍推送的「特殊适配」会被忽略。对于这些情况，建议您更换更好的 ROM 以获得最佳体验。
* 努比亚ROM应用（第三方使用 MiPush 的应用）可能不会自动禁用其 XMPushService 并启动服务，请尝试将框架设为系统应用
* 锤子 ROM 下，Push 可以正确收到通知，但是通知栏没有提示 #143
* 一些通知 Feature 可能无法使用（如通知都会显示为推送框架发出，而不是目标应用）

## 感谢

* @Rachel030219 提供文件
* Android Open Source Project, MultiType, greenDao, SetupWizardLibCompat, Condom, MaterialPreference，GreenDaoUpgradeHelper, epic, Log4a，helplib，RxJava RxAndroid，RxActivityResult，RxPermissions, hiBeaver
* 酷安 @PzHown @lmnm011223 @苏沐晨风丶（未采纳） 提供图标
