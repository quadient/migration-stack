//! ---
//! displayName: Parse Azure AI
//! category: Parser
//! description: Processes AzureAI files located in the folder specified by the project settings.
//! sourceFormat: AzureAI
//! ---
package com.quadient.migration.example.azureAI

import com.quadient.migration.api.Migration
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import com.quadient.migration.example.azureAI.categorizeAndTranslate.categorizeAndTranslate

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)
Migration migration = initMigration(this.binding)

def inputFolder = new File(migration.projectConfig.inputDataPath)
def outputFolder = new File(migration.projectConfig.inputDataPath)
String endpoint = migration.projectConfig.context.get("endpoint")
String key = migration.projectConfig.context.get("key")
String region = migration.projectConfig.context.get("region")

if (endpoint == null || endpoint.empty || key == null || key.empty || region == null || region.empty) {
    throw new IllegalArgumentException("Please set endpoint, key and region in the project context.")
}

if (!outputFolder.exists()) {
    outputFolder.mkdirs()
}

List jsons = new ArrayList()

inputFolder.eachFileMatch(~/(?i).*\.pdf/) { File pdfFile ->
    log.info "Processing file: " + pdfFile.name
    String base64 = ""
    byte[] fileBytes = pdfFile.bytes
    base64 = Base64.encoder.encodeToString(fileBytes)
    String json = '{"base64Source":"' + base64 + '"}'

    File outputFile = new File(outputFolder, pdfFile.name.replaceAll(/\.pdf$/, ".json"))
    if(outputFile.exists()){
        log.info "JSON for this document already exists, skipping: " + pdfFile.name
    } else {
        HttpRequest request = HttpRequest.newBuilder(new URI(endpoint))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .setHeader("Ocp-Apim-Subscription-Key", key)
                .setHeader("Ocp-Apim-Subscription-Region", region)
                .build()
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() > 204) {
            log.info "WARNING: HTTP Response status code " + response.statusCode();
        }
        log.info response.headers().map().get("operation-location").toString()
        sleep(10000) // wait for the operation to complete, adjust as needed

        def urlResponse = response.headers().map().get("operation-location")

        HttpResponse<String> responseFromAzure
        def statusCode = 0
        while(statusCode != 200){
            log.info "Waiting for Azure response..."
            HttpRequest requestFromAzure = HttpRequest.newBuilder(new URI(urlResponse[0]))
                    .GET()
                    .setHeader("Ocp-Apim-Subscription-Key", key)
                    .setHeader("Ocp-Apim-Subscription-Region", region)
                    .build();
            responseFromAzure = HttpClient.newHttpClient().send(requestFromAzure, HttpResponse.BodyHandlers.ofString());

            if (responseFromAzure.statusCode() > 200) {
                log.info "WARNING: HTTP Response status code " + responseFromAzure.statusCode();
                sleep(10000)
            } else {
                statusCode = 200
            }
        }

        log.info responseFromAzure.toString()
        outputFile.text = responseFromAzure.body()
    }

    log.info "JSON from Azure saved:  ${outputFile.name}"

    jsons.add(outputFile)
}

new categorizeAndTranslate().documentsCategorize(jsons, migration)