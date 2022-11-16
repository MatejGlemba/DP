# dropmenote

dropmenote server + db + webview


# Requirements

- apache tomcat9
- java11
- gulp node.js (for webview development)
- matrix synapse server
- postgresql database
 
# build server for local

- database configuration dropmenote-ws module resources/wepapp/META-INF/context.xml
- edit matrix server url in dropmenote.ws.constants.ConfigurationConstant
- edit config.properties (dropmenote-ws module / src/main/resources) 
	- web.app.url=http://localhost:8080
	- web.app.urlparam.scan=/?q=
	- image.file.url=http://localhost:8080/resources/dropmenote/files/images/
- mvn clean install (skip tests)
- in InteliJJ create tomcat application (Edit Configurations -> add Tomcat server) 
- load war file (located in dropmenote-ws module / target dir) 

# build server for remote

- database configuration dropmenote-ws module resources/wepapp/META-INF/context.xml
- edit matrix server url in dropmenote.ws.constants.ConfigurationConstant
- edit config.properties (dropmenote-ws module / src/main/resources) 
	- web.app.url=http://{IP:PORT}
	- web.app.urlparam.scan=/?q=
	- image.file.url=http://{IP:PORT}/resources/dropmenote/files/images/
- mvn clean install (skip tests)
- find war file (located in dropmenote-ws module / target dir)

# build webview 

- edit webview domain in welcome.html - (baseUrl,  tutorialUrl)
- edit server url and port in app/App.js (configuration_baseUrl,  configuration_wsUrl)
- gulp build-js
- for using just min-js file , comment all scripts from CSS to Main script in index.html

# deploy

- unzip war file -> copy whole webview dir into unzipped war file (same level as WEB-INF) 
- copy whole dir into apache tomcat9 webapps dir


# references

- DPM server + db code used from https://bobrik_starbug@bitbucket.org/starbugcompany/starbug-dropmenote-ws.git
- matrix java sdk used from https://github.com/kamax-matrix/matrix-java-sdk
