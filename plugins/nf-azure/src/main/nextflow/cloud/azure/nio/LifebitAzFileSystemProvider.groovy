package nextflow.cloud.azure.nio

import com.azure.storage.blob.BlobServiceClient
import groovy.transform.CompileStatic

import java.nio.file.FileSystem
import java.nio.file.FileSystemNotFoundException

@CompileStatic
class LifebitAzFileSystemProvider extends AzFileSystemProvider {

    public static final String SCHEME = 'az'

    public static final String AZURE_IS_MULTI_STORAGE_ENABLED = 'AZURE_IS_MULTI_STORAGE_ENABLED'

    private final Map<String,String> mapping = [:]

    LifebitAzFileSystemProvider() {
        super()
    }

    @Override
    String getScheme() {
        return SCHEME
    }

    @Override
    FileSystem getFileSystem(URI uri) {
        final bucket = this.getContainerName(uri)
        mapping.put(bucket, this.getAccountName(uri))
        getFileSystem0("$bucket",false)
    }


    @Override
    protected AzFileSystem getFileSystem0(String bucket, boolean canCreate) {
        println("LifebitAzFileSystemProvider getFileSystem0 called with bucket: $bucket")
        def accountName = mapping.get(bucket, null)
        def fs = fileSystems.get(bucket)
        if( !fs ) {
            if( canCreate ) {
                def config = this.env.deepClone()

                if(accountName)
                    config.put(AZURE_STORAGE_ACCOUNT_NAME, accountName)
                fs = newFileSystem0(bucket, config)
            } else {
                throw new FileSystemNotFoundException("Missing Azure storage blob file system for bucket: `$bucket`")
            }
        }

        return fs
    }

    @Override
    AzFileSystem newFileSystem(URI uri, Map<String, ?> config) throws IOException {
        println("LifebitAzFileSystemProvider newFileSystem called with bucekt ${uri.authority} - $config")
        final bucket = getContainerName(uri)
        final accountName = getAccountName(uri)
        mapping.put(bucket, accountName)
        def config2 = config.deepClone()

        if(accountName)
            config2.put(AZURE_STORAGE_ACCOUNT_NAME, accountName)
        newFileSystem0("$bucket", config2)
    }

    @Override
    protected AzFileSystem createFileSystem(BlobServiceClient client, String bucket, Map<String,?> config) {
        println("LifebitAzFileSystemProvider createFileSystem $bucket")
        def result = new AzFileSystem(this, client, bucket)
        return result
    }

    protected String getAccountName(URI uri) {
        final name = super.getContainerName(uri)
        return !name.contains('.') ? null: name.split('\\.')[0]
    }

    @Override
    protected String getContainerName(URI uri) {
        final name = super.getContainerName(uri)
        return !name.contains('.') ? name: name.split('\\.')[1]
    }

    @Override
    AzPath getPath(URI uri) {
        println("LifebitAzFileSystemProvider getPath Uri $uri")
        final bucket = this.getContainerName(uri)
        bucket == '/' ? getPath('/') : getPath("${bucket}/${uri.path}")
    }

    @Override
    AzPath getPath(String path) {
        println("LifebitAzFileSystemProvider getPath $path")


        // -- special root bucket
        if( path == '/' ) {
            final fs = getFileSystem0("/",true)
            return new AzPath(fs, "/")
        }

        // -- remove first slash, if any
        while( path.startsWith("/") )
            path = path.substring(1)

        // -- find the first component ie. the container name
        int p = path.indexOf('/')
        String bucket = p==-1 ? path : path.substring(0,p)

        //multiple storage account support
        if(bucket.contains('.')) {
            bucket = bucket.split('\\.')[1]
            path = bucket + path.substring(path.indexOf('/'))

        }
        // -- get the file system
        final fs = getFileSystem0("$bucket",true)

        // create a new path
        new AzPath(fs, "/$path")
    }

}
