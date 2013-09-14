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
    org.json4s.DefaultFormats + new MotorcyclePreferenceSerializer
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

  before() {
    contentType = formats("json")
  }

  get("/json-1") {
    new MotorcyclePreferences("foo")
  }

  get("/json-2") {
    MotorcyclePreferencesCaseClass("foo")
  }

  get("/say-html") {
    contentType = formats("html")
    jade("index")
  }


  get("/?") {
    f"Hello, from action! (isDevelopment = ${isDevelopmentMode}, serverHost = ${serverHost}, forceSsl = ${needsHttps}})"
  }

}
