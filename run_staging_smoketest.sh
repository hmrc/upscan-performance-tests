#!/bin/bash
sbt -DbaseUrl=https://www.staging.tax.service.gov.uk -Dperftest.runSmokeTest=true gatling:test