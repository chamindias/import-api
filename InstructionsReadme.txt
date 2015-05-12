This is the Git repo of the import/export functions, taken from 

https://github.com/sanjeewa-malalgoda/restful-apim/tree/master/webapp

###########################################################################################

Go to the “webapp” folder and locate the pom.xml

Add the following to the pom.xml

<repository>
<id>wso2-nexus</id>
<name>WSO2 internal Repository</name>
<url>http://maven.wso2.org/nexus/content/groups/wso2-public/</url>
<releases>
<enabled>true</enabled>
<updatePolicy>daily</updatePolicy>
<checksumPolicy>warn</checksumPolicy>
</releases>
</repository>


If you need to only call @GET or @POST without calling the constructor, edit the APIService.java as follows. (path - src/main/java/demo/jaxrs/server)

# Copy and paste the content in the constructor into @GET and @POST methods.

For each edit in “APIService.java”, 
it is needed to rebuild the pom.xml in webapp folder.
Copy the “APIManager.war” in webapp/target folder to  <APIM home>/repository/deployment/server/webapps.

Then run ./wso2server.sh from the bin folder of the <APIM home>


====================================================================


https://docs.google.com/document/d/16iG9BsT25E4nFOVWCWzyETlNteTHtDsypIQtU-iYa5Y/edit
