/*
 *
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package apim.restful.exportimport.utils;

import apim.restful.exportimport.APIImportConstants;
import apim.restful.exportimport.APIService;
import com.google.gson.Gson;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.FaultGatewaysException;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;

import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * This class provides the functions utilized to import an API from an API archive.
 */
public final class APIImportUtil {

    private static final Log log = LogFactory.getLog(APIService.class);
    static APIProvider provider;

    /**
     * This method initializes the Provider when there is a direct request to import an API
     */
    public static void initializeProvider() throws APIManagementException {
        provider = APIManagerFactory.getInstance().getAPIProvider(APIImportConstants.PROVIDER_NAME);
    }

    /**
     * This method uploads a given file to specified location
     *
     * @param uploadedInputStream input stream of the file
     * @param newFileName         name of the file to be created
     * @param storageLocation     destination of the new file
     */
    public static void transferFile(InputStream uploadedInputStream, String newFileName, String storageLocation)
            throws APIManagementException {
        FileOutputStream outFileStream = null;
        try {
            outFileStream = new FileOutputStream(new File(storageLocation, newFileName));
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                outFileStream.write(bytes, 0, read);
            }
        } catch (IOException e) {
            log.error("Error in transferring files.");
            throw new APIManagementException("Error in transferring files.", e);
        } finally {
            try {
                if (outFileStream != null) {
                    outFileStream.flush();
                    outFileStream.close();
                }
            } catch (IOException e) {
                log.error("Error in closing output streams while transferring files.");
            }
        }
    }

    /**
     * This method decompresses the archive
     *
     * @param sourceFile           the archive containing the API
     * @param destinationDirectory location of the archive to be extracted
     * @return extractedFolder the name of the zip
     */
    public static String unzipArchive(File sourceFile, File destinationDirectory) throws APIManagementException {

        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        File destinationFile;
        ZipFile zipfile = null;
        String extractedFolder = null;

        try {
            zipfile = new ZipFile(sourceFile);
            Enumeration zipEntries = null;
            if (zipfile != null) {
                zipEntries = zipfile.entries();
            }
            if (zipEntries != null) {
                int index = 0;
                while (zipEntries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) zipEntries.nextElement();
                    if (entry.isDirectory()) {
                        //This index variable is used to get the extracted folder name.
                        if (index == 0) {
                            extractedFolder = entry.getName().substring(0, entry.getName().length() - 1);
                        }
                        index = -1;
                        new File(destinationDirectory, entry.getName()).mkdir();
                        continue;
                    }
                    inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
                    destinationFile = new File(destinationDirectory, entry.getName());
                    fileOutputStream = new FileOutputStream(destinationFile);
                    copyStreams(inputStream, fileOutputStream);
                }
            }
        } catch (ZipException e) {
            log.error("Error in retrieving archive files.");
            throw new APIManagementException("Error in retrieving archive files.", e);
        } catch (IOException e) {
            log.error("Error in decompressing API archive files.");
            throw new APIManagementException("Error in decompressing API archive files.", e);
        } finally {
            try {
                if (zipfile != null) {
                    zipfile.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                log.error("Error in closing streams while decompressing files.");
            }
        }
        return extractedFolder;
    }

    /**
     * This method copies data from input stream and writes to output stream
     *
     * @param inStream  the input stream of the file to be written
     * @param outStream the output stream of the file to be written
     * @throws APIManagementException by org.wso2.carbon.apimgt.api.APIManagementException
     */

    private static void copyStreams(InputStream inStream, FileOutputStream outStream) throws APIManagementException {
        int count;
        byte data[] = new byte[1024];
        try {
            while ((count = inStream.read(data, 0, 1024)) != -1) {
                outStream.write(data, 0, count);
            }
        } catch (IOException e) {
            log.error("Error in copying API archive files.");
            throw new APIManagementException("Error in copying API archive files.", e);
        }
    }

    /**
     * This method imports an API to the API store
     *
     * @param pathToArchive location of the JSON file representing the API
     */
    public static void importAPI(String pathToArchive) throws APIManagementException {

        Gson gson = new Gson();
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;

        try {
            inputStream = new FileInputStream(pathToArchive + APIImportConstants.JSON_FILE_LOCATION);
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            API importedApi = gson.fromJson(bufferedReader, API.class);
            APIIdentifier apiIdentifier = importedApi.getId();

            Set<Tier> tierSet=importedApi.getAvailableTiers();

            for(Tier x:tierSet){
                System.out.println("************** " + x.getName());
            }


            //Creating API
            provider.addAPI(importedApi);

            //Adding image icon to the API if there is any
            File imageFolder = new File(pathToArchive + APIImportConstants.IMAGE_FILE_LOCATION);
            if (imageFolder.isDirectory() && imageFolder.listFiles() != null && imageFolder.listFiles().length > 0) {
                for (File imageFile : imageFolder.listFiles()) {
                    if (imageFile.getName().contains(APIImportConstants.IMAGE_FILE_NAME)) {
                        String fileExtension = FilenameUtils.getExtension(imageFile.getAbsolutePath());
                        inputStream = new FileInputStream(imageFile.getAbsolutePath());
                        Icon apiImage = new Icon(inputStream, fileExtension);
                        String imageRegistryLocation = provider.addIcon(imageFile.getAbsolutePath(), apiImage);
                        importedApi.setThumbnailUrl(APIUtil.prependTenantPrefix
                                (imageRegistryLocation, importedApi.getId().getProviderName()));
                        APIUtil.setResourcePermissions(importedApi.getId().getProviderName(), null, null,
                                imageRegistryLocation);
                        provider.updateAPI(importedApi);
                    }
                }
            }

            //Adding document(s) to the API if there are any
            String docFileLocation = pathToArchive + APIImportConstants.DOCUMENT_FILE_LOCATION;
            if (checkFileExistence(docFileLocation)) {
                inputStream = new FileInputStream(docFileLocation);
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                Documentation[] documentations = gson.fromJson(bufferedReader, Documentation[].class);
                //For each type of document, separate actions are preformed
                for (Documentation doc : documentations) {
                    if (doc.getSourceType().toString().equalsIgnoreCase(APIImportConstants.INLINE_DOC_TYPE)) {
                        provider.addDocumentation(apiIdentifier, doc);
                        provider.addDocumentationContent(importedApi, doc.getName(), doc.getSummary());
                    } else if (doc.getSourceType().toString().equalsIgnoreCase(APIImportConstants.URL_DOC_TYPE)) {
                        provider.addDocumentation(apiIdentifier, doc);
                    } else if (doc.getSourceType().toString().equalsIgnoreCase(APIImportConstants.FILE_DOC_TYPE)) {
                        inputStream = new FileInputStream(pathToArchive + doc.getFilePath());
                        String fileNameTokens[] = doc.getName().split("\\.");
                        Icon apiDocument = new Icon(inputStream, fileNameTokens[fileNameTokens.length - 1]);
                        String visibleRolesList = importedApi.getVisibleRoles();
                        String[] visibleRoles = new String[0];
                        if (visibleRolesList != null) {
                            visibleRoles = visibleRolesList.split(",");
                        }
                        String filePathDoc = APIUtil.getDocumentationFilePath(apiIdentifier, doc.getName());
                        APIUtil.setResourcePermissions(importedApi.getId().getProviderName(),
                                importedApi.getVisibility(), visibleRoles, filePathDoc);
                        doc.setFilePath(provider.addIcon(filePathDoc, apiDocument));
                        provider.addDocumentation(apiIdentifier, doc);
                    }
                }
            }


            //Adding sequences into the API, if there are any
            String inSequenceFileLocation = pathToArchive + APIImportConstants.IN_SEQUENCE_LOCATION;
            //Adding in-sequence, if any
            if (checkFileExistence(inSequenceFileLocation)) {
                importedApi.setInSequence(APIImportConstants.IN_SEQUENCE_NAME);
                provider.updateAPI(importedApi);
            }
            //Adding out-sequence, if any
            String outSequenceFileLocation = pathToArchive + APIImportConstants.OUT_SEQUENCE_LOCATION;
            if (checkFileExistence(outSequenceFileLocation)) {
                importedApi.setOutSequence(APIImportConstants.OUT_SEQUENCE_NAME);
                provider.updateAPI(importedApi);
            }
            //Adding fault-sequence, if any
            String faultSequenceFileLocation = pathToArchive + APIImportConstants.FAULT_SEQUENCE_LOCATION;
            if (checkFileExistence(faultSequenceFileLocation)) {
                importedApi.setFaultSequence(APIImportConstants.FAULT_SEQUENCE_NAME);
                provider.updateAPI(importedApi);
            }

        } catch (FileNotFoundException e) {
            log.error("Error in importing API.");
            throw new APIManagementException("Error in importing API.", e);
        } catch (FaultGatewaysException e) {
            log.error("Gateway error in importing API.");
            throw new APIManagementException("Gateway error in importing API.", e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                log.error("Error in closing streams.");
            }
        }
    }

    /**
     * This method checks whether a given file exists
     *
     * @param fileLocation location of the file
     * @return true if the file exists, false otherwise
     */
    private static boolean checkFileExistence(String fileLocation) {
        File testFile = new File(fileLocation);
        return testFile.exists();
    }

    //Mapping??


}
