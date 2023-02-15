(azure-page)=

# Azure Cloud

## Requirements

The support for Azure Cloud requires Nextflow version `21.04.0` or later.

(azure-blobstorage)=

## Azure Blob Storage

Nextflow has built-in support for [Azure Blob Storage](https://azure.microsoft.com/en-us/services/storage/blobs/). Files stored in an Azure blob container can be accessed transparently in your pipeline script like any other file in the local file system.

The Blob storage account name and key need to be provided in the Nextflow configuration file as shown below:

```groovy
azure {
    storage {
        accountName = "<YOUR BLOB ACCOUNT NAME>"
        accountKey = "<YOUR BLOB ACCOUNT KEY>"
    }
}
```

Alternatively, the **Shared Access Token** can be specified with the `sasToken` option instead of `accountKey`.

:::{tip}
When creating the Shared Access Token, make sure to allow the resource types `Container` and `Object` and allow the permissions: `Read`, `Write`, `Delete`, `List`, `Add`, `Create`.
:::

:::{tip}
The value of `sasToken` is the token stripped by the character `?` from the beginning of the token.
:::

Once the Blob Storage credentials are set, you can access the files in the blob container like local files by prepending the file path with `az://` followed by the container name. For example, a blob container named `my-data` with a file named `foo.txt` can be specified in your Nextflow script as `az://my-data/foo.txt`.

## Azure File Shares

As of version `nf-azure@0.11.0`, Nextflow has built-in support also for [Azure Files](https://azure.microsoft.com/en-us/services/storage/files/). Files available in the serverless Azure File shares can be mounted concurrently on the nodes of a pool executing the pipeline. These files become immediately available in the file system and can be referred as local files within the processes. This is especially useful when a task needs to access large amounts of data (such as genome indexes) during its execution. An arbitrary number of File shares can be mounted on each pool node.

The Azure File share must exist in the storage account configured for Blob Storage. The name of the source Azure File share and mount path (the destination path where the files are mounted) must be provided. Additional mount options (see the Azure Files documentation) can be set as well for further customisation of the mounting process.

For example:

```groovy
azure {
    storage {
        accountName = "<YOUR BLOB ACCOUNT NAME>"
        accountKey = "<YOUR BLOB ACCOUNT KEY>"
        fileShares {
            <YOUR SOURCE FILE SHARE NAME> {
                mountPath = "<YOUR MOUNT DESTINATION>"
                mountOptions = "<SAME AS MOUNT COMMAND>" //optional
            }
            <YOUR SOURCE FILE SHARE NAME> {
                mountPath = "<YOUR MOUNT DESTINATION>"
                mountOptions = "<SAME AS MOUNT COMMAND>" //optional
            }
        }
    }
}
```

The files in the File share are available to the task in the directory: `<YOUR MOUNT DESTINATION>/<YOUR SOURCE FILE SHARE NAME>`.

For instance, given the following configuration:

```groovy
azure {
    storage {
        // ...

        fileShares {
            dir1 {
                mountPath = "/mnt/mydata/"
            }
        }
    }
}
```

The task can access the File share in `/mnt/mydata/dir1`.

(azure-batch)=

## Azure Batch

[Azure Batch](https://docs.microsoft.com/en-us/azure/batch/) is a managed computing service that allows the execution of containerised workloads in the Azure cloud infrastructure.

Nextflow provides built-in support for Azure Batch, allowing the seamless deployment of Nextflow pipelines in the cloud, in which tasks are offloaded as Batch jobs.

Read the {ref}`Azore Batch executor <azurebatch-executor>` section to learn more about the `azurebatch` executor in Nextflow.

### Get started

1. Create a Batch account in the Azure portal. Take note of the account name and key.
2. Make sure to adjust your quotas to the pipeline's needs. There are limits on certain resources associated with the Batch account. Many of these limits are default quotas applied by Azure at the subscription or account level. Quotas impact the number of Pools, CPUs and Jobs you can create at any given time.
3. Create a Storage account and, within that, an Azure Blob Container in the same location where the Batch account was created. Take note of the account name and key.
4. If you plan to use Azure Files, create an Azure File share within the same Storage account and upload your input data.
5. Associate the Storage account with the Azure Batch account.
6. Make sure every process in your pipeline specifies one or more Docker containers with the {ref}`process-container` directive.
7. Make sure all of your container images are published in a Docker registry that can be accessed by your Azure Batch environment, such as [Docker Hub](https://hub.docker.com/), [Quay](https://quay.io/), or [Azure Container Registry](https://docs.microsoft.com/en-us/azure/container-registry/) .

A minimal Nextflow configuration for Azure Batch looks like the following snippet:

```groovy
process {
    executor = 'azurebatch'
}

azure {
    storage {
        accountName = "<YOUR STORAGE ACCOUNT NAME>"
        accountKey = "<YOUR STORAGE ACCOUNT KEY>"
    }
    batch {
        location = '<YOUR LOCATION>'
        accountName = '<YOUR BATCH ACCOUNT NAME>'
        accountKey = '<YOUR BATCH ACCOUNT KEY>'
        autoPoolMode = true
    }
}
```

In the above example, replace the location placeholder with the name of your Azure region and the account placeholders with the values corresponding to your configuration.

:::{tip}
The list of Azure regions can be found by executing the following Azure CLI command:

```bash
az account list-locations -o table
```
:::

Finally, launch your pipeline with the above configuration:

```bash
nextflow run <PIPELINE NAME> -w az://YOUR-CONTAINER/work
```

Replacing `<PIPELINE NAME>` with a pipeline name e.g. `nextflow-io/rnaseq-nf` and `YOUR-CONTAINER` with a blob container in the storage account defined in the above configuration.

See the [Batch documentation](https://docs.microsoft.com/en-us/azure/batch/quick-create-portal) for further details about the configuration for Azure Batch.

### Pools configuration

When using the `autoPoolMode` option, Nextflow automatically creates a `pool` of compute nodes to execute the jobs in your pipeline. By default, it only uses one compute node of the type `Standard_D4_v3`.

The pool is not removed when the pipeline terminates, unless the configuration setting `deletePoolsOnCompletion = true` is added in your Nextflow configuration file.

Pool specific settings, such as VM type and count, should be provided in the `auto` pool configuration scope, for example:

```groovy
azure {
    batch {
        pools {
            auto {
                vmType = 'Standard_D2_v2'
                vmCount = 10
            }
        }
    }
}
```

:::{warning}
To avoid any extra charges in the Batch account, remember to clean up the Batch pools or use auto scaling.
:::

:::{warning}
Make sure your Batch account has enough resources to satisfy the pipeline's requirements and the pool configuration.
:::

:::{warning}
Nextflow uses the same pool ID across pipeline executions, if the pool features have not changed. Therefore, when using `deletePoolsOnCompletion = true`, make sure the pool is completely removed from the Azure Batch account before re-running the pipeline. The following message is returned when the pool is still shutting down:

```
Error executing process > '<process name> (1)'
Caused by:
    Azure Batch pool '<pool name>' not in active state
```
:::

### Named pools

If you want to have more precise control over the compute node pools used in your pipeline, such as using a different pool depending on the task in your pipeline, you can use the {ref}`process-queue` directive in Nextflow to specify the ID of a Azure Batch compute pool that should be used to execute that process.

The pool is expected to be already available in the Batch environment, unless the setting `allowPoolCreation = true` is provided in the `azure.batch` config scope in the pipeline configuration file. In the latter case, Nextflow will create the pools on-demand.

The configuration details for each pool can be specified using a snippet as shown below:

```groovy
azure {
    batch {
        pools {
            foo {
                vmType = 'Standard_D2_v2'
                vmCount = 10
            }

            bar {
                vmType = 'Standard_E2_v3'
                vmCount = 5
            }
        }
    }
}
```

The above example defines the configuration for two node pools. The first will provision 10 compute nodes of type `Standard_D2_v2`, the second 5 nodes of type `Standard_E2_v3`. See the [Advanced settings](#advanced-settings) below for the complete list of available configuration options.

### Requirements on pre-existing named pools

When Nextflow is configured to use a pool already available in the Batch account, the target pool must satisfy the following requirements:

1. The pool must be declared as `dockerCompatible` (`Container Type` property).
2. The task slots per node must match the number of cores for the selected VM. Otherwise, Nextflow will return an error like "Azure Batch pool 'ID' slots per node does not match the VM num cores (slots: N, cores: Y)".

### Pool autoscaling

Azure Batch can automatically scale pools based on parameters that you define, saving you time and money. With automatic scaling, Batch dynamically adds nodes to a pool as task demands increase, and removes compute nodes as task demands decrease.

To enable this feature for pools created by Nextflow, add the option `autoScale = true` to the corresponding pool configuration scope. For example, when using the `autoPoolMode`, the setting looks like:

```groovy
azure {
    batch {
        pools {
            auto {
                autoScale = true
                vmType = 'Standard_D2_v2'
                vmCount = 5
                maxVmCount = 50
            }
        }
    }
}
```

Nextflow uses the formula shown below to determine the number of VMs to be provisioned in the pool:

```
// Get pool lifetime since creation.
lifespan = time() - time("{{poolCreationTime}}");
interval = TimeInterval_Minute * {{scaleInterval}};

// Compute the target nodes based on pending tasks.
// $PendingTasks == The sum of $ActiveTasks and $RunningTasks
$samples = $PendingTasks.GetSamplePercent(interval);
$tasks = $samples < 70 ? max(0, $PendingTasks.GetSample(1)) : max( $PendingTasks.GetSample(1), avg($PendingTasks.GetSample(interval)));
$targetVMs = $tasks > 0 ? $tasks : max(0, $TargetDedicatedNodes/2);
targetPoolSize = max(0, min($targetVMs, {{maxVmCount}}));

// For first interval deploy 1 node, for other intervals scale up/down as per tasks.
$TargetDedicatedNodes = lifespan < interval ? {{vmCount}} : targetPoolSize;
$NodeDeallocationOption = taskcompletion;
```

The above formula initialises a pool with the number of VMs specified by the `vmCount` option, and scales up the pool on-demand, based on the number of pending tasks, up to `maxVmCount` nodes. If no jobs are submitted for execution, it scales down to zero nodes automatically.

If you need a different strategy, you can provide your own formula using the `scaleFormula` option. See the [Azure Batch](https://docs.microsoft.com/en-us/azure/batch/batch-automatic-scaling) documentation for details.

### Pool nodes

When Nextflow creates a pool of compute nodes, it selects:

- the virtual machine image reference to be installed on the node
- the Batch node agent SKU, a program that runs on each node and provides an interface between the node and the Batch service

Together, these settings determine the Operating System and version installed on each node.

By default, Nextflow creates pool nodes based on CentOS 8, but this behavior can be customised in the pool configuration. Below are configurations for image reference/SKU combinations to select two popular systems.

- Ubuntu 20.04 (default):

  ```groovy
  azure.batch.pools.<name>.sku = "batch.node.ubuntu 20.04"
  azure.batch.pools.<name>.offer = "ubuntu-server-container"
  azure.batch.pools.<name>.publisher = "microsoft-azure-batch"
  ```

- CentOS 8:

  ```groovy
  azure.batch.pools.<name>.sku = "batch.node.centos 8"
  azure.batch.pools.<name>.offer = "centos-container"
  azure.batch.pools.<name>.publisher = "microsoft-azure-batch"
  ```

In the above snippet, replace `<name>` with the name of your Azure node pool.

See the [Advanced settings](#advanced-settings) below and the [Azure Batch nodes](https://docs.microsoft.com/en-us/azure/batch/batch-linux-nodes) documentation for more details.

### Private container registry

As of version `21.05.0-edge`, a private container registry for Docker images can be specified as follows:

```groovy
azure {
    registry {
        server = '<YOUR REGISTRY SERVER>' // e.g.: docker.io, quay.io, <ACCOUNT>.azurecr.io, etc.
        userName = '<YOUR REGISTRY USER NAME>'
        password = '<YOUR REGISTRY PASSWORD>'
    }
}
```

The private registry is an addition, not a replacement, to the existing configuration. Public images from other registries will still be pulled as normal, if they are requested.

:::{note}
When using containers hosted in a private registry, the registry name must also be provided in the container name specified via the {ref}`container <process-container>` directive using the format: `[server]/[your-organization]/[your-image]:[tag]`. Read more about fully qualified image names in the [Docker documentation](https://docs.docker.com/engine/reference/commandline/pull/#pull-from-a-different-registry).
:::

## Active Directory Authentication

As of version ``22.11.0-edge``, [Service Principal](https://learn.microsoft.com/en-us/azure/active-directory/develop/howto-create-service-principal-portal) credentials can optionally be used instead of Shared Keys for Azure Batch and Storage accounts. 

The Service Principal should have the at least the following role assignments:

1. Contributor
2. Storage Blob Data Reader
3. Storage Blob Data Contributor

:::{note}
To assign the necessary roles to the Service Principal, refer to the [official Azure documentation](https://learn.microsoft.com/en-us/azure/role-based-access-control/role-assignments-portal?tabs=current).
:::

The credentials for Service Principal can be specified as follows:

```groovy
azure {
    activeDirectory {
        servicePrincipalId = '<YOUR SERVICE PRINCIPAL CLIENT ID>'
        servicePrincipalSecret = '<YOUR SERVICE PRINCIPAL CLIENT SECRET>'
        tenantId = '<YOUR TENANT ID>'
    }

    storage {
        accountName = '<YOUR STORAGE ACCOUNT NAME>'
    }

    batch {
        accountName = '<YOUR BATCH ACCOUNT NAME>'
        location = '<YOUR BATCH ACCOUNT LOCATION>'
    }
}
```

## Advanced settings

The following configuration options are available:

| Name                                           | Description                                                                                                                                                                                                                           |
| ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `azure.activeDirectory.servicePrincipalId`     | The service principal client ID                                                                                                                                                                                                       |
| `azure.activeDirectory.servicePrincipalSecret` | The service principal client secret                                                                                                                                                                                                   |
| `azure.activeDirectory.tenantId`               | The Azure tenant ID                                                                                                                                                                                                                   |
| `azure.storage.accountName`                    | The blob storage account name                                                                                                                                                                                                         |
| `azure.storage.accountKey`                     | The blob storage account key                                                                                                                                                                                                          |
| `azure.storage.sasToken`                       | The blob storage shared access signature token. This can be provided as an alternative to the `accountKey` setting.                                                                                                                   |
| `azure.storage.tokenDuration`                  | The duration of the shared access signature token created by Nextflow when the `sasToken` option is *not* specified (default: `48h`).                                                                                                 |
| `azure.batch.accountName`                      | The batch service account name.                                                                                                                                                                                                       |
| `azure.batch.accountKey`                       | The batch service account key.                                                                                                                                                                                                        |
| `azure.batch.endpoint`                         | The batch service endpoint e.g. `https://nfbatch1.westeurope.batch.azure.com`.                                                                                                                                                        |
| `azure.batch.location`                         | The name of the batch service region, e.g. `westeurope` or `eastus2`. This is not needed when the endpoint is specified.                                                                                                              |
| `azure.batch.autoPoolMode`                     | Enable the automatic creation of batch pools depending on the pipeline resources demand (default: `true`).                                                                                                                            |
| `azure.batch.allowPoolCreation`                | Enable the automatic creation of batch pools specified in the Nextflow configuration file (default: `false`).                                                                                                                         |
| `azure.batch.deleteJobsOnCompletion`           | Enable the automatic deletion of jobs created by the pipeline execution (default: `true`).                                                                                                                                            |
| `azure.batch.deletePoolsOnCompletion`          | Enable the automatic deletion of compute node pools upon pipeline completion (default: `false`).                                                                                                                                      |
| `azure.batch.copyToolInstallMode`              | Specify where the `azcopy` tool used by Nextflow. When `node` is specified it's copied once during the pool creation. When `task` is provider, it's installed for each task execution (default: `node`).                              |
| `azure.batch.pools.<name>.publisher`           | Specify the publisher of virtual machine type used by the pool identified with `<name>` (default: `microsoft-azure-batch`, requires `nf-azure@0.11.0`).                                                                               |
| `azure.batch.pools.<name>.offer`               | Specify the offer type of the virtual machine type used by the pool identified with `<name>` (default: `centos-container`, requires `nf-azure@0.11.0`).                                                                               |
| `azure.batch.pools.<name>.sku`                 | Specify the ID of the Compute Node agent SKU which the pool identified with `<name>` supports (default: `batch.node.centos 8`, requires `nf-azure@0.11.0`).                                                                           |
| `azure.batch.pools.<name>.vmType`              | Specify the virtual machine type used by the pool identified with `<name>`.                                                                                                                                                           |
| `azure.batch.pools.<name>.vmCount`             | Specify the number of virtual machines provisioned by the pool identified with `<name>`.                                                                                                                                              |
| `azure.batch.pools.<name>.maxVmCount`          | Specify the max of virtual machine when using auto scale option.                                                                                                                                                                      |
| `azure.batch.pools.<name>.autoScale`           | Enable autoscaling feature for the pool identified with `<name>`.                                                                                                                                                                     |
| `azure.batch.pools.<name>.fileShareRootPath`   | If mounting File Shares, this is the internal root mounting point. Must be `/mnt/resource/batch/tasks/fsmounts` for CentOS nodes or `/mnt/batch/tasks/fsmounts` for Ubuntu nodes (default is for CentOS, requires `nf-azure@0.11.0`). |
| `azure.batch.pools.<name>.scaleFormula`        | Specify the scale formula for the pool identified with `<name>`. See Azure Batch [scaling documentation](https://docs.microsoft.com/en-us/azure/batch/batch-automatic-scaling) for details.                                           |
| `azure.batch.pools.<name>.scaleInterval`       | Specify the interval at which to automatically adjust the Pool size according to the autoscale formula. The minimum and maximum value are 5 minutes and 168 hours respectively (default: `10 mins`).                                  |
| `azure.batch.pools.<name>.schedulePolicy`      | Specify the scheduling policy for the pool identified with `<name>`. It can be either `spread` or `pack` (default: `spread`).                                                                                                         |
| `azure.batch.pools.<name>.privileged`          | Enable the task to run with elevated access. Ignored if `runAs` is set (default: `false`).                                                                                                                                            |
| `azure.batch.pools.<name>.runAs`               | Specify the username under which the task is run. The user must already exist on each node of the pool.                                                                                                                               |
| `azure.registry.server`                        | Specify the container registry from which to pull the Docker images (default: `docker.io`, requires `nf-azure@0.9.8`).                                                                                                                |
| `azure.registry.userName`                      | Specify the username to connect to a private container registry (requires `nf-azure@0.9.8`).                                                                                                                                          |
| `azure.registry.password`                      | Specify the password to connect to a private container registry (requires `nf-azure@0.9.8`).                                                                                                                                          |
| `azure.retryPolicy.delay`                      | Delay when retrying failed API requests (default: `500ms`).                                                                                                                                                                           |
| `azure.retryPolicy.maxDelay`                   | Max delay when retrying failed API requests (default: `60s`).                                                                                                                                                                         |
| `azure.retryPolicy.jitter`                     | Jitter value when retrying failed API requests (default: `0.25`).                                                                                                                                                                     |
| `azure.retryPolicy.maxAttempts`                | Max attempts when retrying failed API requests (default: `10`).                                                                                                                                                                       |