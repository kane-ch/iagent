# Financial Metrics Reference

## 1. 基础指标

当前 `query_financial_metrics` 工具稳定支持以下基础利润表指标。查询工具时应使用 Canonical metric。

| Canonical metric | 中文名 | 常见说法 | 说明 |
|---|---|---|---|
| Revenue | 收入 | 营收、销售额、sales、revenues | 营业收入 |
| CostOfRevenue | 成本 | 收入成本、销售成本、COGS、cost | 与收入直接相关的成本 |
| GrossProfit | 毛利润 | 毛利 | 收入减去销售成本后的利润 |
| OperatingExpenses | 营业费用 | 经营费用、opex | 日常经营费用总额 |
| OperatingIncomeLoss | 经营利润 | 营业利润、营业损益、operating income/loss | 核心经营利润或亏损 |
| NetIncomeLoss | 净利润 | 净收益、净损益、net income/loss | 最终净利润或亏损 |

可选利润表指标。如果工具返回这些指标，也可以用于分析：

| Canonical metric | 中文名 | 说明 |
|---|---|---|
| AdvertisingExpense | 广告费用 | 广告、营销相关费用 |
| GeneralAndAdministrativeExpense | 管理费用 | 一般及行政费用 |
| ResearchAndDevelopmentExpense | 研发费用 | 研发投入 |
| SellingGeneralAndAdministrativeExpense | 销售及管理费用 | SG&A |
| IncomeTaxExpenseBenefit | 所得税费用/收益 | 当期所得税费用或税收收益 |
| EarningsPerShareBasic | 基本每股收益 | 未稀释每股收益 |
| EarningsPerShareDiluted | 稀释每股收益 | 稀释后每股收益 |

## 2. 加工指标

加工指标不应直接传给 `query_financial_metrics`。应先查询依赖的基础指标，再计算。

| Derived metric | 中文名 | 公式 | 依赖基础指标 |
|---|---|---|---|
| GrossMargin | 毛利率 | GrossProfit / Revenue | GrossProfit, Revenue |
| CostOfRevenueRatio | 成本率 | CostOfRevenue / Revenue | CostOfRevenue, Revenue |
| OperatingExpenseRatio | 营业费用率 | OperatingExpenses / Revenue | OperatingExpenses, Revenue |
| OperatingProfitMargin | 经营利润率 | OperatingIncomeLoss / Revenue | OperatingIncomeLoss, Revenue |
| NetProfitMargin | 净利率 | NetIncomeLoss / Revenue | NetIncomeLoss, Revenue |
| ROE | 净资产收益率 | NetIncomeLoss / AverageStockholdersEquity | NetIncomeLoss, StockholdersEquity |
| CurrentRatio | 流动比率 | AssetsCurrent / LiabilitiesCurrent | AssetsCurrent, LiabilitiesCurrent |

### 毛利润兜底计算

如果 `GrossProfit` 缺失，但同一公司、同一财年、同一财期、同一币种下存在 `Revenue` 和 `CostOfRevenue`，可以计算：

```text
GrossProfit = Revenue - CostOfRevenue
GrossMargin = (Revenue - CostOfRevenue) / Revenue
```

### ROE

ROE 推荐使用平均股东权益：

```text
AverageStockholdersEquity = (StockholdersEquity_current + StockholdersEquity_prior) / 2
ROE = NetIncomeLoss / AverageStockholdersEquity
```

如果只有当期股东权益，且用户接受简化口径，可以说明口径后计算：

```text
ROE_simple = NetIncomeLoss / StockholdersEquity_current
```

当前工具映射不保证稳定返回 `StockholdersEquity`。如果缺失，不要估算 ROE。

### 流动比率

```text
CurrentRatio = AssetsCurrent / LiabilitiesCurrent
```

当前工具映射不保证稳定返回 `AssetsCurrent` 和 `LiabilitiesCurrent`。如果缺失，不要估算流动比率。

## 3. 同比增长率

对任一基础指标或可计算的加工指标，按同一财期计算同比：

```text
YoY = (CurrentPeriodValue - PriorYearSamePeriodValue) / abs(PriorYearSamePeriodValue)
```

示例：

- FY2025 Revenue YoY：比较 FY2025 Revenue 与 FY2024 Revenue。
- Q3 2025 Revenue YoY：比较 Q3 2025 Revenue 与 Q3 2024 Revenue。

规则：

1. 只比较同一财期，不要用 Q3 比 FY，也不要用 Q3 比 Q2。
2. 上年同期缺失时，YoY 不可计算。
3. 上年同期为 0 时，YoY 不可计算。
4. 上年同期为负值时，可以计算，但应提示“负基数下同比解释性较弱”。
5. 如果用户要求 2022–2025 的同比，工具查询应从 2021 开始，但输出默认只展示 2022–2025。

## 4. 查询依赖表

| 用户想要的指标 | 应查询的基础指标 |
|---|---|
| 收入、收入同比 | Revenue |
| 成本、成本同比 | CostOfRevenue |
| 毛利润 | GrossProfit；若缺失则 Revenue, CostOfRevenue |
| 毛利率 | GrossProfit, Revenue；兜底 Revenue, CostOfRevenue |
| 营业费用率 | OperatingExpenses, Revenue |
| 经营利润率 | OperatingIncomeLoss, Revenue |
| 净利率 | NetIncomeLoss, Revenue |
| ROE | NetIncomeLoss, StockholdersEquity |
| 流动比率 | AssetsCurrent, LiabilitiesCurrent |

## 5. 输出格式建议

金额类指标：

| 财年 | 财期 | 指标 | 值 | 同比 | 来源 |
|---|---|---|---|---|---|

比率类指标：

| 财年 | 财期 | 指标 | 百分比 | 同比 | 计算口径 |
|---|---|---|---|---|---|

格式规则：

- 金额保留工具返回的币种，例如 CNY、USD。
- 百分比通常保留 1–2 位小数。
- 每个加工指标应能追溯到基础指标。
- 缺失数据用“不可计算：缺少 xxx”说明。
