/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.perftests

import io.gatling.core.Predef._
import io.gatling.core.action.builder.SessionHookBuilder
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import org.json4s._
import org.json4s.native.JsonMethods._
import uk.gov.hmrc.performance.conf.{HttpConfiguration, ServicesConfiguration}

import java.nio.file.Paths
import scala.concurrent.duration._
import scala.language.postfixOps

object UpscanRequests extends ServicesConfiguration with HttpConfiguration {

  private val upscanBaseUrl         = baseUrlFor("upscan") + "/upscan"
  private val upscanListenerBaseUrl = baseUrlFor("upscan-listener") + "/upscan-listener"

  private val callBackUrl = "https://upscan-listener.public.mdtp/upscan-listener/listen"

  private val pollingTimeout = readProperty("upscan-performance-tests.pollingTimeoutInSeconds").toInt.seconds

  val initiateTheUploadV1: HttpRequestBuilder = initiateUploadRequest("Initiate V1 file upload",
    s"$upscanBaseUrl/initiate",
    s"""{"callbackUrl": "$callBackUrl"}""")

  val initiateTheUploadV2: HttpRequestBuilder = initiateUploadRequest("Initiate V2 file upload",
    s"$upscanBaseUrl/v2/initiate",
    s"""|{"callbackUrl": "$callBackUrl",
        | "successRedirect": "https://www.google.co.uk",
        | "errorRedirect": "https://www.amazon.co.uk"
        |}""".stripMargin)

  private def initiateUploadRequest(requestName: String, url: String, body: String) =
    http(requestName)
      .post(url)
      .ignoreProtocolHeaders
      .header("User-Agent", "upscan-performance-tests")
      .body(StringBody(body))
      .asJson
      .check(status.is(200))
      .check(bodyString.saveAs("initiateResponse"))

  case class PreparedUpload(reference: String, uploadRequest: UploadFormTemplate)

  case class UploadFormTemplate(href: String, fields: Map[String, String])

  val parseInitiateResponse = new SessionHookBuilder(
    (session: Session) => {
      if (session.isFailed) {
        session
      } else {
        implicit val formats: DefaultFormats.type = DefaultFormats

        val initiateResponse   = session.attributes("initiateResponse").toString
        val uploadFormTemplate = parse(initiateResponse).extract[PreparedUpload]
        session
          .set("uploadHref", uploadFormTemplate.uploadRequest.href)
          .set("fields", uploadFormTemplate.uploadRequest.fields)
          .set("reference", uploadFormTemplate.reference)
      }
    },
    exitable = true
  )

  def uploadFileToAws(filename: String): HttpRequestBuilder = http("Uploading file to AWS")
    .post("${uploadHref}")
    .asMultipartForm
    .bodyPart(StringBodyPart("x-amz-meta-callback-url", "${fields.x-amz-meta-callback-url}"))
    .bodyPart(StringBodyPart("x-amz-date", "${fields.x-amz-date}"))
    .bodyPart(StringBodyPart("x-amz-credential", "${fields.x-amz-credential}"))
    .bodyPart(StringBodyPart("x-amz-meta-original-filename", "${fields.x-amz-meta-original-filename}"))
    .bodyPart(StringBodyPart("x-amz-algorithm", "${fields.x-amz-algorithm}"))
    .bodyPart(StringBodyPart("key", "${fields.key}"))
    .bodyPart(StringBodyPart("acl", "${fields.acl}"))
    .bodyPart(StringBodyPart("x-amz-signature", "${fields.x-amz-signature}"))
    .bodyPart(StringBodyPart("x-amz-meta-session-id", "${fields.x-amz-meta-session-id}"))
    .bodyPart(StringBodyPart("x-amz-meta-request-id", "${fields.x-amz-meta-request-id}"))
    .bodyPart(StringBodyPart("x-amz-meta-consuming-service", "${fields.x-amz-meta-consuming-service}"))
    .bodyPart(StringBodyPart("x-amz-meta-upscan-initiate-received", "${fields.x-amz-meta-upscan-initiate-received}"))
    .bodyPart(StringBodyPart("x-amz-meta-upscan-initiate-response", "${fields.x-amz-meta-upscan-initiate-response}"))
    .bodyPart(StringBodyPart("policy", "${fields.policy}"))
    .bodyPart(RawFileBodyPart("file", getResourceAbsolutePath(filename)))
    .check(status.is(204))

  def uploadFileToUpscanProxy(filename: String): HttpRequestBuilder = http("Uploading file to Upscan Proxy")
    .post("${uploadHref}")
    .disableFollowRedirect
    .asMultipartForm
    .bodyPart(StringBodyPart("x-amz-meta-callback-url", "${fields.x-amz-meta-callback-url}"))
    .bodyPart(StringBodyPart("x-amz-date", "${fields.x-amz-date}"))
    .bodyPart(StringBodyPart("x-amz-credential", "${fields.x-amz-credential}"))
    .bodyPart(StringBodyPart("x-amz-meta-original-filename", "${fields.x-amz-meta-original-filename}"))
    .bodyPart(StringBodyPart("x-amz-algorithm", "${fields.x-amz-algorithm}"))
    .bodyPart(StringBodyPart("key", "${fields.key}"))
    .bodyPart(StringBodyPart("acl", "${fields.acl}"))
    .bodyPart(StringBodyPart("x-amz-signature", "${fields.x-amz-signature}"))
    .bodyPart(StringBodyPart("x-amz-meta-session-id", "${fields.x-amz-meta-session-id}"))
    .bodyPart(StringBodyPart("x-amz-meta-request-id", "${fields.x-amz-meta-request-id}"))
    .bodyPart(StringBodyPart("x-amz-meta-consuming-service", "${fields.x-amz-meta-consuming-service}"))
    .bodyPart(StringBodyPart("x-amz-meta-upscan-initiate-received", "${fields.x-amz-meta-upscan-initiate-received}"))
    .bodyPart(StringBodyPart("x-amz-meta-upscan-initiate-response", "${fields.x-amz-meta-upscan-initiate-response}"))
    .bodyPart(StringBodyPart("success_action_redirect", "${fields.success_action_redirect}"))
    .bodyPart(StringBodyPart("error_action_redirect", "${fields.error_action_redirect}"))
    .bodyPart(StringBodyPart("policy", "${fields.policy}"))
    .bodyPart(RawFileBodyPart("file", getResourceAbsolutePath(filename)))
    .check(header("Location").transform(_.contains("google")).is(true))
    .check(status.is(303))

  private def getResourceAbsolutePath(filename: String) = {
    val res = getClass.getResource(filename)
    val file = Paths.get(res.toURI).toFile
    file.getAbsolutePath
  }

  val pollStatusUpdates =
    asLongAsDuring(!_.attributes.get("status").contains(200), pollingTimeout) {
      exec(
        http("Polling file processing status")
          .get(s"$upscanListenerBaseUrl/poll/" + "${reference}")
          .check(status.in(200, 404).saveAs("status"))
          .silent).pause(500.milliseconds)
    }.actionBuilders

  def verifyFileStatus(expectedStatus: String): HttpRequestBuilder =
    http(s"Verifying final file processing status is: $expectedStatus")
      .get(s"$upscanListenerBaseUrl/poll/" + "${reference}")
      .check(status.is(200))
      .check(jsonPath("$..fileStatus").is(expectedStatus))

}
