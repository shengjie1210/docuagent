# 星澜云平台 · 生产环境故障应急 SOP V2.7

> 文档编号：OPS-SOP-2026-007
> 密级：内部机密
> Owner：基础架构部 - 运维组
> 生效日期：2026 年 6 月 20 日
> 适用范围：cn-north-1（北京）、cn-east-2（上海）生产集群

## 一、故障分级

| 级别 | 定义 | 响应时限 | 恢复目标 |
|---|---|---|---|
| SEV-1 | 核心站点不可用 / 全站 5xx > 5% / 资金交易失败 | 5 分钟内拉起作战室 | 30 分钟 |
| SEV-2 | 单区域不可用 / 关键功能受损但有 workaround | 15 分钟 | 2 小时 |
| SEV-3 | 非核心功能异常 / 性能劣化但未影响主链路 | 1 小时 | 当天 24:00 |
| SEV-4 | 已知问题 / 计划内变更导致的短时抖动 | 4 小时 | 下个迭代 |

> 触发 SEV-1 的硬指标：
> - 全站错误率（5xx）连续 3 分钟 > 5%
> - 支付成功率跌破 95%
> - MySQL 主库复制延迟 > 30 秒

## 二、值班体系

- **一线值班（L1）**：7×24 轮班，每人值守 12 小时，手机号 138-0000-1001。
- **二线值班（L2）**：on-call 响应，5 分钟内必须接电话，手机号 138-0000-1002。
- **值班经理（IC）**：只在 SEV-1/SEV-2 时被拉起，手机号 138-0000-1003。
- **外部紧急热线**：阿里云值班 400-600-8800 转 3、CDN 厂商 400-900-7788。

报警通道：
- PagerDuty 主通道；
- 飞书群「星澜-作战室」备用；
- 邮件报警仅作留痕，不作为主要通道。

## 三、常见故障应急动作

### 3.1 全站 5xx 突增

1. 先看 Grafana「error-rate」面板，定位是接入层（Nginx）、应用层还是 DB 层。
2. 若接入层 Nginx 5xx：直接执行 `ansible gateway -m shell -a "systemctl reload nginx"`，30 秒内恢复。
3. 若应用层 Pod CrashLoopBackOff：优先回滚到上一版镜像，命令：
   `kubectl rollout undo deployment/api-server -n prod`
4. 若 DB 连接打满：先在 RDS 控制台执行 `SHOW PROCESSLIST`，杀掉占用 > 60 秒的慢查询。

### 3.2 支付失败（SEV-1）

- 立即在飞书「财务-值班」群呼叫财务值班同事。
- 切流命令：`kubectl set env deployment/payment-gateway -n prod CHANNEL_MODE=degraded`，
  切到降级通道（走微信支付备用商户号 MCH-20260011）。
- **禁止**在故障未恢复时直接重启支付网关 Pod，会导致对账文件丢失。

### 3.3 MySQL 主库切换

1. 确认主库状态：`mysqladmin -h <old-master> ping`，连续 3 次不通才判定主库宕机。
2. 在 Atlas 控制台执行 `atlas failover --cluster=prod-main --confirm`。
3. 切换后 **10 分钟内**禁止执行任何 DDL，等待从库追平。
4. 切换完成后必须通知 DBA（dba-oncall@stellar-example.cn）补充对账。

## 四、变更窗口

- 生产变更窗口：周二、周四 21:00–23:00。
- 大版本（含数据库 DDL）：每月最后一个周六 02:00–06:00。
- 法定节假日、双十一（11.1–11.11）、6.18（6.15–6.20）为 **冻结期**，禁止一切上线。

## 五、复盘与归档

- SEV-1 故障必须在 **48 小时内** 产出复盘文档，存放于飞书空间「星澜-故障库」。
- 复盘模板：时间线 / 根因 / 影响面 / 改进项 / Owner / Deadline。
- 改进项未在 Deadline 前关闭，自动升级到部门 OKR。

## 六、联系方式汇总

| 角色 | 电话 | 邮箱 |
|---|---|---|
| 一线值班 L1 | 138-0000-1001 | l1-oncall@stellar-example.cn |
| 二线值班 L2 | 138-0000-1002 | l2-oncall@stellar-example.cn |
| 值班经理 IC | 138-0000-1003 | ic@stellar-example.cn |
| DBA 团队 | 010-6600-9999 | dba-oncall@stellar-example.cn |
| 安全应急 | 138-0000-1111 | sec-oncall@stellar-example.cn |
