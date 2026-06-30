package io.invest.iagent.skill;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.harness.agent.HarnessAgent;
import io.invest.AgentConfig4Test;
import io.invest.iagent.service.extraction.model.Segment;
import io.invest.iagent.service.extraction.model.SegmentMetricDTO;
import io.invest.iagent.tools.filing.FinancialSegmentMetricsTool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Assert;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@SpringBootTest(classes = AgentConfig4Test.class)
@TestPropertySource(locations = "classpath:test.properties")
public class SegmentFinancialReportSkillTest {

    @Autowired
    private HarnessAgent baseAgent ;

    private RuntimeContext context;



    @BeforeEach
    public void init() {
    }

    @Test
    public void test_excel() {
        String companyName = "阿里巴巴";
        String template = """
                生成公司[%s]所有分部的财务报表，
                执行流程如下：
                1、调用工具get_stock_ticker获取公司的股票代码。
                2、调用工具export_segment_financial_excel生成财务报表。
                3、检查财务报表文件是否创建成功。
                特别注意：
                1、严格禁止只输出执行方式，但不去真正执行。
                2、严格禁止不通过get_stock_ticker工具获取股票代码。
                """;

        Msg qaMsg = this.buildUserMsg(String.format(template, companyName));
        Msg response = baseAgent.call(qaMsg).block();
        String responseText = Objects.requireNonNull(response).getTextContent();
        Assert.notNull(responseText, "question response");
    }

    private Msg buildUserMsg(String content){
        return Msg.builder()
                .role(MsgRole.USER)
                .textContent(content)
                .build();
    }

    @Test
    public void test_tool_baba() {
        Path workspace = Paths.get(System.getProperty("user.dir")).resolve("workspace");
        FinancialSegmentMetricsTool tool = new FinancialSegmentMetricsTool(workspace);
        List<SegmentMetricDTO> segments = tool.queryFinancialMetricsFlatter("BABA") ;
        Assertions.assertNotNull(segments);
    }

}
