package ai.lifebit.extension

import com.azure.identity.ManagedIdentityCredentialBuilder
import com.microsoft.azure.batch.BatchClient
import com.microsoft.azure.batch.auth.BatchCredentials
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.Global
import nextflow.cloud.azure.batch.AzBatchExecutor
import nextflow.cloud.azure.batch.AzBatchService

@Slf4j
@CompileStatic
class LifebitAzBatchService extends AzBatchService {

    public final static String BATCH_ENDPOINT = "https://batch.core.windows.net/"

    LifebitAzBatchService(AzBatchExecutor executor) {
        super(executor)
        println("Lifebit Az Batch Service called...")
    }

    @Override
    protected BatchClient createBatchClient() {
        log.debug "[AZURE BATCH] Executor options=${this.config.batch()}"

        if(this.config.managedIdentity().isConfigured()) {
            def cred = createBatchCredentialsWithManagedIdentity()
            Global.onCleanup((it)->client.protocolLayer().restClient().close())
            return BatchClient.open(cred as BatchCredentials)
        }

        return super.createBatchClient()

    }

    protected createBatchCredentialsWithManagedIdentity() {
        log.debug "[AZURE BATCH] Creating Azure Batch client using Managed Identity credentials"

        def credentialBuilder = new ManagedIdentityCredentialBuilder()
        if(this.config.managedIdentity().resourceId) {
            credentialBuilder.resourceId(this.config.managedIdentity().resourceId)
        } else if(this.config.managedIdentity().resourceId) {
            credentialBuilder.clientId(this.config.managedIdentity().clientId)
        }

        return new MsiCredentials(credentialBuilder.build(), BATCH_ENDPOINT, this.config.batch().endpoint)
    }
}
