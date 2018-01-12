package com.vixiar.indicor.Activities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.app.Activity;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vixiar.indicor.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PDFViewActivity extends Activity implements View.OnClickListener
{
    private ImageView m_pdfView;
    private ParcelFileDescriptor m_FileDescriptor;
    private PdfRenderer m_PdfRenderer;
    private PdfRenderer.Page m_CurrentPage;
    private ImageView m_ImageView;
    private ImageView m_ButtonPrevious;
    private ImageView m_ButtonNext;
    private TextView m_screenName;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfview);
        m_ImageView = findViewById(R.id.pdfView);
        m_pdfView = findViewById(R.id.pdfView);
        m_ButtonNext = findViewById(R.id.forwardArrow);
        m_ButtonPrevious = findViewById(R.id.backArrow);
        m_screenName = findViewById(R.id.txtScreenName);
        m_ButtonPrevious.setOnClickListener(this);
        m_ButtonNext.setOnClickListener(this);

        ImageView battIcon = findViewById(R.id.batteryIcon);
        battIcon.setVisibility(View.INVISIBLE);

        TextView closeButton = findViewById(R.id.navButton);
        closeButton.setText(getText(R.string.close));
        closeButton.setOnClickListener(this);

        try
        {
            openRenderer(this);
            showPage(0);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Toast.makeText(this,
                    "Something Wrong: " + e.toString(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showPage(int index)
    {
        if (m_PdfRenderer.getPageCount() <= index)
        {
            return;
        }
        // Make sure to close the current page before opening another one.
        if (null != m_CurrentPage)
        {
            m_CurrentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        m_CurrentPage = m_PdfRenderer.openPage(index);

        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(m_CurrentPage.getWidth(), m_CurrentPage.getHeight(), Bitmap.Config.ARGB_8888);

        int height = getResources().getDisplayMetrics().heightPixels;
        Bitmap scaledBitmap = BitmapScaler.scaleToFitHeight(bitmap, height);

        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        m_CurrentPage.render(scaledBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
        // We are ready to show the Bitmap to user.
        m_ImageView.setImageBitmap(scaledBitmap);
        updateUi();
    }

    private void openRenderer(Context context) throws IOException
    {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), "IFU.pdf");
        if (!file.exists())
        {
            // Since PdfRenderer cannot handle the compressed asset file directly, we copy it into
            // the cache directory.
            InputStream asset = context.getAssets().open("IFU.pdf");
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1)
            {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        m_FileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        // This is the PdfRenderer we use to render the PDF.
        if (m_FileDescriptor != null)
        {
            m_PdfRenderer = new PdfRenderer(m_FileDescriptor);
        }
    }


    /**
     * Updates the state of 2 control buttons in response to the current page index.
     */
    private void updateUi()
    {
        int index = m_CurrentPage.getIndex();
        int pageCount = m_PdfRenderer.getPageCount();

        if (index == 0)
        {
            m_ButtonPrevious.setEnabled(false);
            m_ButtonPrevious.setAlpha((float) 0.30);
        }
        else
        {
            m_ButtonPrevious.setEnabled(true);
            m_ButtonPrevious.setAlpha((float) 1.0);
        }

        if (index + 1 >= pageCount)
        {
            m_ButtonNext.setEnabled(false);
            m_ButtonNext.setAlpha((float) 0.30);
        }
        else
        {
            m_ButtonNext.setEnabled(true);
            m_ButtonNext.setAlpha((float) 1.0);
        }
        m_screenName.setText("Instructions for Use - " + (index + 1) + " of " + pageCount);
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.backArrow:
                // Move to the previous page
                showPage(m_CurrentPage.getIndex() - 1);
                break;

            case R.id.forwardArrow:
                // Move to the next page
                showPage(m_CurrentPage.getIndex() + 1);
                break;

            case R.id.navButton:
                onBackPressed();
                break;
        }
    }

    public static class BitmapScaler
    {
        // Scale and maintain aspect ratio given a desired width
        // BitmapScaler.scaleToFitWidth(bitmap, 100);
        public static Bitmap scaleToFitWidth(Bitmap b, int width)
        {
            float factor = width / (float) b.getWidth();
            return Bitmap.createScaledBitmap(b, width, (int) (b.getHeight() * factor), true);
        }


        // Scale and maintain aspect ratio given a desired height
        // BitmapScaler.scaleToFitHeight(bitmap, 100);
        public static Bitmap scaleToFitHeight(Bitmap b, int height)
        {
            float factor = height / (float) b.getHeight();
            return Bitmap.createScaledBitmap(b, (int) (b.getWidth() * factor), height, true);
        }
    }
}

