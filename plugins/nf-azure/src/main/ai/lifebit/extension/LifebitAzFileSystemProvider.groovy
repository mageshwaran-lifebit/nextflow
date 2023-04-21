package ai.lifebit.extension

import groovy.transform.CompileStatic
import nextflow.cloud.azure.nio.AzFileSystemProvider

import java.nio.file.FileSystem

@CompileStatic
class LifebitAzFileSystemProvider extends AzFileSystemProvider {

    public static final String SCHEME = 'az'

    public static final String AZURE_IS_MULTI_STORAGE_ENABLED = 'AZURE_IS_MULTI_STORAGE_ENABLED'

    private Map<String, String> env = System.getenv()

    LifebitAzFileSystemProvider() {
        super()
    }

    @Override
    String getScheme() {
        return SCHEME
    }


    @Override
    FileSystem getFileSystem(URI uri) {
        return super.getFileSystem(uri)
    }


    FileSystem getFileSystem2(URI uri) {
        final isMultiStorageAccountEnabled = env.get(AZURE_IS_MULTI_STORAGE_ENABLED, "false") as Boolean

        if(!isMultiStorageAccountEnabled) {
            return super.getFileSystem(uri)
        }

        def currentAccountName = env.get(AZURE_STORAGE_ACCOUNT_NAME) as String

        def containerName = this.getContainerName(uri)
        env.put(AZURE_STORAGE_ACCOUNT_NAME, containerName.split("::")[0])
        def fs =  super.getFileSystem(new URI(uri.getScheme(), containerName.split("::")[1], uri.getPath(), uri.getQuery(), uri.getFragment()))


        env.put(AZURE_STORAGE_ACCOUNT_NAME, currentAccountName)
        return fs
    }
}
