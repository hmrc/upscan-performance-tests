#!/bin/bash
sbt '; clean; set javaOptions += "-DbaseUrl=https://www.staging.tax.service.gov.uk"; gatling:test'