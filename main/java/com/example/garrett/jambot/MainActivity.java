package com.example.garrett.jambot;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.opengl.EGLConfig;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {

    Button startRec, stopRec, playBack;

    Boolean recording;


    public static class Triangle {

        private final int mProgram;

        private final String vertexShaderCode =
                "attribute vec4 vPosition;" +
                        "void main() {" +
                        "  gl_Position = vPosition;" +
                        "}";

        private final String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform vec4 vColor;" +
                        "void main() {" +
                        "  gl_FragColor = vColor;" +
                        "}";

        private FloatBuffer vertexBuffer;

        // number of coordinates per vertex in this array
        static final int COORDS_PER_VERTEX = 3;
        static float triangleCoords[] = {   // in counterclockwise order:
                0.0f,  0.622008459f, 0.0f, // top
                -0.5f, -0.311004243f, 0.0f, // bottom left
                0.5f, -0.311004243f, 0.0f  // bottom right
        };

        // Set color with red, green, blue and alpha (opacity) values
        float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };

        public Triangle() {
            // initialize vertex byte buffer for shape coordinates
            ByteBuffer bb = ByteBuffer.allocateDirect(
                    // (number of coordinate values * 4 bytes per float)
                    triangleCoords.length * 4);
            // use the device hardware's native byte order
            bb.order(ByteOrder.nativeOrder());

            // create a floating point buffer from the ByteBuffer
            vertexBuffer = bb.asFloatBuffer();
            // add the coordinates to the FloatBuffer
            vertexBuffer.put(triangleCoords);
            // set the buffer to read the first coordinate
            vertexBuffer.position(0);

            int vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                    vertexShaderCode);
            int fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                    fragmentShaderCode);


            // create empty OpenGL ES Program
            mProgram = GLES20.glCreateProgram();

            // add the vertex shader to program
            GLES20.glAttachShader(mProgram, vertexShader);

            // add the fragment shader to program
            GLES20.glAttachShader(mProgram, fragmentShader);

            // creates OpenGL ES program executables
            GLES20.glLinkProgram(mProgram);
        }

        private int mPositionHandle;
        private int mColorHandle;

        private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
        private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

        public void draw() {
            // Add program to OpenGL ES environment
            GLES20.glUseProgram(mProgram);

            // get handle to vertex shader's vPosition member
            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(mPositionHandle);

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    vertexStride, vertexBuffer);

            // get handle to fragment shader's vColor member
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

            // Set color for drawing the triangle
            GLES20.glUniform4fv(mColorHandle, 1, color, 0);

            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(mPositionHandle);
        }
    }



    public static class MyGLRenderer implements GLSurfaceView.Renderer {

        private Triangle mTriangle;
        // private Square   mSquare;

        @Override
        public void onSurfaceCreated(GL10 unused, javax.microedition.khronos.egl.EGLConfig config) {
            // Set the background frame color
            //GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

            mTriangle = new Triangle();
            // initialize a square
            // mSquare = new Square();
            mTriangle.draw();

        }

        public void onDrawFrame(GL10 unused) {
            // Redraw background color
            //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            mTriangle.draw();
        }

        public void onSurfaceChanged(GL10 unused, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
        }

        public static int loadShader(int type, String shaderCode){

            // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
            // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
            int shader = GLES20.glCreateShader(type);

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);

            return shader;
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startRec = findViewById(R.id.startrec);
        stopRec = findViewById(R.id.stoprec);
        playBack = findViewById(R.id.playback);

        startRec.setOnClickListener(startRecOnClickListener);
        stopRec.setOnClickListener(stopRecOnClickListener);
        playBack.setOnClickListener(playBackOnClickListener);

        stopRec.setEnabled(false);

        // Check for record, write, read permissions
        int MY_PERMISSIONS_REQUEST = 1;
        if(ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED);
        {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST);
        }

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        GLSurfaceView mGLView = findViewById(R.id.surfaceView);

    }

    OnClickListener startRecOnClickListener
            = new OnClickListener() {

        @Override
        public void onClick(View arg0) {

            Thread recordThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    recording = true;
                    startRecord();
                }

            });

            recordThread.start();
            startRec.setEnabled(false);
            stopRec.setEnabled(true);

        }
    };

    OnClickListener stopRecOnClickListener
            = new OnClickListener(){

        @Override
        public void onClick(View arg0) {
            recording = false;
            startRec.setEnabled(true);
            stopRec.setEnabled(false);
        }
    };

    OnClickListener playBackOnClickListener
            = new OnClickListener(){

        @Override
        public void onClick(View v) {
            playRecord();
        }

    };

    private void startRecord() {

        File file = new File(Environment.getExternalStorageDirectory(), "test.pcm");

        int sampleFreq = 44100;

        final String promptStartRecord =
                "startRecord()\n"
                        + file.getAbsolutePath() + "\n"
                        + String.valueOf(sampleFreq);

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(MainActivity.this,
                        promptStartRecord,
                        Toast.LENGTH_LONG).show();
            }
        });

        try {
            file.createNewFile();

            OutputStream outputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

            int minBufferSize = AudioRecord.getMinBufferSize(sampleFreq,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            short[] audioData = new short[minBufferSize];

            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    sampleFreq,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize);

            audioRecord.startRecording();

            while (recording) {
                int numberOfShort = audioRecord.read(audioData, 0, minBufferSize);
                for (int i = 0; i < numberOfShort; i++) {
                    dataOutputStream.writeShort(audioData[i]);
                }
            }

            audioRecord.stop();
            dataOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    void playRecord(){

        File file = new File(Environment.getExternalStorageDirectory(), "test.pcm");

        int shortSizeInBytes = Short.SIZE/Byte.SIZE;

        int bufferSizeInBytes = (int)(file.length()/shortSizeInBytes);
        short[] audioData = new short[bufferSizeInBytes];

        try {
            InputStream inputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            int i = 0;
            while(dataInputStream.available() > 0){
                audioData[i] = dataInputStream.readShort();
                i++;
            }

            dataInputStream.close();

            int sampleFreq = 44100;

            final String promptPlayRecord =
                    "PlayRecord()\n"
                            + file.getAbsolutePath() + "\n"
                            + String.valueOf(sampleFreq);

            Toast.makeText(MainActivity.this,
                    promptPlayRecord,
                    Toast.LENGTH_LONG).show();

            AudioTrack audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleFreq,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeInBytes,
                    AudioTrack.MODE_STREAM);

            audioTrack.play();
            audioTrack.write(audioData, 0, bufferSizeInBytes);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

class MyGLSurfaceView extends GLSurfaceView {

    private final MainActivity.MyGLRenderer mRenderer;

    public MyGLSurfaceView(Context context, AttributeSet attrs){
        super(context, attrs);

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        mRenderer = new MainActivity.MyGLRenderer();

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
    }
}
