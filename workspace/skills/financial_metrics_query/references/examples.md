# Financial Metrics Query Examples

## 示例 1：年度基础指标、利润率和同比

用户：

> 查询 AAPL 2022 到 2025 年收入、成本、营业利润、毛利率、营业利润率和同比增长。

分析：

1. 用户要求年度数据，财期使用 `FY`。
2. 需要展示 2022–2025，但要计算 2022 的同比，需要查询 2021 数据作为上一年基数。
3. 加工指标依赖：
   - 毛利率：`GrossProfit,Revenue`，或 `Revenue,CostOfRevenue` 兜底。
   - 营业利润率：`OperatingIncomeLoss,Revenue`。
4. 工具只查询基础指标。

工具调用：

```text
query_financial_metrics(
  ticker="AAPL",
  metrics="Revenue,CostOfRevenue,GrossProfit,OperatingIncomeLoss",
  start_fiscal_year="2021",
  end_fiscal_year="2025",
  fiscal_periods="FY",
  source_preference="auto"
)
```

后处理：

```text
GrossMargin = GrossProfit / Revenue
OperatingProfitMargin = OperatingIncomeLoss / Revenue
RevenueYoY = (Revenue_current - Revenue_prior_year_same_period) / abs(Revenue_prior_year_same_period)
CostYoY = (CostOfRevenue_current - CostOfRevenue_prior_year_same_period) / abs(CostOfRevenue_prior_year_same_period)
OperatingIncomeYoY = (OperatingIncomeLoss_current - OperatingIncomeLoss_prior_year_same_period) / abs(OperatingIncomeLoss_prior_year_same_period)
```

展示：

- 默认只展示 2022–2025。
- 2021 仅作为同比基数，不展示，除非用户要求。

## 示例 2：季度基础指标和同比

用户：

> 查询 PDD 2025 年 Q3 收入、成本、营业费用、净利润和同比。

分析：

1. 用户指定 Q3。
2. 需要 2025 Q3 和 2024 Q3 才能计算同比。
3. PDD 这类中概股季度数据可能来自 6-K 本地文件，因此默认使用 `auto`。

工具调用：

```text
query_financial_metrics(
  ticker="PDD",
  metrics="Revenue,CostOfRevenue,OperatingExpenses,NetIncomeLoss",
  start_fiscal_year="2024",
  end_fiscal_year="2025",
  fiscal_periods="Q3",
  source_preference="auto"
)
```

后处理：

```text
RevenueYoY = (Revenue_2025_Q3 - Revenue_2024_Q3) / abs(Revenue_2024_Q3)
CostOfRevenueYoY = (CostOfRevenue_2025_Q3 - CostOfRevenue_2024_Q3) / abs(CostOfRevenue_2024_Q3)
OperatingExpensesYoY = (OperatingExpenses_2025_Q3 - OperatingExpenses_2024_Q3) / abs(OperatingExpenses_2024_Q3)
NetIncomeLossYoY = (NetIncomeLoss_2025_Q3 - NetIncomeLoss_2024_Q3) / abs(NetIncomeLoss_2024_Q3)
```

## 示例 3：所有季度的经营利润率

用户：

> 查询 AAPL 2025 年四个季度的经营利润率。

分析：

1. 财期使用 `Q1,Q2,Q3,Q4`。
2. 经营利润率依赖 `OperatingIncomeLoss,Revenue`。
3. 不需要同比时，不要额外查询 2024。

工具调用：

```text
query_financial_metrics(
  ticker="AAPL",
  metrics="OperatingIncomeLoss,Revenue",
  start_fiscal_year="2025",
  end_fiscal_year="2025",
  fiscal_periods="Q1,Q2,Q3,Q4",
  source_preference="auto"
)
```

后处理：

```text
OperatingProfitMargin = OperatingIncomeLoss / Revenue
```

## 示例 4：净利率和净利润同比

用户：

> 查询 LI 2023 到 2025 年净利率和净利润同比。

分析：

1. 默认年度数据，财期使用 `FY`。
2. 需要计算 2023 的同比，因此工具查询从 2022 开始。
3. 净利率依赖 `NetIncomeLoss,Revenue`。

工具调用：

```text
query_financial_metrics(
  ticker="LI",
  metrics="NetIncomeLoss,Revenue",
  start_fiscal_year="2022",
  end_fiscal_year="2025",
  fiscal_periods="FY",
  source_preference="auto"
)
```

后处理：

```text
NetProfitMargin = NetIncomeLoss / Revenue
NetIncomeLossYoY = (NetIncomeLoss_current - NetIncomeLoss_prior_year_same_period) / abs(NetIncomeLoss_prior_year_same_period)
NetProfitMarginYoY = (NetProfitMargin_current - NetProfitMargin_prior_year_same_period) / abs(NetProfitMargin_prior_year_same_period)
```

## 示例 5：ROE 和流动比率不可计算时的回答

用户：

> 查询 AAPL 2024 到 2025 年 ROE 和流动比率。

分析：

ROE 需要：

- `NetIncomeLoss`
- `StockholdersEquity`，最好还需要上一期股东权益用于平均权益。

流动比率需要：

- `AssetsCurrent`
- `LiabilitiesCurrent`

工具调用可尝试：

```text
query_financial_metrics(
  ticker="AAPL",
  metrics="NetIncomeLoss,StockholdersEquity,AssetsCurrent,LiabilitiesCurrent",
  start_fiscal_year="2023",
  end_fiscal_year="2025",
  fiscal_periods="FY",
  source_preference="auto"
)
```

如果工具结果未返回 `StockholdersEquity`、`AssetsCurrent`、`LiabilitiesCurrent`，回答：

> 当前工具返回的数据中缺少计算 ROE 所需的股东权益，以及计算流动比率所需的流动资产和流动负债。因此无法可靠计算 ROE 和流动比率。为避免误导，不使用估算值或编造数据。

## 示例 6：用户只给公司名称

用户：

> 查询苹果公司 2024 到 2025 年收入和毛利率。

执行：

1. 先用股票查询工具解析“苹果公司”为 `AAPL`。
2. 再调用：

```text
query_financial_metrics(
  ticker="AAPL",
  metrics="Revenue,GrossProfit,CostOfRevenue",
  start_fiscal_year="2024",
  end_fiscal_year="2025",
  fiscal_periods="FY",
  source_preference="auto"
)
```

后处理：

```text
GrossMargin = GrossProfit / Revenue
```

如果 `GrossProfit` 缺失：

```text
GrossMargin = (Revenue - CostOfRevenue) / Revenue
```

## 示例 7：Q4 数据推断（FY - Q1-Q2-Q3）

用户：

> 查询 LI 2025 年四个季度的收入、经营利润、同比变化。

分析：

1. 用户要求所有四个季度，财期使用 `Q1,Q2,Q3,Q4`。
2. 如果工具返回了 `FY`（全年）和 `Q1`、`Q2`、`Q3`，但缺少 `Q4`，可以通过 `FY - Q1 - Q2 - Q3` 推算 Q4。
3. 需要同时查询全年数据作为推断基准。

工具调用：

```text
query_financial_metrics(
  ticker="LI",
  metrics="Revenue,OperatingIncomeLoss",
  start_fiscal_year="2024",
  end_fiscal_year="2025",
  fiscal_periods="FY,Q1,Q2,Q3,Q4",
  source_preference="auto"
)
```

后处理（Q4 推断逻辑）：

```text
# 当 Q4 数据缺失但 FY、Q1、Q2、Q3 都存在时
if exists(FY) and exists(Q1) and exists(Q2) and exists(Q3) and not exists(Q4):
    Revenue_Q4_calculated = Revenue_FY - Revenue_Q1 - Revenue_Q2 - Revenue_Q3
    OperatingIncomeLoss_Q4_calculated = OperatingIncomeLoss_FY - OperatingIncomeLoss_Q1 - OperatingIncomeLoss_Q2 - OperatingIncomeLoss_Q3
    
    # 标注为推算值
    Output: "Q4（推算值，FY - Q1 - Q2 - Q3）"
else:
    Output: "Q4 数据不可用（缺少全年或季度数据用于推断）"
```

推断数据的同比计算：

```text
# 如果有上一年 Q4 实际数据
RevenueYoY_Q4 = (Revenue_Q4_calculated - Revenue_Q4_2024) / abs(Revenue_Q4_2024)

# 如果上一年 Q4 也缺失但有上一年 FY/Q1/Q2/Q3，则同样推算上一年 Q4
if exists(FY_2024) and exists(Q1_2024) and exists(Q2_2024) and exists(Q3_2024) and not exists(Q4_2024):
    Revenue_Q4_2024_calculated = Revenue_FY_2024 - Revenue_Q1_2024 - Revenue_Q2_2024 - Revenue_Q3_2024
    RevenueYoY_Q4 = (Revenue_Q4_calculated - Revenue_Q4_2024_calculated) / abs(Revenue_Q4_2024_calculated)
```
