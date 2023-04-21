package ai.lifebit.extension

import groovy.transform.CompileStatic
import nextflow.cloud.azure.nio.AzFileSystemProvider

@CompileStatic
class AzManagedIdentityOpts {

    private Map<String, String> sysEnv

    boolean isEnabled
    String clientId
    String resourceId

    AzManagedIdentityOpts(Map config, Map<String, String> env = null) {
        assert config != null
        this.sysEnv = env == null ? new HashMap<String, String>(System.getenv()) : env

        this.isEnabled = (config.enabled ?: sysEnv.get(LifebitAzHelper.AZURE_IDENTITY_IS_ENABLED)) as Boolean
        this.clientId = config.clientId ?: sysEnv.get(LifebitAzHelper.AZURE_IDENTITY_CLIENT_ID)
        this.resourceId = config.resourceId ?: sysEnv.get(LifebitAzHelper.AZURE_IDENTITY_RESOURCE_ID)
    }

    Map<String, Object> getEnv() {
        Map<String, Object> props = new HashMap<>();
        props.put(LifebitAzHelper.AZURE_IDENTITY_IS_ENABLED, this.isEnabled)
        props.put(LifebitAzHelper.AZURE_IDENTITY_CLIENT_ID, this.clientId)
        props.put(LifebitAzHelper.AZURE_IDENTITY_RESOURCE_ID, this.resourceId)
        return props
    }

    boolean isConfigured() {
        return this.isEnabled
    }
}
