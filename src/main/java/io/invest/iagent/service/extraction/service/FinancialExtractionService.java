package io.invest.iagent.service.extraction.service;

import com.google.common.collect.Lists;
import lombok.Data;
import io.invest.iagent.service.extraction.config.CompanyConfigLoader;
import io.invest.iagent.service.extraction.extractor.DataExtractor;
import io.invest.iagent.service.extraction.mapper.MetricMapper;
import io.invest.iagent.service.extraction.model.*;
import io.invest.iagent.service.extraction.parser.HtmlReportParser;
import io.invest.iagent.service.extraction.parser.ReportParser;
import io.invest.iagent.service.extraction.recognizer.SegmentRecognizer;
import io.invest.iagent.service.extraction.validator.QualityValidator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 财务数据提取主服务
 * 整合所有模块，提供统一的提取接口
 */
@Slf4j
public class FinancialExtractionService {

    private final ReportParser reportParser;
    private final MetricMapper metricMapper;
    private final QualityValidator qualityValidator;
    private final CompanyConfigLoader configLoader;
    private final FinancialFileFilter fileFilter;
    private SegmentRecognizer segmentRecognizer;
    private DataExtractor dataExtractor;
    @Getter
    private CompanyConfig companyConfig;

    public FinancialExtractionService(Path workspace) {
        this.metricMapper = new MetricMapper();
        this.reportParser = new HtmlReportParser();
        this.qualityValidator = new QualityValidator();
        this.fileFilter = new FinancialFileFilter(workspace) ;
        this.configLoader = new CompanyConfigLoader();
    }

    /**
     * 使用指定公司代码初始化
     */
    public FinancialExtractionService(String companyCode,Path workspace) {
        this(workspace);
        this.companyConfig = configLoader.loadConfig(companyCode);
        this.segmentRecognizer = new SegmentRecognizer(companyConfig);
        this.dataExtractor = new DataExtractor(segmentRecognizer, metricMapper);
    }

    /**
     * 使用公司配置初始化
     */
    public FinancialExtractionService(CompanyConfig companyConfig,Path workspace) {
        this(workspace);
        this.companyConfig = companyConfig;
        this.segmentRecognizer = new SegmentRecognizer(companyConfig);
        this.dataExtractor = new DataExtractor(segmentRecognizer, metricMapper);
    }

    public List<Segment> extractFromHtmlFile(String tickerCode,String fiscalYearStart,String fiscalYearEnd) throws IOException {
        List<Path> files = fileFilter.filter(tickerCode, fiscalYearStart, fiscalYearEnd);
        if(CollectionUtils.isEmpty(files)){
            return List.of() ;
        }
        List<Segment> segments = files.stream().map(t-> {
            try {
                return extractFromHtmlFile(t.toFile());
            } catch (IOException e) {
                log.error("extract failed:{}",t.toAbsolutePath(),e);
                return null;
            }
        }).filter(Objects::nonNull).flatMap(List::stream).toList() ;
        return merge(segments);
    }

    private List<Segment> merge(List<Segment> segments){
        if(CollectionUtils.isEmpty(segments)){
            return List.of() ;
        }
        return segments.stream()
                .collect(Collectors.toMap(Segment::getSegmentCode,t->t, this::doMerge))
                .values().stream().toList();
    }

    private Segment doMerge(Segment s1,Segment s2){
        if(Objects.isNull(s1)){
            return s2 ;
        }
        if(Objects.isNull(s2)){
            return s1 ;
        }
        // metrics
        List<SegmentMetric> metrics = Lists.newArrayList();
        if(!CollectionUtils.isEmpty(s1.getMetrics())){
            metrics.addAll(s1.getMetrics()) ;
        }
        if(!CollectionUtils.isEmpty(s2.getMetrics())){
            metrics.addAll(s2.getMetrics()) ;
        }
        // children
        List<Segment> children = Lists.newArrayList();
        if(!CollectionUtils.isEmpty(s1.getChildren())){
            children.addAll(s1.getChildren()) ;
        }
        if(!CollectionUtils.isEmpty(s2.getChildren())){
            children.addAll(s2.getChildren()) ;
        }
        // wrap
        s1.setMetrics(metrics);
        s1.setChildren(children);
        return s1 ;
    }

    /**
     * 从HTML文件中提取财务数据
     */
    public List<Segment> extractFromHtmlFile(File htmlFile) throws IOException {
        log.info("Extracting financial data from HTML file: {}", htmlFile.getName());
        
        // 1. 解析HTML，提取表格
        List<FinancialTable> tables = reportParser.parse(htmlFile);
        log.info("Parsed file {} financial tables", tables.size());
        
        // 2. 从表格中提取分部数据
        List<Segment> segments = dataExtractor.extractFromMultipleTables(tables);
        log.info("Extracted file {} segments with financial data", segments.size());
        
        return segments;
    }

    /**
     * 从HTML内容字符串中提取财务数据
     */
    public List<Segment> extractFromHtmlContent(String htmlContent) {
        log.info("Extracting financial data from HTML content, length: {}", htmlContent.length());
        
        // 1. 解析HTML，提取表格
        List<FinancialTable> tables = reportParser.parseHtml(htmlContent);
        log.info("Parsed {} financial tables", tables.size());
        
        // 2. 从表格中提取分部数据
        List<Segment> segments = dataExtractor.extractFromMultipleTables(tables);
        log.info("Extracted {} segments with financial data", segments.size());
        
        return segments;
    }

    /**
     * 提取并进行质量校验
     */
    public ExtractionResult extractAndValidate(File htmlFile) throws IOException {
        List<Segment> segments = extractFromHtmlFile(htmlFile);
        ValidationResult validationResult = qualityValidator.validate(segments);
        
        ExtractionResult result = new ExtractionResult();
        result.setSegments(segments);
        result.setValidationResult(validationResult);
        result.setCompanyConfig(companyConfig);
        
        return result;
    }

    public void setCompanyConfig(CompanyConfig companyConfig) {
        this.companyConfig = companyConfig;
        this.segmentRecognizer = new SegmentRecognizer(companyConfig);
        this.dataExtractor = new DataExtractor(segmentRecognizer, metricMapper);
    }

    /**
     * 提取结果封装
     */
    @Data
    public static class ExtractionResult {
        private List<Segment> segments;
        private ValidationResult validationResult;
        private CompanyConfig companyConfig;
    }
}
