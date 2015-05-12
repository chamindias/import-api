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

package apim.restful.exportimport;


import com.sun.jersey.multipart.FormDataParam;
import apim.restful.exportimport.utils.APIImportUtil;
import org.wso2.carbon.apimgt.api.APIManagementException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.InputStream;

/**
 * This class provides the service to import an API from an API archive.
 */
@Path("/")
public class APIService {

    /**
     * @param uploadedInputStream input stream from the REST request
     * @return response indicating the status of the process
     */
    @POST
    @Path("/import-api")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAPI(@FormDataParam("file") InputStream uploadedInputStream) {

        try{
            APIImportUtil.initializeProvider();
            String currentDirectory = System.getProperty("user.dir");
            String createdFolders = APIImportConstants.CREATED_FOLDER;
            File folderPath = new File(currentDirectory + createdFolders);
            boolean folderCreateStatus = folderPath.mkdirs();
            if (folderCreateStatus) {
                String uploadFileName = APIImportConstants.UPLOAD_FILE_NAME;
                String absolutePath = currentDirectory + createdFolders;
                APIImportUtil.transferFile(uploadedInputStream, uploadFileName, absolutePath);
                String extractedFolder = APIImportUtil.unzipArchive(new File(absolutePath + uploadFileName), new File(absolutePath));
                APIImportUtil.importAPI(absolutePath + extractedFolder);
                return Response.status(Status.CREATED).build();
            }
            else {
                return Response.status(Status.BAD_REQUEST).build();
            }
        } catch (APIManagementException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}

