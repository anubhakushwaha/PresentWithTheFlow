package com.example.ankushw.leaphackathon;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.ParcelFileDescriptor;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;
import static java.nio.charset.StandardCharsets.UTF_8;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> ids;
    private static final String TAG = "LeapHackathon";

    private static final String STATE_CURRENT_PAGE_INDEX = "current_page_index";

    /**
     * The filename of the PDF.
     */
    private static final String FILENAME = "sample.pdf";

    /**
     * File descriptor of the PDF.
     */
    private ParcelFileDescriptor mFileDescriptor;

    /**
     * {@link android.graphics.pdf.PdfRenderer} to render the PDF.
     */
    private PdfRenderer mPdfRenderer;

    /**
     * Page that is currently shown on the screen.
     */
    private PdfRenderer.Page mCurrentPage;

    /**
     * {@link android.widget.ImageView} that shows a PDF page as a {@link android.graphics.Bitmap}
     */
    private ImageView mImageView;

    /**
     * {@link android.widget.Button} to move to the previous page.
     */
    private Button mButtonPrevious;

    /**
     * {@link android.widget.Button} to move to the next page.
     */
    private Button mButtonNext;

    private Button present;

    /**
     * PDF page index
     */
    private int mPageIndex;

    private RelativeLayout appLayout;
    private TextView connecting;
    private ImageView clientImage;
    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    // Our handle to Nearby Connections
    private ConnectionsClient connectionsClient;
    String option;
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private String opponentEndpointId;
    String page="page"+1;

    public static final String FRAGMENT_PDF_RENDERER_BASIC = "pdf_renderer_basic";

    public void renderImage(String image, ImageView imageView) {
        String uri= "@drawable/"+image;
        int imageResource=getResources().getIdentifier(uri, null, getPackageName());
        imageView.setImageDrawable(getResources().getDrawable(imageResource));
    }
    // Callbacks for receiving payloads
    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    Log.i(TAG, "Recieved*****************");

                    //File transfer
                    if(payload.getType() == 2) {
                        //File newFile = payload.asFile().asJavaFile();

                    }
                    else {
                        String page = new String(payload.asBytes(), UTF_8);
                        showPage(Integer.parseInt(page));
                    }
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                }
            };

    // Callbacks for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    Log.i(TAG, "onEndpointFound: endpoint found, connecting");
                    connectionsClient.requestConnection("master", endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(String endpointId) {}
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageIndex=0;
        ids = new ArrayList<>();
        connectionsClient = Nearby.getConnectionsClient(this);

        try {
            openRenderer(this);
            //showPage(mPageIndex);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error! " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        setContentView(R.layout.activity_main);
        // Retain view references.
        mImageView = (ImageView) findViewById(R.id.image);
        mButtonPrevious = (Button) findViewById(R.id.previous);
        mButtonNext = (Button) findViewById(R.id.next);
        present = findViewById(R.id.present);
        option =getIntent().getStringExtra("selection");
        appLayout = findViewById(R.id.applayout);
        connecting = findViewById(R.id.connecting);
        appLayout.setVisibility(View.GONE);

        if(option.compareTo("yes")==0) {
            Log.d("TAG", "Started Advertising");
            startAdvertising();
        }
        else {
            Log.d("tag", "Started Discovery");
            startDiscovery();
            mButtonPrevious.setEnabled(false);
            mButtonNext.setEnabled(false);
            present.setEnabled(false);
        }
    }

    public void sendPage(String page){
        for(int i=0;i<ids.size();i++) {
            connectionsClient.sendPayload(
                    ids.get(i), Payload.fromBytes((page.getBytes(UTF_8))));
        }
    }

    public void present(View v){
        Log.i(TAG, "SEND*****************");
        sendPage(Integer.toString(mCurrentPage.getIndex()));
    }

    /** Broadcasts our presence using Nearby Connections so other players can find us. */
    private void startAdvertising() {
        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startAdvertising(
                "master", getPackageName(), connectionLifecycleCallback, new AdvertisingOptions(STRATEGY));
    }

    /** Starts looking for other players using Nearby Connections. */
    private void startDiscovery() {
        // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startDiscovery(
                getPackageName(), endpointDiscoveryCallback, new DiscoveryOptions(STRATEGY));
    }

    private File createFileFromInputStream(InputStream inputStream, String my_file_name) {

        try{
            File f = new File(my_file_name);
            OutputStream outputStream = new FileOutputStream(f);
            byte buffer[] = new byte[1024];
            int length = 0;

            while((length=inputStream.read(buffer)) > 0) {
                outputStream.write(buffer,0,length);
            }

            outputStream.close();
            inputStream.close();

            return f;
        }catch (IOException e) {
            //Logging exception
        }

        return null;
    }

    // Callbacks for connections to other devices
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.i(TAG, "onConnectionInitiated: accepting connection");
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                    //opponentName = connectionInfo.getEndpointName();
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        Log.i(TAG, "onConnectionResult: connection successful");
                        appLayout.setVisibility(View.VISIBLE);
                        connecting.setVisibility(View.GONE);
                        if(option.compareTo("yes")==0) {
                                showPage(mPageIndex);

//                            AssetManager am = getAssets();
//                            InputStream inputStream = null;
//                            try {
//                                inputStream = am.open("sample.pdf");
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                            File file = createFileFromInputStream(inputStream, "sample");
//                            try {
//                                connectionsClient.sendPayload(
//                                        opponentEndpointId, Payload.fromFile(file));
//                            } catch (FileNotFoundException e) {
//                                e.printStackTrace();
//                            }
                        }
                        else {
                            mButtonNext.setVisibility(View.GONE);
                            mButtonPrevious.setVisibility(View.GONE);
                            present.setVisibility(View.GONE);
                        }
                        ids.add(endpointId);

                    } else {
                        Log.i(TAG, "onConnectionResult: connection failed");
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.i(TAG, "onDisconnected: disconnected from the opponent");

                }
            };
    /** Disconnects from the opponent and reset the UI. */
    public void stop(View view) {
        connectionsClient.disconnectFromEndpoint(opponentEndpointId);
        //resetGame();
    }
    @Override
    protected void onStop() {
        connectionsClient.stopAllEndpoints();
        //resetGame();

        super.onStop();
    }
    @Override
    protected void onStart() {
        super.onStart();

        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }

    }

    /** Returns true if the app was granted all the permissions. Otherwise, returns false. */
    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /** Handles user acceptance (or denial) of our permission request. */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "missing permission", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        recreate();
    }

    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // Since PdfRenderer cannot handle the compressed asset file directly, we copy it into
            // the cache directory.
            InputStream asset = context.getAssets().open(FILENAME);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        // This is the PdfRenderer we use to render the PDF.
        if (mFileDescriptor != null) {
            //Toast.makeText(this,"%%%%%%%%%%%%%", Toast.LENGTH_LONG).show();
            mPdfRenderer = new PdfRenderer(mFileDescriptor);
        }
    }

    /**
     * Shows the specified page of PDF to the screen.
     *
     * @param index The page index.
     */
    private void showPage(int index) {

        if (mPdfRenderer.getPageCount() <= index) {
            return;
        }
        // Make sure to close the current page before opening another one.
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }

        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(),
                Bitmap.Config.ARGB_8888);
        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        // We are ready to show the Bitmap to user.
        mImageView.setImageBitmap(bitmap);
        // Toast.makeText(this, "*********  "+ mImageView.getMaxHeight(), Toast.LENGTH_LONG).show();
        updateUi();
    }

    /**
     * Updates the state of 2 control buttons in response to the current page index.
     */
    private void updateUi() {
        int index = mCurrentPage.getIndex();
        int pageCount = mPdfRenderer.getPageCount();
        mButtonPrevious.setEnabled(0 != index);
        mButtonNext.setEnabled(index + 1 < pageCount);
        setTitle(getString(R.string.app_name_with_index, index + 1, pageCount));
    }

    public void previous(View v){
        showPage(mCurrentPage.getIndex() - 1);
        sendPage(Integer.toString(mCurrentPage.getIndex()));
    }

    public void next(View v){
        showPage(mCurrentPage.getIndex() + 1);
        sendPage(Integer.toString(mCurrentPage.getIndex()));
    }

}
