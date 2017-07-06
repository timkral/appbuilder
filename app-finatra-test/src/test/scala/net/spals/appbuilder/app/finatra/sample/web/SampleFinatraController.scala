package net.spals.appbuilder.app.finatra.sample.web

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import net.spals.appbuilder.annotations.service.AutoBindSingleton

/**
  * A sample Finatra [[Controller]]
  *
  * @author tkral
  */
@AutoBindSingleton
private[finatra] class SampleFinatraController extends Controller {

  get("/ping") { request: Request =>
    "pong"
  }

  get("/name") { request: Request =>
    response.ok.body("Bob")
  }

  post("/foo") { request: Request =>
    "bar"
  }
}
