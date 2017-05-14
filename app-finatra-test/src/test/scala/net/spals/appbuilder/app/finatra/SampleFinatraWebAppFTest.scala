package net.spals.appbuilder.app.finatra

import com.google.inject.name.Names
import com.google.inject.{Key, Stage, TypeLiteral}
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.annotations.FlagImpl
import net.spals.appbuilder.app.finatra.sample.{SampleCustomService, SampleFinatraWebApp}
import net.spals.appbuilder.model.core.ModelSerializer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.{hasKey, is, notNullValue}
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
        "net.spals.appbuilder.app.finatra.sample.SampleFinatraWebApp:net.spals.appbuilder.app.finatra.sample.SampleAutoBoundModule"),
      Array("GuiceModule", "net.spals.appbuilder.app.finatra.sample.SampleGuiceModule"),
      Array("TwitterModule", "net.spals.appbuilder.app.finatra.sample.SampleTwitterModule")
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
    assertThat(serviceInjector.getInstance(classOf[SampleCustomService]), notNullValue())
  }

  @Test def testModelInjection() {
    val serviceInjector = sampleApp.getServiceInjector

    val modelSerializerMapKey = new TypeLiteral[java.util.Map[String, ModelSerializer]](){}
    val modelSerializerMap = serviceInjector.getInstance(Key.get(modelSerializerMapKey))
    assertThat(modelSerializerMap, hasKey("pojo"))
  }
}
