package com.giniem.gindpubs.workers;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.giniem.gindpubs.client.GindMandator;

import java.io.File;

public class DownloaderTask extends AsyncTask<String, Long, String> {

    //The mandator that should be notified after the task is done
    private GindMandator mandator;
    //The task id
    private int taskId;

    private DownloadManager dm;
    private String downloadUrl;
    private String fileName;
    private String fileTitle;
    private String fileDescription;
    private String relativeDirPath;
    private int visibility;
    Uri downloadedFile;
	
	public DownloaderTask(Context context,
                          GindMandator mandator,
                          final int taskId,
                          final String downloadUrl,
                          final String fileName,
                          final String fileTitle,
                          final String fileDesc,
                          final String relDirPath,
                          final int visibility) {
        this.mandator = mandator;
        this.taskId = taskId;
        this.dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.fileTitle = fileTitle;
        this.fileDescription = fileDesc;
        this.relativeDirPath = relDirPath;
        this.visibility = visibility;
	}


    @Override
	protected String doInBackground(String... params) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setDescription(fileDescription);
        request.setTitle(fileTitle);

        // in order for this if to run, you must use the android 3.2 to compile your app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(visibility);
        }
        request.setDestinationInExternalPublicDir(relativeDirPath, fileName);
        long downloadId = dm.enqueue(request);

        Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);

        boolean downloading = true;
        String result = "";
        while(downloading) {
            Cursor c = this.dm.query(query);
            c.moveToFirst();
            int status  = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));

            switch (status) {
                case DownloadManager.STATUS_PAUSED:
                    //Do nothing
                    break;
                case DownloadManager.STATUS_PENDING:
                    //Do nothing
                    break;
                case DownloadManager.STATUS_RUNNING:
                    long totalBytes = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    long bytesSoFar = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    long progress = (bytesSoFar * 100 / totalBytes);
                    Log.d(this.getClass().getName(), "RUNNING Download of " + this.fileName + " progress: " + progress + "%");
                    publishProgress(progress, bytesSoFar, totalBytes);
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    publishProgress(100L,
                            c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                            c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)));
                    downloading = false;
                    Log.d(this.getClass().getName(), "SUCCESSFULLY Downloaded " + this.fileName );
                    result = "SUCCESS";
                    downloadedFile = dm.getUriForDownloadedFile(downloadId);
                    break;
                case DownloadManager.STATUS_FAILED:
                    Log.e(this.getClass().getName(), "ERROR Downloading " + this.fileName );
                    downloading = false;
                    break;
            }
            c.close();
        }

        return result;
	}
	
	@Override
	protected void onProgressUpdate(Long... progress) {
        mandator.updateProgress(taskId, progress[0], progress[1], progress[2]);
    }
	
	@Override
	protected void onPostExecute(final String result) {
        String filePath = this.relativeDirPath + File.separator + this.fileName;
        mandator.postExecute(taskId, result, downloadedFile.getPath());
    }

}
