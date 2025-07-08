# ECGDreamLab

ECGDreamLab是一个专注于心电图(ECG)数据采集、分析和睡眠健康管理的安卓应用程序。该项目旨在通过蓝牙低功耗(BLE)技术连接ECG设备，实时监测、记录和分析用户的心电数据，为用户提供睡眠质量评估和改善方案。

## 项目概述

NeuroSleep应用(ECGDreamLab项目的产品名)将神经科学、睡眠医学与现代移动技术相结合，通过便携式ECG设备采集用户的心电数据，结合人工智能算法进行分析，为用户提供专业的睡眠健康管理方案。

## 蓝牙通信模块详解

ECGDreamLab的蓝牙通信模块是应用程序与ECG硬件设备交互的核心组件，基于蓝牙低功耗(BLE)技术实现，具有低能耗、高可靠性和实时数据传输能力。

### 架构设计

蓝牙通信模块采用了面向对象的设计模式，主要包含以下几个关键组件：

1. **BleClient**：蓝牙通信的核心类，负责管理蓝牙连接和数据通信的整个生命周期
   - 设备发现与扫描管理
   - 连接建立与维护
   - GATT服务与特征值操作
   - 数据收发处理

2. **BleCallback**：回调接口，定义了蓝牙通信过程中的关键事件处理方法
   - `onScanResult`: 设备扫描结果回调
   - `onConnected`: 设备连接成功回调
   - `onDisconnected`: 设备断开连接回调
   - `onDataReceived`: 数据接收回调

3. **BleConfig**：配置类，包含蓝牙通信所需的UUID常量定义
   - 服务UUID：`SERVICE_FIR`, `SERVICE_SEC`
   - 特征值UUID：`CHARA_CTRL`, `CHARA_INFO`, `CHARA_DATA`

4. **ByteUtil**：数据处理工具类，负责蓝牙数据包的编码与解码
   - 命令打包：`packCollectCommand`, `packTransferCommand`
   - ECG数据解析：`parseEcgDataPacket`, `parseEcgPoint`

### 通信协议

蓝牙通信采用自定义协议，基于GATT服务模型：

1. **服务与特征值结构**
   - 主服务(`SERVICE_FIR`)：处理控制命令和状态信息
     - 控制特征值(`CHARA_CTRL`): 用于接收上位机发送的控制命令
     - 信息特征值(`CHARA_INFO`): 用于心电数据之外的状态应答
   - 数据服务(`SERVICE_SEC`)：
     - 数据特征值(`CHARA_DATA`): 用于传输ECG数据

2. **命令格式**
   - 所有命令均以`0xFA`开始，以`0xFB`结束
   - 采集控制命令(0x01)：控制ECG数据采集的开始和停止
   - 传输控制命令(0x02)：控制蓝牙数据传输的开始和停止

3. **数据包格式**
   - ECG数据包：每个数据包包含8个采样点
   - 每个采样点包含10字节：1字节状态标志 + 3个3字节ECG导联数据

### 数据处理流程

1. **设备连接流程**
   - 启动设备扫描：`startScan()`
   - 过滤目标设备：通过设备名称前缀(`EcgConfig.DEVICE_NAME_PREFIX`)识别
   - 建立GATT连接：`connectToDevice(device)`
   - 服务发现：连接成功后自动调用`gatt.discoverServices()`
   - 特征值初始化：`initCharacteristic()`

2. **数据接收与解析**
   - 启用通知：`setCharacteristicNotification(dataCharacteristic, true)`
   - 数据接收回调：`onCharacteristicChanged()`
   - 数据解析：`ByteUtil.parseEcgDataPacket(data)`将原始字节数组转换为ECG数据模型
   - 数据应用：将解析后的数据传递给`EcgProcessor`进行进一步处理

3. **信号处理**
   - 滤波处理：通过`EcgProcessor`应用低通和高通滤波
   - R峰检测：基于Pan-Tomkins算法
   - 心率计算：基于R峰间距计算
   - HRV指标分析：计算SDNN、RMSSD和pNN50等心率变异性指标

### 性能优化

1. **扫描策略优化**
   - 使用低功耗扫描模式：`ScanSettings.SCAN_MODE_LOW_POWER`
   - 限制扫描时间：10秒自动停止
   - 设备数量限制：最多扫描50个设备，避免资源浪费

2. **连接管理**
   - 自动重连机制
   - 连接状态监控
   - 错误处理与恢复

3. **数据处理优化**
   - 批量数据处理：每次处理多个数据点
   - 高效数据解析算法
   - 内存使用优化：重用数据缓冲区

## 核心功能

### 1. 实时ECG监测与分析
- 通过蓝牙连接ECG设备，实时采集心电数据
- 实时显示ECG波形图和心率变化曲线
- 支持波形缩放和全屏显示功能
- 心率变异性(HRV)分析，包括RMSSD、SDNN等指标

### 2. 数据标记与实验参与
- 支持用户在ECG数据上添加自定义标签(如睡眠状态、情绪状态等)
- 提供实验参与功能，用户可加入科研实验项目
- 支持数据上传至云端服务器进行进一步分析

### 3. 睡眠训练与改善
- 提供多种类型的训练课程：
  - 放松训练：帮助用户缓解压力，改善睡眠质量
  - 激励训练：提升用户精神状态和专注力
  - 认知训练：增强记忆力和认知能力
  - 音乐冥想：通过音乐引导用户进入冥想状态
  - 评估测试：评估用户的睡眠和认知状态

### 4. 睡眠趋势分析
- 长期追踪用户的睡眠健康数据
- 提供数据可视化展示，帮助用户了解自己的睡眠模式变化

### 5. 个性化设置与账户管理
- 用户注册、登录和个人信息管理
- 应用主题和界面个性化设置
- 设备管理与配置

## 技术架构

### 前端架构
- **MVVM架构模式**：使用ViewModel和LiveData实现UI与数据的分离
- **Kotlin语言**：采用现代Android开发语言，提供更安全和简洁的代码
- **Jetpack组件**：包括LifeCycle、ViewModel、LiveData、Room等
- **协程(Coroutines)**：处理异步操作，提高应用响应性

### 核心模块

1. **蓝牙通信模块**
   - BleClient：封装蓝牙连接和数据传输
   - 设备扫描、连接与数据收发
   - 数据包解析与处理

2. **数据处理模块**
   - ECG信号处理：去直流、滤波
   - 心率计算与HRV分析
   - 数据存储与管理

3. **用户界面模块**
   - 自定义视图：ECG波形显示
   - 实时图表：心率曲线展示
   - 响应式UI设计

4. **用户认证模块**
   - 注册、登录流程
   - 令牌管理
   - 权限验证

5. **云端同步模块**
   - RESTful API通信
   - 数据上传与下载
   - 实验数据管理

## 开发环境与依赖

- Android Studio
- Kotlin 1.8+
- Android SDK 33+
- Gradle 8.0+
- 主要第三方库：
  - Retrofit：网络通信
  - Room：本地数据库
  - LiveChart：数据可视化
  - 其他Android Jetpack组件

## 项目结构

```
app/
├── core/               # 核心功能模块
│   ├── auth/           # 用户认证
│   ├── ble/            # 蓝牙通信
│   └── main/           # 主页面逻辑
├── data/               # 数据层
│   ├── database/       # 本地数据库
│   ├── model/          # 数据模型
│   ├── network/        # 网络请求
│   └── repository/     # 数据仓库
└── feature/            # 特性模块
    ├── home/           # 首页功能
    ├── record/         # 记录功能
    ├── settings/       # 设置页面
    ├── train/          # 训练功能
    └── trend/          # 趋势分析
```

## 未来计划

1. 增强AI算法，提供更精准的睡眠分析
2. 增加社区功能，用户可分享和比较睡眠数据
3. 增加更多训练课程和冥想内容
4. 开发配套的可穿戴设备，提升用户体验
5. 支持更多健康数据的整合分析

## 联系我们

如有任何问题或建议，请通过以下方式联系我们：

- 电子邮件：support@ecgdreamlab.com
- 官方网站：www.ecgdreamlab.com

---

© 2025 ECGDreamLab 团队，保留所有权利

