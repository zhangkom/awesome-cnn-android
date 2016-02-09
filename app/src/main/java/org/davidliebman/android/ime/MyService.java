package org.davidliebman.android.ime;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.KeyboardView;
import android.os.AsyncTask;
import android.text.InputType;
import android.text.Layout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MyService extends InputMethodService implements CNNEditor {

    Example example;
    boolean mExampleLoadComplete = false;
    boolean mExampleBlockOutput = false;
    boolean mExampleTreatOutput = false;
    boolean mExampleNoCharacterPressed = false;
    boolean mExampleNoBrush = false;
    boolean mExampleInitInService = false;
    Context mContext;
    MyService mMyService;
    View mMyServiceView;

    double[][] screen = new double[28][28];
    boolean write = true;
    int type = Operation.EVAL_SINGLE_ALPHA_UPPER;

    Operation [] operations;
    String mDisplay = "";

    int characterLeft = 0, characterRight = 0, characterTop = 0, characterBottom = 0;

    private Canvas mCanvas;

    private InnerView view;
    private int mViewHeight = 0, mViewWidth = 0;
    int marginTop = 5, marginBottom = 5, marginLeft = 5, marginRight = 5;

    public static final int ONE_SIDE = 28;
    public int RULE_POSITION = 18; //22
    Paint mPaint = new Paint();

    int mWindowHeight, mWindowWidth;
    //InputConnection mConnection;


    @Override
    public void onCreate() {

        super.onCreate();
        try {
            example = new Example(this, this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (!mExampleInitInService) {
            new ExampleInstantiate().execute(0);
        }
        else {
            try {
                example.setNetworks();
            }
            catch (Exception e) {

                e.printStackTrace();
            }
            mExampleLoadComplete = true;
            mDisplay = "output ready: ";
            //TextView mOutput = (TextView) mMyServiceView.findViewById(R.id.textView);
            //mOutput.setText(mDisplay);
        }


        //mConnection = this.getCurrentInputConnection();
    }

    @Override
    public View onCreateInputView() {

        FrameLayout inputView = (FrameLayout) getLayoutInflater().inflate( R.layout.ime_main, null);

        setWindowDimensions();


        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mWindowHeight/2);
        //(FrameLayout.LayoutParams) inputView.getLayoutParams();
        LinearLayout topHalf = (LinearLayout)inputView.findViewById(R.id.topHalf);
        topHalf.setLayoutParams(lp);

        view = new InnerView(this);


        FrameLayout screenLoc = (FrameLayout) inputView.findViewById(R.id.innerView);
        screenLoc.addView(view);


        int mWindowWidthTemp = 0;
        int mWindowHeightTemp = 0;
        final FrameLayout.LayoutParams lp2 = (FrameLayout.LayoutParams) view.getLayoutParams();


        lp2.width = mWindowWidth/2;
        //lp2.gravity = Gravity.CENTER_HORIZONTAL;
        view.setLayoutParams(lp2);

        /*
        mWindowWidthTemp = view.getMeasuredWidth();
        mWindowHeightTemp = view.getMeasuredHeight();
        if (mWindowHeightTemp < mWindowWidthTemp && mWindowHeightTemp > 0) mWindowWidthTemp = mWindowHeightTemp;

        FrameLayout.LayoutParams lp3 = (FrameLayout.LayoutParams) view.getLayoutParams();
        lp3.width = mWindowWidthTemp;
        lp3.height = mWindowHeightTemp;
        view.setLayoutParams(lp3);

        Log.e("ime", "height " + lp3.height);
        */

        mContext = this;
        mMyService = this;
        mMyServiceView = inputView;


        Button mRightAccept = (Button) inputView.findViewById(R.id.rightAccept);
        mRightAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (!mExampleBlockOutput && !mExampleNoCharacterPressed) {
                    new OperationSingle().execute(0);
                    //mOutput.setText(mDisplay);
                }
                else {
                    setOutput(" ");
                }
            }

        });

        final Button mWriteErase = (Button) inputView.findViewById(R.id.writeErase);
        mWriteErase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (write) {
                    write = false;
                    mWriteErase.setText("ERASE");
                    //System.out.println("erase");
                }
                else {
                    write = true;
                    mWriteErase.setText("WRITE");
                    //System.out.println("write");
                }
            }
        });

        final Button mToggle = (Button) inputView.findViewById(R.id.toggle);
        mToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (type) {
                    case Operation.EVAL_SINGLE_ALPHA_LOWER:
                        mToggle.setText("UPPER");
                        type = Operation.EVAL_SINGLE_ALPHA_UPPER;
                        break;
                    case Operation.EVAL_SINGLE_ALPHA_UPPER:
                        mToggle.setText("#NUM#");
                        type = Operation.EVAL_SINGLE_NUMERIC;
                        break;
                    case Operation.EVAL_SINGLE_NUMERIC:
                        mToggle.setText("lower");
                        type = Operation.EVAL_SINGLE_ALPHA_LOWER;
                        break;
                    default:
                        mToggle.setText("lower");
                        type = Operation.EVAL_SINGLE_ALPHA_LOWER;
                        break;
                }

            }
        });

        Button mLeftErase = (Button) inputView.findViewById(R.id.leftErase);
        mLeftErase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearScreen();
                mExampleBlockOutput = false;
            }
        });

        return inputView;
    }





    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        int type = info.inputType & InputType.TYPE_CLASS_TEXT;
        Log.e("ime ", " type " + type);
        if (type == 1) {
            if(mExampleLoadComplete) {
                mDisplay = "";
            }
            //Intent i = new Intent(this, MyService.class);
            //startActivity(i);
        }

        super.onStartInputView(info, restarting);
    }

    @Override
    public void addOperations ( Operation op1, Operation op2, Operation op3) {
        operations = new Operation[] {op1,op2,op3};
    }






    public double [][] getScreen() { return screen ; }

    public void setOutput( String in ) {
        InputConnection mConnection = getCurrentInputConnection();
        mConnection.commitText(in,1);
        mDisplay = mDisplay + in;
        TextView mOutput = (TextView) mMyServiceView.findViewById(R.id.textView);
        mOutput.setText(mDisplay);
        //System.out.println("setOutput " + mDisplay);
    }

    public void clearScreen() {
        for (int i = 0; i < 28; i ++ ) {
            for (int j = 0; j < 28; j ++ ) {
                screen[i][j] = 0.0d;
            }
        }
        view.invalidate();
    }

    public void examineScreen() {
        mExampleNoCharacterPressed = true;
        characterLeft = characterTop = 28;
        characterRight = characterBottom = 0;
        for (int i = 0; i < 28; i ++ ) {
            for (int j = 0; j < 28; j ++ ) {
                if(screen[i][j] >= 0.5d) {
                    mExampleNoCharacterPressed = false;
                    if (i < characterTop) { characterTop = i;}
                    if (j < characterLeft) {characterLeft = j;}
                    if (j > characterRight) {characterRight = j;}
                    if (i > characterBottom) {characterBottom = i;}
                }
            }
        }

    }

    public void resize() {

        double[][] screenOut = new double[ONE_SIDE][ONE_SIDE];

        float mag =  (characterBottom - characterTop)/(float) (RULE_POSITION - 1);
        int mLeftMove =(int) ((characterLeft + ( ONE_SIDE - characterRight)) /2.0f) - characterLeft;

        if(type == Operation.EVAL_SINGLE_ALPHA_UPPER  ) {

            if (mag >= 1.0f) mag = 1.0f;

            for (int i = 0; i < 28; i++) {
                for (int j = 0; j < 28; j++) {

                    int yy = (int) (i * mag + characterTop);
                    int xx = mLeftMove + j;

                    if (yy >= 0 && yy < ONE_SIDE && xx < ONE_SIDE && xx >= 0 && screen[yy][j] >= 0.5d) {
                        screenOut[i][xx] = 1.0d;
                    }
                }
            }
            screen = screenOut;
        }
        if (type == Operation.EVAL_SINGLE_ALPHA_LOWER) {

            int ii = 0;
            mag = 1.0f;

            if ( characterBottom < RULE_POSITION ) {

                ii = (characterTop + ( RULE_POSITION - characterBottom));
            }
            else {
                ii = characterTop;
            }

            for (int i = 0; i < 28; i++) {
                for (int j = 0; j < 28; j++) {

                    int yy = (int) (i * mag + characterTop);
                    int xx = mLeftMove + j;

                    if (yy >= 0 && yy < ONE_SIDE && xx < ONE_SIDE &&
                            xx >= 0 && i + ii < ONE_SIDE && i + ii >= 0 && screen[yy][j] >= 0.5d) {
                        screenOut[i+ii][xx] = 1.0d;
                    }
                }
            }
            screen = screenOut;
        }


    }

    public void setWindowDimensions() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        mWindowHeight = metrics.heightPixels;
        mWindowWidth = metrics.widthPixels;
    }


    class InnerView extends View {



        public InnerView(Context c) {
            super(c);

        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float xx = (mViewWidth - (marginLeft+marginRight)) / (float) ONE_SIDE;
            float yy = (mViewHeight - (marginTop+marginBottom)) /(float) ONE_SIDE;

            if(type == Operation.EVAL_SINGLE_ALPHA_LOWER) {
                mPaint.setColor(Color.BLUE);
                canvas.drawRect(0, yy * RULE_POSITION, mViewWidth, (yy * RULE_POSITION) + 2, mPaint);
            }

            mPaint.setColor(Color.LTGRAY);
            canvas.drawRect(0,0,mViewWidth,mViewHeight,mPaint);

            for (int i = 0; i < 28; i++) {
                for (int j = 0; j < 28; j++) {
                    if (screen[j][i] > 0.5d) {

                        int xpos = (int) (i * xx) + marginLeft;
                        int ypos = (int) (j * yy) + marginTop;

                        mPaint.setColor(Color.BLACK);
                        canvas.drawRect(xpos, ypos, xpos+ (int) (xx - 2), ypos + (int) (yy - 2), mPaint);
                    }
                }
            }


        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mViewWidth = w;
            mViewHeight = h;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            if(mExampleBlockOutput) return true;

            int sizex = mViewWidth - (marginRight + marginLeft);
            int sizey = mViewHeight - (marginTop + marginBottom);

            int xx = (int) event.getX() - marginLeft;
            int yy = (int) event.getY() - marginTop;

            int posx = (int) (xx / (float) sizex * ONE_SIDE);
            int posy = (int) (yy / (float) sizey * ONE_SIDE);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:

                    brushScreen(posx,posy);

                    invalidate();

                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }

            return true;
            //return super.onTouchEvent(event);

        }

        public  void brushScreen(int x, int y) {
            if(mExampleNoBrush) {
                screen[y][x] = 1.0d;
                return;
            }
            for (int i = y - 1; i < y + 1; i ++) {
                for (int j = x - 1; j < x + 1; j ++) {
                    if (i >= 0 && i < 28 && j >= 0 && j < 28) {
                        if (write) {
                            screen[i][j] = 1.0d;
                            //Log.e("color","color");
                        }
                        else {
                            screen[i][j] = 0.0d;
                        }
                    }
                }
            }
        }
    }

    class ExampleInstantiate extends AsyncTask< Integer , Integer , Integer > {

        @Override
        protected Integer doInBackground(Integer... params) {
            try {
                example.setNetworks();
            }
            catch (Exception e) {

                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            mExampleLoadComplete = true;
            mDisplay = "output ready: ";
            TextView mOutput = (TextView) mMyServiceView.findViewById(R.id.textView);
            mOutput.setText(mDisplay);
            super.onPostExecute(integer);
        }
    }

    class OperationSingle extends AsyncTask<Integer, Integer, String> {

        @Override
        protected void onPreExecute() {
            mExampleBlockOutput = true;
            examineScreen();

            if (mExampleTreatOutput) {
                resize();
            }
            super.onPreExecute();
        }



        @Override
        protected String doInBackground(Integer... params) {
            String mOutput = "";
            if(mExampleLoadComplete) {
                if (operations != null && operations.length == 3) {
                    try {

                        switch (type) {
                            case Operation.EVAL_SINGLE_ALPHA_LOWER:

                                operations[0].startOperation(getScreen());
                                mOutput = (operations[0].getOutput());
                                break;
                            case Operation.EVAL_SINGLE_ALPHA_UPPER:

                                operations[1].startOperation(getScreen());
                                mOutput = (operations[1].getOutput());
                                break;
                            case Operation.EVAL_SINGLE_NUMERIC:

                                operations[2].startOperation(getScreen());
                                mOutput = (operations[2].getOutput());
                                break;
                        }
                        //clearScreen();

                    } catch (Exception p) {
                        p.printStackTrace();
                    }
                }
            }


            return mOutput;
        }

        @Override
        protected void onPostExecute(String in) {
            setOutput(in);
            clearScreen();
            mExampleBlockOutput = false;
            super.onPostExecute(in);
        }
    }
}
