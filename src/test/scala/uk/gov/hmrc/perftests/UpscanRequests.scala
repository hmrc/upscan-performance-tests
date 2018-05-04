package uk.gov.hmrc.perftests

import io.gatling.commons.util.ClockSingleton
import io.gatling.core.Predef._
import io.gatling.core.action.builder.SessionHookBuilder
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import org.json4s._
import org.json4s.native.JsonMethods._
import uk.gov.hmrc.performance.conf.{HttpConfiguration, ServicesConfiguration}

import scala.concurrent.duration._

object UpscanRequests extends ServicesConfiguration with HttpConfiguration {

  private val upscanBaseUrl        = baseUrlFor("upscan") + "/upscan"
  private val upscaListenerBaseUrl = baseUrlFor("upscan-listener") + "/upscan-listener"

  val callBackUrl = "https://upscan-listener.public.mdtp/upscan-listener/listen"

  private val fileSize = 5 * 1024 * 1024

  val fileBody: Array[Byte] = Array.fill[Byte](fileSize)(0)

  val initiateTheUpload: HttpRequestBuilder =
    http("Upscan Initiate")
      .post(s"$upscanBaseUrl/initiate")
      .body(
        StringBody(s"""{ "callbackUrl": "$callBackUrl" }""")
      )
      .asJSON
      .check(status.is(200))
      .check(jsonPath("$").find.saveAs("initiateResponse"))

  case class PreparedUpload(reference: String, uploadRequest: UploadFormTemplate)

  case class UploadFormTemplate(href: String, fields: Map[String, String])

  val parseInitiateResponse = new SessionHookBuilder(
    (session: Session) => {
      if (session.isFailed) {
        session
      } else {
        implicit val formats = DefaultFormats

        val initiateResponse   = session.attributes("initiateResponse").toString
        val uploadFormTemplate = parse(initiateResponse).extract[PreparedUpload]
        session
          .set("uploadHref", uploadFormTemplate.uploadRequest.href)
          .set("fields", uploadFormTemplate.uploadRequest.fields)
          .set("reference", uploadFormTemplate.reference)
      }
    }
  )

  val generateFileBody: SessionHookBuilder = new SessionHookBuilder(
    (session: Session) => {
      session.set("fileBody", fileBody)
    }
  )

  val uploadFileToAws: HttpRequestBuilder = http("Uploading file to AWS")
    .post("${uploadHref}")
    .asMultipartForm
    .bodyPart(StringBodyPart("x-amz-meta-callback-url", "${fields.x-amz-meta-callback-url}"))
    .bodyPart(StringBodyPart("x-amz-date", "${fields.x-amz-date}"))
    .bodyPart(StringBodyPart("x-amz-credential", "${fields.x-amz-credential}"))
    .bodyPart(StringBodyPart("x-amz-algorithm", "${fields.x-amz-algorithm}"))
    .bodyPart(StringBodyPart("key", "${fields.key}"))
    .bodyPart(StringBodyPart("acl", "${fields.acl}"))
    .bodyPart(StringBodyPart("x-amz-signature ", "${fields.x-amz-signature}"))
    .bodyPart(StringBodyPart("policy", "${fields.policy}"))
    .bodyPart(ByteArrayBodyPart("file", "${fileBody}"))
    .check(status.is(204))

  val queryUpscanListener: HttpRequestBuilder = http("Fetching file status from upscan-listener")
    .get(s"$upscaListenerBaseUrl/poll/" + "${reference}")
    .check(status.in(200, 404).saveAs("status"))

  val storeLoopStartTime = new SessionHookBuilder(
    (session: Session) => {
      session.set("loopStartTime", ClockSingleton.nowMillis)
    }
  )

  val pollForResult =
    asLongAs(
      session =>
        !session.attributes.get("status").contains(200) &&
          (ClockSingleton.nowMillis - session.attributes("loopStartTime").asInstanceOf[Long]) < (90 * 1000)) {
      exec(queryUpscanListener).pause(500 milliseconds)
    }.actionBuilders

  val checkIfResultFound = new SessionHookBuilder(
    (session: Session) => {
      if (!session.attributes.get("status").contains(200)) {
        session.markAsFailed
      } else {
        session
      }
    }
  )
}
