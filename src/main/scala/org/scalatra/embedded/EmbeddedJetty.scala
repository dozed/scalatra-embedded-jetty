package org.scalatra.embedded

import org.scalatra._
import org.scalatra.json._

import org.json4s._
import org.json4s.JsonDSL._

class EmbeddedJetty extends EmbeddedJettyStack with ApiFormats with JacksonJsonSupport {

  case class MotorcyclePreferencesCaseClass(stuff: String)

  class MotorcyclePreferences(val stuff: String)

  object MotorcyclePreferences {

    def fromString(s: String) = new MotorcyclePreferences(s)

    def toString(m: MotorcyclePreferences) = m.stuff

  }

  protected implicit val jsonFormats: Formats = {
    org.json4s.DefaultFormats // + new MotorcyclePreferenceSerializer
  }

  override protected def renderPipeline: RenderPipeline = {
    renderToJsonUsingFormats orElse super.renderPipeline
  }

  private def renderToJsonUsingFormats: RenderPipeline = {
    case a: Any if jsonFormats.customSerializer.isDefinedAt(a) =>
      jsonFormats.customSerializer.lift(a) match {
        case Some(jv: JValue) => jv
        case None => super.renderPipeline(a)
      }
    case JsonResult(jv) => jv
  }

  class MotorcyclePreferenceSerializer
    extends CustomSerializer[MotorcyclePreferences](
      format => ( {
        case v: JString => {
          MotorcyclePreferences.fromString(v.toString)
        }
      }, {
        case d: MotorcyclePreferences => {
          JString(MotorcyclePreferences.toString(d))
        }
      })) {}

  implicit object MotorcycleFormat extends JsonFormat[MotorcyclePreferences] {
    def read(value: JValue): MotorcyclePreferences = {
      (for {
        stuff <- (value \ "stuff").extractOpt[String]
      } yield new MotorcyclePreferences(stuff)).get
    }

    def write(obj: MotorcyclePreferences): JValue = {
      "stuff" -> obj.stuff
    }
  }

  case class JsonResult(jv: JValue)

  before() {
    contentType = formats("json")
  }

  get("/json-1") {
    new MotorcyclePreferences("foo")
  }

  get("/json-2") {
    MotorcyclePreferencesCaseClass("foo")
  }

  def route: Any = {
    new MotorcyclePreferences("test")
  }

  def writeToJson(x: Any) = ???

  get("/json-3") {
    new MotorcyclePreferences("test").asJValue
  }

  get("/json-4") {
    JsonResult(new MotorcyclePreferences("test").asJValue)
  }

  get("/say-html") {
    contentType = formats("html")
    jade("index")
  }


  get("/?") {
    f"Hello, from action! (isDevelopment = ${isDevelopmentMode}, serverHost = ${serverHost}, forceSsl = ${needsHttps}})"
  }

}
