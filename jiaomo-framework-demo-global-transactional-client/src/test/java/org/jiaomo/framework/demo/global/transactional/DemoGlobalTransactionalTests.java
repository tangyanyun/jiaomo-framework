/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jiaomo.framework.demo.global.transactional;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jiaomo.framework.DemoGlobalTransactionalClientApplication;

import org.jiaomo.framework.autoconfigure.metrics.adapter.MetricsAdapter;
import org.jiaomo.framework.commons.exception.BusinessException;
import org.jiaomo.framework.commons.function.ThrowingRunnable;
import org.jiaomo.framework.commons.function.ThrowingSupplier;
import org.jiaomo.framework.commons.jackson.JsonJacksonSerializer;
import org.jiaomo.framework.demo.global.transactional.service.application.DemoLocalApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/// 测试请首先启动如下服务：
/// 1.yuanqi-framework-demo-global-transactional-eureka
/// 2.yuanqi-framework-demo-global-transactional-server
/// 3.yuanqi-framework-demo-global-transactional-client

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {DemoGlobalTransactionalClientApplication.class})
@TestPropertySource(properties = {
		"server.port=8764",
		"eureka.client.register-with-eureka=false",
		"metrics.enabled=true",
		"metrics.report.output-file=metrics_test.log"
})
@Slf4j
@Disabled
public class DemoGlobalTransactionalTests {
	private static final String ACCOUNT_NUMBER = "6230580000123456789";

	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DemoLocalApplicationService demoLocalApplicationService;

	@AfterAll
	public static void afterAll() {
		ThrowingRunnable.runWithoutThrowing(MetricsAdapter::report);
		ThrowingRunnable.run(() -> Thread.sleep(1000L));
	}

	@BeforeEach
	@AfterEach
	public void beforeEach() {
		log.info("--------------------");
	}

	@Test
	//本地 ApplicationService具有 @GlobalTransactional 注解，DomainService 和 LocalDomainService 实现类的方法上使用 @AutonomousTransactional 注解
	//1.demoLocalApplicationService.tx01WithAnnotation(本地ApplicationService) 使用 @GlobalTransactional
	//2.demoGlobalTransactionalFacade.tccTry 调用 demoGlobalTransactionalDomainService.tccTryWithAnnotation,其具有 @AutonomousTransactional，Facade上无分布式事务注解
	//3.demoLocalDomainService.tryTxWithAnnotation 具有 @AutonomousTransactional	@Test
	public void test01() {
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test01");
		demoLocalApplicationService.tx01WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test01 amount:{}",amount);
		log.info("test01 status:{}",status);
		log.info("test01 remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(11L)));
		Assertions.assertEquals((byte)1,status);
		Assertions.assertEquals(ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test02() {//本地 ApplicationService具有 GlobalTransactional 注解，Facade 和 LocalDomainService 实现类的方法上使用 AutonomousTransactional 注解
		//1.demoLocalApplicationService.tx02WithAnnotation(本地ApplicationService) 使用 @GlobalTransactional
		//2.demoGlobalTransactionalWithAnnotationFacade.tccTryWithAnnotation0 调用 demoGlobalTransactionalDomainService.tccTry, Facade具有 @AutonomousTransactional
		//3.demoLocalDomainService.tryTxWithAnnotation 具有 @AutonomousTransactional 注解
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test02");
		demoLocalApplicationService.tx02WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test02 amount:{}",amount);
		log.info("test02 status:{}",status);
		log.info("test02 remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(11L)));
		Assertions.assertEquals((byte)1,status);
		Assertions.assertEquals(ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test03() {//本地 ApplicationService具有 GlobalTransactional 注解，Facade,DomainService 和 LocalDomainService 实现类的方法上均使用 @AutonomousTransactional 注解
		//1.demoLocalApplicationService.tx03WithAnnotation(本地ApplicationService) 使用 @GlobalTransactional
		//2.demoGlobalTransactionalWithAnnotationFacade.tccTryWithAnnotation 调用 demoGlobalTransactionalDomainService.tccTryWithAnnotation  Facade和DomainService上均有@AutonomousTransactional注解(其中DomainService上的分布式事务注解被忽略)
		//3.demoLocalDomainService.tryTxWithAnnotation 具有 @AutonomousTransactional 注解
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test03");
		demoLocalApplicationService.tx03WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test03 amount:{}",amount);
		log.info("test03 status:{}",status);
		log.info("test03 remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(11L)));
		Assertions.assertEquals((byte)1,status);
		Assertions.assertEquals(ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	//本地 ApplicationService具有 @GlobalTransactional 注解，DomainService 和 LocalDomainService 实现类的方法上使用 @AutonomousTransactional 注解
	//1.demoLocalApplicationService.tx01WithAnnotation(本地ApplicationService) 使用 @GlobalTransactional
	//2.demoGlobalTransactionalFacade.tccTry 调用 demoGlobalTransactionalDomainService.tccTryWithAnnotation,其具有 @AutonomousTransactional，Facade上无分布式事务注解
	//3.demoLocalDomainService.tryTxWithAnnotation 具有 @AutonomousTransactional	@Test
	public void test04() {
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test04");
		Assertions.assertThrows(RuntimeException.class,() -> demoLocalApplicationService.tx01WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,4L));
		MetricsAdapter.stop(time);

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test04 amount:{}",amount);
		log.info("test04 status:{}",status);
		log.info("test04 remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.TEN));
		Assertions.assertEquals((byte)4,status);
		Assertions.assertEquals("cancelTx" + ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test05() {//本地 ApplicationService具有 GlobalTransactional 注解，Facade 和 LocalDomainService 实现类的方法上使用 AutonomousTransactional 注解
		//1.demoLocalApplicationService.tx02WithAnnotation(本地ApplicationService) 使用 @GlobalTransactional
		//2.demoGlobalTransactionalWithAnnotationFacade.tccTryWithAnnotation0 调用 demoGlobalTransactionalDomainService.tccTry, Facade具有 @AutonomousTransactional
		//3.demoLocalDomainService.tryTxWithAnnotation 具有 @AutonomousTransactional 注解
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test05");
		Assertions.assertThrows(RuntimeException.class,() -> demoLocalApplicationService.tx02WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,4L));
		MetricsAdapter.stop(time);

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test05 amount:{}",amount);
		log.info("test05 status:{}",status);
		log.info("test05 remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.TEN));
		Assertions.assertEquals((byte)4,status);
		Assertions.assertEquals("cancelTx" + ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test06() {//本地 ApplicationService具有 GlobalTransactional 注解，Facade,DomainService 和 LocalDomainService 实现类的方法上均使用 @AutonomousTransactional 注解
		//1.demoLocalApplicationService.tx03WithAnnotation(本地ApplicationService) 使用 @GlobalTransactional
		//2.demoGlobalTransactionalWithAnnotationFacade.tccTryWithAnnotation 调用 demoGlobalTransactionalDomainService.tccTryWithAnnotation  Facade和DomainService上均有@AutonomousTransactional注解(其中DomainService上的分布式事务注解被忽略)
		//3.demoLocalDomainService.tryTxWithAnnotation 具有 @AutonomousTransactional 注解
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test06");
		Assertions.assertThrows(RuntimeException.class,() -> demoLocalApplicationService.tx03WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,4L));
		MetricsAdapter.stop(time);

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test06 amount:{}",amount);
		log.info("test06 status:{}",status);
		log.info("test06 remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.TEN));
		Assertions.assertEquals((byte)4,status);
		Assertions.assertEquals("cancelTx" + ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test07() {
		//1.DemoLocalApplicationService.tx07WithAnnotation 具有 @GlobalTransactional
		//2.demoGlobalTransactionalFacade.appTran, Facade无分布式事务注解，其调用demoGlobalTransactionalApplicationService.appTranWithAnnotation具有@GlobalTransactional,为REQUIRED被忽略
		//3.demoGlobalTransactionalDomainService.tccTryWithAnnotation 具有 @AutonomousTransactional
		//4.demoGlobalTransactionalDomainService.tccTryWithAnnotation 具有 @AutonomousTransactional
		//5.demoLocalDomainService.tryTxWithAnnotation 具有 @AutonomousTransactional 注解

		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test07");
		demoLocalApplicationService.tx07WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test01 amount:{}",amount);
		log.info("test01 status:{}",status);
		log.info("test01 remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(12L)));
		Assertions.assertEquals((byte)1,status);
		Assertions.assertEquals(ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test08() {
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test08");
		demoLocalApplicationService.tx08WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test08 amount:{}",amount);
		log.info("test08 status:{}",status);
		log.info("test08 remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(12L)));
		Assertions.assertEquals((byte)1,status);
		Assertions.assertEquals(ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test09() {
		//demoLocalDomainService.tryTxWithAnnotation无分布式事务，confirmTx不会被调用
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test09");
		demoLocalApplicationService.tx09(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test09 amount:{}",amount);
		log.info("test09 status:{}",status);
		log.info("test09 remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(12L)));
		Assertions.assertEquals((byte)1,status);
		Assertions.assertEquals("",remark);
	}

	@Test
	public void test10() {
		//demoLocalDomainService.tryTxWithAnnotation抛出异常
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test10");
		Assertions.assertThrows(RuntimeException.class,() -> demoLocalApplicationService.tx10WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,4L));
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test10 amount:{}",amount);
		log.info("test10 status:{}",status);
		log.info("test10 remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.TEN));
		Assertions.assertEquals((byte)4,status);
		Assertions.assertEquals("cancelTx" + ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test11() {
		//demoLocalDomainService.tryTxWithAnnotation抛出RuntimeException异常，但需要BusinessException才rollback
		//所以只有demoLocalDomainService.tryTxWithAnnotation本身的数据库事务被rollback，其它分布式事务均被commit
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test11");
		Assertions.assertThrows(RuntimeException.class,() -> demoLocalApplicationService.tx11WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,4L));
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test11 amount:{}",amount);
		log.info("test11 status:{}",status);
		log.info("test11 remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(11L)));
		Assertions.assertEquals((byte)1,status);
		Assertions.assertEquals(ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test12() {
		//demoLocalDomainService.tryTxWithAnnotation抛出RuntimeException异常
		//但demoGlobalTransactionalWithAnnotationFacade.appTranWithAnnotationRequiresNew为独立分布式事务，所以appTranWithAnnotationRequiresNew被提交
		//只有demoLocalDomainService.tryTxWithAnnotation被回滚（demoLocalDomainService.cancelTx被调用），demoLocalDomainService.tryTxWithAnnotation本身的数据库事务被回滚
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test12");
		Assertions.assertThrows(RuntimeException.class,() -> demoLocalApplicationService.tx12WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,4L));
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test12 amount:{}",amount);
		log.info("test12 status:{}",status);
		log.info("test12 remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(11L)));
		Assertions.assertEquals((byte)1,status);
		Assertions.assertEquals("cancelTx" + ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test13() {
		//DemoGlobalTransactionalDomainService.sagaTxWithAnnotation抛出RuntimeException异常
		//导致demoGlobalTransactionalWithAnnotationFacade.appTranWithAnnotationRequiresNew独立分布式事务被回滚
		//由于抛出了异常,demoLocalDomainService.tryTxWithAnnotation不会被调用
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test13");
		Assertions.assertThrows(RuntimeException.class,() -> demoLocalApplicationService.tx12WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,5L));
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test13 amount:{}",amount);
		log.info("test14 status:{}",status);
		log.info("test15 remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(9L)));
		Assertions.assertEquals((byte)4,status);
		Assertions.assertEquals("",remark);
	}

	@Test
	public void test14() {
		//DemoGlobalTransactionalDomainService.sagaTxWithAnnotation抛出RuntimeException异常
		//导致demoGlobalTransactionalWithAnnotationFacade.appTranWithAnnotationRequiresNew独立分布式事务被回滚
		//抛出的RuntimeException异常被catch，会继续执行demoLocalDomainService.tryTxWithAnnotation
		//demoLocalDomainService.tryTxWithAnnotation为独立事务不受影响，被提交（demoLocalDomainService.confirmTx被调用）
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test14");
		demoLocalApplicationService.tx14WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,5L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test14 amount:{}",amount);
		log.info("test14 status:{}",status);
		log.info("test14 remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(10L)));
		Assertions.assertEquals((byte)4,status);
		Assertions.assertEquals(ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test16() {
		//DemoGlobalTransactionalDomainService.sagaTxWithAnnotation抛出RuntimeException异常
		//导致demoGlobalTransactionalApplicationService.appTranWithAnnotation分布式事务被回滚
		//抛出的RuntimeException异常被catch，会继续执行demoLocalDomainService.tryTxWithAnnotation
		//demoLocalDomainService.tryTxWithAnnotation所在的分布式事务为SUPPORTS,无分布式事务，所以demoLocalDomainService.confirmTx不会被调用
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test16");
		demoLocalApplicationService.tx16WithAnnotationSupports(ACCOUNT_NUMBER,BigDecimal.TEN,5L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test16 amount:{}",amount);
		log.info("test16 status:{}",status);
		log.info("test16 remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(10L)));
		Assertions.assertEquals((byte)4,status);
		Assertions.assertEquals("",remark);
	}

	@Test
	public void test18() {
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test18");
		Assertions.assertThrows(BusinessException.class,() -> demoLocalApplicationService.tx18WithAnnotationMandatory(ACCOUNT_NUMBER,BigDecimal.TEN,5L));
		MetricsAdapter.stop(time);
	}

	@Test
	public void test20() {
		//1.DemoLocalApplicationService.tx20WithAnnotation 具有 @GlobalTransactional,REQUIRED
		//2.demoGlobalTransactionalFacade.appTranMandatory, Facade无分布式事务注解，其调用demoGlobalTransactionalApplicationService.appTranWithAnnotationMandatory具有@GlobalTransactional,MANDATORY被忽略
		//3.demoGlobalTransactionalDomainService.tccTryWithAnnotation 具有 @AutonomousTransactional
		//4.demoGlobalTransactionalDomainService.tccTryWithAnnotation 具有 @AutonomousTransactional
		//5.demoLocalDomainService.tryTxWithAnnotation 具有 @AutonomousTransactional 注解

		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test20");
		demoLocalApplicationService.tx20WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test amount:{}",amount);
		log.info("test status:{}",status);
		log.info("test remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(12L)));
		Assertions.assertEquals((byte)1,status);
		Assertions.assertEquals(ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test22() {
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test22");
		Assertions.assertThrows(Exception.class,() -> demoLocalApplicationService.tx22WithAnnotationNotSupported(ACCOUNT_NUMBER,BigDecimal.TEN,5L));
		MetricsAdapter.stop(time);
	}

	@Test
	public void test24() {
		//1.DemoLocalApplicationService.tx24WithAnnotationNotSupported 具有 @GlobalTransactional,NotSupported
		//2.demoGlobalTransactionalFacade.appTran,Facade无分布式事务注解，其调用demoGlobalTransactionalApplicationService.appTranWithAnnotation具有@GlobalTransactional,REQUIRED
		//3.demoGlobalTransactionalDomainService.tccTryWithAnnotation 具有 @AutonomousTransactional
		//4.demoGlobalTransactionalDomainService.tccTryWithAnnotation 具有 @AutonomousTransactional
		//5.demoLocalDomainService.tryTxWithAnnotation 具有 @AutonomousTransactional 注解，但由于NotSupported所以无分布式事务

		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test24");
		demoLocalApplicationService.tx24WithAnnotationNotSupported(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test amount:{}",amount);
		log.info("test status:{}",status);
		log.info("test remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(12L)));
		Assertions.assertEquals((byte)1,status);
		Assertions.assertEquals("",remark);
	}

	@Test
	public void test26() {
		//1.DemoLocalApplicationService.tx26WithAnnotation 具有 @GlobalTransactional
		//2.demoGlobalTransactionalFacade.appTran,Facade无分布式事务注解，其调用demoGlobalTransactionalApplicationService.appTranWithAnnotationNotSupported@GlobalTransactional,NotSupported
		//3.demoGlobalTransactionalDomainService.tccTryWithAnnotation 具有 @AutonomousTransactional，但由于NotSupported所以无分布式事务
		//4.demoGlobalTransactionalDomainService.tccTryWithAnnotation 具有 @AutonomousTransactional，但由于NotSupported所以无分布式事务
		//5.demoLocalDomainService.tryTxWithAnnotation 具有 @AutonomousTransactional 注解

		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test26");
		demoLocalApplicationService.tx26WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test amount:{}",amount);
		log.info("test status:{}",status);
		log.info("test remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(12L)));
		Assertions.assertEquals((byte)0,status);
		Assertions.assertEquals(ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test28() {
		//1.DemoLocalApplicationService.tx26WithAnnotation 具有 @GlobalTransactional
		//2.demoGlobalTransactionalFacade.appTran,Facade无分布式事务注解，其调用demoGlobalTransactionalApplicationService.appTranWithAnnotationNotSupported@GlobalTransactional,NotSupported
		//3.demoGlobalTransactionalDomainService.tccTryWithAnnotation 具有 @AutonomousTransactional，但由于NotSupported所以无分布式事务
		//4.demoGlobalTransactionalDomainService.tccTryWithAnnotation 具有 @AutonomousTransactional，但由于NotSupported所以无分布式事务
		//5.demoLocalDomainService.tryTxWithAnnotation 具有 @AutonomousTransactional 注解

		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test28");
		demoLocalApplicationService.tx28WithAnnotationNever(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test amount:{}",amount);
		log.info("test status:{}",status);
		log.info("test remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(12L)));
		Assertions.assertEquals((byte)1,status);
		Assertions.assertEquals("",remark);
	}

	@Test
	public void test30() {
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test30");
		Assertions.assertThrows(Exception.class,() -> demoLocalApplicationService.tx30WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,5L));
		MetricsAdapter.stop(time);
	}

	@Test
	public void test32() {
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test32");
		demoLocalApplicationService.tx32WithAnnotationNever(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test amount:{}",amount);
		log.info("test status:{}",status);
		log.info("test remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(12L)));
		Assertions.assertEquals((byte)0,status);
		Assertions.assertEquals("",remark);
	}

	@Test
	public void test51() {
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test51");
		demoLocalApplicationService.tx51(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test amount:{}",amount);
		log.info("test status:{}",status);
		log.info("test remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(11L)));
		Assertions.assertEquals((byte)1,status);
		Assertions.assertEquals(ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test52() {
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test52");
		demoLocalApplicationService.tx52(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test amount:{}",amount);
		log.info("test status:{}",status);
		log.info("test remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(11L)));
		Assertions.assertEquals((byte)1,status);
		Assertions.assertEquals(ACCOUNT_NUMBER + "-2",remark);
	}

	@Test
	public void test53() {
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test53");
		demoLocalApplicationService.tx53(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test amount:{}",amount);
		log.info("test status:{}",status);
		log.info("test remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(11L)));
		Assertions.assertEquals((byte)1,status);
		Assertions.assertEquals(ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test54() {
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test54");
		demoLocalApplicationService.tx54WithAnnotation(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test amount:{}",amount);
		log.info("test status:{}",status);
		log.info("test remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(11L)));
		Assertions.assertEquals((byte)1,status);
		Assertions.assertEquals(ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test55() {
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test55");
		Assertions.assertThrows(RuntimeException.class,() -> demoLocalApplicationService.tx51(ACCOUNT_NUMBER,BigDecimal.TEN,4L));
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test amount:{}",amount);
		log.info("test status:{}",status);
		log.info("test remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(10L)));
		Assertions.assertEquals((byte)4,status);
		Assertions.assertEquals("cancelTx" + ACCOUNT_NUMBER + "-2" + "This is a message from tryTx.",remark);
	}

	@Test
	public void test56() {
		jdbcTemplate.update("delete from tx_demo where account_number = ?",ACCOUNT_NUMBER);
		Timer.Context time = MetricsAdapter.time("DemoGlobalTransactionalTests.test56");
		Assertions.assertThrows(RuntimeException.class,() -> demoLocalApplicationService.tx52(ACCOUNT_NUMBER,BigDecimal.TEN,4L));
		MetricsAdapter.stop(time);
		ThrowingRunnable.run(() -> Thread.sleep(100L));

		BigDecimal amount = jdbcTemplate.queryForObject("select amount from tx_demo where account_number = ?",BigDecimal.class,ACCOUNT_NUMBER);
		Byte status = jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,ACCOUNT_NUMBER);
		String remark = jdbcTemplate.queryForObject("select remark from tx_demo where account_number = ?",String.class,ACCOUNT_NUMBER);
		log.info("test amount:{}",amount);
		log.info("test status:{}",status);
		log.info("test remark:{}",remark);
		Assertions.assertEquals(0,amount.compareTo(BigDecimal.valueOf(10L)));
		Assertions.assertEquals((byte)4,status);
		Assertions.assertEquals("cancelTx" + ACCOUNT_NUMBER + "-2",remark);
	}

	@Test
	public void testBenchmark1() {
		int testTimes = 1000;
		int testThreadNum = 5;
		AtomicInteger successNum = new AtomicInteger(0);
		AtomicLong successSpent = new AtomicLong(0);

		ThreadPoolExecutor threadPool = new ThreadPoolExecutor(testThreadNum, testThreadNum, 30, TimeUnit.SECONDS,
				new LinkedBlockingDeque<Runnable>(4), runnable -> new Thread(runnable,"benchmark-" + runnable.hashCode()),
				(runnable, executor) -> ThrowingRunnable.runWithoutThrowing(() -> executor.getQueue().put(runnable)));

		demoLocalApplicationService.txBenchmark1(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		long lStart = System.currentTimeMillis();
		for (int idx=0;idx<testTimes;idx++) {
			threadPool.submit(() -> {
				long lStartTimestamp = System.currentTimeMillis();
				demoLocalApplicationService.txBenchmark1(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
				successSpent.addAndGet(System.currentTimeMillis() - lStartTimestamp);
				successNum.incrementAndGet();
			});
		}

		threadPool.shutdown();
		ThrowingSupplier.getWithoutThrowing(() -> threadPool.awaitTermination(3600,TimeUnit.SECONDS));
		long lSpent = System.currentTimeMillis() - lStart;
		log.error("benchmark test finished testThreadNum:{} testTimes:{} successNum:{}",testThreadNum,testTimes,successNum.get());
		log.error("benchmark test finished spent:{} successSpent:{} averageSpent:{} tps:{}",lSpent,successSpent.get(),successSpent.get() / successNum.get(),successNum.get() * 1000L / lSpent);
	}

	@Test
	public void testBenchmark2() {
		int testTimes = 1000;
		int testThreadNum = 5;
		AtomicInteger successNum = new AtomicInteger(0);
		AtomicLong successSpent = new AtomicLong(0);

		ThreadPoolExecutor threadPool = new ThreadPoolExecutor(testThreadNum, testThreadNum, 30, TimeUnit.SECONDS,
				new LinkedBlockingDeque<Runnable>(4), runnable -> new Thread(runnable,"benchmark-" + runnable.hashCode()),
				(runnable, executor) -> ThrowingRunnable.runWithoutThrowing(() -> executor.getQueue().put(runnable)));

		demoLocalApplicationService.txBenchmark2(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
		long lStart = System.currentTimeMillis();
		for (int idx=0;idx<testTimes;idx++) {
			threadPool.submit(() -> {
				long lStartTimestamp = System.currentTimeMillis();
				demoLocalApplicationService.txBenchmark2(ACCOUNT_NUMBER,BigDecimal.TEN,1L);
				successSpent.addAndGet(System.currentTimeMillis() - lStartTimestamp);
				successNum.incrementAndGet();
			});
		}

		threadPool.shutdown();
		ThrowingSupplier.getWithoutThrowing(() -> threadPool.awaitTermination(3600,TimeUnit.SECONDS));
		long lSpent = System.currentTimeMillis() - lStart;
		log.error("benchmark test finished testThreadNum:{} testTimes:{} successNum:{}",testThreadNum,testTimes,successNum.get());
		log.error("benchmark test finished spent:{} successSpent:{} averageSpent:{} tps:{}",lSpent,successSpent.get(),successSpent.get() / successNum.get(),successNum.get() * 1000L / lSpent);
	}
}
