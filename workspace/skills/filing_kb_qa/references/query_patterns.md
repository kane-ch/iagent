# KB Retrieval Query Patterns

## 原理

`retrieve_filing_kb` 使用向量相似度搜索。查询效果取决于 query 文本与 chunk 文本的语义重叠。

构造一个好的 query 时，应同时考虑：

- 用户原始问题中的关键词（中文）。
- 对应的英文同义词。
- 财报典型的叙述方式和术语。
- 可能相关的财报段落类型，如：
  - MD&A / 管理层讨论与分析
  - Results of Operations / 经营业绩
  - Risk Factors / 风险因素
  - Business Overview / 业务概览
  - Cost / 成本
  - Revenue / 收入
  - Segment / 分部
  - Outlook / 前景

## 构建公式

```text
中文关键词 + 英文关键词 + 财报术语 + driver 词
```

## 示例

### 经营利润下降原因

用户问：

> 为什么经营利润下降了？

推荐 query：

```text
经营利润下降 原因 成本 费用 毛利率 研发投入 销售费用 管理费用 价格策略 竞争 operating profit decline net income loss operating income margin cost increase expense research development sales marketing
```

### 收入增长原因

用户问：

> 收入为什么增长？

推荐 query：

```text
收入增长 原因 drivers revenue growth sales volume increase pricing mix business segment product channel market demand
```

### 成本上升

用户问：

> 成本上升的原因是什么？

推荐 query：

```text
成本上升 原因 原材料 人工 采购 供应链 物流 履约成本 制造费用 cost increase materials labor logistics fulfillment manufacturing supply chain
```

### 毛利率变化

用户问：

> 毛利率为什么下降了？

推荐 query：

```text
毛利率下降 原因 成本 价格 产品结构 定价 促销 竞争 原材料成本 gross margin decline cost increase pricing product mix promotion
```

### 管理层讨论

用户问：

> 管理层如何看今年的业绩？

推荐 query：

```text
管理层讨论 业绩 经营 回顾 outlook 2025 management discussion results of operations financial performance review outlook seasonality trends
```

### 风险因素

用户问：

> 有哪些风险因素？

推荐 query：

```text
风险因素 市场风险 竞争 监管 技术风险 宏观经济 政策风险 risk factors regulatory competition technology macroeconomy market geopolitical
```

### 研发投入

用户问：

> 研发费用增加了多少？

注意：如果只是要精确数值，应优先使用 `query_financial_metrics`。如果用户问的是“研发投入方向”、“研发投入的策略原因”，则使用知识库检索。

```text
研发 研究 development R&D investment strategy focus areas product development innovation technology platform
```

### 综合问题

用户问：

> LI 2025 年为什么亏损了？是成本还是费用导致的？

```text
经营利润下降 亏损 成本收入比 费用 研发 销售 管理 毛利率 经营利润率 net loss operating loss revenue cost expense operating margin
```

加过滤条件：

```text
ticker="LI", fiscal_year="2025"
```

### 跨年度对比原因

用户问：

> 为什么 2025 年比 2024 年利润少了？

需要 query 涵盖可能的对比方向：

```text
利润变化 同比 年度对比 2024 2025 成本 费用 毛利率 非经常性损益 profit change year-over-year comparison cost expense gross margin non-recurring
```

可用多个 ticker 或跨年 chunks 对比分析。

## 特殊情况处理

### 4.1 无明确主题

当用户问题比较散时，选择最常见的财报分析主题作为默认：

```text
经营业绩 收入 成本 费用 利润 管理层讨论 results of operations revenue cost expenses profit MD&A
```

### 4.2 中文为主或英文为主

- 中文发生的问题用中英文混合 query。
- 英文原文的财报 chunk 因为 embedding 是中文+英文混合，中英文混合 query 效果更好。

### 4.3 ticker 明确的公司

如果用户给了 ticker，使用：

- `ticker` 过滤，避免其他公司的干扰。
- `fiscal_year` 过滤，避免年份不匹配。
- `form_type` 过滤（如 10-K、20-F），如果问题涉及特定财报类型。