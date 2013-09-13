package org.scalatra.embedded

import org.scalatra.ApiFormats

class EmbeddedJetty extends EmbeddedJettyStack {

  get("/?") {
    f"Hello, from action! (isDevelopment = ${isDevelopmentMode}, serverHost = ${serverHost}, forceSsl = ${needsHttps}})"
  }
  
}
