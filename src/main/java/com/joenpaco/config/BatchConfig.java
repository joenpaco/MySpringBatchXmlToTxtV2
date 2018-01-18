package com.joenpaco.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import com.joenpaco.models.ExamResult;
import com.joenpaco.springbatch.ExamResultItemProcessor;
import com.joenpaco.springbatch.ExamResultJobListener;

@Configuration
@PropertySource(ResourceNames.PROPERTIES)
@ComponentScan(basePackages = { ResourceNames.PACKAGE })
@EnableBatchProcessing
public class BatchConfig {

	@Autowired
	private Environment environment;

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	final static String Step1 = "step1";
	final static Integer size = 10;
	

	@Bean
	public JobRepository jobRepository() throws Exception {
		return new MapJobRepositoryFactoryBean().getObject();
	}

	@Bean
	public SimpleJobLauncher jobLauncher() throws Exception {
		SimpleJobLauncher simpleJobLauncher = new SimpleJobLauncher();
		simpleJobLauncher.setJobRepository(jobRepository());

		return simpleJobLauncher;
	}
	
	//ItemWriter write a line into output flat file
	@Bean
	public FlatFileItemWriter<ExamResult> flatFileItemWriter() {
		FlatFileItemWriter<ExamResult> flatFileItemWriter = new FlatFileItemWriter<ExamResult>(); 
		Resource resource = new FileSystemResource(environment.getProperty("file.target.name"));
		flatFileItemWriter.setResource(resource);
		flatFileItemWriter.setLineAggregator(lineAggregator());
		return flatFileItemWriter;
	}
	
	@Bean
	public DelimitedLineAggregator<ExamResult> lineAggregator() {
		DelimitedLineAggregator<ExamResult> lineAggregator = new DelimitedLineAggregator<ExamResult>();
		lineAggregator.setDelimiter("|");
		lineAggregator.setFieldExtractor(fieldExtractor());
		return lineAggregator;
	}
	
	@Bean
	public BeanWrapperFieldExtractor<ExamResult> fieldExtractor() {
		BeanWrapperFieldExtractor<ExamResult> fieldExtractor = new BeanWrapperFieldExtractor<ExamResult>();
		String[] names = {"studentName", "percentage", "dob"};
		fieldExtractor.setNames(names);
		return fieldExtractor;
	}
	
	//ItemReader which reads data from XML file
	@Bean
	public StaxEventItemReader<ExamResult> xmlItemReader() {
		StaxEventItemReader<ExamResult> xmlItemReader = new StaxEventItemReader<ExamResult>();
		Resource resource = new FileSystemResource(environment.getProperty("file.origin.name"));
		String fragmentRootElementName = "ExamResult";
		xmlItemReader.setResource(resource);
		xmlItemReader.setFragmentRootElementName(fragmentRootElementName);
		xmlItemReader.setUnmarshaller(unmarshaller());
		return xmlItemReader;
	}
	
	@Bean
	public Jaxb2Marshaller unmarshaller() {
		Jaxb2Marshaller unmarshaller = new Jaxb2Marshaller();
		unmarshaller.setClassesToBeBound(ExamResult.class);
		return unmarshaller;
	}
	
//  Optional ItemProcessor to perform business logic/filtering on the input records
  @Bean
	public ExamResultItemProcessor processor() {
		return new ExamResultItemProcessor();
	}
  
//  Optional JobExecutionListener to perform business logic before and after the job
  @Bean
	public ExamResultJobListener jobListener() {
		return new ExamResultJobListener();
	}
  
//  Step will need a transaction manager
  @Bean
	public ResourcelessTransactionManager transactionManager() {
		return new ResourcelessTransactionManager();
	}
  
//  Actual Job
	@Bean
	public Job job() {
		return jobBuilderFactory.get("job").incrementer(new RunIdIncrementer()).listener(jobListener()).flow(step1())
				.end().build();

	}

	@Bean
	public Step step1() {
		return stepBuilderFactory.get(Step1).<ExamResult, ExamResult>chunk(size).reader(xmlItemReader())
				.processor(processor()).writer(flatFileItemWriter()).build();
	}

		
}
