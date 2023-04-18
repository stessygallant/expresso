package com.sgitmanagement.expresso.base;

/**
 * 
 */
public interface DocumentUploadNotification {
	public void documentUploaded(Integer resourceId, Integer documentId) throws Exception;
}
