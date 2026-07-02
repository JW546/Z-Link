# 面向无人设备的自然语言意图驱动协同系统

本项目不是普通 Web App，也不是中心化远程调度平台。前端仅用于演示和实验可视化，系统核心围绕：

自然语言意图 -> DSL 任务契约 -> IDL/DDS 发布订阅 -> 多设备 Agent 自主协同 -> 冲突消解 -> 可解释路线决策

## 当前项目已经实现

- 场景驱动策略评估；
- 权重搜索优化；
- AgentPolicy 对比；
- RL / MARL 接口预留。

## 当前项目尚未完成

- 真实强化学习训练；
- 神经网络模型训练；
- 多智能体端到端训练；
- 真实设备数据训练。

## RL / MARL 接口边界

`source/policy` 中预留了 `PolicyEnvironment`、`Episode`、`StepResult`、`ReplayRecord`、`PolicyTrainer`、`RandomPolicy`、`HeuristicPolicy`、`RLTrainingConfig` 和 `MARLTrainingConfig`，用于后续接入 DQN、PPO、SAC 和 MARL。

这些接口和 baseline 只提供数据结构、环境协议、回放记录格式、策略接口和配置对象，不包含真实 episode 采样、训练曲线、神经网络优化或多智能体端到端训练。

## 测试基线

后端测试与 API/E2E 基线：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\test_baseline.ps1
```
