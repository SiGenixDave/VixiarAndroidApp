package com.vixiar.indicor.Activities;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.vixiar.indicor.BLEInterface.IndicorBLEServiceInterface;
import com.vixiar.indicor.BLEInterface.IndicorBLEServiceInterfaceCallbacks;
import com.vixiar.indicor.BuildConfig;
import com.vixiar.indicor.Data.PatientInfo;
import com.vixiar.indicor.Data.PeakValleyDetect;
import com.vixiar.indicor.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class FactoryTestActivity extends Activity implements IndicorBLEServiceInterfaceCallbacks
{
    private static LineGraphSeries m_PPGSeries;
    private static LineGraphSeries m_PressureSeries;
    private static Double m_PPGLastX = 0.0;
    private static Double m_pressureLastX = 0.0;
    private static int m_nLastDataIndex = 0;
    private String m_fileTestDateTime;

    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";

    private UsbManager usbManager;
    private UsbDevice device;
    private UsbSerialDevice serialPort;
    private UsbDeviceConnection connection;

    private boolean m_BUSBEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_factory_test);
        IndicorBLEServiceInterface.getInstance().initialize(this, this);
        IndicorBLEServiceInterface.getInstance().ConnectToIndicor();

        GraphView PPGgraph = findViewById(R.id.PPGgraph);
        GraphView pressureGraph = findViewById(R.id.pressuregraph);

        m_PPGSeries = new LineGraphSeries<>();
        PPGgraph.addSeries(m_PPGSeries);
        PPGgraph.getViewport().setXAxisBoundsManual(true);
        PPGgraph.getViewport().setMinX(0);
        PPGgraph.getViewport().setMaxX(10);
        m_PPGSeries.setColor(Color.BLUE);
        m_PPGSeries.setThickness(2);
        PPGgraph.getGridLabelRenderer().setVerticalAxisTitle("PPG");
        PPGgraph.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");
        PPGgraph.getGridLabelRenderer().setHorizontalAxisTitleTextSize(30f);
        PPGgraph.getGridLabelRenderer().setVerticalAxisTitleTextSize(30f);
        PPGgraph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        PPGgraph.setTitleTextSize(45f);
        PPGgraph.getViewport().setDrawBorder(true);

        m_PressureSeries = new LineGraphSeries<>();
        pressureGraph.addSeries(m_PressureSeries);
        pressureGraph.getViewport().setXAxisBoundsManual(true);
        pressureGraph.getViewport().setMinX(0);
        pressureGraph.getViewport().setMaxX(10);
        m_PressureSeries.setColor(Color.BLUE);
        m_PressureSeries.setThickness(2);
        pressureGraph.getGridLabelRenderer().setVerticalAxisTitle("Pressure");
        pressureGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");
        pressureGraph.getGridLabelRenderer().setHorizontalAxisTitleTextSize(30f);
        pressureGraph.getGridLabelRenderer().setVerticalAxisTitleTextSize(30f);
        pressureGraph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        pressureGraph.setTitleTextSize(45f);
        pressureGraph.getViewport().setDrawBorder(true);

        SetupUSBSerialPort();
    }

    public void iFullyConnected()
    {
        Log.i(TAG, "Connected");
        HeaderFooterControl.getInstance().ShowBatteryIcon(this, IndicorBLEServiceInterface.getInstance().GetLastReadBatteryLevel());

        TextView tv = findViewById(R.id.textViewMessage);
        if (m_BUSBEnabled)
        {
            tv.setText(tv.getText() + " USB is streaming");
        }
        else
        {
            tv.setText(tv.getText() + " USB is not streaming");
        }
    }

    @Override
    public void onBackPressed()
    {
        // save any data collected
        SaveDataToCSVFile(this );
        super.onBackPressed();
    }

    public void iBatteryLevelRead(int level)
    {
    }

    public void iRestart()
    {
        IndicorBLEServiceInterface.getInstance().ConnectToIndicor();
    }


    public void iDisconnected()
    {
    }

    public void iError(int e)
    {
        onBackPressed();
    }

    public void iRealtimeDataNotification()
    {
        int currentDataIndex = PatientInfo.getInstance().getRealtimeData().GetRawData().size();

        for (int i = m_nLastDataIndex; i < currentDataIndex; i++)
        {
            m_PPGSeries.appendData(new DataPoint(m_PPGLastX, PatientInfo.getInstance().getRealtimeData().GetFilteredData().get(i).m_PPG), true, 500);
            m_PPGLastX += 0.02;

            m_PressureSeries.appendData(new DataPoint(m_pressureLastX, PatientInfo.getInstance().getRealtimeData().GetFilteredData().get(i).m_pressure), true, 500);
            m_pressureLastX += 0.02;

            if (m_BUSBEnabled)
            {
                String outLine = FormatDoubleForPrint(PatientInfo.getInstance().getRealtimeData().GetFilteredData().get(i).m_PPG) +
                        ", " +
                FormatDoubleForPrint(PatientInfo.getInstance().getRealtimeData().GetFilteredData().get(i).m_pressure) +
                        "\r\n";
                serialPort.write(outLine.getBytes());
            }
        }
        m_nLastDataIndex = currentDataIndex;
    }

    public boolean SaveDataToCSVFile(Context context)
    {
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();

        // get the date and time
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.getDefault());
        Date date = new Date();
        m_fileTestDateTime = simpleDateFormat.format(date);

        String fileName = m_fileTestDateTime + "_factory_test" + ".csv";
        String filePath = baseDir + File.separator + fileName;
        File file = new File(filePath);

        try
        {
            FileOutputStream fos = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(fos);
            WriteCSVContents(pw);
            file.setWritable(true);
            pw.flush();
            pw.close();
            fos.close();

            // now we need to force android to rescan the file system so the file will show up
            // if you want to load it via usb
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
            Log.i(TAG, "******* File not found. Did you"
                    + " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return true;
    }

    private boolean WriteCSVContents(PrintWriter writer)
    {
        writer.println("Test date time, " + m_fileTestDateTime);
        writer.println("Application version, " + BuildConfig.VERSION_NAME);
        writer.println("Handheld serial number," + PatientInfo.getInstance().get_handheldSerialNumber());
        writer.println("Firmware version, " + PatientInfo.getInstance().get_firmwareRevision());

        // print all of the raw realtime data
        double t = 0.0;
        writer.println("Time (sec.), PPG (raw), Pressure (mmHg)");
        for (int i = 0; i < PatientInfo.getInstance().getRealtimeData().GetRawData().size(); i++)
        {
            writer.println(FormatDoubleForPrint(t) + ", " + PatientInfo.getInstance().getRealtimeData().GetRawData().get(i).m_PPG + ", " +
                    FormatDoubleForPrint(PatientInfo.getInstance().getRealtimeData().GetRawData().get(i).m_pressure));
            t += 0.02;
        }
        return true;
    }

    private String FormatDoubleForPrint(double value)
    {
        String result = String.format("%1$,.2f", value);

        // get rid of the commas cause it's going to a csv file
        String clean = result.replaceAll(",", "");

        return clean;
    }

    private void SetupUSBSerialPort()
    {
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty())
        {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet())
            {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 1659 || deviceVID == 1027)
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                }
                else
                {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(ACTION_USB_PERMISSION))
            {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted)
                {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null)
                    {
                        if (serialPort.open())
                        { //Set Serial Connection Parameters.
                            m_BUSBEnabled = true;
                            serialPort.setBaudRate(115200);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                        }
                        else
                        {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    }
                    else
                    {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                }
                else
                {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            }
            else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED))
            {
                //onClickStart(startButton);
            }
            else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED))
            {
                //onClickStop(stopButton);

            }
        }

        ;
    };


}
