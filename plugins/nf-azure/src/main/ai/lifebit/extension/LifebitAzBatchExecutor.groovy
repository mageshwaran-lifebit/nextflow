package ai.lifebit.extension

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import nextflow.Global
import nextflow.cloud.azure.batch.AzBatchExecutor
import nextflow.cloud.azure.batch.AzBatchService
import nextflow.cloud.azure.batch.AzHelper
import nextflow.cloud.azure.config.AzConfig
import nextflow.util.ServiceName

@Slf4j
@CompileStatic
@ServiceName('azurebatch')
class LifebitAzBatchExecutor extends AzBatchExecutor {

    private AzConfig config
    private AzBatchService batchService

    LifebitAzBatchExecutor() {
        super()
    }


    @Override
    AzConfig getConfig() {
        return config
    }

    @Override
    AzBatchService getBatchService() {
        return batchService
    }


    @Override
    protected void initBatchService() {
        this.config = AzConfig.getConfig(session)
        this.batchService = new LifebitAzBatchService(this)

        // Generate an account SAS token using either activeDirectory configs or storage account keys
        if (!this.config.managedIdentity().isConfigured() && !config.storage().sasToken) {
            config.storage().sasToken = config.activeDirectory().isConfigured()
                    ? AzHelper.generateContainerSasWithActiveDirectory(workDir, config.storage().tokenDuration)
                    : AzHelper.generateAccountSasWithAccountKey(workDir, config.storage().tokenDuration)
        }

        Global.onCleanup((it) -> batchService.close())
    }
}
