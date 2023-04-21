package nextflow.cloud.azure.nio

import com.azure.storage.blob.BlobServiceClient
import groovy.transform.CompileStatic

import java.nio.file.FileSystem
import java.nio.file.FileSystemNotFoundException

@CompileStatic
class LifebitAzFileSystemProvider extends AzFileSystemProvider {

    public static final String SCHEME = 'az'

    public static final String AZURE_IS_MULTI_STORAGE_ENABLED = 'AZURE_IS_MULTI_STORAGE_ENABLED'


    LifebitAzFileSystemProvider() {
        super()
    }

    @Override
    String getScheme() {
        return SCHEME
    }

    @Override
    FileSystem getFileSystem(URI uri) {
        final accountName = getAccountName(uri)
        final bucket = getContainerName(uri)
        getFileSystem0("$accountName::$bucket",false)
    }


    @Override
    protected AzFileSystem getFileSystem0(String key, boolean canCreate) {
        println("LifebitAzFileSystemProvider getFileSystem0 called with Key: $key")
        def fs = fileSystems.get(key)
        if( !fs ) {
            if( canCreate ) {
                def config = this.env.deepClone()
                config.put(AZURE_STORAGE_ACCOUNT_NAME, key.split('::')[1])
//                config.put(AZURE_STORAGE_CONTAINER_NAME, getContainerName2(uri))
                fs = newFileSystem0(key, config)
            } else {
                throw new FileSystemNotFoundException("Missing Azure storage blob file system for Key: `$key`")
            }
        }

        return fs
    }

    @Override
    AzFileSystem newFileSystem(URI uri, Map<String, ?> config) throws IOException {
        println("LifebitAzFileSystemProvider newFileSystem called with bucekt ${uri.authority} - $config")
        final bucket = getContainerName(uri)
        final accountName = getAccountName(uri)
        def config2 = config.deepClone()
        config2.put(AZURE_STORAGE_ACCOUNT_NAME, accountName)
        newFileSystem0("$accountName::$bucket", config2)
    }

    @Override
    protected AzFileSystem createFileSystem(BlobServiceClient client, String key, Map<String,?> config) {
        println("LifebitAzFileSystemProvider createFileSystem $key")
        def result = new AzFileSystem(this, client, key.split('::').length > 1? key.split('::')[1] : key)
        return result
    }

    protected String getAccountName(URI uri) {
        return super.getContainerName(uri)
    }

    @Override
    protected String getContainerName(URI uri) {
        return super.getContainerName(new URI(SCHEME, uri.path.substring(1), uri.query, uri.fragment))
    }

//    @Override
//    AzPath getPath(URI uri) {
//        println("LifebitAzFileSystemProvider getPath Uri $uri")
//        final bucket = this.getContainerName(uri)
//        bucket == '/' ? getPath('/') : getPath("${getAccountName(uri)}/${uri.path}")
//    }

    @Override
    AzPath getPath(String path) {
        println("LifebitAzFileSystemProvider getPath $path")

        /** GET ACCOUNT NAME */
        while( path.startsWith("/") )
            path = path.substring(1)

        // -- find the first component ie. the container name
        URI uri = new URI("$SCHEME://$path")
        final accountName = uri.authority
        path = uri.path
        /** GET ACCOUNT NAME */

        // -- special root bucket
        if( path == '/' ) {
            final fs = getFileSystem0("$accountName::/",true)
            return new AzPath(fs, "/")
        }

        // -- remove first slash, if any
        while( path.startsWith("/") )
            path = path.substring(1)

        // -- find the first component ie. the container name
        int p = path.indexOf('/')
        final bucket = p==-1 ? path : path.substring(0,p)

        // -- get the file system
        final fs = getFileSystem0("$accountName::$bucket",true)

        // create a new path
        new AzPath(fs, "/$path")
    }

}
