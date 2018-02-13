# Cerberus Archaius Client

[ ![Download](https://api.bintray.com/packages/nike/maven/cerberus-archaius-client/images/download.svg) ](https://bintray.com/nike/maven/cerberus-archaius-client/_latestVersion)
[![][travis img]][travis]
[![Coverage Status](https://coveralls.io/repos/github/Nike-Inc/cerberus-archaius-client/badge.svg)](https://coveralls.io/github/Nike-Inc/cerberus-archaius-client)
[![][license img]][license]

A java based client library that surfaces Cerberus secrets via Archaius.

[Archaius](https://github.com/Netflix/archaius) is a configuration management library created by Netflix which 
enables dynamic runtime properties from multiple configuration sources such as URLs, JDBC, and Amazon DynamoDB.

To learn more about Cerberus, please visit the [Cerberus website](http://engineering.nike.com/cerberus/).

## Quickstart

### Using the Archaius Provider

There are two ways in which you can use the Archaius provider that comes with the cerberus client.

#### CerberusConfigurationSource or NamespacedCerberusConfigurationSource

There are two configuration source classes provided by this client.  The major difference is how the properties pulled 
from Cerberus are keyed.

**CerberusConfigurationSource**

Properties pulled from Cerberus and added to the configuration source will be keyed exactly the same way they are in
Cerberus.

For example, if you configure this to pull properties from the a path, such as `app/demo/config`, each key/value pair
at that node will be exposed as-is throughout all of Archaius.  If there are no chances of a collision or things are 
scoped appropriately, this may not be an issue.

**NamespacedCerberusConfigurationSource**

Properties pulled from Cerberus and added to the configuration source will be keyed using the full path to the property.

For example, if we have property `foo` and `bar` at the path, `app/demo/config`, each will be added to the configuration 
source with the key of `app.demo.config.foo` and `app.demo.config.bar` respectively.  This is useful in mitigating the 
chances for property name collision in Archaius.  

*The examples below use NamespacedCerberusConfigurationSource*

#### Add the Configuration Source to the ConfigurationManager

Using this pattern, the properties stored in Cerberus will treated as just another set of properties in addition to 
existing sources like property files. All properties can then be accessed using the 
DynamicPropertyFactory.getInstance().getXXX methods. This can be achieve with either nonpolling or polling configuration.

##### Nonpolling configuration (Recommended)

Most applications do not need to poll Cerberus for changes.  If secrets change, the instances can simply be restarted
or redeployed to pick up new values.
The code shown below is a sample implementation of AbstractModule that instantiates a ConcurrentMapConfiguration and 
adds it to the existing ConfigurationManager.

``` java
    public class CerberusArchaiusModule extends AbstractModule {
        private final Logger logger = LoggerFactory.getLogger(getClass());
    
        private static final String SECRETS_PATH_PROP_NAME = "cerberus.config.path";
        private static final String SECRETS_DEFAULT_VAULT_PATH = "app/cerberus-demo/config";
    
        @Override
        protected void configure() {
            /*
             * First let's look up the path for where to read in Cerberus.
             * This examples show us attempting to source it from an Archaius property, but defaulting if not found.
             */
            final String vaultPath = DynamicPropertyFactory.getInstance()
                    .getStringProperty(SECRETS_PATH_PROP_NAME, SECRETS_DEFAULT_VAULT_PATH)
                    .get();
    
            /*
             * So long as we've got a path, let's pull the config into Archaius from Cerberus.
             */
            if (vaultPath != null && !vaultPath.isEmpty()) {
                logger.info("Adding Cerberus as Configuration source. Vault Path = " + vaultPath);
    
                /*
                 * Create a configuration source passing in a Cerberus client using the ArchaiusCerberusClientFactory and
                 * the path we looked up above.  This factory will attempt to resolve cerberus configuration detailts,
                 * like the URL and token from Archaius properties.
                 */
                final NamespacedCerberusConfigurationSource namespacedCerberusConfigurationSource = new NamespacedCerberusConfigurationSource(
                        ArchaiusCerberusClientFactory.getClient(), vaultPath);
                      
                /*
                 * Read secrets from Cerberus.
                 */
                AbstractConfiguration cerberusConfig = null;
                try {
                     cerberusConfig = namespacedCerberusConfigurationSource.getConfig();
                } catch (Exception e) {
                    throw new RuntimeException("Unable to read secrets from Cerberus", e);
                }
    
                final ConcurrentCompositeConfiguration configInstance = (ConcurrentCompositeConfiguration) ConfigurationManager
                        .getConfigInstance();
    
                configInstance.addConfiguration(cerberusConfig);
            } else {
                logger.info("Property corresponding to the Vault path for secrets not found! "
                        + "Cerberus not added as Configuration source");
            }
        }
    }
```

##### Polling configuration

Polling allows an application to pick up changes dynamically at runtime.  Usually this is not needed because simply
restarting or redeploying is good enough for most applications.
The code shown below is a sample implementation of AbstractModule that instantiates a PolledConfigurationSource and 
adds it to the existing ConfigurationManager.

``` java
    public class CerberusArchaiusModule extends AbstractModule {
        private final Logger logger = LoggerFactory.getLogger(getClass());
    
        private static final int POLL_INIT_DELAY = 1000;
        private static final int SECRETS_POLL_INTERVAL = (int) TimeUnit.HOURS.toMillis(1);
        private static final String SECRETS_PATH_PROP_NAME = "cerberus.config.path";
        private static final String SECRETS_DEFAULT_VAULT_PATH = "app/cerberus-demo/config";
    
        @Override
        protected void configure() {
            /*
             * First let's look up the path for where to read in Cerberus.
             * This examples show us attempting to source it from an Archaius property, but defaulting if not found.
             */
            final String vaultPath = DynamicPropertyFactory.getInstance()
                    .getStringProperty(SECRETS_PATH_PROP_NAME, SECRETS_DEFAULT_VAULT_PATH)
                    .get();
    
            /*
             * So long as we've got a path, let's configure the polling of config from Cerberus into Archaius.
             */
            if (vaultPath != null && !vaultPath.isEmpty()) {
                logger.info("Adding Cerberus as Configuration source. Vault Path = " + vaultPath);
    
                /*
                 * Create a configuration source passing in a Cerberus client using the ArchaiusCerberusClientFactory and
                 * the path we looked up above.  This factory will attempt to resolve cerberus configuration detailts,
                 * like the URL and token from Archaius properties.
                 */
                final PolledConfigurationSource polledConfigurationSource = new NamespacedCerberusConfigurationSource(
                        ArchaiusCerberusClientFactory.getClient(), vaultPath);
    
                /*
                 * Setup the scheduler for how often this configuration source should be refreshed.
                 */
                final AbstractPollingScheduler abstractPollingScheduler = new FixedDelayPollingScheduler(
                        POLL_INIT_DELAY, SECRETS_POLL_INTERVAL, true);
    
                /*
                 * Wrap that in a DynamicConfiguration object and add it to the configuration instance.
                 */
                final DynamicConfiguration cerberusConfig = new DynamicConfiguration(polledConfigurationSource,
                        abstractPollingScheduler);
    
                final ConcurrentCompositeConfiguration configInstance = (ConcurrentCompositeConfiguration) ConfigurationManager
                        .getConfigInstance();
    
                configInstance.addConfiguration(cerberusConfig);
            } else {
                logger.info("Property corresponding to the Vault path for secrets not found! "
                        + "Cerberus not added as Configuration source");
            }
        }
    }
```

##### Use a DynamicConfiguration object

With this pattern, the ConfigurationManager instance that exists is not modified in any way, and the
properties stored in Cerberus are read using the cerberusConfig.getXXX methods. This might be useful in cases where the
properties that need to distinct but have their names duplicated across different sources that cannot be worked around -
say, in case of external dependencies using Cerberus, which use a property having the same name. 	

The code snippet shown below is an example of how this can be done.

``` java
    public class CerberusArchaiusModule extends AbstractModule {
        private final Logger logger = LoggerFactory.getLogger(getClass());
    
        private static final int POLL_INIT_DELAY = 1000;
        private static final int SECRETS_POLL_INTERVAL = (int) TimeUnit.HOURS.toMillis(1);
        private static final String SECRETS_PATH_PROP_NAME = "cerberus.config.path";
        private static final String SECRETS_DEFAULT_VAULT_PATH = "app/cerberus-demo/config";
    
        @Override
        protected void configure() {
            /*
             * First let's look up the path for where to read in Cerberus.
             * This examples show us attempting to source it from an Archaius property, but defaulting if not found.
             */
            final String vaultPath = DynamicPropertyFactory.getInstance()
                    .getStringProperty(SECRETS_PATH_PROP_NAME, SECRETS_DEFAULT_VAULT_PATH)
                    .get();
    
            /*
             * So long as we've got a path, let's configure the polling of config from Cerberus into Archaius.
             */
            if (vaultPath != null && !vaultPath.isEmpty()) {
                logger.info("Adding Cerberus as Configuration source. Vault Path = " + vaultPath);
    
                /*
                 * Create a configuration source passing in a Cerberus client using the ArchaiusCerberusClientFactory and
                 * the path we looked up above.  This factory will attempt to resolve cerberus configuration detailts,
                 * like the URL and token from Archaius properties.
                 */
                final PolledConfigurationSource polledConfigurationSource = new NamespacedCerberusConfigurationSource(
                        ArchaiusCerberusClientFactory.getClient(), vaultPath);
    
                /*
                 * Setup the scheduler for how often this configuration source should be refreshed.
                 */
                final AbstractPollingScheduler abstractPollingScheduler = new FixedDelayPollingScheduler(
                        POLL_INIT_DELAY, SECRETS_POLL_INTERVAL, true);
    
                /*
                 * Wrap that in a DynamicConfiguration object and add it to the configuration instance.
                 */
                final DynamicConfiguration cerberusConfig = new DynamicConfiguration(polledConfigurationSource,
                        abstractPollingScheduler);
    
                /*
                 * Now we bind that dynamic configuration so that it can be used elsewhere within the context.
                 */
                bind(DynamicConfiguration.class)
                        .annotatedWith(Names.named("cerberus.config"))
                        .toInstance(cerberusConfig);
            } else {
                logger.info("Property corresponding to the Vault path for secrets not found! "
                        + "Cerberus not added as Configuration source");
            }
        }
    }
```

## Further Details

Cerberus Archaius client is a small project. It only has a few classes and they are all fully documented. For further details please see the source code, including javadocs and unit tests.

<a name="license"></a>
## License

Cerberus Archaius client is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

[travis]:https://travis-ci.org/Nike-Inc/cerberus-archaius-client
[travis img]:https://api.travis-ci.org/Nike-Inc/cerberus-archaius-client.svg?branch=master

[license]:LICENSE.txt
[license img]:https://img.shields.io/badge/License-Apache%202-blue.svg

[toc]:#table_of_contents
