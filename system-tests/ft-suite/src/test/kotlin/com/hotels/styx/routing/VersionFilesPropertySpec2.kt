/*
  Copyright (C) 2013-2019 Expedia Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.hotels.styx.routing

import com.hotels.styx.StyxConfig
import com.hotels.styx.StyxServer
import com.hotels.styx.api.HttpHeaderNames.HOST
import com.hotels.styx.api.HttpRequest.get
import com.hotels.styx.api.HttpResponseStatus.OK
import com.hotels.styx.client.StyxHttpClient
import com.hotels.styx.startup.StyxServerComponents
import com.hotels.styx.support.ResourcePaths.fixturesHome
import io.kotlintest.Spec
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.TimeUnit.MILLISECONDS

class VersionFilesPropertySpec2 : StringSpec() {
    val fileLocation = fixturesHome(VersionFilesPropertySpec2::class.java,"/version.txt")
    val originsOk = fixturesHome(VersionFilesPropertySpec2::class.java, "/conf/origins/origins-correct.yml")
    val yamlText = """
        services:
          factories:
            backendServiceRegistry:
              class: "com.hotels.styx.proxy.backends.file.FileBackedBackendServicesRegistry${'$'}Factory"
              config: {originsFile: "$originsOk"}

        admin:
          connectors:
            http:
              port: 0

        userDefined:
          versionFiles: $fileLocation
      """.trimIndent()

    init {
        "Gets the version text property" {
            val request = get("/version.txt")
                    .header(HOST, "${styxServer.adminHttpAddress().hostName}:${styxServer.adminHttpAddress().port}")
                    .build();

            val response = client.send(request)
                    .toMono()
                    .block();

            response?.status() shouldBe (OK)
            response?.bodyAs(UTF_8) shouldBe ("MyFakeVersionText\n")
        }
    }

    val client: StyxHttpClient = StyxHttpClient.Builder()
            .threadName("functional-test-client")
            .connectTimeout(1000, MILLISECONDS)
            .maxHeaderSize(2 * 8192)
            .build()

    val styxServer = StyxServer(StyxServerComponents.Builder()
            .styxConfig(StyxConfig.fromYaml(yamlText))
            .build())

    override fun beforeSpec(spec: Spec) {
        styxServer.startAsync().awaitRunning()
    }

    override fun afterSpec(spec: Spec) {
        styxServer.stopAsync().awaitTerminated()
    }
}
