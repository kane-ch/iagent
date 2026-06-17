---
name: filing-kb-qa
description: 基于财报知识库回答用户问题。适用于用户追问经营利润、收入、成本、费用、风险因素、管理层讨论、下降原因、增长原因、业务变化原因等需要从知识库检索证据后再由 LLM 汇总回答的问题。
---

# Filing Knowledge Base QA

## 功能说明

当用户的问题需要从已构建的财报知识库中检索证据，并基于检索结果进行总结回答时，使用本 skill。

典型问题：

- “经营利润下降的原因是什么？”
- “收入增长主要来自哪些业务？”
- “成本上升的原因是什么？”
- “管理层如何解释毛利率下降？”
- “财报中提到哪些风险因素？”
- “这家公司 2025 年业绩变化的原因是什么？”

本 skill 的目标是：

1. 理解用户问题。
2. 生成适合知识库检索的 query。
3. 调用 `retrieve_filing_kb` 获取相关财报片段。
4. 基于检索到的片段进行归纳总结。
5. 输出答案时标明依据和引用信息。

## 可用工具

优先调用：`retrieve_filing_kb`

工具参数：

- `query`：检索问题或关键词组合。
- `ticker`：可选，股票代码过滤，例如 `AAPL`、`PDD`、`LI`。
- `top_k`：可选，返回条数，默认 5。复杂问题建议 8–12。
- `fiscal_year`：可选，财年过滤，例如 `2025`。
- `form_type`：可选，表单类型过滤，例如 `10-K`、`20-F`、`6-K`。

如果用户明确要求构建或维护知识库，可以使用：

- `preprocess_filing_kb`
- `build_filing_kb`
- `sync_filing_kb`
- `list_filing_kb`
- `delete_filing_kb`

但普通问答优先只用 `retrieve_filing_kb`。

## 资源列表

- `${workspace}/skills/filing_kb_qa/references/query_patterns.md`：检索 query 构造规则和示例。
- `${workspace}/skills/filing_kb_qa/references/answer_format.md`：回答格式、引用和不确定性处理规范。

## 执行流程

### 1. 判断是否需要知识库检索

当用户问题涉及“原因、解释、管理层讨论、风险、策略、业务变化、竞争、成本费用变化、毛利率变化、经营利润变化”等定性分析时，应使用知识库检索。

如果用户只是要具体数值，例如“2025 年收入是多少”，优先使用财务指标查询工具，而不是知识库检索。

### 2. 识别过滤条件

从用户问题中抽取：

- 公司或 ticker。
- 财年或时间段。
- 表单类型，如果用户明确指定，如年报、季报、20-F、10-K、6-K。
- 主题，例如经营利润下降、成本上升、收入增长、风险因素。

如果用户没有提供 ticker，但提供公司名称，先使用股票查询工具解析 ticker。

### 3. 构造检索 query

query 应包含：

- 用户原问题的关键词。
- 同义表达。
- 可能出现在财报中的英文/中文词。
- 与财务指标相关的 driver 词。

示例：

用户问：

> LI 经营利润下降的原因是什么？

可构造：

```text
经营利润下降 原因 operating profit decline operating income loss margin cost expenses gross margin R&D sales marketing management discussion
```

用户问：

> 收入增长主要来自哪里？

可构造：

```text
收入增长 原因 revenue growth increase sales growth product mix delivery volume pricing business segment management discussion
```

### 4. 调用 `retrieve_filing_kb`

建议：

- 简单问题：`top_k=5`
- 原因分析：`top_k=8`
- 跨多个主题或多年：`top_k=10–12`

如果用户指定公司和年份：

```text
retrieve_filing_kb(
  query="经营利润下降 原因 成本 费用 毛利率 管理层讨论 operating profit decline",
  ticker="LI",
  top_k=8,
  fiscal_year="2025",
  form_type=null
)
```

### 5. 基于检索结果回答

回答必须基于工具返回的 chunks，不要脱离证据自由发挥。

组织方式：
``
1. 先给结论摘要。
2. 分点说明主要原因。
3. 每个原因后附依据，例如 document_id、form_type、filing_date、sectionTitle 或 sourceFileName。
4. 如果检索结果不足，明确说明“当前知识库未检索到足够证据”。

### 6. 处理检索为空

如果 `retrieve_filing_kb` 返回空：

- 不要编造。
- 回答用户：当前知识库没有检索到相关片段。
- 可建议：先调用 `build_filing_kb` 或 `sync_filing_kb` 构建/同步该公司的知识库。

## 注意事项

- 本 skill 不直接下载财报。若知识库未构建，应提示用户先构建或调用构建工具。
- 不要把知识库检索当成精确数值查询。数值查询应使用财务指标工具。
- 如果用户问题需要“数值变化 + 原因解释”，应先用财务指标工具确认变化，再用 `retrieve_filing_kb` 查找原因。
- 答案必须区分“财报明确提到的原因”和“基于检索片段推断的可能原因”。
- 检索结果之间存在冲突时，应说明冲突，不要强行合并成确定结论。
- 输出时应尽量引用 source/citation 信息，方便用户追溯。
