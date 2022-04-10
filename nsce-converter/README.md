**Generated by [Diesel](http://go.up.com/diesel):** `yo diesel-templates:event-handler`

# nsce-converter

This project is a simple implementation for an EEB 2.0 event handler running in ICE.

Additional features can be found in the EEB 2.0 Quick Start Guide at [go/eeb2start](http://wiki.www.uprr.com/confluence/x/eQV7Bg)

Look through the project for TODO's that you need to fill in (one's even in this project)

# Get started

1. Update your JMS queue and connection factory in `src/main/resources/application.properties`
2. Set up credentials to connect to JMS to receive messages [(see below)](https://git.tla.uprr.com/managed/100400/101850/edit/master/projects/event-handler/README.md#setting-up-credentials-to-connect-to-jms-to-receive-messages)
3. Add your bindings dependency and update `InboundMessageListenerConfig`
4. Setup `EventProcessorService` with your actual configuration
5. Deploy to ICE!

## Setting up credentials to connect to JMS to receive messages

Pre-work: Create app id for JMS via SID/JIRA to JMS or identify existing app id to reuse.

### OPTION 1 (Preferred): Use cyberark

1. Set up cyberark on your ICE cube as documented on the "console" tab of your ICE cube
2. Edit application-dev.properties to pass in the applicationId configured for you by cyberark set-up.
3. Repeat steps 1 & 2 for test and prod.

### OPTION 2 (Faster but less secure): Hard-code credentials

1. Edit application-dev.properties setting jms.listener.credential.store to value "simple"
2. Edit application-dev.properties setting jms.listener.applicationId to your app id
3. Edit application-dev.properties adding jms.listener.password set to your application id's password for connecting to JMS
4. Repeat steps 1-3 for test and prod property files

# Deployment to ICE

```
mvn deploy
```

_DISABLE MANAGED LOGGING_ the uprr-logging library supersedes current managed logging,
and a new ICE image (2.0) is coming out to more gracefully handle the intent of managed
logging.

# Features

- SpringBoot usage using Java Configuration (with AutoScan)
- JMS credentials retrieved from CyberArk
- Simple event handler implementation using [EEB2 Subscriber Toolkit][eeb-sub]
- Error handling via Exponential JMS Backoff Retry, see details at [Exponential JMS Retry Listener] and [Configuration]
- Standard ICE deployment hooks included in POM
- EEB2 event publishing using [EEB2 Publisher Toolkit][eeb-pub]

# Points of Interest

- com.uprr.pac.event_handler.Application is the main application
- Error handling is com.uprr.enterprise.exponential_backoff.jms_retry.ExponentialBackoffMessageRetryingListenerAdapter
- ESB 2.0 client configuration is provided in the class com.uprr.pac.service.common.config.ClientConfig. This uses a customized version of RestTemplate which includes, among other UP specific customizations, UAuth token in the request header for authentication. The customizer uses "UP JAAS module" to fetch the token. If you are testing in a local windows machine or a machine where "UP JAAS Module" is not configured, like Jenkins server, there  is a fallback mechanism (using Remote Siteminder service) to fetch the token. In order to opt in for RemoteTokenProvider, use the following environment variable in tests:
	
	-Dremote.token.provider.enabled=true


# TODO

- Payload logging should be on by default.
- TrustInterceptor contains ESB trust checking
- Switch to community payload converters when available for 1.X.X toolkits [Community Payloads][eeb-converter]

[eeb-sub]: http://wiki.www.uprr.com/confluence/x/TAR1Bw
[eeb-converter]: http://git-dev.uit.tla.uprr.com:10080/eeb2/eeb2-community-payload-converters
[eeb-pub]: http://wiki.www.uprr.com/confluence/x/on1kBw
[Exponential JMS Retry Listener]: http://wiki.www.uprr.com/confluence/x/ziMDCQ
[Configuration]: http://wiki.www.uprr.com/confluence/x/-yoDCQ