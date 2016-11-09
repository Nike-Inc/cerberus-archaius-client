Cerberus Archaius client library
==========================

This is a shared library that provides an Archaius client for projects using Cerberus.

## Usage

Include in your project:

    compile 'com.nike.cpe:cerberus-archaius-client:<version>'


### Using the Archaius provider

There are two ways in which you can use the Archaius provider that comes with the cerberus client.

1. Add the configuration source to the ConfigurationManager - this way, the properties stored in Cerberus will treated
as just another set of properties in addition to existing sources like property files. All properties can then be 
accessed using the DynamicPropertyFactory.getInstance().getXXX methods.

The code shown below is a sample implementation of AbstractModule that instantiates a PolledConfigurationSource and 
adds it to the existing ConfigurationManager.
	
	public class ArchaiusGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        final PolledConfigurationSource polledConfigurationSource = new CerberusConfigurationSource(
                ArchaiusCerberusClientFactory.getClient(), "path to SDB");
        final AbstractPollingScheduler abstractPollingScheduler = new FixedDelayPollingScheduler(1000, 1000 * 60 * 30,
                true);
        final DynamicConfiguration cerberusConfig = new DynamicConfiguration(polledConfigurationSource,
                abstractPollingScheduler);
        final ConcurrentCompositeConfiguration configInstance = (ConcurrentCompositeConfiguration) ConfigurationManager
                .getConfigInstance();
        configInstance.addConfiguration(cerberusConfig);
    	   }
	}

2. Use DynamicConfiguration as is - the ConfigurationManager instance that exists is not modified in any way, and the
properties stored in Cerberus are read using the cerberusConfig.getXXX methods. This might be useful in cases where the
properties that need to distinct but have their names duplicated across different sources that cannot be worked around -
say, in case of external dependencies using Cerberus, which use a property having the same name. 	

The code snippet shown below is an example of how this can be done.

	   ...
	   ...
	   final PolledConfigurationSource polledConfigurationSourceA = new CerberusConfigurationSource(
                ArchaiusCerberusClientFactory.getClient(), "path to SDB of source A");
       final PolledConfigurationSource polledConfigurationSourceB = new CerberusConfigurationSource(
                ArchaiusCerberusClientFactory.getClient(), "path to SDB of source B");
        final AbstractPollingScheduler abstractPollingScheduler = new FixedDelayPollingScheduler(1000, 1000 * 60 * 30,
                true);
        final DynamicConfiguration cerberusConfigA = new DynamicConfiguration(polledConfigurationSourceA,
                abstractPollingScheduler);
        final DynamicConfiguration cerberusConfigB = new DynamicConfiguration(polledConfigurationSourceB,
                abstractPollingScheduler);
        final String propValueFromSourceA = cerberusConfigA.getString("stringPropKeyNameDuplicated");        
        final String propValueFromSourceB = cerberusConfigB.getString("stringPropKeyNameDuplicated");
        ...
                
In the above example, the property with the name "stringPropKeyNameDuplicated" is found in both sources A and B, 
but are handled distinctly.
