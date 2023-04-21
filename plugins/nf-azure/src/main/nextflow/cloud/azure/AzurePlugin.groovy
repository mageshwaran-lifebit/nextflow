/*
 * Copyright 2021, Microsoft Corp
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
package nextflow.cloud.azure

import ai.lifebit.extension.LifebitAzFileSystemProvider
import groovy.transform.CompileStatic
import nextflow.cloud.azure.nio.AzFileSystemProvider
import nextflow.file.FileHelper
import nextflow.plugin.BasePlugin
import org.pf4j.PluginWrapper

/**
 * Azure cloud plugin for Nextflow
 * 
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class AzurePlugin extends BasePlugin {

    AzurePlugin(PluginWrapper wrapper) {
        super(wrapper)
    }

    @Override
    void start() {
        super.start()
        // register Azure file system
//        FileHelper.getOrInstallProvider(AzFileSystemProvider)
        FileHelper.getOrInstallProvider(LifebitAzFileSystemProvider)
    }
}
