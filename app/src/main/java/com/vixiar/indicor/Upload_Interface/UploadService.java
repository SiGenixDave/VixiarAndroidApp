package com.vixiar.indicor.Upload_Interface;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.content.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.vixiar.indicor.Application.NavigatorApplication;
import com.vixiar.indicor.Data.PatientInfo;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by gsevinc on 11/28/2017.
 */

/**
 * Service for managing the Upload Service to Dropbox.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
// This is required to allow us to use the lollipop
public class UploadService extends Service
{
    private final static String TAG = UploadService.class.getSimpleName();
    private DbxClientV2 client;
    private boolean m_Paused = false;

    // the ID used to filter messages from the service to the handler class
    final static String MESSAGE_ID = "vixiarUploadService";
    private static final String ACCESS_TOKEN = "mVQKNQQQJKIAAAAAAAAAt6b3VKaHqzX2sJc7eoay-g0ThGikD5vx6_U-7MfAjs_h";

    private final IBinder m_Binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent)
    {
        return m_Binder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        return super.onUnbind(intent);
    }

    public class LocalBinder extends Binder
    {
        UploadService getService()
        {
            return UploadService.this;
        }

    }

    /*
        Initialize by starting dropbox. Having do a new thread here, since otherwise networking functions cannot start
     */
    public boolean Initialize() throws DbxException
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    startDropbox();
                }
                catch (DbxException | IOException e)
                {
                    Log.e("Dropbox exception: ", e.getMessage());
                }

            }
        }).start();
        return true;
    }

    /*
        Method to start dropbox api. Right now creates a test file and uploads to dropbox
     */
    public void startDropbox() throws DbxException, IOException
    {
        if (isNetworkAvailable())
        {
            Log.i(TAG, "Connecting to dropbox");
            DbxRequestConfig config = DbxRequestConfig.newBuilder("Indicor/1.0").build();
            client = new DbxClientV2(config, ACCESS_TOKEN);
            Log.i(TAG, "Connected to dropbox:" + client.users().getCurrentAccount().getName().getDisplayName());
        }
        else
        {
            // set a service flag to false
        }

        // Run upload check every 10 seconds
        Timer timer = new Timer();
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                try
                {
                    if (!m_Paused)
                    {
                        checkForFilesToUpload();
                    }
                }
                catch (DbxException | IOException e)
                {
                    Log.e(TAG, "Error uploading files");
                }
            }
        }, 0, 10000);
    }

    /*
        Check if there are files to upload, if so, call uploadFilesToDropbox
     */
    public void checkForFilesToUpload() throws DbxException, IOException
    {
        if (isNetworkAvailable())
        {
            String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            File baseDirFile = new File(baseDir);
            List<String> filePaths = getFilesInPath(baseDirFile);
            if (filePaths.size() != 0)
            {
                Log.i(TAG, "Have files to upload.");
                for (String filePath : filePaths)
                {
                    Log.i(TAG, "uploading file " + filePath);

                    // get the patient id for this file
                    String patientID = GetPatientIDFromCSVFile(filePath);

                    if (patientID != null)
                    {
                        uploadFileToDropbox(filePath, getDropboxDirectory(patientID));
                        DeleteFile(filePath);
                    }
                }
            }
            else
            {
                Log.i(TAG, "No files to upload.");
            }
        }
    }

    /*
        Gets the current directory for indicor application within the OS
     */
    public String getDropboxDirectory(String patientID) throws DbxException
    {
        ListFolderResult result = client.files().listFolder("");
        String pathToUpload = "";
        boolean patientFolderExists = false;
        while (true)
        {
            for (Metadata metadata : result.getEntries())
            {
                String pathLower = metadata.getPathLower();
                if (pathLower.startsWith("/vixiar-data"))
                {
                    pathToUpload = pathLower;
                }
                if (patientID != null)
                {
                    if (pathLower.endsWith(patientID))
                    {
                        patientFolderExists = true;
                    }
                }
            }

            if (!result.getHasMore())
            {
                break;
            }

            result = client.files().listFolderContinue(result.getCursor());
        }

        // get the subfolder from the settings
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(NavigatorApplication.getAppContext());
        String subFolder = sp.getString("study_location", "Vixiar_Internal-Testing");
        pathToUpload += "/" + subFolder + "/" + patientID;
        if (!patientFolderExists)
        {
            createFolder(pathToUpload);
        }
        return pathToUpload;
    }

    /*
        Get list of file paths to upload
     */
    public List<String> getFilesInPath(File path)
    {
        ArrayList<String> inFiles = new ArrayList<String>();
        File[] fileNames = path.listFiles();

        if (fileNames != null)
        {
            for (File file : fileNames)
            {
                if (file.getName().toLowerCase().endsWith(".csv"))
                {
                    inFiles.add(file.getAbsolutePath());
                }
            }
        }
        return inFiles;
    }

    private void DeleteFile(String filePath)
    {
        File file = new File(filePath);

        boolean deleted = file.delete();
    }

    /*
        Creates a test file to test with dropbox
     */
    public String createTestFile() throws IOException
    {
        String filename = "tester.csv";
        File file = new File(getApplicationContext().getFilesDir(), filename);
        String string = "Hello world!";
        FileOutputStream outputStream;

        try
        {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return file.getAbsolutePath();
    }

    /*
        Check internet connectivity
     */
    private boolean isNetworkAvailable()
    {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    /*
        Uploads file to Dropbox, given a file path and an upload path.
     */
    public void uploadFileToDropbox(String filePath, String uploadPath) throws DbxException, IOException
    {
        if (isNetworkAvailable())
        {
            Log.i(TAG, "Have internet connection.");
            File file = new File(filePath);
            Log.i(TAG, "Path: " + file.getCanonicalPath());
            try (InputStream in = new FileInputStream(filePath))
            {
                FileMetadata metadata = client.files().uploadBuilder(uploadPath + "/" + file.getName())
                        .uploadAndFinish(in);
                Thread.sleep(10000);
                //TODO add content hash check here with metadata.getContentHash(), if successful then delete file

            }
            catch (DbxException | IOException | InterruptedException e)
            {
                Log.e(TAG, "Error uploading file");
            }
        }
        else
        {
            //set a service flag to false
            Log.i(TAG, "No internet connection.");
        }
    }

    public void Pause()
    {
        m_Paused = true;
    }

    public void Resume()
    {
        m_Paused = false;
    }

    public void createFolder(String folderName) throws DbxException
    {
        try
        {
            FolderMetadata folder = client.files().createFolder(folderName);
        }
        catch (CreateFolderErrorException err)
        {
            if (err.errorValue.isPath() && err.errorValue.getPathValue().isConflict())
            {
                Log.e(TAG, "Something already exists at the path.");
            }
            else
            {
                Log.e(TAG, "Some other CreateFolderErrorException occurred..." + err.toString());
            }
        }
        catch (Exception err)
        {
            Log.e(TAG, "Some other Exception occurred..." + err.toString());
        }
    }

    private static String GetPatientIDFromCSVFile(String file)
    {
        String patID = null;

        FileInputStream inputStream = null;
        Scanner fileScanner = null;
        try
        {
            inputStream = new FileInputStream(file);
            fileScanner = new Scanner(inputStream, "UTF-8");

            while (fileScanner.hasNextLine())
            {
                String line = fileScanner.nextLine();

                // find the line that's the header for the PPG samples
                if (line.contains("Subject ID"))
                {
                    {
                        String[] splitLine = line.split(",");
                        patID = splitLine[1].replaceAll(" ", "").toUpperCase();
                    }
                }
                // note that Scanner suppresses exceptions
                if (fileScanner.ioException() != null)
                {
                    throw fileScanner.ioException();
                }
            }
        }
        catch (IOException e)
        {

        }
        finally
        {
            if (inputStream != null)
            {
                try
                {
                    inputStream.close();
                }
                catch (IOException e)
                {
                }
            }
            if (fileScanner != null)
            {
                fileScanner.close();
            }
        }
        return patID;
    }
}