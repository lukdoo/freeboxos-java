/*
 * Copyright (c) 2013, Mathieu Velten. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.matmaul.freeboxos.client.fs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.methods.HttpGet;
import org.json.simple.JSONObject;
import org.matmaul.freeboxos.client.FreeboxException;
import org.matmaul.freeboxos.client.internal.RestManager;
import org.matmaul.freeboxos.client.internal.Response;
import org.matmaul.freeboxos.client.internal.Response.EmptyResponse;
import org.matmaul.freeboxos.client.internal.Responses;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

public class FsManager {
	protected RestManager restManager;

	public FsManager(RestManager restManager) {
		this.restManager = restManager;
	}
	
	public List<FsTask> getFsTasks() throws FreeboxException {
		return restManager.get("fs/tasks/", Responses.FsTasksResponse.class);
	}

	public FsTask getFsTask(long id) throws FreeboxException {
		return restManager.get("fs/tasks/" + id, Responses.FsTaskResponse.class);
	}

	public FileInfo getFileInfo(String path) throws FreeboxException {
		return restManager.get("fs/info/" + FileInfo.encodePath(path), Responses.FileInfoResponse.class);
	}

	@SuppressWarnings("unchecked")
	public FsTask mv(Collection<FileInfo> files, FileInfo dstFolder, String conflictMode) throws FreeboxException {
		JSONObject req = new JSONObject();
		req.put("files", Collections2.transform(files, new Function<FileInfo, String>() {

			public String apply(FileInfo fi) {
				return fi.getPath();
			}
		}));
		req.put("dst", dstFolder.getPath());
		req.put("mode", conflictMode);

		return restManager.post("fs/mv/", RestManager.createJsonEntity(req), Responses.FsTaskResponse.class);
	}

	@SuppressWarnings("unchecked")
	public FsTask rm(Collection<FileInfo> files) throws FreeboxException {
		JSONObject req = new JSONObject();
		req.put("files", Collections2.transform(files, new Function<FileInfo, String>() {

			public String apply(FileInfo fi) {
				return fi.getPath();
			}
		}));

		return restManager.post("fs/rm/", RestManager.createJsonEntity(req), Responses.FsTaskResponse.class);
	}

	@SuppressWarnings("unchecked")
	public FsTask cp(Collection<FileInfo> files, FileInfo dstFolder, String conflictMode) throws FreeboxException {
		// TODO change to Collection<String> encodedPaths and move to... hum not sure
		JSONObject req = new JSONObject();
		req.put("files", Collections2.transform(files, new Function<FileInfo, String>() {

			public String apply(FileInfo fi) {
				return fi.getPath();
			}
		}));
		req.put("dst", dstFolder.getPath());
		req.put("mode", conflictMode);

		return restManager.post("fs/cp/", RestManager.createJsonEntity(req), Responses.FsTaskResponse.class);
	}

	protected Map<String, FileInfo> lsEncoded(String encodedPath) throws FreeboxException {
		List<FileInfo> fileInfos = restManager.get("fs/ls/" + encodedPath, Responses.FileInfosResponse.class);
		LinkedHashMap<String, FileInfo> map = new LinkedHashMap<String, FileInfo>();
		for (FileInfo fileInfo : fileInfos) {
			map.put(fileInfo.getName(), fileInfo);
		}
		return map;
	}

	public Map<String, FileInfo> ls(FileInfo folder) throws FreeboxException {
		String path = "";
		if (folder != null) {
			path = folder.getPath();
		}
		return lsEncoded(path);
	}

	@SuppressWarnings("unchecked")
	public String rename(FileInfo file, String newName) throws FreeboxException {
		JSONObject req = new JSONObject();
		req.put("src", file.getPath());
		req.put("dst", newName);

		return restManager.post("fs/rename/", RestManager.createJsonEntity(req), Response.StringResponse.class);
	}

	public InputStream download(FileInfo file) throws FreeboxException {
		HttpGet get = new HttpGet(restManager.getBaseAddress() + "dl/" + file.getPath());
		return restManager.execute(get, true);
	}

	@SuppressWarnings("unchecked")
	public void mkdir(FileInfo parent, String dirName) throws FreeboxException {
		JSONObject req = new JSONObject();
		req.put("parent", parent.getPath());
		req.put("dirname", dirName);

		restManager.post("fs/mkdir/", RestManager.createJsonEntity(req), EmptyResponse.class);
	}
	
	@SuppressWarnings("unchecked")
	public long upload(InputStream content, long length, FileInfo folder, String fileName) throws FreeboxException {
		JSONObject req = new JSONObject();
		req.put("dirname", folder.getPath());
		req.put("upload_name", fileName);
		long uploadId = restManager.post("upload/", RestManager.createJsonEntity(req), Responses.IdHolderResponse.class).getId();
		try {
			restManager.post("upload/" + uploadId + "/send", RestManager.createMultipartEntity(content, fileName), EmptyResponse.class);
		} catch (IOException e) {
			throw new FreeboxException("", "Erreur de la lecture de l'input stream");
		}
		return uploadId;
	}

	public FileUpload getFileUpload(long id) throws FreeboxException {
		return restManager.get("upload/" + id, Responses.FileUploadResponse.class);
	}

	public List<FileUpload> listUploads() throws FreeboxException {
		return restManager.get("upload/", Responses.FileUploadsResponse.class);
	}

	public void deleteUpload(long id) throws FreeboxException {
		restManager.delete("upload/" + id, EmptyResponse.class);
	}

	public void cleanTerminatedUploads() throws FreeboxException {
		restManager.delete("upload/clean", EmptyResponse.class);
	}

	public void cancelUpload(long id) throws FreeboxException {
		restManager.delete("upload/" + id + "/cancel", EmptyResponse.class);
	}


}
