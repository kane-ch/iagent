package io.invest.iagent.service.extraction.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 公司配置
 * 包含公司的业务线定义、指标映射规则等
 */
@Data
public class CompanyConfig {

    private String companyCode;
    private String companyName;
    private String market;
    private String defaultCurrency;
    private String defaultUnit;
    private List<SegmentConfig> segments;
    private List<MetricMappingRule> metricMappingRules;

    public CompanyConfig() {
        this.segments = new ArrayList<>();
        this.metricMappingRules = new ArrayList<>();
    }

    /**
     * 业务分部配置
     */
    @Data
    public static class SegmentConfig {
        private String segmentCode;
        private String segmentName;
        private List<String> aliases;
        private int level;
        private String parentCode;

        public SegmentConfig() {
            this.aliases = new ArrayList<>();
        }

        /**
         * 检查文本是否匹配该分部
         */
        public boolean matches(String text) {
            if (text == null) {
                return false;
            }
            String lowerText = text.toLowerCase().trim();
            if (segmentName != null && lowerText.contains(segmentName.toLowerCase())) {
                return true;
            }
            if (segmentCode != null && lowerText.contains(segmentCode.toLowerCase())) {
                return true;
            }
            for (String alias : aliases) {
                if (lowerText.contains(alias.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 指标映射规则
     */
    @Data
    public static class MetricMappingRule {
        private String standardMetricCode;
        private List<String> rawMetricNames;
        private String formula;

        public MetricMappingRule() {
            this.rawMetricNames = new ArrayList<>();
        }
    }
}
