package net.spals.appbuilder.model.core.pojo

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.{is, notNullValue}
import org.testng.annotations.{DataProvider, Test}

/**
  * Unit tests for [[PojoModelSerializer]]
  *
  * @author tkral
  */
class PojoScalaModelSerializerTest {

  @DataProvider
  def modelEqualityProvider(): Array[Array[AnyRef]] = {
    Array(
      // Case: Tuple1
      Array(("value")),
      // Case: Tuple2
      Array((1L,"value")),
      Array(List("value1", "value2")),
      Array(Map("key" -> "value")),
      Array(CaseClass(id = 1, name = "value"))
    )
  }

  @Test(dataProvider = "modelEqualityProvider")
  def testModelEquality(modelObject: AnyRef) {
    val modelSerializer = new PojoModelSerializer

    val serializedModelObject = modelSerializer.serialize(modelObject)
    assertThat(serializedModelObject, is(notNullValue))

    val deserializedModelObject = modelSerializer.deserialize(serializedModelObject)
    assertThat(deserializedModelObject, is(modelObject))
  }

  private case class CaseClass(id: Int, name: String)
}
