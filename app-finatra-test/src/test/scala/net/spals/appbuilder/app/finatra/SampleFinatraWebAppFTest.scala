package net.spals.appbuilder.app.finatra

import com.google.inject.name.Names
import com.google.inject.{Key, Stage, TypeLiteral}
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.annotations.FlagImpl
import net.spals.appbuilder.app.finatra.sample.web.{SampleFinatraController, SampleFinatraExceptionMapper, SampleFinatraFilter}
import net.spals.appbuilder.app.finatra.sample.{SampleFinatraCustomService, SampleFinatraWebApp}
import net.spals.appbuilder.executor.core.ExecutorServiceFactory
import net.spals.appbuilder.filestore.core.{FileStore, FileStorePlugin}
import net.spals.appbuilder.mapstore.core.{MapStore, MapStorePlugin}
import net.spals.appbuilder.message.core.consumer.MessageConsumerPlugin
import net.spals.appbuilder.message.core.producer.MessageProducerPlugin
import net.spals.appbuilder.message.core.{MessageConsumer, MessageConsumerCallback, MessageProducer}
import net.spals.appbuilder.model.core.ModelSerializer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.{hasKey, is, notNullValue}
import org.mockito.ArgumentMatchers.{any, isA}
import org.mockito.Mockito.verify
import org.testng.annotations.{AfterClass, BeforeClass, DataProvider, Test}

/**
  * Functional tests for a sample [[FinatraWebApp]]
  *
  * @author tkral
  */
class SampleFinatraWebAppFTest {

  private val sampleApp = new SampleFinatraWebApp()
  private val testServerWrapper = new EmbeddedHttpServer(
    twitterServer = sampleApp,
    stage = Stage.PRODUCTION
  )

  @BeforeClass def classSetup() {
    testServerWrapper.start()
  }

  @AfterClass def classTearDown(): Unit = {
    testServerWrapper.close()
  }

  @DataProvider def serviceConfigProvider(): Array[Array[AnyRef]] = {
    Array(
      Array("fileStore.system", "localFS"),
      Array("mapStore.system", "mapDB")
    )
  }

  @Test(dataProvider = "serviceConfigProvider")
  def testServiceConfig(configKey: String, expectedConfigValue: AnyRef) {
    val serviceConfig = sampleApp.getServiceConfig
    assertThat(serviceConfig.getAnyRef(configKey), is(expectedConfigValue))
  }

  @Test(dataProvider = "serviceConfigProvider")
  def testServiceConfigInjection(configKey: String, expectedConfigValue: AnyRef) {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(Key.get(classOf[String], new FlagImpl(configKey))),
      is(String.valueOf(expectedConfigValue)))
  }

  @DataProvider def customModuleInjectionProvider(): Array[Array[AnyRef]] = {
    Array(
      Array("AutoBoundModule",
        "net.spals.appbuilder.app.finatra.sample.SampleFinatraWebApp:net.spals.appbuilder.app.finatra.sample.SampleFinatraAutoBoundModule"),
      Array("GuiceModule", "net.spals.appbuilder.app.finatra.sample.SampleFinatraGuiceModule"),
      Array("TwitterModule", "net.spals.appbuilder.app.finatra.sample.SampleFinatraTwitterModule")
    )
  }

  @Test(dataProvider = "customModuleInjectionProvider")
  def testCustomModuleInjection(keyName: String, expectedBindValue: String) {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(Key.get(classOf[String], Names.named(keyName))),
      is(expectedBindValue))
  }

  @Test def testCustomServiceInjection() {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[SampleFinatraCustomService]), notNullValue())
  }

  @Test def testExecutorInjection() {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[ExecutorServiceFactory]), notNullValue())
  }

  @Test def testFileStoreInjection() {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[FileStore]), notNullValue())

    val fileStorePluginMapKey = new TypeLiteral[java.util.Map[String, FileStorePlugin]](){}
    val fileStorePluginMap = serviceInjector.getInstance(Key.get(fileStorePluginMapKey))
    assertThat(fileStorePluginMap, Matchers.aMapWithSize[String, FileStorePlugin](1))
    assertThat(fileStorePluginMap, hasKey("localFS"))
  }

  @Test def testMapStoreInjection() {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[MapStore]), notNullValue())

    val mapStorePluginMapKey = new TypeLiteral[java.util.Map[String, MapStorePlugin]](){}
    val mapStorePluginMap = serviceInjector.getInstance(Key.get(mapStorePluginMapKey))
    assertThat(mapStorePluginMap, Matchers.aMapWithSize[String, MapStorePlugin](1))
    assertThat(mapStorePluginMap, hasKey("mapDB"))
  }

  @Test def testMessageConsumerInjection() {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[MessageConsumer]), notNullValue())

    val messageConsumerPluginMapKey = new TypeLiteral[java.util.Map[String, MessageConsumerPlugin]](){}
    val messageConsumerPluginMap = serviceInjector.getInstance(Key.get(messageConsumerPluginMapKey))
    assertThat(messageConsumerPluginMap, Matchers.aMapWithSize[String, MessageConsumerPlugin](1))
    assertThat(messageConsumerPluginMap, hasKey("blockingQueue"))
  }

  @Test def testMessageConsumerCallbackInjection() {
    val serviceInjector = sampleApp.getServiceInjector

    val messageCallbackSetKey = new TypeLiteral[java.util.Set[MessageConsumerCallback[_]]](){}
    val messageCallbackSet = serviceInjector.getInstance(Key.get(messageCallbackSetKey))
    assertThat(messageCallbackSet, notNullValue())
  }

  @Test def testMessageProducerInjection() {
    val serviceInjector = sampleApp.getServiceInjector
    assertThat(serviceInjector.getInstance(classOf[MessageProducer]), notNullValue())

    val messageProducerPluginMapKey = new TypeLiteral[java.util.Map[String, MessageProducerPlugin]](){}
    val messageProducerPluginMap = serviceInjector.getInstance(Key.get(messageProducerPluginMapKey))
    assertThat(messageProducerPluginMap, Matchers.aMapWithSize[String, MessageProducerPlugin](1))
    assertThat(messageProducerPluginMap, hasKey("blockingQueue"))
  }

  @Test def testModelInjection() {
    val serviceInjector = sampleApp.getServiceInjector

    val modelSerializerMapKey = new TypeLiteral[java.util.Map[String, ModelSerializer]](){}
    val modelSerializerMap = serviceInjector.getInstance(Key.get(modelSerializerMapKey))
    assertThat(modelSerializerMap, Matchers.aMapWithSize[String, ModelSerializer](1))
    assertThat(modelSerializerMap, hasKey("pojo"))
  }

  @Test def testWebControllerInjection() {
    verify(sampleApp.mockRouter).add(isA(classOf[SampleFinatraController]))
  }

  @Test def testWebExceptionMapperInjection() {
    verify(sampleApp.mockRouter).exceptionMapper(isA(classOf[SampleFinatraExceptionMapper]))(any())
  }

  @Test def testWebFilterInjection() {
    verify(sampleApp.mockRouter).filter(isA(classOf[SampleFinatraFilter]))
  }
}
