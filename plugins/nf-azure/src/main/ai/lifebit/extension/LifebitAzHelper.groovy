package ai.lifebit.extension

import nextflow.cloud.azure.nio.AzPath

import java.nio.file.Path

import com.azure.identity.ManagedIdentityCredentialBuilder
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import groovy.transform.CompileStatic
import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import nextflow.cloud.azure.config.AzConfig
import nextflow.cloud.azure.batch.AzHelper
import nextflow.util.Duration

@CompileStatic
@Slf4j
class LifebitAzHelper {

    public static final String AZURE_IS_MULTI_STORAGE_ENABLED = 'AZURE_IS_MULTI_STORAGE_ENABLED'

    public static final String AZURE_IDENTITY_IS_ENABLED = 'AZURE_IDENTITY_IS_ENABLED'
    public static final String AZURE_IDENTITY_CLIENT_ID = 'AZURE_IDENTITY_CLIENT_ID'
    public static final String AZURE_IDENTITY_RESOURCE_ID = 'AZURE_IDENTITY_RESOURCE_ID'

    @Memoized
    static BlobServiceClient createBlobServiceWithManagedIdentity(Map<String, ?> config, String accountName) {
        log.debug "Creating Azure Blob storage client using Service Principal credentials"

        final endpoint = String.format(Locale.ROOT, "https://%s.blob.core.windows.net", accountName);

        final credentialBuilder = new ManagedIdentityCredentialBuilder()
        if(config.get(AZURE_IDENTITY_RESOURCE_ID)) {
            credentialBuilder.resourceId(config.get(AZURE_IDENTITY_RESOURCE_ID) as String)
        } else if(config.get(AZURE_IDENTITY_CLIENT_ID)) {
            credentialBuilder.clientId(config.get(AZURE_IDENTITY_CLIENT_ID) as String)
        }

        return new BlobServiceClientBuilder()
                .credential(credentialBuilder.build())
                .endpoint(endpoint)
                .buildClient()
    }

    static def Map<String, String> getEnvs(AzConfig config, String sasToken) {
        def copy = new LinkedHashMap<String,String>()

        copy.put('AZ_SAS', "none")
        if(config.managedIdentity().isEnabled) {
            copy.put('AZCOPY_AUTO_LOGIN_TYPE', 'MSI')
            if(config.managedIdentity().resourceId) {
                copy.put('AZCOPY_MSI_RESOURCE_STRING', config.managedIdentity().resourceId)
            } else if(config.managedIdentity().clientId) {
                copy.put('AZCOPY_MSI_CLIENT_ID', config.managedIdentity().clientId)
            }
            copy.put('HOME', '$PWD')
//        } else if(config.activeDirectory().tenantId && config.activeDirectory().servicePrincipalId && config.activeDirectory().servicePrincipalSecret) {
//            copy.put('AZCOPY_AUTO_LOGIN_TYPE', 'SPN')
//            copy.put('AZCOPY_TENANT_ID', config.activeDirectory().tenantId)
//            copy.put('AZCOPY_SPA_APPLICATION_ID', config.activeDirectory().servicePrincipalId)
//            copy.put('AZCOPY_SPA_CLIENT_SECRET', config.activeDirectory().servicePrincipalSecret)
//            copy.put('AZ_SAS', "")
        } else{
            copy.put('AZ_SAS', sasToken)
        }
        return copy
    }

    static def generateContainerSasWithManagedIdentity(Path path, Duration duration) {
        final key = AzHelper.generateUserDelegationKey(az0(path), duration)
        return AzHelper.generateContainerUserDelegationSas(az0(path).containerClient(), duration, key)
    }

    /**
     * This method is clone of az0 method from AzHelper, since it is private. Once that is changed, we can remove this.
     * @param path
     * @return
     */
    static private AzPath az0(Path path){
        if( path !instanceof AzPath )
            throw new IllegalArgumentException("Not a valid Azure path: $path [${path?.getClass()?.getName()}]")
        return (AzPath)path
    }

}
