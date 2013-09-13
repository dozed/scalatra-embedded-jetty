package org.scalatra.embedded

import org.scalatra.ApiFormats
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import org.json4s.DefaultFormats

class EmbeddedJetty extends EmbeddedJettyStack with ApiFormats with JacksonJsonSupport {

  implicit val jsonFormats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  get("/say-html") {
    contentType = formats("html")
    jade("index")
  }

  get("/say-json") {
    "hellooo"
  }

  get("/?") {
    f"Hello, from action! (isDevelopment = ${isDevelopmentMode}, serverHost = ${serverHost}, forceSsl = ${needsHttps}})"
  }
  
}
