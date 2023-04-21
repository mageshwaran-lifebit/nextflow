package ai.lifebit.extension

import groovy.transform.CompileStatic

@CompileStatic
class LifebitBaseAzConfig {

    private AzManagedIdentityOpts managedIdentityOpts

    LifebitBaseAzConfig(Map azure) {
        println("************** AZ CONFIG: $azure")
        this.managedIdentityOpts = new AzManagedIdentityOpts((Map) azure.managedIdentity ?: Collections.emptyMap())
    }

    AzManagedIdentityOpts managedIdentity() { managedIdentityOpts }
}
