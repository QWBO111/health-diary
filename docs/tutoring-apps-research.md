# 家教记录功能 · GitHub 开源应用调研报告

调研日期：2026-08-12

## 核心结论

1. **没有找到**把“家教收入/课时记录 + 排课表”与“健身 + 饮食 + 日记”整合在同一个 App 里的成熟开源项目。
2. 家教管理类开源项目数量少、星标低，且多数是 Web / CLI / 桌面端，没有成熟的安卓原生应用。
3. 课程表 / 排课类安卓开源生态很成熟（TimetableView 722★、时光课表 619★ 等），周视图、课程重叠处理、提醒等交互可以直接借鉴。
4. 结论：**继续自研**，不集成现成应用；把“课表视图交互 + 收入按月汇总 + 排课冲突检测”作为功能设计参考。

## 家教管理 / 收入课时类

| 项目 | 星标 | 语言 | 许可证 | 说明 | 参考价值 |
|---|---|---|---|---|---|
| [Preceptly](https://github.com/andreaseirich/preceptly) | 低 | Python/Django | 未标注 | 私人家教管理系统：学生、合同、排课、通勤时间、收入，可选 LLM 备课 | 收入与排课的数据模型、按月统计思路 |
| [TutorDesk](https://github.com/peals-pline/TutorDesk) | 7 | TypeScript | MIT | Local-first 家教工作台：学生、课程、作业 | “本地优先”隐私思路与我们的纯本地定位一致 |
| [TuitionConnect](https://github.com/nus-cs2103-AY2324S1/tp) | 低 | Java/CLI | MIT | 家教业务 CLI：学生、排课、进度、财务，按月收入统计 | 月度收入聚合功能点 |
| [my-tutor](https://github.com/vias2019/my-tutor) | 4 | JavaScript | 未标注 | 私教课程日历排期 | 日历式排课交互 |
| [Talmidon](https://github.com/yt314/Talmidon) | 2 | C# | 未标注 | 私教 CRM + 进度追踪 Web 应用 | 学生档案思路（非必需） |
| TutorFlow（Devpost） | 闭源 | - | - | 排课冲突检测、收入状态（已上/已开票/已收） | 功能清单验证我们的冲突检测设计 |

## 课程表 / 排课类（安卓）

| 项目 | 星标 | 语言 | 许可证 | 说明 | 参考价值 |
|---|---|---|---|---|---|
| [TimetableView](https://github.com/zfman/TimetableView) | 722 | Java | MIT | 开源 Android 课程表控件：课程重叠自动处理、空白格点击、自定义配色 | 周课表 UI 与重叠检测的实现参考 |
| [时光课表](https://github.com/XingHeYuZhuan/shiguangschedule) | 619 | Kotlin | Apache-2.0 | 极简无广告课程表：教务导入、今日课表、小组件 | 周视图 UI / 交互参考 |
| [ClassSchedule](https://github.com/xxyangyoulin/ClassSchedule) | 162 | Java | Apache-2.0 | Material Design 课程表 | 视觉风格参考 |
| [YunShuClassSchedule](https://github.com/itning/YunShuClassSchedule) | 51 | Kotlin | Apache-2.0 | 上下课提醒、自动静音 | 提醒功能参考（可选） |
| [WakeUp / WakeupSchedule_Kotlin](https://github.com/YZune/WakeupSchedule_Kotlin) | 经典 | Kotlin | - | 老牌课程表 App 的 Kotlin 重构版 | 周/今日视图切换交互 |

## “一站式”整合类

| 项目 | 星标 | 语言 | 说明 | 参考价值 |
|---|---|---|---|---|
| [Korosuke](https://github.com/prathamcmd/Korosuke) | 1 | TypeScript | 学生视角 all-in-one：健身计划 + 课程表 + 提醒 + 记账 | 与“健身 + 课表 + 记账”整合思路最接近，但功能浅、非安卓原生，仅作概念参考 |

## 给我们的启发（已应用到本模块设计）

- 收入页：按“月份”聚合总收入 / 总课时 / 上课次数（借鉴 Preceptly / TuitionConnect）。
- 课表页：周一~周日一周视图 + 同一天时间重叠提示（借鉴 TimetableView / TutorFlow 的冲突检测）。
- 数据全部本地存储，默认无网络权限（与 TutorDesk 的 local-first 一致）。
