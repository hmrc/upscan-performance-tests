/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.performance.simulation.PerformanceTestRunner
import uk.gov.hmrc.perftests.UpscanRequests._

class Simulations extends PerformanceTestRunner {

  setup("v1-clean-pdf", "V1 Upload clean pdf")
    .withActions(
      initiateTheUploadV1,
      parseInitiateResponse,
      createModifiedFile("/upload/test.pdf"),
      uploadFileToAws("/upload/test.pdf"),
      deleteModifiedFile)
    .withActions(pollStatusUpdates: _*)
    .withActions(verifyFileStatus("READY"))

  setup("v1-large-pdf", "V1 Upload large pdf")
    .withActions(
      initiateTheUploadV1,
      parseInitiateResponse,
      createModifiedFile("/upload/large-file-test.pdf"),
      uploadFileToAws("/upload/large-file-test.pdf"),
      deleteModifiedFile)
    .withActions(pollStatusUpdates: _*)
    .withActions(verifyFileStatus("READY"))

  setup("v1-virus", "V1 Upload virus")
    .withActions(
      initiateTheUploadV1,
      parseInitiateResponse,
      createModifiedFile("/upload/eicar-standard-av-test-file"),
      uploadFileToAws("/upload/eicar-standard-av-test-file"),
      deleteModifiedFile)
    .withActions(pollStatusUpdates: _*)
    .withActions(verifyFileStatus("FAILED"))

  setup("v1-invalid-txt-filetype", "V1 Upload invalid .txt file type")
    .withActions(
      initiateTheUploadV1,
      parseInitiateResponse,
      createModifiedFile("/upload/test.txt"),
      uploadFileToAws("/upload/test.txt"),
      deleteModifiedFile)
    .withActions(pollStatusUpdates: _*)
    .withActions(verifyFileStatus("FAILED"))

  setup("v2-clean-pdf", "V2 Upload clean pdf")
    .withActions(
      initiateTheUploadV2,
      parseInitiateResponse,
      createModifiedFile("/upload/test.pdf"),
      uploadFileToUpscanProxy("/upload/test.pdf"),
      deleteModifiedFile)
    .withActions(pollStatusUpdates: _*)
    .withActions(verifyFileStatus("READY"))

  setup("v2-large-pdf", "V2 Upload large pdf")
    .withActions(
      initiateTheUploadV2,
      parseInitiateResponse,
      createModifiedFile("/upload/large-file-test.pdf"),
      uploadFileToUpscanProxy("/upload/large-file-test.pdf"),
      deleteModifiedFile)
    .withActions(pollStatusUpdates: _*)
    .withActions(verifyFileStatus("READY"))

  setup("v2-virus", "V2 Upload virus")
    .withActions(
      initiateTheUploadV2,
      parseInitiateResponse,
      createModifiedFile("/upload/eicar-standard-av-test-file"),
      uploadFileToUpscanProxy("/upload/eicar-standard-av-test-file"),
      deleteModifiedFile)
    .withActions(pollStatusUpdates: _*)
    .withActions(verifyFileStatus("FAILED"))

  setup("v2-invalid-txt-filetype", "V2 Upload invalid .txt file type")
    .withActions(
      initiateTheUploadV2,
      parseInitiateResponse,
      createModifiedFile("/upload/test.txt"),
      uploadFileToUpscanProxy("/upload/test.txt"),
      deleteModifiedFile)
    .withActions(pollStatusUpdates: _*)
    .withActions(verifyFileStatus("FAILED"))

  runSimulation()
}
