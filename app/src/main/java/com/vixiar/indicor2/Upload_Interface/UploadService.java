package com.vixiar.indicor2.Upload_Interface;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.vixiar.indicor2.Application.NavigatorApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

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
    private final static boolean USE_PATIENT_SUBFOLDERS = false;
    private DbxClientV2 m_dbxClient;
    private boolean m_Paused = false;
    private boolean m_connected = false;

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
                    StartDropbox();
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
    private void StartDropbox() throws DbxException, IOException
    {
        if (IsNetworkAvailable())
        {
            m_connected = ConnectToDropbox();
        }
        else
        {
            m_connected = false;
        }

        // Run upload check every 10 seconds
        Timer timer = new Timer();
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                if (!m_Paused)
                {
                    if (m_connected)
                    {
                        CheckForFilesToUpload();
                    }
                    else
                    {
                        if (IsNetworkAvailable())
                        {
                            Log.i(TAG, "Network available");
                            m_connected = ConnectToDropbox();
                        }
                        else
                        {
                            Log.i(TAG, "Network not available");
                            m_connected = false;
                        }
                    }
                }
            }
        }, 0, 10000);
    }

    private boolean ConnectToDropbox()
    {
        try
        {
            Log.i(TAG, "Connecting to dropbox");
            DbxRequestConfig config = DbxRequestConfig.newBuilder("Indicor/1.0").build();
            m_dbxClient = new DbxClientV2(config, ACCESS_TOKEN);
            Log.i(TAG, "Connected to dropbox:" + m_dbxClient.users().getCurrentAccount().getName().getDisplayName());
            return (m_dbxClient != null);
        }
        catch (DbxException e)
        {
            Log.e(TAG, "Error connecting");
        }
        return false;
    }

    /*
        Check if there are files to upload, if so, call uploadFilesToDropbox
     */
    private void CheckForFilesToUpload()
    {
        try
        {
            if (IsNetworkAvailable())
            {
                String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                File baseDirFile = new File(baseDir);
                List<String> filePaths = GetFilesInPath(baseDirFile);
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
                            if (UploadFileToDropbox(filePath, GetDropboxDirectory(patientID)) == true)
                            {
                                // delete the file if the copy worked
                                Log.i(TAG, "deleting file - " + filePath);
                                DeleteFile(filePath);
                            }
                        }
                    }
                }
                else
                {
                    Log.i(TAG, "No files to upload.");
                }
            }
        }
        catch (DbxException | IOException e)
        {
            Log.e(TAG, "Dropbox exception");
        }
    }

    /*
        Gets the current directory for indicor application within the OS
     */
    private String GetDropboxDirectory(String patientID) throws DbxException
    {
        // get the subfolder from the settings
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(NavigatorApplication.getAppContext());
        String subFolder = sp.getString("study_location", "Vixiar_Internal-Testing");
        String pathToUpload;

        if (USE_PATIENT_SUBFOLDERS)
        {
            pathToUpload = "/vixiar-data/" + subFolder + "/" + patientID.toUpperCase();
        }
        else
        {
            pathToUpload = "/vixiar-data/" + subFolder;
        }

        Log.i(TAG, "Path to upload = " + pathToUpload);
        CreateFolder(pathToUpload);
        return pathToUpload;
    }

    /*
        Get list of file paths to upload
     */
    private List<String> GetFilesInPath(File path)
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
    private String createTestFile() throws IOException
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
    private boolean IsNetworkAvailable()
    {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        boolean available = (activeNetworkInfo != null && activeNetworkInfo.isConnected());

        if (!available)
        {
            m_connected = false;
        }
        return available;
    }


    /*
        Uploads file to Dropbox, given a file path and an upload path.
     */
    private boolean UploadFileToDropbox(String filePath, String uploadPath) throws DbxException, IOException
    {
        boolean status = false;

        if (IsNetworkAvailable())
        {
            Log.i(TAG, "Have internet connection.");
            File file = new File(filePath);
            Log.i(TAG, "Path: " + file.getCanonicalPath());
            try (InputStream in = new FileInputStream(filePath))
            {
                String fileName = uploadPath + "/" + file.getName();
                FileMetadata metadata = m_dbxClient.files().uploadBuilder(fileName).uploadAndFinish(in);
                Thread.sleep(10000);
                //TODO add content hash check here with metadata.getContentHash(), if successful then delete file
                status = CheckIfFileWasTransferred(fileName);
            }
            catch (DbxException | IOException | InterruptedException e)
            {
                Log.e(TAG, "Error uploading file");
                status = false;
            }
        }
        else
        {
            //set a service flag to false
            Log.i(TAG, "No internet connection.");
            status = false;
        }
        return status;
    }

    public void Pause()
    {
        m_Paused = true;
    }

    public void Resume()
    {
        m_Paused = false;
    }

    private void CreateFolder(String folderName) throws DbxException
    {
        try
        {
            FolderMetadata folder = m_dbxClient.files().createFolder(folderName);
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

    private boolean CheckIfFileWasTransferred(String file)
    {
        try
        {
            m_dbxClient.files().getMetadata(file);
            return true;
        }
        catch (GetMetadataErrorException e)
        {
            if (e.getMessage().contains("{\".tag\":\"path\",\"path\":\"not_found\"}"))
            {
                return false;
            }
        }
        catch (DbxException e)
        {
            System.out.println("DbxException");
        }
        return false;
    }

    private String GetPatientIDFromCSVFile(String file)
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